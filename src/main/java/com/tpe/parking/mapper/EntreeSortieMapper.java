package com.tpe.parking.mapper;

import com.tpe.parking.dto.request.EntreeSortieRequestDTO;
import com.tpe.parking.dto.response.EntreeSortieResponseDTO;
import com.tpe.parking.entity.EntreeSortie;

public class EntreeSortieMapper {

    private EntreeSortieMapper() {
    }

    public static EntreeSortie toEntity(EntreeSortieRequestDTO dto) {
        EntreeSortie entity = new EntreeSortie();
        entity.setDateHeureEntree(dto.getDateHeureEntree());
        return entity;
    }

    public static EntreeSortieResponseDTO toResponse(EntreeSortie entity) {
        EntreeSortieResponseDTO dto = new EntreeSortieResponseDTO();
        dto.setId(entity.getId());
        dto.setDateHeureEntree(entity.getDateHeureEntree());
        dto.setDateHeureSortie(entity.getDateHeureSortie());
        dto.setMontantPaye(entity.getMontantPaye());
        dto.setEnCours(entity.estEnCours());
        if (entity.getPlace() != null) {
            dto.setPlaceId(entity.getPlace().getId());
            dto.setPlaceNumero(entity.getPlace().getNumero());
        }
        if (entity.getVehicule() != null) {
            dto.setVehiculeId(entity.getVehicule().getId());
            dto.setVehiculeImmatriculation(entity.getVehicule().getImmatriculation());
            if (entity.getVehicule().getUtilisateur() != null) {
                dto.setUtilisateurId(entity.getVehicule().getUtilisateur().getId());
                dto.setUtilisateurNom(entity.getVehicule().getUtilisateur().getPrenom() + " " +
                        entity.getVehicule().getUtilisateur().getNom());
            }
        }
        if (entity.getDateHeureEntree() != null && entity.getDateHeureSortie() != null) {
            long minutes = java.time.Duration.between(entity.getDateHeureEntree(), entity.getDateHeureSortie()).toMinutes();
            dto.setDureeMinutes(minutes);
            dto.setDureeFormatee(formatDuree(minutes));
        }
        return dto;
    }

    private static String formatDuree(long minutes) {
        long heures = minutes / 60;
        long mins = minutes % 60;
        if (heures > 0) {
            return heures + "h" + (mins > 0 ? String.format("%02d", mins) : "00");
        }
        return mins + "min";
    }
}
