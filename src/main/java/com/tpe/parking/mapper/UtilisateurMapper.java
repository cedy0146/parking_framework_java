package com.tpe.parking.mapper;

import com.tpe.parking.dto.request.UtilisateurRequestDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.entity.Utilisateur;

public class UtilisateurMapper {

    private UtilisateurMapper() {
    }

    public static Utilisateur toEntity(UtilisateurRequestDTO dto) {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setNom(dto.getNom());
        utilisateur.setPrenom(dto.getPrenom());
        utilisateur.setEmail(dto.getEmail());
        utilisateur.setMotDePasse(dto.getMotDePasse());
        utilisateur.setTelephone(dto.getTelephone());
        utilisateur.setRole(dto.getRole());
        return utilisateur;
    }

    public static UtilisateurResponseDTO toResponse(Utilisateur entity) {
        UtilisateurResponseDTO dto = new UtilisateurResponseDTO();
        dto.setId(entity.getId());
        dto.setNom(entity.getNom());
        dto.setPrenom(entity.getPrenom());
        dto.setEmail(entity.getEmail());
        dto.setTelephone(entity.getTelephone());
        dto.setRole(entity.getRole());
        dto.setDateCreation(entity.getDateCreation());
        return dto;
    }

    public static void updateEntityFromDto(UtilisateurRequestDTO dto, Utilisateur entity) {
        entity.setNom(dto.getNom());
        entity.setPrenom(dto.getPrenom());
        entity.setEmail(dto.getEmail());
        if (dto.getMotDePasse() != null && !dto.getMotDePasse().isBlank()) {
            entity.setMotDePasse(dto.getMotDePasse());
        }
        entity.setTelephone(dto.getTelephone());
        entity.setRole(dto.getRole());
    }
}
