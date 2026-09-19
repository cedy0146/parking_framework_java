package com.tpe.parking.service;

import com.tpe.parking.dto.request.AbonnementRequestDTO;
import com.tpe.parking.dto.response.AbonnementResponseDTO;
import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.TypeAbonnement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AbonnementService {

    AbonnementResponseDTO ajouter(AbonnementRequestDTO dto);

    AbonnementResponseDTO recupererParId(Long id);

    AbonnementResponseDTO recupererActifParVehiculeEtDate(Long vehiculeId, LocalDate date);

    Page<AbonnementResponseDTO> trouverTous(Long utilisateurId, StatutAbonnement statut, TypeAbonnement type, Pageable pageable);

    AbonnementResponseDTO modifier(Long id, AbonnementRequestDTO dto);

    AbonnementResponseDTO resilier(Long id);

    void supprimer(Long id);
}
