package com.tpe.parking.dto.response;

import java.math.BigDecimal;

public class TarificationResponseDTO {

    private Long entreeSortieId;
    private String placeNumero;
    private String vehiculeImmatriculation;
    private long dureeMinutes;
    private String dureeFormatee;
    private BigDecimal tarifHoraire;
    private BigDecimal montantTotal;
    private boolean aAbonnement;
    private String detailsCalcul;

    public TarificationResponseDTO() {
    }

    public Long getEntreeSortieId() {
        return entreeSortieId;
    }

    public void setEntreeSortieId(Long entreeSortieId) {
        this.entreeSortieId = entreeSortieId;
    }

    public String getPlaceNumero() {
        return placeNumero;
    }

    public void setPlaceNumero(String placeNumero) {
        this.placeNumero = placeNumero;
    }

    public String getVehiculeImmatriculation() {
        return vehiculeImmatriculation;
    }

    public void setVehiculeImmatriculation(String vehiculeImmatriculation) {
        this.vehiculeImmatriculation = vehiculeImmatriculation;
    }

    public long getDureeMinutes() {
        return dureeMinutes;
    }

    public void setDureeMinutes(long dureeMinutes) {
        this.dureeMinutes = dureeMinutes;
    }

    public String getDureeFormatee() {
        return dureeFormatee;
    }

    public void setDureeFormatee(String dureeFormatee) {
        this.dureeFormatee = dureeFormatee;
    }

    public BigDecimal getTarifHoraire() {
        return tarifHoraire;
    }

    public void setTarifHoraire(BigDecimal tarifHoraire) {
        this.tarifHoraire = tarifHoraire;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public boolean isAAbonnement() {
        return aAbonnement;
    }

    public void setAAbonnement(boolean aAbonnement) {
        this.aAbonnement = aAbonnement;
    }

    public String getDetailsCalcul() {
        return detailsCalcul;
    }

    public void setDetailsCalcul(String detailsCalcul) {
        this.detailsCalcul = detailsCalcul;
    }
}
