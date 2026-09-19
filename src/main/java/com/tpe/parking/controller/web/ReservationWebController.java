package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.security.SecurityUtils;
import com.tpe.parking.service.ReservationService;
import com.tpe.parking.service.UtilisateurService;
import com.tpe.parking.service.VehiculeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/reservations")
public class ReservationWebController {

    private final ReservationService service;
    private final UtilisateurService utilisateurService;
    private final VehiculeService vehiculeService;
    private final SecurityUtils securityUtils;

    public ReservationWebController(ReservationService service,
                                     UtilisateurService utilisateurService,
                                     VehiculeService vehiculeService,
                                     SecurityUtils securityUtils) {
        this.service = service;
        this.utilisateurService = utilisateurService;
        this.vehiculeService = vehiculeService;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String lister(
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(required = false) StatutReservation statut,
            @RequestParam(required = false) Long placeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort,
            Model model) {

        if (securityUtils.estUSER()) {
            utilisateurId = securityUtils.getUtilisateurConnecteId();
        }

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<ReservationResponseDTO> reservations = service.trouverTous(utilisateurId, statut, placeId, pageable);

        model.addAttribute("reservations", reservations);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", reservations.getTotalPages());
        model.addAttribute("statutFiltre", statut);
        model.addAttribute("statutsReservation", StatutReservation.values());
        model.addAttribute("estUSER", securityUtils.estUSER());
        return "reservations/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireAjout(Model model) {
        Long userId = securityUtils.getUtilisateurConnecteId();
        Utilisateur utilisateur = securityUtils.getUtilisateurConnecte()
                .orElseThrow(() -> new IllegalStateException("Utilisateur non connecté"));

        model.addAttribute("reservation", new ReservationRequestDTO());
        model.addAttribute("estUSER", securityUtils.estUSER());
        model.addAttribute("utilisateurConnecteId", userId);

        if (securityUtils.estUSER()) {
            model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, userId, PageRequest.of(0, 100)).getContent());
        } else {
            model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
            model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
        }

        return "reservations/formulaire";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(
            @Valid @ModelAttribute("reservation") ReservationRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (securityUtils.estUSER()) {
            dto.setUtilisateurId(securityUtils.getUtilisateurConnecteId());
        }

        if (result.hasErrors()) {
            model.addAttribute("estUSER", securityUtils.estUSER());
            model.addAttribute("utilisateurConnecteId", securityUtils.getUtilisateurConnecteId());
            if (securityUtils.estUSER()) {
                model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, dto.getUtilisateurId(), PageRequest.of(0, 100)).getContent());
            } else {
                model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
                model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
            }
            return "reservations/formulaire";
        }

        try {
            service.creer(dto);
            redirectAttributes.addFlashAttribute("success", "Réservation créée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservations/nouveau";
        }

        return "redirect:/reservations";
    }

    @GetMapping("/confirmer/{id}")
    public String confirmer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (securityUtils.estUSER()) {
                service.confirmerSiProprietaire(id, securityUtils.getUtilisateurConnecteId());
            } else {
                service.confirmer(id);
            }
            redirectAttributes.addFlashAttribute("success", "Réservation confirmée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservations";
    }

    @GetMapping("/annuler/{id}")
    public String annuler(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (securityUtils.estUSER()) {
                service.annulerSiProprietaire(id, securityUtils.getUtilisateurConnecteId());
            } else {
                service.annuler(id);
            }
            redirectAttributes.addFlashAttribute("success", "Réservation annulée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservations";
    }

    @GetMapping("/terminer/{id}")
    public String terminer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (securityUtils.estUSER()) {
                service.terminerSiProprietaire(id, securityUtils.getUtilisateurConnecteId());
            } else {
                service.terminer(id);
            }
            redirectAttributes.addFlashAttribute("success", "Réservation terminée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservations";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            if (securityUtils.estUSER()) {
                service.supprimerSiProprietaire(id, securityUtils.getUtilisateurConnecteId());
            } else {
                service.supprimer(id);
            }
            redirectAttributes.addFlashAttribute("success", "Réservation supprimée");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/reservations";
    }
}
