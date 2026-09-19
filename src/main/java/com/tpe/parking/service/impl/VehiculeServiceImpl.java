package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.VehiculeRequestDTO;
import com.tpe.parking.dto.response.VehiculeResponseDTO;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.entity.Vehicule;
import com.tpe.parking.exception.ConflictException;
import com.tpe.parking.exception.ResourceNotFoundException;
import com.tpe.parking.mapper.VehiculeMapper;
import com.tpe.parking.repository.UtilisateurRepository;
import com.tpe.parking.repository.VehiculeRepository;
import com.tpe.parking.service.VehiculeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VehiculeServiceImpl implements VehiculeService {

    private final VehiculeRepository repository;
    private final UtilisateurRepository utilisateurRepository;

    public VehiculeServiceImpl(VehiculeRepository repository, UtilisateurRepository utilisateurRepository) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public VehiculeResponseDTO ajouter(VehiculeRequestDTO dto) {
        if (repository.existsByImmatriculation(dto.getImmatriculation())) {
            throw new ConflictException("Un véhicule avec l'immatriculation '" + dto.getImmatriculation() + "' existe déjà");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", dto.getUtilisateurId()));

        Vehicule entity = VehiculeMapper.toEntity(dto);
        entity.setUtilisateur(utilisateur);
        entity = repository.save(entity);
        return VehiculeMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculeResponseDTO recupererParId(Long id) {
        Vehicule entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", id));
        return VehiculeMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public VehiculeResponseDTO recupererParImmatriculation(String immatriculation) {
        Vehicule entity = repository.findByImmatriculation(immatriculation)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", "immatriculation", immatriculation));
        return VehiculeMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<VehiculeResponseDTO> trouverTous(String immatriculation, String marque, Long utilisateurId, Pageable pageable) {
        return repository.rechercher(immatriculation, marque, utilisateurId, pageable)
                .map(VehiculeMapper::toResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public VehiculeResponseDTO modifier(Long id, VehiculeRequestDTO dto) {
        Vehicule entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", id));

        if (!entity.getImmatriculation().equals(dto.getImmatriculation()) &&
                repository.existsByImmatriculation(dto.getImmatriculation())) {
            throw new ConflictException("Un véhicule avec l'immatriculation '" + dto.getImmatriculation() + "' existe déjà");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", dto.getUtilisateurId()));

        VehiculeMapper.updateEntityFromDto(dto, entity);
        entity.setUtilisateur(utilisateur);
        entity = repository.save(entity);
        return VehiculeMapper.toResponse(entity);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Vehicule", id);
        }
        repository.deleteById(id);
    }
}
