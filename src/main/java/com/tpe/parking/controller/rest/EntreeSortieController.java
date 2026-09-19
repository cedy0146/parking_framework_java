package com.tpe.parking.controller.rest;

import com.tpe.parking.dto.request.EntreeSortieRequestDTO;
import com.tpe.parking.dto.response.EntreeSortieResponseDTO;
import com.tpe.parking.dto.response.TarificationResponseDTO;
import com.tpe.parking.service.EntreeSortieService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/parking")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER','OPERATOR')")
public class EntreeSortieController {

    private final EntreeSortieService service;

    public EntreeSortieController(EntreeSortieService service) {
        this.service = service;
    }

    @PostMapping("/entries")
    public ResponseEntity<EntreeSortieResponseDTO> enregistrerEntree(@Valid @RequestBody EntreeSortieRequestDTO dto) {
        EntreeSortieResponseDTO response = service.enregistrerEntree(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/exits")
    public ResponseEntity<EntreeSortieResponseDTO> enregistrerSortie(@RequestParam Long vehiculeId) {
        return ResponseEntity.ok(service.enregistrerSortie(vehiculeId));
    }

    @GetMapping("/entries/{id}")
    public ResponseEntity<EntreeSortieResponseDTO> recupererParId(@PathVariable Long id) {
        return ResponseEntity.ok(service.recupererParId(id));
    }

    @GetMapping("/entries/active/{vehiculeId}")
    public ResponseEntity<EntreeSortieResponseDTO> recupererEntreeEnCours(@PathVariable Long vehiculeId) {
        return ResponseEntity.ok(service.recupererEntreeEnCours(vehiculeId));
    }

    @GetMapping("/entries")
    public ResponseEntity<Page<EntreeSortieResponseDTO>> trouverTous(
            @RequestParam(required = false) Long vehiculeId,
            @RequestParam(required = false) Long placeId,
            @RequestParam(required = false) Boolean enCours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        return ResponseEntity.ok(service.trouverTous(vehiculeId, placeId, enCours, pageable));
    }

    @GetMapping("/billing/{id}")
    public ResponseEntity<TarificationResponseDTO> calculerTarification(@PathVariable Long id) {
        return ResponseEntity.ok(service.calculerTarification(id));
    }

    @GetMapping("/occupancy")
    public ResponseEntity<Page<EntreeSortieResponseDTO>> occupation(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(service.trouverTous(null, null, true, pageable));
    }

    @GetMapping("/revenue")
    public ResponseEntity<BigDecimal> revenus(
            @RequestParam(required = false) LocalDateTime debut,
            @RequestParam(required = false) LocalDateTime fin) {
        if (debut != null && fin != null) {
            return ResponseEntity.ok(service.calculerRevenuPeriode(debut, fin));
        }
        return ResponseEntity.ok(service.calculerRevenuTotal());
    }
}
