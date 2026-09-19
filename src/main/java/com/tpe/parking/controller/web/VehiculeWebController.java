package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.VehiculeRequestDTO;
import com.tpe.parking.dto.response.UtilisateurResponseDTO;
import com.tpe.parking.dto.response.VehiculeResponseDTO;
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
@RequestMapping("/vehicules")
public class VehiculeWebController {

    private final VehiculeService service;
    private final UtilisateurService utilisateurService;

    public VehiculeWebController(VehiculeService service, UtilisateurService utilisateurService) {
        this.service = service;
        this.utilisateurService = utilisateurService;
    }

    @GetMapping
    public String lister(
            @RequestParam(required = false) String immatriculation,
            @RequestParam(required = false) String marque,
            @RequestParam(required = false) Long utilisateurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String[] sort,
            Model model) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<VehiculeResponseDTO> vehicules = service.trouverTous(immatriculation, marque, utilisateurId, pageable);

        model.addAttribute("vehicules", vehicules);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", vehicules.getTotalPages());
        model.addAttribute("immatriculation", immatriculation);
        model.addAttribute("marque", marque);
        return "vehicules/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireAjout(Model model) {
        model.addAttribute("vehicule", new VehiculeRequestDTO());
        model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
        return "vehicules/formulaire";
    }

    @PostMapping("/sauvegarder")
    public String sauvegarder(
            @Valid @ModelAttribute("vehicule") VehiculeRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
            return "vehicules/formulaire";
        }

        service.ajouter(dto);
        redirectAttributes.addFlashAttribute("success", "Véhicule ajouté avec succès");
        return "redirect:/vehicules";
    }

    @GetMapping("/modifier/{id}")
    public String formulaireModification(@PathVariable Long id, Model model) {
        VehiculeResponseDTO vehicule = service.recupererParId(id);
        VehiculeRequestDTO dto = new VehiculeRequestDTO();
        dto.setImmatriculation(vehicule.getImmatriculation());
        dto.setMarque(vehicule.getMarque());
        dto.setModele(vehicule.getModele());
        dto.setCouleur(vehicule.getCouleur());
        dto.setTypeVehicule(vehicule.getTypeVehicule());
        dto.setUtilisateurId(vehicule.getUtilisateurId());

        model.addAttribute("vehicule", dto);
        model.addAttribute("id", id);
        model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
        return "vehicules/formulaire";
    }

    @PostMapping("/modifier/{id}")
    public String modifier(
            @PathVariable Long id,
            @Valid @ModelAttribute("vehicule") VehiculeRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("utilisateurs", utilisateurService.trouverTous(null, null, PageRequest.of(0, 100)).getContent());
            return "vehicules/formulaire";
        }

        service.modifier(id, dto);
        redirectAttributes.addFlashAttribute("success", "Véhicule modifié avec succès");
        return "redirect:/vehicules";
    }

    @GetMapping("/supprimer/{id}")
    public String supprimer(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        service.supprimer(id);
        redirectAttributes.addFlashAttribute("success", "Véhicule supprimé avec succès");
        return "redirect:/vehicules";
    }
}
