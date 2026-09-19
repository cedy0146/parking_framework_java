package com.tpe.parking.controller.rest;

import com.tpe.parking.dto.request.PlaceRequestDTO;
import com.tpe.parking.dto.response.PlaceResponseDTO;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import com.tpe.parking.service.PlaceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/parking/spaces")
public class PlaceController {

    private final PlaceService service;

    public PlaceController(PlaceService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<PlaceResponseDTO> ajouter(@Valid @RequestBody PlaceRequestDTO dto) {
        PlaceResponseDTO response = service.ajouter(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR','USER')")
    public ResponseEntity<PlaceResponseDTO> recupererParId(@PathVariable Long id) {
        return ResponseEntity.ok(service.recupererParId(id));
    }

    @GetMapping("/numero/{numero}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR','USER')")
    public ResponseEntity<PlaceResponseDTO> recupererParNumero(@PathVariable String numero) {
        return ResponseEntity.ok(service.recupererParNumero(numero));
    }

    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR','USER')")
    public ResponseEntity<List<PlaceResponseDTO>> trouverDisponibles(
            @RequestParam(required = false) TypePlace type) {
        if (type != null) {
            return ResponseEntity.ok(service.trouverDisponiblesParType(type));
        }
        return ResponseEntity.ok(service.trouverTous(null, null, null, StatutPlace.LIBRE,
                PageRequest.of(0, 1000)).getContent());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR','USER')")
    public ResponseEntity<Page<PlaceResponseDTO>> trouverTous(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) TypePlace type,
            @RequestParam(required = false) StatutPlace statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "numero,asc") String[] sort) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        return ResponseEntity.ok(service.trouverTous(numero, zone, type, statut, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<PlaceResponseDTO> modifier(
            @PathVariable Long id,
            @Valid @RequestBody PlaceRequestDTO dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PatchMapping("/{id}/statut")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
    public ResponseEntity<Void> changerStatut(
            @PathVariable Long id,
            @RequestParam StatutPlace statut) {
        service.changerStatut(id, statut);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
