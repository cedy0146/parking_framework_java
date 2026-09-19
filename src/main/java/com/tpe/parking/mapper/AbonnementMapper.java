package com.tpe.parking.mapper;

import com.tpe.parking.dto.request.AbonnementRequestDTO;
import com.tpe.parking.dto.response.AbonnementResponseDTO;
import com.tpe.parking.entity.Abonnement;

import java.time.LocalDate;

public class AbonnementMapper {

    private AbonnementMapper() {
    }

    public static Abonnement toEntity(AbonnementRequestDTO dto) {
        Abonnement abonnement = new Abonnement();
        abonnement.setType(dto.getType());
        abonnement.setDateDebut(dto.getDateDebut());
        abonnement.setDateFin(dto.getDateFin());
        abonnement.setPrix(dto.getPrix());
        return abonnement;
    }

    public static AbonnementResponseDTO toResponse(Abonnement entity) {
        AbonnementResponseDTO dto = new AbonnementResponseDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getType());
        dto.setDateDebut(entity.getDateDebut());
        dto.setDateFin(entity.getDateFin());
        dto.setStatut(entity.calculerStatut());
        dto.setPrix(entity.getPrix());
        dto.setDateCreation(entity.getDateCreation());
        dto.setEstActif(entity.estActif());
        if (entity.getUtilisateur() != null) {
            dto.setUtilisateurId(entity.getUtilisateur().getId());
            dto.setUtilisateurNom(entity.getUtilisateur().getPrenom() + " " + entity.getUtilisateur().getNom());
        }
        if (entity.getVehicule() != null) {
            dto.setVehiculeId(entity.getVehicule().getId());
            dto.setVehiculeImmatriculation(entity.getVehicule().getImmatriculation());
        }
        return dto;
    }

    public static void updateEntityFromDto(AbonnementRequestDTO dto, Abonnement entity) {
        entity.setType(dto.getType());
        entity.setDateDebut(dto.getDateDebut());
        entity.setDateFin(dto.getDateFin());
        if (dto.getPrix() != null) {
            entity.setPrix(dto.getPrix());
        }
    }
}
