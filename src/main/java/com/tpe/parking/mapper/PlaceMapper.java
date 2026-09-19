package com.tpe.parking.mapper;

import com.tpe.parking.dto.request.PlaceRequestDTO;
import com.tpe.parking.dto.response.PlaceResponseDTO;
import com.tpe.parking.entity.Place;

public class PlaceMapper {

    private PlaceMapper() {
    }

    public static Place toEntity(PlaceRequestDTO dto) {
        Place place = new Place();
        place.setNumero(dto.getNumero());
        place.setZone(dto.getZone());
        place.setType(dto.getType());
        place.setStatut(dto.getStatut() != null ? dto.getStatut() : com.tpe.parking.enumeration.StatutPlace.LIBRE);
        return place;
    }

    public static PlaceResponseDTO toResponse(Place entity) {
        PlaceResponseDTO dto = new PlaceResponseDTO();
        dto.setId(entity.getId());
        dto.setNumero(entity.getNumero());
        dto.setZone(entity.getZone());
        dto.setType(entity.getType());
        dto.setStatut(entity.getStatut());
        return dto;
    }

    public static void updateEntityFromDto(PlaceRequestDTO dto, Place entity) {
        entity.setNumero(dto.getNumero());
        entity.setZone(dto.getZone());
        entity.setType(dto.getType());
        if (dto.getStatut() != null) {
            entity.setStatut(dto.getStatut());
        }
    }
}
