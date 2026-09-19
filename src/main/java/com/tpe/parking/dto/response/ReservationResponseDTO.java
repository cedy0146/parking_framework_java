package com.tpe.parking.dto.response;

import com.tpe.parking.enumeration.StatutReservation;

import java.time.LocalDateTime;

public class ReservationResponseDTO {

    private Long id;
    private LocalDateTime dateHeureArriveePrevue;
    private LocalDateTime dateHeureDepartPrevue;
    private StatutReservation statut;
    private LocalDateTime dateCreation;
    private Long utilisateurId;
    private String utilisateurNom;
    private Long vehiculeId;
    private String vehiculeImmatriculation;
    private Long placeId;
    private String placeNumero;

    public ReservationResponseDTO() {
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
}
