package com.tpe.parking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class VehiculeRequestDTO {

    @NotBlank(message = "L'immatriculation est obligatoire")
    @Size(max = 20, message = "L'immatriculation ne doit pas dépasser 20 caractères")
    private String immatriculation;

    @NotBlank(message = "La marque est obligatoire")
    @Size(max = 50, message = "La marque ne doit pas dépasser 50 caractères")
    private String marque;

    @Size(max = 50, message = "Le modèle ne doit pas dépasser 50 caractères")
    private String modele;

    @Size(max = 30, message = "La couleur ne doit pas dépasser 30 caractères")
    private String couleur;

    @Size(max = 30, message = "Le type ne doit pas dépasser 30 caractères")
    private String typeVehicule;

    @NotNull(message = "L'utilisateur est obligatoire")
    @Positive(message = "L'identifiant utilisateur doit être positif")
    private Long utilisateurId;

    public VehiculeRequestDTO() {
    }

    public VehiculeRequestDTO(String immatriculation, String marque, String modele, Long utilisateurId) {
        this.immatriculation = immatriculation;
        this.marque = marque;
        this.modele = modele;
        this.utilisateurId = utilisateurId;
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

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }
}
