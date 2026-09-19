package com.tpe.parking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "entrees_sorties")
public class EntreeSortie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_heure_entree", nullable = false)
    private LocalDateTime dateHeureEntree;

    @Column(name = "date_heure_sortie")
    private LocalDateTime dateHeureSortie;

    @Column(name = "montant_paye", precision = 10, scale = 2)
    private BigDecimal montantPaye;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicule_id", nullable = false)
    private Vehicule vehicule;

    public EntreeSortie() {
        this.dateHeureEntree = LocalDateTime.now();
    }

    public EntreeSortie(Place place, Vehicule vehicule) {
        this.place = place;
        this.vehicule = vehicule;
        this.dateHeureEntree = LocalDateTime.now();
    }

    public boolean estEnCours() {
        return this.dateHeureSortie == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDateHeureEntree() {
        return dateHeureEntree;
    }

    public void setDateHeureEntree(LocalDateTime dateHeureEntree) {
        this.dateHeureEntree = dateHeureEntree;
    }

    public LocalDateTime getDateHeureSortie() {
        return dateHeureSortie;
    }

    public void setDateHeureSortie(LocalDateTime dateHeureSortie) {
        this.dateHeureSortie = dateHeureSortie;
    }

    public BigDecimal getMontantPaye() {
        return montantPaye;
    }

    public void setMontantPaye(BigDecimal montantPaye) {
        this.montantPaye = montantPaye;
    }

    public Place getPlace() {
        return place;
    }

    public void setPlace(Place place) {
        this.place = place;
    }

    public Vehicule getVehicule() {
        return vehicule;
    }

    public void setVehicule(Vehicule vehicule) {
        this.vehicule = vehicule;
    }
}
