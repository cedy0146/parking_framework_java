package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.AbonnementRequestDTO;
import com.tpe.parking.dto.response.AbonnementResponseDTO;
import com.tpe.parking.entity.Abonnement;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.entity.Vehicule;
import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.TypeAbonnement;
import com.tpe.parking.exception.BusinessException;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.exception.ResourceNotFoundException;
import com.tpe.parking.mapper.AbonnementMapper;
import com.tpe.parking.repository.AbonnementRepository;
import com.tpe.parking.repository.UtilisateurRepository;
import com.tpe.parking.repository.VehiculeRepository;
import com.tpe.parking.service.AbonnementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@Transactional
public class AbonnementServiceImpl implements AbonnementService {

    private final AbonnementRepository repository;
    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;

    public AbonnementServiceImpl(AbonnementRepository repository,
                                  UtilisateurRepository utilisateurRepository,
                                  VehiculeRepository vehiculeRepository) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
        this.vehiculeRepository = vehiculeRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AbonnementResponseDTO ajouter(AbonnementRequestDTO dto) {
        if (dto.getDateDebut().isAfter(dto.getDateFin())) {
            throw new BusinessException("La date de début doit être antérieure à la date de fin");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", dto.getUtilisateurId()));

        Vehicule vehicule = vehiculeRepository.findById(dto.getVehiculeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", dto.getVehiculeId()));

        repository.findByVehiculeIdAndStatut(vehicule.getId(), StatutAbonnement.ACTIF)
                .ifPresent(a -> {
                    throw new BusinessException("Le véhicule a déjà un abonnement actif");
                });

        Abonnement entity = AbonnementMapper.toEntity(dto);
        entity.setUtilisateur(utilisateur);
        entity.setVehicule(vehicule);
        entity = repository.save(entity);
        return AbonnementMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public AbonnementResponseDTO recupererParId(Long id) {
        Abonnement entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement", id));
        return AbonnementMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public AbonnementResponseDTO recupererActifParVehiculeEtDate(Long vehiculeId, LocalDate date) {
        Abonnement entity = repository.trouverActifPourDate(vehiculeId, date)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement actif", "vehiculeId", vehiculeId.toString()));
        return AbonnementMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AbonnementResponseDTO> trouverTous(Long utilisateurId, StatutAbonnement statut, TypeAbonnement type, Pageable pageable) {
        return repository.rechercher(utilisateurId, statut, type, pageable)
                .map(AbonnementMapper::toResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AbonnementResponseDTO modifier(Long id, AbonnementRequestDTO dto) {
        Abonnement entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement", id));

        if (entity.getStatut() == StatutAbonnement.RESILIE) {
            throw new InvalidOperationException("Un abonnement résilié ne peut pas être modifié");
        }

        AbonnementMapper.updateEntityFromDto(dto, entity);
        entity = repository.save(entity);
        return AbonnementMapper.toResponse(entity);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public AbonnementResponseDTO resilier(Long id) {
        Abonnement entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement", id));

        if (entity.getStatut() == StatutAbonnement.RESILIE) {
            throw new InvalidOperationException("L'abonnement est déjà résilié");
        }

        if (entity.getStatut() == StatutAbonnement.EXPIRE) {
            throw new InvalidOperationException("Un abonnement expiré ne peut pas être résilié");
        }

        entity.setStatut(StatutAbonnement.RESILIE);
        entity = repository.save(entity);
        return AbonnementMapper.toResponse(entity);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Abonnement", id);
        }
        repository.deleteById(id);
    }
}
