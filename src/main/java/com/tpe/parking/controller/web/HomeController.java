package com.tpe.parking.controller.web;

import com.tpe.parking.dto.response.DashboardStatsDto;
import com.tpe.parking.service.DashboardService;
import com.tpe.parking.service.EntreeSortieService;
import com.tpe.parking.service.PlaceService;
import com.tpe.parking.service.ReservationService;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.StatutReservation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    private final PlaceService placeService;
    private final ReservationService reservationService;
    private final EntreeSortieService entreeSortieService;
    private final DashboardService dashboardService;

    public HomeController(PlaceService placeService, ReservationService reservationService,
                          EntreeSortieService entreeSortieService, DashboardService dashboardService) {
        this.placeService = placeService;
        this.reservationService = reservationService;
        this.entreeSortieService = entreeSortieService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String accueil(HttpServletRequest request) {
        /* Landing publique pour les visiteurs ; les utilisateurs connectés
           vont directement à leur tableau de bord. */
        if (request.getUserPrincipal() != null) {
            return "redirect:/accueil";
        }
        return "landing";
    }

    @GetMapping("/accueil")
    public String pageAccueil(Model model, HttpServletRequest request) {
        model.addAttribute("placesLibres", placeService.compterParStatut(StatutPlace.LIBRE));
        model.addAttribute("placesOccupees", placeService.compterParStatut(StatutPlace.OCCUPEE));
        model.addAttribute("reservationsActives", reservationService.trouverTous(null, StatutReservation.CONFIRMEE, null,
                org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements());
        model.addAttribute("enStationnement", entreeSortieService.compterEnCours());
        model.addAttribute("stats", dashboardService.construire(avecRevenus(request)));
        return "accueil";
    }

    /** JSON rafraîchi par /js/dashboard.js ; mêmes droits que la page. */
    @GetMapping("/accueil/stats")
    @ResponseBody
    public DashboardStatsDto stats(HttpServletRequest request) {
        return dashboardService.construire(avecRevenus(request));
    }

    /** Montants réservés aux rôles autorisés sur la facturation (aligné /api/parking/revenue). */
    private boolean avecRevenus(HttpServletRequest request) {
        return request.isUserInRole("ADMIN") || request.isUserInRole("MANAGER") || request.isUserInRole("OPERATOR");
    }

    /* La page de connexion n'est plus servie : GET /login renvoie vers la
       landing avec les paramètres ?login… que /js/login-modal.js traduit en
       ouverture de modale. Le POST /login reste géré par Spring Security. */
    @GetMapping("/login")
    public String login(HttpServletRequest request) {
        if (request.getUserPrincipal() != null) {
            return "redirect:/accueil";
        }
        if (request.getParameter("error") != null) {
            return "redirect:/?login&error";
        }
        if (request.getParameter("logout") != null) {
            return "redirect:/?login&logout";
        }
        if (request.getParameter("expired") != null) {
            return "redirect:/?login&expired";
        }
        return "redirect:/?login";
    }

    // Toutes les méthodes : Spring Security forward vers /403 avec la méthode
    // d'origine (POST sur une action refusée), qui sinon renverrait un 405.
    @RequestMapping("/403")
    public String accesRefuse() {
        return "403";
    }
}
