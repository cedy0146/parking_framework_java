package com.tpe.parking.entity;

import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Le numéro est obligatoire")
    @Size(max = 10)
    @Column(nullable = false, unique = true, length = 10)
    private String numero;

    @NotBlank(message = "La zone est obligatoire")
    @Size(max = 10)
    @Column(nullable = false, length = 10)
    private String zone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypePlace type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutPlace statut;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EntreeSortie> entreesSorties = new ArrayList<>();

    public Place() {
        this.statut = StatutPlace.LIBRE;
    }

    public Place(String numero, String zone, TypePlace type) {
        this.numero = numero;
        this.zone = zone;
        this.type = type;
        this.statut = StatutPlace.LIBRE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public TypePlace getType() {
        return type;
    }

    public void setType(TypePlace type) {
        this.type = type;
    }

    public StatutPlace getStatut() {
        return statut;
    }

    public void setStatut(StatutPlace statut) {
        this.statut = statut;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public List<EntreeSortie> getEntreesSorties() {
        return entreesSorties;
    }

    public void setEntreesSorties(List<EntreeSortie> entreesSorties) {
        this.entreesSorties = entreesSorties;
    }
}
