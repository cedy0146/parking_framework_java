package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.EntreeSortieRequestDTO;
import com.tpe.parking.dto.response.EntreeSortieResponseDTO;
import com.tpe.parking.service.EntreeSortieService;
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

import java.time.LocalDateTime;

@Controller
@RequestMapping("/entrees-sorties")
public class EntreeSortieWebController {

    private final EntreeSortieService service;
    private final VehiculeService vehiculeService;

    public EntreeSortieWebController(EntreeSortieService service, VehiculeService vehiculeService) {
        this.service = service;
        this.vehiculeService = vehiculeService;
    }

    @GetMapping
    public String lister(
            @RequestParam(required = false) Long vehiculeId,
            @RequestParam(required = false) Long placeId,
            @RequestParam(required = false) Boolean enCours,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,desc") String[] sort,
            Model model) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<EntreeSortieResponseDTO> entreesSorties = service.trouverTous(vehiculeId, placeId, enCours, pageable);

        model.addAttribute("entreesSorties", entreesSorties);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", entreesSorties.getTotalPages());
        model.addAttribute("enCoursFiltre", enCours);
        model.addAttribute("nbEnCours", service.compterEnCours());
        return "entrees-sorties/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireEntree(Model model) {
        EntreeSortieRequestDTO dto = new EntreeSortieRequestDTO();
        dto.setDateHeureEntree(LocalDateTime.now());
        model.addAttribute("entreeSortie", dto);
        model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
        return "entrees-sorties/entree";
    }

    @PostMapping("/enregistrer-entree")
    public String enregistrerEntree(
            @Valid @ModelAttribute("entreeSortie") EntreeSortieRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
            return "entrees-sorties/entree";
        }

        try {
            service.enregistrerEntree(dto);
            redirectAttributes.addFlashAttribute("success", "Entrée enregistrée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/entrees-sorties/nouveau";
        }

        return "redirect:/entrees-sorties";
    }

    @GetMapping("/sortie/{vehiculeId}")
    public String enregistrerSortie(@PathVariable Long vehiculeId, RedirectAttributes redirectAttributes) {
        try {
            service.enregistrerSortie(vehiculeId);
            redirectAttributes.addFlashAttribute("success", "Sortie enregistrée avec succès");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/entrees-sorties";
    }

    @GetMapping("/facturation/{id}")
    public String facturation(@PathVariable Long id, Model model) {
        model.addAttribute("tarification", service.calculerTarification(id));
        return "entrees-sorties/facturation";
    }
}
