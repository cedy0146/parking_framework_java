package com.tpe.parking.controller.rest;

import com.tpe.parking.dto.request.VehiculeRequestDTO;
import com.tpe.parking.dto.response.VehiculeResponseDTO;
import com.tpe.parking.service.VehiculeService;
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
@RequestMapping("/api/vehicules")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
public class VehiculeController {

    private final VehiculeService service;

    public VehiculeController(VehiculeService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<VehiculeResponseDTO> ajouter(@Valid @RequestBody VehiculeRequestDTO dto) {
        VehiculeResponseDTO response = service.ajouter(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehiculeResponseDTO> recupererParId(@PathVariable Long id) {
        return ResponseEntity.ok(service.recupererParId(id));
    }

    @GetMapping("/immatriculation/{immatriculation}")
    public ResponseEntity<VehiculeResponseDTO> recupererParImmatriculation(@PathVariable String immatriculation) {
        return ResponseEntity.ok(service.recupererParImmatriculation(immatriculation));
    }

    @GetMapping
    public ResponseEntity<Page<VehiculeResponseDTO>> trouverTous(
            @RequestParam(required = false) String immatriculation,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        return ResponseEntity.ok(service.trouverTous(immatriculation, marque, utilisateurId, pageable));
    }

    @PutMapping("/{id}")
    public ResponseEntity<VehiculeResponseDTO> modifier(
            @PathVariable Long id,
            @Valid @RequestBody VehiculeRequestDTO dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
