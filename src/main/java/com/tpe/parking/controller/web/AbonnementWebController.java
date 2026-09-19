package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.AbonnementRequestDTO;
import com.tpe.parking.dto.response.AbonnementResponseDTO;
import com.tpe.parking.dto.response.VehiculeResponseDTO;
import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.TypeAbonnement;
import com.tpe.parking.security.SecurityUtils;
import com.tpe.parking.service.AbonnementService;
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

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/abonnements")
public class AbonnementWebController {

    private final AbonnementService service;
    private final UtilisateurService utilisateurService;
    private final VehiculeService vehiculeService;
    private final SecurityUtils securityUtils;

    public AbonnementWebController(AbonnementService service,
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
            @RequestParam(required = false) StatutAbonnement statut,
            @RequestParam(required = false) TypeAbonnement type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort,
            Model model) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<AbonnementResponseDTO> abonnements = service.trouverTous(utilisateurId, statut, type, pageable);

        model.addAttribute("abonnements", abonnements);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", abonnements.getTotalPages());
        model.addAttribute("statutFiltre", statut);
        model.addAttribute("typeFiltre", type);
        model.addAttribute("statutsAbonnement", StatutAbonnement.values());
        model.addAttribute("typesAbonnement", TypeAbonnement.values());
        return "abonnements/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireAjout(Model model) {
        model.addAttribute("abonnement", new AbonnementRequestDTO());
        model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
        model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
        model.addAttribute("typesAbonnement", TypeAbonnement.values());
        return "abonnements/formulaire";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(
            @Valid @ModelAttribute("abonnement") AbonnementRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
            model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
            model.addAttribute("typesAbonnement", TypeAbonnement.values());
            return "abonnements/formulaire";
        }

        try {
            service.ajouter(dto);
            redirectAttributes.addFlashAttribute("success", "Abonnement créé avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/abonnements/nouveau";
        }

        return "redirect:/abonnements";
    }

    @GetMapping("/resilier/{id}")
    public String resilier(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.resilier(id);
            redirectAttributes.addFlashAttribute("success", "Abonnement résilié");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/abonnements";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.supprimer(id);
        redirectAttributes.addFlashAttribute("success", "Abonnement supprimé");
        return "redirect:/abonnements";
    }

    @GetMapping("/mon-abonnement")
    public String monAbonnement(Model model) {
        Long userId = securityUtils.getUtilisateurConnecteId();

        List<AbonnementResponseDTO> abonnements = service.trouverTous(userId, null, null,
                PageRequest.of(0, 100)).getContent();

        List<VehiculeResponseDTO> vehicules = vehiculeService.trouverTous(null, null, userId,
                PageRequest.of(0, 100)).getContent();

        model.addAttribute("abonnements", abonnements);
        model.addAttribute("vehicules", vehicules);
        model.addAttribute("estUSER", true);
        return "abonnements/mon-abonnement";
    }
}
