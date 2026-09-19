package com.tpe.parking.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public class EntreeSortieRequestDTO {

    @NotNull(message = "La date/heure d'entrée est obligatoire")
    private LocalDateTime dateHeureEntree;

    @NotNull(message = "Le véhicule est obligatoire")
    @Positive(message = "L'identifiant véhicule doit être positif")
    private Long vehiculeId;

    @Positive(message = "L'identifiant place doit être positif")
    private Long placeId;

    public EntreeSortieRequestDTO() {
    }

    public EntreeSortieRequestDTO(LocalDateTime dateHeureEntree, Long vehiculeId) {
        this.dateHeureEntree = dateHeureEntree;
        this.vehiculeId = vehiculeId;
    }

    public LocalDateTime getDateHeureEntree() {
        return dateHeureEntree;
    }

    public void setDateHeureEntree(LocalDateTime dateHeureEntree) {
        this.dateHeureEntree = dateHeureEntree;
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
