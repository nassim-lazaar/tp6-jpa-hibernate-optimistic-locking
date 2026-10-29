package com.example.retry;

import com.example.model.Reservation;
import com.example.service.ReservationService;

import javax.persistence.OptimisticLockException;

public class ReservationRetryHandler {

    private static final int MAX_RETRIES = 3;
    private final ReservationService service;

    public ReservationRetryHandler(ReservationService reservationService) {
        this.service = reservationService;
    }

    public Reservation modifierAvecRetry(Reservation reservation) {
        int tentative = 0;
        while (tentative < MAX_RETRIES) {
            try {
                return service.modifierReservation(reservation);
            } catch (OptimisticLockException e) {
                tentative++;
                System.out.println("  [RETRY-HANDLER] Conflit de version detecte ! Tentative de rejeu " + tentative + "/" + MAX_RETRIES);

                Reservation fraiche = service.trouverParId(reservation.getId());
                if (fraiche == null) {
                    throw new IllegalStateException("Reservation introuvable lors du rechargement.");
                }

                // Reapplication des modifications demandees sur la version a jour
                fraiche.setSalle(reservation.getSalle());
                fraiche.setDateDebut(reservation.getDateDebut());
                fraiche.setDateFin(reservation.getDateFin());

                reservation = fraiche;
            }
        }
        throw new OptimisticLockException("Echec apres " + MAX_RETRIES + " tentatives de retry.");
    }
}
