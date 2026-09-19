package com.tpe.parking.mapper;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.entity.Reservation;

public class ReservationMapper {

    private ReservationMapper() {
    }

    public static Reservation toEntity(ReservationRequestDTO dto) {
        Reservation reservation = new Reservation();
        reservation.setDateHeureArriveePrevue(dto.getDateHeureArriveePrevue());
        reservation.setDateHeureDepartPrevue(dto.getDateHeureDepartPrevue());
        return reservation;
    }

    public static ReservationResponseDTO toResponse(Reservation entity) {
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(entity.getId());
        dto.setDateHeureArriveePrevue(entity.getDateHeureArriveePrevue());
        dto.setDateHeureDepartPrevue(entity.getDateHeureDepartPrevue());
        dto.setStatut(entity.getStatut());
        dto.setDateCreation(entity.getDateCreation());
        if (entity.getUtilisateur() != null) {
            dto.setUtilisateurId(entity.getUtilisateur().getId());
            dto.setUtilisateurNom(entity.getUtilisateur().getPrenom() + " " + entity.getUtilisateur().getNom());
        }
        if (entity.getVehicule() != null) {
            dto.setVehiculeId(entity.getVehicule().getId());
            dto.setVehiculeImmatriculation(entity.getVehicule().getImmatriculation());
        }
        if (entity.getPlace() != null) {
            dto.setPlaceId(entity.getPlace().getId());
            dto.setPlaceNumero(entity.getPlace().getNumero());
        }
        return dto;
    }
}
