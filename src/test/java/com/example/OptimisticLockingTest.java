package com.example;

import com.example.model.Reservation;
import com.example.retry.ReservationRetryHandler;
import com.example.service.ReservationServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.OptimisticLockException;
import java.time.LocalDateTime;

import static org.junit.Assert.*;

public class OptimisticLockingTest {

    private ReservationServiceImpl service;

    @Before
    public void setUp() {
        service = new ReservationServiceImpl();
    }

    @After
    public void tearDown() {
        if (service != null) {
            service.close();
        }
    }

    @Test
    public void testVersionIncrementOnUpdate() {
        LocalDateTime now = LocalDateTime.now();
        Reservation res = new Reservation("Salle 1", now, now.plusHours(1));
        service.creerReservation(res);

        assertEquals(0, res.getVersion());

        res.setSalle("Salle 1 - Modifiee");
        Reservation updated = service.modifierReservation(res);

        assertEquals(1, updated.getVersion());
        assertEquals("Salle 1 - Modifiee", updated.getSalle());
    }

    @Test(expected = OptimisticLockException.class)
    public void testConcurrentConflictThrowsOptimisticLockException() {
        LocalDateTime now = LocalDateTime.now();
        Reservation res = new Reservation("Salle Conflit", now, now.plusHours(2));
        service.creerReservation(res);

        Reservation tx1 = service.trouverParId(res.getId());
        Reservation tx2 = service.trouverParId(res.getId());

        // TX 1 updates first -> version in DB becomes 1
        tx1.setSalle("Salle Conflit - TX1");
        service.modifierReservation(tx1);

        // TX 2 tries to update with stale version 0 -> throws OptimisticLockException
        tx2.setSalle("Salle Conflit - TX2");
        service.modifierReservation(tx2);
    }

    @Test
    public void testRetryHandlerSuccess() {
        LocalDateTime now = LocalDateTime.now();
        Reservation res = new Reservation("Salle Retry", now, now.plusHours(2));
        service.creerReservation(res);

        Reservation tx1 = service.trouverParId(res.getId());
        Reservation tx2 = service.trouverParId(res.getId());

        tx1.setSalle("Salle Retry - TX1");
        service.modifierReservation(tx1);

        ReservationRetryHandler handler = new ReservationRetryHandler(service);
        tx2.setSalle("Salle Retry - TX2 Finale");
        Reservation resolue = handler.modifierAvecRetry(tx2);

        assertNotNull(resolue);
        assertEquals("Salle Retry - TX2 Finale", resolue.getSalle());
        assertEquals(2, resolue.getVersion());
    }
}
