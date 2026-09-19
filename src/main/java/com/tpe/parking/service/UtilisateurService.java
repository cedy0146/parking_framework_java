package com.tpe.parking.service;

import com.tpe.parking.dto.request.UtilisateurRequestDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.enumeration.RoleUtilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UtilisateurService {

    UtilisateurResponseDTO ajouter(UtilisateurRequestDTO dto);

    UtilisateurResponseDTO recupererParId(Long id);

    UtilisateurResponseDTO recupererParEmail(String email);

    Page<UtilisateurResponseDTO> trouverTous(String nom, RoleUtilisateur role, Pageable pageable);

    UtilisateurResponseDTO modifier(Long id, UtilisateurRequestDTO dto);

    void supprimer(Long id);
}
