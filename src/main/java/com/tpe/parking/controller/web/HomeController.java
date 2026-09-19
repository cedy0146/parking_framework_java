package com.tpe.parking.controller.web;

import com.tpe.parking.service.EntreeSortieService;
import com.tpe.parking.service.PlaceService;
import com.tpe.parking.service.ReservationService;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.StatutReservation;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final PlaceService placeService;
    private final ReservationService reservationService;
    private final EntreeSortieService entreeSortieService;

    public HomeController(PlaceService placeService, ReservationService reservationService,
                          EntreeSortieService entreeSortieService) {
        this.placeService = placeService;
        this.reservationService = reservationService;
        this.entreeSortieService = entreeSortieService;
    }

    @GetMapping("/")
    public String accueil() {
        return "redirect:/accueil";
    }

    @GetMapping("/accueil")
    public String pageAccueil(Model model) {
        model.addAttribute("placesLibres", placeService.compterParStatut(StatutPlace.LIBRE));
        model.addAttribute("placesOccupees", placeService.compterParStatut(StatutPlace.OCCUPEE));
        model.addAttribute("reservationsActives", reservationService.trouverTous(null, StatutReservation.CONFIRMEE, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("enStationnement", entreeSortieService.compterEnCours());
        return "accueil";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/403")
    public String accesRefuse() {
        return "403";
    }
}
