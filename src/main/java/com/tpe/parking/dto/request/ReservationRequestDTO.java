package com.tpe.parking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class ReservationRequestDTO {

    @NotNull(message = "La date/heure d'arrivée prévue est obligatoire")
    private LocalDateTime dateHeureArriveePrevue;

    @NotNull(message = "La date/heure de départ prévue est obligatoire")
    private LocalDateTime dateHeureDepartPrevue;

    @NotNull(message = "L'utilisateur est obligatoire")
    @Positive(message = "L'identifiant utilisateur doit être positif")
    private Long utilisateurId;

    @NotNull(message = "Le véhicule est obligatoire")
    @Positive(message = "L'identifiant véhicule doit être positif")
    private Long vehiculeId;

    @Positive(message = "L'identifiant place doit être positif")
    private Long placeId;

    public ReservationRequestDTO() {
    }

    public ReservationRequestDTO(LocalDateTime dateHeureArriveePrevue, LocalDateTime dateHeureDepartPrevue,
                                  Long utilisateurId, Long vehiculeId) {
        this.dateHeureArriveePrevue = dateHeureArriveePrevue;
        this.dateHeureDepartPrevue = dateHeureDepartPrevue;
        this.utilisateurId = utilisateurId;
        this.vehiculeId = vehiculeId;
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

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }
}
