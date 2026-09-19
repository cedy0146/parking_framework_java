package com.tpe.parking.mapper;

import com.tpe.parking.dto.request.VehiculeRequestDTO;
import com.tpe.parking.dto.response.VehiculeResponseDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.entity.Vehicule;

public class VehiculeMapper {

    private VehiculeMapper() {
    }

    public static Vehicule toEntity(VehiculeRequestDTO dto) {
        Vehicule vehicule = new Vehicule();
        vehicule.setImmatriculation(dto.getImmatriculation());
        vehicule.setMarque(dto.getMarque());
        vehicule.setModele(dto.getModele());
        vehicule.setCouleur(dto.getCouleur());
        vehicule.setTypeVehicule(dto.getTypeVehicule());
        return vehicule;
    }

    public static VehiculeResponseDTO toResponse(Vehicule entity) {
        VehiculeResponseDTO dto = new VehiculeResponseDTO();
        dto.setId(entity.getId());
        dto.setImmatriculation(entity.getImmatriculation());
        dto.setMarque(entity.getMarque());
        dto.setModele(entity.getModele());
        dto.setCouleur(entity.getCouleur());
        dto.setTypeVehicule(entity.getTypeVehicule());
        dto.setDateEnregistrement(entity.getDateEnregistrement());
        if (entity.getUtilisateur() != null) {
            dto.setUtilisateurId(entity.getUtilisateur().getId());
            dto.setUtilisateurNom(entity.getUtilisateur().getPrenom() + " " + entity.getUtilisateur().getNom());
        }
        return dto;
    }

    public static void updateEntityFromDto(VehiculeRequestDTO dto, Vehicule entity) {
        entity.setImmatriculation(dto.getImmatriculation());
        entity.setMarque(dto.getMarque());
        entity.setModele(dto.getModele());
        entity.setCouleur(dto.getCouleur());
        entity.setTypeVehicule(dto.getTypeVehicule());
    }
}
