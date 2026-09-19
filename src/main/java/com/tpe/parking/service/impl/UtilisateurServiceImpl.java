package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.UtilisateurRequestDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.enumeration.RoleUtilisateur;
import com.tpe.parking.exception.ConflictException;
import com.tpe.parking.exception.ResourceNotFoundException;
import com.tpe.parking.mapper.UtilisateurMapper;
import com.tpe.parking.repository.UtilisateurRepository;
import com.tpe.parking.service.UtilisateurService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UtilisateurServiceImpl implements UtilisateurService {

    private final UtilisateurRepository repository;

    public UtilisateurServiceImpl(UtilisateurRepository repository) {
        this.repository = repository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UtilisateurResponseDTO ajouter(UtilisateurRequestDTO dto) {
        if (repository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Un utilisateur avec l'email '" + dto.getEmail() + "' existe déjà");
        }
        Utilisateur entity = UtilisateurMapper.toEntity(dto);
        entity = repository.save(entity);
        return UtilisateurMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponseDTO recupererParId(Long id) {
        Utilisateur entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        return UtilisateurMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public UtilisateurResponseDTO recupererParEmail(String email) {
        Utilisateur entity = repository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
        return UtilisateurMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UtilisateurResponseDTO> trouverTous(String nom, RoleUtilisateur role, Pageable pageable) {
        return repository.rechercher(nom, role, pageable).map(UtilisateurMapper::toResponse);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public UtilisateurResponseDTO modifier(Long id, UtilisateurRequestDTO dto) {
        Utilisateur entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));

        if (!entity.getEmail().equals(dto.getEmail()) && repository.existsByEmail(dto.getEmail())) {
            throw new ConflictException("Un utilisateur avec l'email '" + dto.getEmail() + "' existe déjà");
        }

        UtilisateurMapper.updateEntityFromDto(dto, entity);
        entity = repository.save(entity);
        return UtilisateurMapper.toResponse(entity);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Utilisateur", id);
        }
        repository.deleteById(id);
    }
}
