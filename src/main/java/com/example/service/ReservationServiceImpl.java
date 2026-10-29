package com.example.service;

import com.example.model.Reservation;

import javax.persistence.*;
import java.util.List;

public class ReservationServiceImpl implements ReservationService {

    private final EntityManagerFactory emf;

    public ReservationServiceImpl() {
        this.emf = Persistence.createEntityManagerFactory("reservationPU");
    }

    public ReservationServiceImpl(EntityManagerFactory factory) {
        this.emf = factory;
    }

    @Override
    public Reservation creerReservation(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(reservation);
            tx.commit();
            return reservation;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erreur de creation", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Reservation modifierReservation(Reservation reservation) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Reservation merged = em.merge(reservation);
            tx.commit();
            return merged;
        } catch (OptimisticLockException ole) {
            if (tx.isActive()) tx.rollback();
            throw ole;
        } catch (RollbackException re) {
            if (tx.isActive()) tx.rollback();
            Throwable cause = re.getCause();
            while (cause != null) {
                if (cause instanceof OptimisticLockException) {
                    throw (OptimisticLockException) cause;
                }
                if (cause instanceof org.hibernate.StaleObjectStateException) {
                    throw new OptimisticLockException(cause.getMessage(), cause);
                }
                cause = cause.getCause();
            }
            throw re;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            throw new RuntimeException("Erreur de mise a jour", e);
        } finally {
            em.close();
        }
    }

    @Override
    public Reservation trouverParId(Long id) {
        EntityManager em = emf.createEntityManager();
        try {
            return em.find(Reservation.class, id);
        } finally {
            em.close();
        }
    }

    @Override
    public List<Reservation> listerReservations() {
        EntityManager em = emf.createEntityManager();
        try {
            return em.createQuery("SELECT r FROM Reservation r ORDER BY r.id", Reservation.class).getResultList();
        } finally {
            em.close();
        }
    }

    public void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
    }
}
