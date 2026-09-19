package com.tpe.parking.dto.response;

import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;

public class PlaceResponseDTO {

    private Long id;
    private String numero;
    private String zone;
    private TypePlace type;
    private StatutPlace statut;

    public PlaceResponseDTO() {
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
}
