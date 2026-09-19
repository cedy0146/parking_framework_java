package com.tpe.parking.service;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.enumeration.StatutReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReservationService {

    ReservationResponseDTO creer(ReservationRequestDTO dto);

    ReservationResponseDTO recupererParId(Long id);

    Page<ReservationResponseDTO> trouverTous(Long utilisateurId, StatutReservation statut, Long placeId, Pageable pageable);

    ReservationResponseDTO confirmer(Long id);

    ReservationResponseDTO annuler(Long id);

    ReservationResponseDTO terminer(Long id);

    void supprimer(Long id);

    ReservationResponseDTO confirmerSiProprietaire(Long id, Long utilisateurId);

    ReservationResponseDTO annulerSiProprietaire(Long id, Long utilisateurId);

    ReservationResponseDTO terminerSiProprietaire(Long id, Long utilisateurId);

    void supprimerSiProprietaire(Long id, Long utilisateurId);
}
