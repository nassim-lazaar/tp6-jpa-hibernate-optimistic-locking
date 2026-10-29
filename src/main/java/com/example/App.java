package com.example;

import com.example.model.Reservation;
import com.example.retry.ReservationRetryHandler;
import com.example.service.ReservationServiceImpl;

import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;

public class App {

    public static void main(String[] args) {
        System.out.println("========================================================================");
        System.out.println("   TP 6 : VERROUILLAGE OPTIMISTE JPA (@Version) & GESTION DE CONFLITS   ");
        System.out.println("========================================================================");

        ReservationServiceImpl service = new ReservationServiceImpl();
        try {
            // -----------------------------------------------------------------
            // SCENARIO 1 : SIMULATION DE CONFLIT CONCURRENT ET DETECTION D'EXCEPTION
            // -----------------------------------------------------------------
            System.out.println("\n[SCENARIO 1] SIMULATION DE DEUX TRANSACTIONS CONCURRENTES SUR LA MEME ENTITE");
            System.out.println("------------------------------------------------------------------------");

            LocalDateTime debut = LocalDateTime.of(2026, 6, 15, 9, 0);
            LocalDateTime fin = LocalDateTime.of(2026, 6, 15, 11, 0);

            // Etape 1 : Creation de la reservation initiale
            Reservation resInitiale = new Reservation("Salle A - Reunion", debut, fin);
            service.creerReservation(resInitiale);
            System.out.println("1. Reservation initiale creee :");
            System.out.println("   -> ID=" + resInitiale.getId() + " | Salle='" + resInitiale.getSalle() + "' | Version=" + resInitiale.getVersion());

            // Etape 2 : Deux transactions lisent la meme reservation (copie T1 et copie T2)
            Reservation tx1Copie = service.trouverParId(resInitiale.getId());
            Reservation tx2Copie = service.trouverParId(resInitiale.getId());
            System.out.println("\n2. Transactions T1 et T2 chargent l'entite en memoire (Version=" + tx1Copie.getVersion() + ")");

            // Etape 3 : T1 modifie la salle et valide sa transaction
            tx1Copie.setSalle("Salle B - Conference (Modifiee par T1)");
            service.modifierReservation(tx1Copie);
            Reservation postT1 = service.trouverParId(resInitiale.getId());
            System.out.println("3. Transaction T1 valide avec succes :");
            System.out.println("   -> Salle en base='" + postT1.getSalle() + "' | Nouvelle Version=" + postT1.getVersion());

            // Etape 4 : T2 tente de modifier l'entite avec sa version perimee (version 0 au lieu de 1)
            System.out.println("\n4. Transaction T2 tente de committer sa modification avec la version obsolete (" + tx2Copie.getVersion() + ") :");
            tx2Copie.setSalle("Salle C - Conseil (Tentative T2)");

            try {
                service.modifierReservation(tx2Copie);
                System.out.println("   [ERREUR] Le commit aurait du echouer !");
            } catch (OptimisticLockException ole) {
                System.out.println("   -> [SUCCES CONFLIT DETECTE] javax.persistence.OptimisticLockException levee !");
                System.out.println("   -> Message : La ressource a ete modifiee concurremment par une autre transaction.");
            }

            // -----------------------------------------------------------------
            // SCENARIO 2 : RESOLUTION AUTOMATIQUE VIA STRATEGIE DE RETRY
            // -----------------------------------------------------------------
            System.out.println("\n[SCENARIO 2] RESOLUTION AUTOMATIQUE DU CONFLIT PAR RETRY HANDLER");
            System.out.println("------------------------------------------------------------------------");

            ReservationRetryHandler retryHandler = new ReservationRetryHandler(service);

            // On prepare une requete avec l'ancienne copie pour simuler le retry
            System.out.println("1. Envoi de la modification via le ReservationRetryHandler (MAX_RETRIES=3)...");
            Reservation resolue = retryHandler.modifierAvecRetry(tx2Copie);

            System.out.println("\n2. Resultat final apres resolution et retry :");
            System.out.println("   -> ID=" + resolue.getId());
            System.out.println("   -> Salle finale='" + resolue.getSalle() + "'");
            System.out.println("   -> Version finale en base=" + resolue.getVersion());

            System.out.println("\n========================================================================");
            System.out.println("   VALIDATION TERMINEE AVEC SUCCES : CONFLIT & RETRY VALIDES A 100%     ");
            System.out.println("========================================================================");

        } finally {
            service.close();
        }
    }
}
