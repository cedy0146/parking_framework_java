package com.tpe.parking.controller.web;

import com.tpe.parking.dto.request.PlaceRequestDTO;
import com.tpe.parking.dto.response.PlaceResponseDTO;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import com.tpe.parking.security.SecurityUtils;
import com.tpe.parking.service.PlaceService;
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

import java.util.List;

@Controller
@RequestMapping("/places")
public class PlaceWebController {

    private final PlaceService service;
    private final SecurityUtils securityUtils;

    public PlaceWebController(PlaceService service, SecurityUtils securityUtils) {
        this.service = service;
        this.securityUtils = securityUtils;
    }

    @GetMapping
    public String lister(
            @RequestParam(required = false) String numero,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) TypePlace type,
            @RequestParam(required = false) StatutPlace statut,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "numero,asc") String[] sort,
            Model model) {

        Sort.Direction direction = sort.length > 1 ?
                Sort.Direction.fromString(sort[1]) : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort[0]));
        Page<PlaceResponseDTO> places = service.trouverTous(numero, zone, type, statut, pageable);

        model.addAttribute("places", places);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", places.getTotalPages());
        model.addAttribute("numero", numero);
        model.addAttribute("zone", zone);
        model.addAttribute("typeFiltre", type);
        model.addAttribute("statutFiltre", statut);
        model.addAttribute("typesPlace", TypePlace.values());
        model.addAttribute("statutsPlace", StatutPlace.values());
        return "places/liste";
    }

    @GetMapping("/nouveau")
    public String formulaireAjout(Model model, HttpServletRequest request) {
        model.addAttribute("place", new PlaceRequestDTO());
        model.addAttribute("typesPlace", TypePlace.values());
        // AJAX : seul le fragment du formulaire est renvoyé (affiché en modale).
        return CrudAjax.estAjax(request) ? "places/formulaire :: formContent" : "places/formulaire";
    }

    @PostMapping("/sauvegarder")
    public Object sauvegarder(
            @Valid @ModelAttribute("place") PlaceRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean ajax = CrudAjax.estAjax(request);

        if (result.hasErrors()) {
            model.addAttribute("typesPlace", TypePlace.values());
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "places/formulaire :: formContent";
            }
            return "places/formulaire";
        }

        try {
            service.ajouter(dto);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/places";
        }

        if (ajax) {
            return CrudAjax.succes("Place ajoutée avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Place ajoutée avec succès");
        return "redirect:/places";
    }

    @GetMapping("/modifier/{id}")
    public String formulaireModification(@PathVariable Long id, Model model, HttpServletRequest request) {
        PlaceResponseDTO place = service.recupererParId(id);
        PlaceRequestDTO dto = new PlaceRequestDTO();
        dto.setNumero(place.getNumero());
        dto.setZone(place.getZone());
        dto.setType(place.getType());
        dto.setStatut(place.getStatut());

        model.addAttribute("place", dto);
        model.addAttribute("id", id);
        model.addAttribute("typesPlace", TypePlace.values());
        model.addAttribute("statutsPlace", StatutPlace.values());
        return CrudAjax.estAjax(request) ? "places/formulaire :: formContent" : "places/formulaire";
    }

    @PostMapping("/modifier/{id}")
    public Object modifier(
            @PathVariable Long id,
            @Valid @ModelAttribute("place") PlaceRequestDTO dto,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {

        boolean ajax = CrudAjax.estAjax(request);

        if (result.hasErrors()) {
            model.addAttribute("id", id);
            model.addAttribute("typesPlace", TypePlace.values());
            model.addAttribute("statutsPlace", StatutPlace.values());
            if (ajax) {
                response.setStatus(HttpStatus.UNPROCESSABLE_ENTITY.value());
                return "places/formulaire :: formContent";
            }
            return "places/formulaire";
        }

        try {
            service.modifier(id, dto);
        } catch (Exception e) {
            if (ajax) {
                return CrudAjax.erreur(e);
            }
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/places";
        }

        if (ajax) {
            return CrudAjax.succes("Place modifiée avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Place modifiée avec succès");
        return "redirect:/places";
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
            return "redirect:/places";
        }

        if (ajax) {
            return CrudAjax.succes("Place supprimée avec succès");
        }
        redirectAttributes.addFlashAttribute("success", "Place supprimée avec succès");
        return "redirect:/places";
    }

    @GetMapping("/plan")
    public String planParking(Model model) {
        model.addAttribute("placesLibres", service.compterParStatut(StatutPlace.LIBRE));
        model.addAttribute("placesOccupees", service.compterParStatut(StatutPlace.OCCUPEE));
        model.addAttribute("placesReservees", service.compterParStatut(StatutPlace.RESERVEE));
        model.addAttribute("placesHorsService", service.compterParStatut(StatutPlace.HORS_SERVICE));
        model.addAttribute("toutesLesPlaces", service.trouverTous(null, null, null, null,
                PageRequest.of(0, 1000)).getContent());
        return "places/plan";
    }

    @GetMapping("/disponibles-consultation")
    public String disponiblesConsultation(Model model) {
        List<PlaceResponseDTO> disponibles = service.trouverDisponiblesParType(null);
        model.addAttribute("placesDisponibles", disponibles);
        model.addAttribute("estUSER", true);
        return "places/disponibles-consultation";
    }
}
