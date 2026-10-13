package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.EntreeSortieRequestDTO;
import com.tpe.parking.dto.response.EntreeSortieResponseDTO;
import com.tpe.parking.service.EntreeSortieService;
import com.tpe.parking.service.VehiculeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
    public String formulaireEntree(Model model, HttpServletRequest request) {
        EntreeSortieRequestDTO dto = new EntreeSortieRequestDTO();
        dto.setDateHeureEntree(LocalDateTime.now());
        model.addAttribute("entreeSortie", dto);
        model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
        return CrudAjax.estAjax(request) ? "entrees-sorties/entree :: formContent" : "entrees-sorties/entree";
    }

    @PostMapping("/enregistrer-entree")
    public Object enregistrerEntree(
            @Valid @ModelAttribute("entreeSortie") EntreeSortieRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean ajax = CrudAjax.estAjax(request);

        if (result.hasErrors()) {
            model.addAttribute("vehicules", vehiculeService.trouverTous(null, null, null, PageRequest.of(0, 100)).getContent());
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "entrees-sorties/entree :: formContent";
            }
            return "entrees-sorties/entree";
        }

        try {
            service.enregistrerEntree(dto);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/entrees-sorties/nouveau";
        }

        if (ajax) {
            return CrudAjax.succes("Entrée enregistrée avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Entrée enregistrée avec succès");
        return "redirect:/entrees-sorties";
    }

    @PostMapping("/sortie/{vehiculeId}")
    public Object enregistrerSortie(@PathVariable Long vehiculeId, RedirectAttributes redirectAttributes,
                                    HttpServletRequest request) {
        boolean ajax = CrudAjax.estAjax(request);
        try {
            service.enregistrerSortie(vehiculeId);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/entrees-sorties";
        }

        if (ajax) {
            return CrudAjax.succes("Sortie enregistrée avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Sortie enregistrée avec succès");
        return "redirect:/entrees-sorties";
    }

    @GetMapping("/facturation/{id}")
    public Object facturation(@PathVariable Long id, Model model, HttpServletRequest request) {
        Object tarification = service.calculerTarification(id);

        if (CrudAjax.estAjax(request)) {
            // Rendu en alerte récapitulative : JSON avec les informations de facturation.
            return ResponseEntity.ok(tarificationMap(tarification));
        }

        model.addAttribute("tarification", tarification);
        return "entrees-sorties/facturation";
    }

    /** Copie les champs affichés de la tarification dans une map JSON. */
    private Map<String, Object> tarificationMap(Object tarification) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (tarification == null) {
            body.put("success", false);
            body.put("message", "Facturation indisponible");
            return body;
        }
        body.put("success", true);
        body.put("vehiculeImmatriculation", valeur(tarification, "getVehiculeImmatriculation"));
        body.put("placeNumero", valeur(tarification, "getPlaceNumero"));
        body.put("duree", valeur(tarification, "getDureeFormatee"));
        body.put("tarifHoraire", valeur(tarification, "getTarifHoraire"));
        body.put("montantTotal", valeur(tarification, "getMontantTotal"));
        body.put("aAbonnement", valeur(tarification, "getAAbonnement"));
        body.put("detailsCalcul", valeur(tarification, "getDetailsCalcul"));
        return body;
    }

    /** Lecture réflexive limitée aux getters de la tarification. */
    private Object valeur(Object cible, String getter) {
        try {
            return cible.getClass().getMethod(getter).invoke(cible);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
