package com.tpe.parking.entity;

import com.tpe.parking.enumeration.StatutReservation;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_heure_arrivee_prevue", nullable = false)
    private LocalDateTime dateHeureArriveePrevue;

    @Column(name = "date_heure_depart_prevue", nullable = false)
    private LocalDateTime dateHeureDepartPrevue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutReservation statut;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id")
    private Place place;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.statut == null) {
            this.statut = StatutReservation.EN_ATTENTE;
        }
    }

    public Reservation() {
    }

    public Reservation(LocalDateTime dateHeureArriveePrevue, LocalDateTime dateHeureDepartPrevue,
                       Utilisateur utilisateur, Vehicule vehicule) {
        this.dateHeureArriveePrevue = dateHeureArriveePrevue;
        this.dateHeureDepartPrevue = dateHeureDepartPrevue;
        this.utilisateur = utilisateur;
        this.vehicule = vehicule;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateHeureArriveePrevue() {
        return dateHeureArriveePrevue;
    }

    public void setDateHeureArriveePrevue(LocalDateTime dateHeureArriveePrevue) {
        this.dateHeureArriveePrevue = dateHeureArriveePrevue;
    }

    public LocalDateTime getDateHeureDepartPrevue() {
        return dateHeureDepartPrevue;
    }

    public void setDateHeureDepartPrevue(LocalDateTime dateHeureDepartPrevue) {
        this.dateHeureDepartPrevue = dateHeureDepartPrevue;
    }

    public StatutReservation getStatut() {
        return statut;
    }

    public void setStatut(StatutReservation statut) {
        this.statut = statut;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }
}
