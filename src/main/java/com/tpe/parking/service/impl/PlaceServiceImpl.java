package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.PlaceRequestDTO;
import com.tpe.parking.dto.response.PlaceResponseDTO;
import com.tpe.parking.entity.Place;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import com.tpe.parking.exception.ConflictException;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.exception.ResourceNotFoundException;
import com.tpe.parking.mapper.PlaceMapper;
import com.tpe.parking.repository.PlaceRepository;
import com.tpe.parking.service.PlaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class PlaceServiceImpl implements PlaceService {

    private final PlaceRepository repository;

    public PlaceServiceImpl(PlaceRepository repository) {
        this.repository = repository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public PlaceResponseDTO ajouter(PlaceRequestDTO dto) {
        if (repository.existsByNumero(dto.getNumero())) {
            throw new ConflictException("Une place avec le numéro '" + dto.getNumero() + "' existe déjà");
        }
        Place entity = PlaceMapper.toEntity(dto);
        entity = repository.save(entity);
        return PlaceMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponseDTO recupererParId(Long id) {
        Place entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", id));
        return PlaceMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public PlaceResponseDTO recupererParNumero(String numero) {
        Place entity = repository.findByNumero(numero)
                .orElseThrow(() -> new ResourceNotFoundException("Place", "numero", numero));
        return PlaceMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PlaceResponseDTO> trouverTous(String numero, String zone, TypePlace type, StatutPlace statut, Pageable pageable) {
        return repository.rechercher(numero, zone, type, statut, pageable)
                .map(PlaceMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlaceResponseDTO> trouverDisponiblesParType(TypePlace type) {
        return repository.trouverDisponiblesParType(StatutPlace.LIBRE, type).stream()
                .map(PlaceMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public PlaceResponseDTO modifier(Long id, PlaceRequestDTO dto) {
        Place entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", id));

        if (!entity.getNumero().equals(dto.getNumero()) && repository.existsByNumero(dto.getNumero())) {
            throw new ConflictException("Une place avec le numéro '" + dto.getNumero() + "' existe déjà");
        }

        PlaceMapper.updateEntityFromDto(dto, entity);
        entity = repository.save(entity);
        return PlaceMapper.toResponse(entity);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Place", id);
        }
        repository.deleteById(id);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public void changerStatut(Long id, StatutPlace nouveauStatut) {
        Place entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Place", id));

        if (entity.getStatut() == StatutPlace.HORS_SERVICE && nouveauStatut != StatutPlace.HORS_SERVICE) {
            throw new InvalidOperationException("Impossible de changer le statut d'une place hors service. Réactivation requise d'abord.");
        }

        entity.setStatut(nouveauStatut);
        repository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public long compterParStatut(StatutPlace statut) {
        return repository.countByStatut(statut);
    }

    @Override
    @Transactional(readOnly = true)
    public long compterParStatutEtZone(StatutPlace statut, String zone) {
        return repository.countByStatutAndZone(statut, zone);
    }
}
