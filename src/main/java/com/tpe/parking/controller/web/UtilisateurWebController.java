package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.UtilisateurRequestDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.enumeration.RoleUtilisateur;
import com.tpe.parking.service.UtilisateurService;
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
@RequestMapping("/utilisateurs")
public class UtilisateurWebController {

    private final UtilisateurService service;

    public UtilisateurWebController(UtilisateurService service) {
        this.service = service;
    }

    @GetMapping
    public String lister(
            @RequestParam(required = false) String nom,
            @RequestParam(required = false) RoleUtilisateur role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            Model model) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<UtilisateurResponseDTO> utilisateurs = service.trouverTous(nom, role, pageable);

        model.addAttribute("utilisateurs", utilisateurs);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", utilisateurs.getTotalPages());
        model.addAttribute("nom", nom);
        model.addAttribute("role", role);
        model.addAttribute("roles", RoleUtilisateur.values());
        return "utilisateurs/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireAjout(Model model, HttpServletRequest request) {
        model.addAttribute("utilisateur", new UtilisateurRequestDTO());
        model.addAttribute("roles", RoleUtilisateur.values());
        return CrudAjax.estAjax(request) ? "utilisateurs/formulaire :: formContent" : "utilisateurs/formulaire";
    }

    @PostMapping("/sauvegarder")
    public Object sauvegarder(
            @Valid @ModelAttribute("utilisateur") UtilisateurRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean ajax = CrudAjax.estAjax(request);

        if (result.hasErrors()) {
            model.addAttribute("roles", RoleUtilisateur.values());
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "utilisateurs/formulaire :: formContent";
            }
            return "utilisateurs/formulaire";
        }

        try {
            service.ajouter(dto);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/utilisateurs/nouveau";
        }

        if (ajax) {
            return CrudAjax.succes("Utilisateur ajouté avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Utilisateur ajouté avec succès");
        return "redirect:/utilisateurs";
    }

    @GetMapping("/modifier/{id}")
    public String formulaireModification(@PathVariable Long id, Model model, HttpServletRequest request) {
        UtilisateurResponseDTO utilisateur = service.recupererParId(id);
        UtilisateurRequestDTO dto = new UtilisateurRequestDTO();
        dto.setNom(utilisateur.getNom());
        dto.setPrenom(utilisateur.getPrenom());
        dto.setEmail(utilisateur.getEmail());
        dto.setTelephone(utilisateur.getTelephone());
        dto.setRole(utilisateur.getRole());

        model.addAttribute("utilisateur", dto);
        model.addAttribute("id", id);
        model.addAttribute("roles", RoleUtilisateur.values());
        return CrudAjax.estAjax(request) ? "utilisateurs/formulaire :: formContent" : "utilisateurs/formulaire";
    }

    @PostMapping("/modifier/{id}")
    public Object modifier(
            @PathVariable Long id,
            @Valid @ModelAttribute("utilisateur") UtilisateurRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean ajax = CrudAjax.estAjax(request);

        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("roles", RoleUtilisateur.values());
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "utilisateurs/formulaire :: formContent";
            }
            return "utilisateurs/formulaire";
        }

        try {
            service.modifier(id, dto);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/utilisateurs";
        }

        if (ajax) {
            return CrudAjax.succes("Utilisateur modifié avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Utilisateur modifié avec succès");
        return "redirect:/utilisateurs";
    }

    @PostMapping("/supprimer/{id}")
    public Object supprimer(@PathVariable Long id, RedirectAttributes redirectAttributes,
                            HttpServletRequest request) {
        boolean ajax = CrudAjax.estAjax(request);
        try {
            service.supprimer(id);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/utilisateurs";
        }

        if (ajax) {
            return CrudAjax.succes("Utilisateur supprimé avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Utilisateur supprimé avec succès");
        return "redirect:/utilisateurs";
    }
}
