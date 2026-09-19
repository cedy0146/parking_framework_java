package com.tpe.parking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EntreeSortieResponseDTO {

    private Long id;
    private LocalDateTime dateHeureEntree;
    private LocalDateTime dateHeureSortie;
    private BigDecimal montantPaye;
    private boolean enCours;
    private Long placeId;
    private String placeNumero;
    private Long vehiculeId;
    private String vehiculeImmatriculation;
    private Long utilisateurId;
    private String utilisateurNom;
    private Long dureeMinutes;
    private String dureeFormatee;

    public EntreeSortieResponseDTO() {
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

    public boolean isEnCours() {
        return enCours;
    }

    public void setEnCours(boolean enCours) {
        this.enCours = enCours;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }

    public String getPlaceNumero() {
        return placeNumero;
    }

    public void setPlaceNumero(String placeNumero) {
        this.placeNumero = placeNumero;
    }

    public Long getVehiculeId() {
        return vehiculeId;
    }

    public void setVehiculeId(Long vehiculeId) {
        this.vehiculeId = vehiculeId;
    }

    public String getVehiculeImmatriculation() {
        return vehiculeImmatriculation;
    }

    public void setVehiculeImmatriculation(String vehiculeImmatriculation) {
        this.vehiculeImmatriculation = vehiculeImmatriculation;
    }

    public Long getUtilisateurId() {
        return utilisateurId;
    }

    public void setUtilisateurId(Long utilisateurId) {
        this.utilisateurId = utilisateurId;
    }

    public String getUtilisateurNom() {
        return utilisateurNom;
    }

    public void setUtilisateurNom(String utilisateurNom) {
        this.utilisateurNom = utilisateurNom;
    }

    public Long getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(Long dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public String getDureeFormatee() {
        return dureeFormatee;
    }

    public void setDureeFormatee(String dureeFormatee) {
        this.dureeFormatee = dureeFormatee;
    }
}
