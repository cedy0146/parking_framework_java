package com.tpe.parking.controller.rest;

import com.tpe.parking.dto.request.AbonnementRequestDTO;
import com.tpe.parking.dto.response.AbonnementResponseDTO;
import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.TypeAbonnement;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.security.SecurityUtils;
import com.tpe.parking.service.AbonnementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/subscriptions")
public class AbonnementController {

    private final AbonnementService service;
    private final SecurityUtils securityUtils;

    public AbonnementController(AbonnementService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AbonnementResponseDTO> ajouter(@Valid @RequestBody AbonnementRequestDTO dto) {
        AbonnementResponseDTO response = service.ajouter(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<AbonnementResponseDTO> recupererParId(@PathVariable Long id) {
        AbonnementResponseDTO dto = service.recupererParId(id);
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            if (!userId.equals(dto.getUtilisateurId())) {
                throw new InvalidOperationException("Vous ne pouvez consulter que votre propre abonnement");
            }
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<AbonnementResponseDTO> recupererActif(
            @RequestParam Long vehiculeId,
            @RequestParam(required = false) LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }
        AbonnementResponseDTO dto = service.recupererActifParVehiculeEtDate(vehiculeId, date);
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            if (!userId.equals(dto.getUtilisateurId())) {
                throw new InvalidOperationException("Vous ne pouvez consulter que votre propre abonnement");
            }
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<Page<AbonnementResponseDTO>> trouverTous(
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) StatutAbonnement statut,
            @RequestParam(required = false) TypeAbonnement type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        if (securityUtils.estUSER()) {
            utilisateurId = securityUtils.getUtilisateurConnecteId();
        }

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        return ResponseEntity.ok(service.trouverTous(utilisateurId, statut, type, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AbonnementResponseDTO> modifier(
            @PathVariable Long id,
            @Valid @RequestBody AbonnementRequestDTO dto) {
        return ResponseEntity.ok(service.modifier(id, dto));
    }

    @PatchMapping("/{id}/resilier")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AbonnementResponseDTO> resilier(@PathVariable Long id) {
        return ResponseEntity.ok(service.resilier(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        service.supprimer(id);
        return ResponseEntity.noContent().build();
    }
}
