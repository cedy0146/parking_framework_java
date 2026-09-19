package com.tpe.parking.service;

import com.tpe.parking.dto.request.VehiculeRequestDTO;
import com.tpe.parking.dto.response.VehiculeResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VehiculeService {

    VehiculeResponseDTO ajouter(VehiculeRequestDTO dto);

    VehiculeResponseDTO recupererParId(Long id);

    VehiculeResponseDTO recupererParImmatriculation(String immatriculation);

    Page<VehiculeResponseDTO> trouverTous(String immatriculation, String marque, Long utilisateurId, Pageable pageable);

    VehiculeResponseDTO modifier(Long id, VehiculeRequestDTO dto);

    void supprimer(Long id);
}
