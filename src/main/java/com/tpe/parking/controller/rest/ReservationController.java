package com.tpe.parking.controller.rest;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.security.SecurityUtils;
import com.tpe.parking.service.ReservationService;
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
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService service;
    private final SecurityUtils securityUtils;

    public ReservationController(ReservationService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<ReservationResponseDTO> creer(@Valid @RequestBody ReservationRequestDTO dto) {
        if (securityUtils.estUSER()) {
            dto.setUtilisateurId(securityUtils.getUtilisateurConnecteId());
        }
        ReservationResponseDTO response = service.creer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<ReservationResponseDTO> recupererParId(@PathVariable Long id) {
        ReservationResponseDTO dto = service.recupererParId(id);
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            if (!userId.equals(dto.getUtilisateurId())) {
                throw new InvalidOperationException("Vous ne pouvez consulter que vos propres réservations");
            }
        }
        return ResponseEntity.ok(dto);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<Page<ReservationResponseDTO>> trouverTous(
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Long placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort) {

        if (securityUtils.estUSER()) {
            utilisateurId = securityUtils.getUtilisateurConnecteId();
        }

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        return ResponseEntity.ok(service.trouverTous(utilisateurId, statut, placeId, pageable));
    }

    @PatchMapping("/{id}/confirmer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<ReservationResponseDTO> confirmer(@PathVariable Long id) {
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            return ResponseEntity.ok(service.confirmerSiProprietaire(id, userId));
        }
        return ResponseEntity.ok(service.confirmer(id));
    }

    @PatchMapping("/{id}/annuler")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<ReservationResponseDTO> annuler(@PathVariable Long id) {
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            return ResponseEntity.ok(service.annulerSiProprietaire(id, userId));
        }
        return ResponseEntity.ok(service.annuler(id));
    }

    @PatchMapping("/{id}/terminer")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<ReservationResponseDTO> terminer(@PathVariable Long id) {
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            return ResponseEntity.ok(service.terminerSiProprietaire(id, userId));
        }
        return ResponseEntity.ok(service.terminer(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','USER')")
    public ResponseEntity<Void> supprimer(@PathVariable Long id) {
        if (securityUtils.estUSER()) {
            Long userId = securityUtils.getUtilisateurConnecteId();
            service.supprimerSiProprietaire(id, userId);
        } else {
            service.supprimer(id);
        }
        return ResponseEntity.noContent().build();
    }
}
