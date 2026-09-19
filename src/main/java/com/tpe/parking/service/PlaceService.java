package com.tpe.parking.service;

import com.tpe.parking.dto.request.PlaceRequestDTO;
import com.tpe.parking.dto.response.PlaceResponseDTO;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlaceService {

    PlaceResponseDTO ajouter(PlaceRequestDTO dto);

    PlaceResponseDTO recupererParId(Long id);

    PlaceResponseDTO recupererParNumero(String numero);

    Page<PlaceResponseDTO> trouverTous(String numero, String zone, TypePlace type, StatutPlace statut, Pageable pageable);

    List<PlaceResponseDTO> trouverDisponiblesParType(TypePlace type);

    PlaceResponseDTO modifier(Long id, PlaceRequestDTO dto);

    void supprimer(Long id);

    void changerStatut(Long id, StatutPlace nouveauStatut);

    long compterParStatut(StatutPlace statut);

    long compterParStatutEtZone(StatutPlace statut, String zone);
}
