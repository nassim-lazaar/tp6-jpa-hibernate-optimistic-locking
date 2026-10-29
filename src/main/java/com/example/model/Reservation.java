package com.example.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_salle", nullable = false)
    private String salle;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin", nullable = false)
    private LocalDateTime dateFin;

    @Version
    @Column(name = "version_opt")
    private int version;

    public Reservation() {
    }

    public Reservation(String designationSalle, LocalDateTime debutCreneau, LocalDateTime finCreneau) {
        this.salle = designationSalle;
        this.dateDebut = debutCreneau;
        this.dateFin = finCreneau;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long idReservation) {
        this.id = idReservation;
    }

    public String getSalle() {
        return salle;
    }

    public void setSalle(String designationSalle) {
        this.salle = designationSalle;
    }

    public LocalDateTime getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDateTime debutCreneau) {
        this.dateDebut = debutCreneau;
    }

    public LocalDateTime getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDateTime finCreneau) {
        this.dateFin = finCreneau;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Reservation{" +
                "id=" + id +
                ", salle='" + salle + '\'' +
                ", dateDebut=" + dateDebut +
                ", dateFin=" + dateFin +
                ", version=" + version +
                '}';
    }
}
