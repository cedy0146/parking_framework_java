package com.tpe.parking.dto.request;

import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PlaceRequestDTO {

    @NotBlank(message = "Le numéro est obligatoire")
    @Size(max = 10, message = "Le numéro ne doit pas dépasser 10 caractères")
    private String numero;

    @NotBlank(message = "La zone est obligatoire")
    @Size(max = 10, message = "La zone ne doit pas dépasser 10 caractères")
    private String zone;

    @NotNull(message = "Le type est obligatoire")
    private TypePlace type;

    private StatutPlace statut;

    public PlaceRequestDTO() {
    }

    public PlaceRequestDTO(String numero, String zone, TypePlace type) {
        this.numero = numero;
        this.zone = zone;
        this.type = type;
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
}
