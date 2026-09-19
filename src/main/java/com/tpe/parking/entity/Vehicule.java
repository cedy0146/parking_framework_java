package com.tpe.parking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicules")
public class Vehicule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "L'immatriculation est obligatoire")
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String immatriculation;

    @NotBlank(message = "La marque est obligatoire")
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String marque;

    @Size(max = 50)
    @Column(length = 50)
    private String modele;

    @Size(max = 30)
    @Column(length = 30)
    private String couleur;

    @Size(max = 30)
    @Column(length = 30)
    private String typeVehicule;

    @Column(name = "date_enregistrement", nullable = false, updatable = false)
    private LocalDateTime dateEnregistrement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Abonnement> abonnements = new ArrayList<>();

    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EntreeSortie> entreesSorties = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.dateEnregistrement = LocalDateTime.now();
    }

    public Vehicule() {
    }

    public Vehicule(String immatriculation, String marque, String modele, Utilisateur utilisateur) {
        this.immatriculation = immatriculation;
        this.marque = marque;
        this.modele = modele;
        this.utilisateur = utilisateur;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImmatriculation() {
        return immatriculation;
    }

    public void setImmatriculation(String immatriculation) {
        this.immatriculation = immatriculation;
    }

    public String getMarque() {
        return marque;
    }

    public void setMarque(String marque) {
        this.marque = marque;
    }

    public String getModele() {
        return modele;
    }

    public void setModele(String modele) {
        this.modele = modele;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    public String getTypeVehicule() {
        return typeVehicule;
    }

    public void setTypeVehicule(String typeVehicule) {
        this.typeVehicule = typeVehicule;
    }

    public LocalDateTime getDateEnregistrement() {
        return dateEnregistrement;
    }

    public void setDateEnregistrement(LocalDateTime dateEnregistrement) {
        this.dateEnregistrement = dateEnregistrement;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public List<Abonnement> getAbonnements() {
        return abonnements;
    }

    public void setAbonnements(List<Abonnement> abonnements) {
        this.abonnements = abonnements;
    }

    public List<EntreeSortie> getEntreesSorties() {
        return entreesSorties;
    }

    public void setEntreesSorties(List<EntreeSortie> entreesSorties) {
        this.entreesSorties = entreesSorties;
    }
}
