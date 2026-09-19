package com.tpe.parking.dto.request;

import com.tpe.parking.enumeration.TypeAbonnement;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public class AbonnementRequestDTO {

    @NotNull(message = "Le type d'abonnement est obligatoire")
    private TypeAbonnement type;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    @Positive(message = "Le prix doit être positif")
    private BigDecimal prix;

    @NotNull(message = "L'utilisateur est obligatoire")
    @Positive(message = "L'identifiant utilisateur doit être positif")
    private Long utilisateurId;

    @NotNull(message = "Le véhicule est obligatoire")
    @Positive(message = "L'identifiant véhicule doit être positif")
    private Long vehiculeId;

    public AbonnementRequestDTO() {
    }

    public AbonnementRequestDTO(TypeAbonnement type, LocalDate dateDebut, LocalDate dateFin,
                                 BigDecimal prix, Long utilisateurId, Long vehiculeId) {
        this.type = type;
        this.dateDebut = dateDebut;
        this.dateFin = dateFin;
        this.prix = prix;
        this.utilisateurId = utilisateurId;
        this.vehiculeId = vehiculeId;
    }

    public TypeAbonnement getType() {
        return type;
    }

    public void setType(TypeAbonnement type) {
        this.type = type;
    }

    public LocalDate getDateDebut() {
        return dateDebut;
    }

    public void setDateDebut(LocalDate dateDebut) {
        this.dateDebut = dateDebut;
    }

    public LocalDate getDateFin() {
        return dateFin;
    }

    public void setDateFin(LocalDate dateFin) {
        this.dateFin = dateFin;
    }

    public BigDecimal getPrix() {
        return prix;
    }

    public void setPrix(BigDecimal prix) {
        this.prix = prix;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public Long getVehiculeId() {
        return vehiculeId;
    }

    public void setVehiculeId(Long vehiculeId) {
        this.vehiculeId = vehiculeId;
    }
}
