package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.security.SecurityUtils;
import com.tpe.parking.service.ReservationService;
import com.tpe.parking.service.UtilisateurService;
import com.tpe.parking.service.VehiculeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
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
    public String formulaireAjout(Model model, HttpServletRequest request) {
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

        return CrudAjax.estAjax(request) ? "reservations/formulaire :: formContent" : "reservations/formulaire";
    }

    @PostMapping("/sauvegarder")
    public Object sauvegarder(
            @Valid @ModelAttribute("reservation") ReservationRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean ajax = CrudAjax.estAjax(request);

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
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "reservations/formulaire :: formContent";
            }
            return "reservations/formulaire";
        }

        try {
            service.creer(dto);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservations/nouveau";
        }

        if (ajax) {
            return CrudAjax.succes("Réservation créée avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Réservation créée avec succès");
        return "redirect:/reservations";
    }

    private Object executer(Runnable action, String messageSuccess,
                            RedirectAttributes redirectAttributes, HttpServletRequest request) {
        boolean ajax = CrudAjax.estAjax(request);
        try {
            action.run();
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/reservations";
        }
        if (ajax) {
            return CrudAjax.succes(messageSuccess);
        }
        redirectAttributes.addFlashAttribute("success", messageSuccess);
        return "redirect:/reservations";
    }

    private void executerPourUtilisateur(Long id, Runnable actionUtilisateur, Runnable actionGestion) {
        if (securityUtils.estUSER()) {
            actionUtilisateur.run();
        } else {
            actionGestion.run();
        }
    }

    @PostMapping("/confirmer/{id}")
    public Object confirmer(@PathVariable Long id, RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {
        return executer(() -> executerPourUtilisateur(id,
                        () -> service.confirmerSiProprietaire(id, securityUtils.getUtilisateurConnecteId()),
                        () -> service.confirmer(id)),
                "Réservation confirmée", redirectAttributes, request);
    }

    @PostMapping("/annuler/{id}")
    public Object annuler(@PathVariable Long id, RedirectAttributes redirectAttributes,
                          HttpServletRequest request) {
        return executer(() -> executerPourUtilisateur(id,
                        () -> service.annulerSiProprietaire(id, securityUtils.getUtilisateurConnecteId()),
                        () -> service.annuler(id)),
                "Réservation annulée", redirectAttributes, request);
    }

    @PostMapping("/terminer/{id}")
    public Object terminer(@PathVariable Long id, RedirectAttributes redirectAttributes,
                           HttpServletRequest request) {
        return executer(() -> executerPourUtilisateur(id,
                        () -> service.terminerSiProprietaire(id, securityUtils.getUtilisateurConnecteId()),
                        () -> service.terminer(id)),
                "Réservation terminée", redirectAttributes, request);
    }

    @PostMapping("/supprimer/{id}")
    public Object supprimer(@PathVariable Long id, RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {
        return executer(() -> executerPourUtilisateur(id,
                        () -> service.supprimerSiProprietaire(id, securityUtils.getUtilisateurConnecteId()),
                        () -> service.supprimer(id)),
                "Réservation supprimée", redirectAttributes, request);
    }
}
