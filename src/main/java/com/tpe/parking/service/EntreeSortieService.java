package com.tpe.parking.service;

import com.tpe.parking.dto.request.EntreeSortieRequestDTO;
import com.tpe.parking.dto.response.EntreeSortieResponseDTO;
import com.tpe.parking.dto.response.TarificationResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface EntreeSortieService {

    EntreeSortieResponseDTO enregistrerEntree(EntreeSortieRequestDTO dto);

    EntreeSortieResponseDTO enregistrerSortie(Long vehiculeId);

    EntreeSortieResponseDTO recupererParId(Long id);

    EntreeSortieResponseDTO recupererEntreeEnCours(Long vehiculeId);

    Page<EntreeSortieResponseDTO> trouverTous(Long vehiculeId, Long placeId, Boolean enCours, Pageable pageable);

    TarificationResponseDTO calculerTarification(Long entreeSortieId);

    BigDecimal calculerRevenuPeriode(LocalDateTime debut, LocalDateTime fin);

    BigDecimal calculerRevenuTotal();

    long compterEnCours();
}
