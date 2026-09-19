package com.tpe.parking.controller.rest;

import com.tpe.parking.dto.request.UtilisateurRequestDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.enumeration.RoleUtilisateur;
import com.tpe.parking.service.UtilisateurService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/utilisateurs")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class UtilisateurController {

    private final UtilisateurService service;

    public UtilisateurController(UtilisateurService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<UtilisateurResponseDTO> ajouter(@Valid @RequestBody UtilisateurRequestDTO dto) {
        UtilisateurResponseDTO response = service.ajouter(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDTO> recupererParId(@PathVariable Long id) {
        return ResponseEntity.ok(service.recupererParId(id));
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<UtilisateurResponseDTO> recupererParEmail(@PathVariable String email) {
        return ResponseEntity.ok(service.recupererParEmail(email));
    }

    @GetMapping
    public ResponseEntity<Page<UtilisateurResponseDTO>> trouverTous(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) RoleUtilisateur role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        return ResponseEntity.ok(service.trouverTous(nom, role, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurResponseDTO> modifier(
            @PathVariable Long id,
            @Valid @RequestBody UtilisateurRequestDTO dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
