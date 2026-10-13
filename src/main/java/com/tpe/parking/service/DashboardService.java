package com.tpe.parking.service;

import com.tpe.parking.dto.response.DashboardStatsDto;
import com.tpe.parking.dto.response.DashboardStatsDto.*;
import com.tpe.parking.entity.Abonnement;
import com.tpe.parking.entity.EntreeSortie;
import com.tpe.parking.entity.Reservation;
import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.repository.AbonnementRepository;
import com.tpe.parking.repository.EntreeSortieRepository;
import com.tpe.parking.repository.PlaceRepository;
import com.tpe.parking.repository.ReservationRepository;
import com.tpe.parking.web.Labels;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Agrégats du tableau de bord : lecture seule, sans N+1, calculés
 * par requête d'agrégation ou en Java sur des volumes bornés.
 * Les libellés d'affichage viennent de web.Labels (valeurs d'énumération
 * jamais exposées telles quelles à l'écran).
 */
@Service
public class DashboardService {

    private static final int MAX_PLANS_PAR_ZONE = 60;
    private static final DateTimeFormatter LIBELLE_JOUR = DateTimeFormatter.ofPattern("dd/MM");

    private final PlaceRepository placeRepository;
    private final ReservationRepository reservationRepository;
    private final AbonnementRepository abonnementRepository;
    private final EntreeSortieRepository entreeSortieRepository;
    private final Labels labels;

    public DashboardService(PlaceRepository placeRepository, ReservationRepository reservationRepository,
                            AbonnementRepository abonnementRepository, EntreeSortieRepository entreeSortieRepository,
                            Labels labels) {
        this.placeRepository = placeRepository;
        this.reservationRepository = reservationRepository;
        this.abonnementRepository = abonnementRepository;
        this.entreeSortieRepository = entreeSortieRepository;
        this.labels = labels;
    }

    @Transactional(readOnly = true)
    public DashboardStatsDto construire(boolean avecRevenus) {
        Places places = compterPlaces();
        List<ZoneStat> parZone = compterParZone();
        Statuts reservations = compterReservations();
        Abonnements abonnements = compterAbonnements();
        Mouvements mouvements = compterMouvements();
        Revenus revenus = avecRevenus ? calculerRevenus() : null;
        Serie serie7j = construireSerie7j(avecRevenus);
        SerieLongue serieReservations7j = construireSerieReservations7j();
        List<ReservationRecente> reservationsRecentes = reservationsRecentes();
        List<MouvementRecent> mouvementsRecents = mouvementsRecents();
        List<ZonePlan> planParZone = planParZone();

        return new DashboardStatsDto(places, parZone, reservations, abonnements, mouvements, revenus,
                serie7j, serieReservations7j, reservationsRecentes, mouvementsRecents, planParZone);
    }

    private Places compterPlaces() {
        long libres = placeRepository.countByStatut(StatutPlace.LIBRE);
        long reservees = placeRepository.countByStatut(StatutPlace.RESERVEE);
        long occupees = placeRepository.countByStatut(StatutPlace.OCCUPEE);
        long horsService = placeRepository.countByStatut(StatutPlace.HORS_SERVICE);
        long total = libres + reservees + occupees + horsService;
        double taux = total > 0 ? Math.round(occupees * 1000.0 / total) / 10.0 : 0.0;
        return new Places(libres, reservees, occupees, horsService, total, taux);
    }

    private List<ZoneStat> compterParZone() {
        Map<String, Map<StatutPlace, Long>> parZone = new TreeMap<>();
        for (Object[] ligne : placeRepository.compterParZoneEtStatut()) {
            String zone = (String) ligne[0];
            StatutPlace statut = (StatutPlace) ligne[1];
            long nombre = (Long) ligne[2];
            parZone.computeIfAbsent(zone, z -> new EnumMap<>(StatutPlace.class)).put(statut, nombre);
        }
        List<ZoneStat> resultats = new ArrayList<>();
        parZone.forEach((zone, statuts) -> {
            long libres = statuts.getOrDefault(StatutPlace.LIBRE, 0L);
            long reservees = statuts.getOrDefault(StatutPlace.RESERVEE, 0L);
            long occupees = statuts.getOrDefault(StatutPlace.OCCUPEE, 0L);
            long horsService = statuts.getOrDefault(StatutPlace.HORS_SERVICE, 0L);
            resultats.add(new ZoneStat(zone, labels.zone(zone), libres, reservees, occupees, horsService,
                    libres + reservees + occupees + horsService));
        });
        return resultats;
    }

    private Statuts compterReservations() {
        return new Statuts(
                reservationRepository.countByStatut(StatutReservation.EN_ATTENTE),
                reservationRepository.countByStatut(StatutReservation.CONFIRMEE),
                reservationRepository.countByStatut(StatutReservation.ANNULEE),
                reservationRepository.countByStatut(StatutReservation.TERMINEE));
    }

    private Abonnements compterAbonnements() {
        return new Abonnements(
                abonnementRepository.countByStatut(StatutAbonnement.ACTIF),
                abonnementRepository.countByStatut(StatutAbonnement.EXPIRE),
                abonnementRepository.countByStatut(StatutAbonnement.RESILIE));
    }

    private Mouvements compterMouvements() {
        LocalDateTime debutJour = LocalDate.now().atStartOfDay();
        LocalDateTime hierMemeHeure = LocalDateTime.now().minusDays(1);
        return new Mouvements(
                entreeSortieRepository.countByDateHeureEntreeAfter(debutJour),
                entreeSortieRepository.countByDateHeureSortieAfter(debutJour),
                entreeSortieRepository.countByDateHeureSortieIsNull(),
                entreeSortieRepository.compterEnStationnementA(hierMemeHeure));
    }

    /** Aujourd'hui, vs hier, ce mois, moyenne par jour sur 30 jours. */
    private Revenus calculerRevenus() {
        LocalDate aujourdHui = LocalDate.now();
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime debutJour = aujourdHui.atStartOfDay();
        LocalDateTime debutHier = aujourdHui.minusDays(1).atStartOfDay();
        LocalDateTime debutMois = aujourdHui.with(TemporalAdjusters.firstDayOfMonth()).atStartOfDay();
        LocalDateTime debut30Jours = aujourdHui.minusDays(29).atStartOfDay();
        BigDecimal jour = entreeSortieRepository.calculerRevenuPeriode(debutJour, maintenant);
        BigDecimal hier = entreeSortieRepository.calculerRevenuPeriode(debutHier, debutJour);
        BigDecimal mois = entreeSortieRepository.calculerRevenuPeriode(debutMois, maintenant);
        BigDecimal total30 = entreeSortieRepository.calculerRevenuPeriode(debut30Jours, maintenant);
        BigDecimal moyenne30 = total30.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        return new Revenus(jour, hier, mois, moyenne30);
    }

    /** Série 7 jours glissants (aujourd'hui inclus) : entrées, sorties, revenus par jour. */
    private Serie construireSerie7j(boolean avecRevenus) {
        LocalDate fin = LocalDate.now();
        LocalDate debut = fin.minusDays(6);
        LocalDateTime debutTemps = debut.atStartOfDay();

        Map<LocalDate, Long> entreesParJour = new HashMap<>();
        for (EntreeSortie e : entreeSortieRepository.findByDateHeureEntreeGreaterThanEqual(debutTemps)) {
            entreesParJour.merge(e.getDateHeureEntree().toLocalDate(), 1L, Long::sum);
        }
        Map<LocalDate, Long> sortiesParJour = new HashMap<>();
        Map<LocalDate, BigDecimal> revenusParJour = new HashMap<>();
        if (avecRevenus) {
            for (EntreeSortie e : entreeSortieRepository.findByDateHeureSortieGreaterThanEqual(debutTemps)) {
                LocalDate jour = e.getDateHeureSortie().toLocalDate();
                sortiesParJour.merge(jour, 1L, Long::sum);
                if (e.getMontantPaye() != null) {
                    revenusParJour.merge(jour, e.getMontantPaye(), BigDecimal::add);
                }
            }
        } else {
            for (EntreeSortie e : entreeSortieRepository.findByDateHeureSortieGreaterThanEqual(debutTemps)) {
                sortiesParJour.merge(e.getDateHeureSortie().toLocalDate(), 1L, Long::sum);
            }
        }

        List<String> labels = new ArrayList<>();
        List<Long> entrees = new ArrayList<>();
        List<Long> sorties = new ArrayList<>();
        List<BigDecimal> revenus = avecRevenus ? new ArrayList<>() : null;
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            labels.add(jour.format(LIBELLE_JOUR));
            entrees.add(entreesParJour.getOrDefault(jour, 0L));
            sorties.add(sortiesParJour.getOrDefault(jour, 0L));
            if (revenus != null) {
                revenus.add(revenusParJour.getOrDefault(jour, BigDecimal.ZERO));
            }
        }
        return new Serie(labels, entrees, sorties, revenus);
    }

    /** Réservations créées par jour sur 7 jours, pour la sparkline de la carte dédiée. */
    private SerieLongue construireSerieReservations7j() {
        LocalDate fin = LocalDate.now();
        LocalDate debut = fin.minusDays(6);
        Map<LocalDate, Long> parJour = new HashMap<>();
        for (Reservation r : reservationRepository.findByDateCreationGreaterThanEqual(debut.atStartOfDay())) {
            parJour.merge(r.getDateCreation().toLocalDate(), 1L, Long::sum);
        }
        List<String> labels = new ArrayList<>();
        List<Long> valeurs = new ArrayList<>();
        for (LocalDate jour = debut; !jour.isAfter(fin); jour = jour.plusDays(1)) {
            labels.add(jour.format(LIBELLE_JOUR));
            valeurs.add(parJour.getOrDefault(jour, 0L));
        }
        return new SerieLongue(labels, valeurs);
    }

    private List<ReservationRecente> reservationsRecentes() {
        return reservationRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dateCreation")))
                .getContent().stream()
                .map(r -> new ReservationRecente(
                        r.getId(),
                        nomComplet(r),
                        r.getVehicule() != null ? r.getVehicule().getImmatriculation() : "-",
                        r.getPlace() != null ? r.getPlace().getNumero() : "-",
                        r.getStatut() != null ? r.getStatut().name() : "-",
                        labels.reservation(r.getStatut()),
                        labels.reservationCss(r.getStatut()),
                        r.getDateHeureArriveePrevue(),
                        labels.dateCourte(r.getDateHeureArriveePrevue())))
                .toList();
    }

    private static String nomComplet(Reservation r) {
        if (r.getUtilisateur() == null) {
            return "-";
        }
        String prenom = r.getUtilisateur().getPrenom() != null ? r.getUtilisateur().getPrenom() : "";
        String nom = r.getUtilisateur().getNom() != null ? r.getUtilisateur().getNom() : "";
        String complet = (prenom + " " + nom).trim();
        return complet.isEmpty() ? "-" : complet;
    }

    private List<MouvementRecent> mouvementsRecents() {
        return entreeSortieRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "dateHeureEntree")))
                .getContent().stream()
                .map(e -> new MouvementRecent(
                        e.getId(),
                        e.getVehicule() != null ? e.getVehicule().getImmatriculation() : "-",
                        e.getPlace() != null ? e.getPlace().getNumero() : "-",
                        e.getDateHeureEntree(),
                        labels.dateCourte(e.getDateHeureEntree()),
                        e.getDateHeureSortie(),
                        e.getDateHeureSortie() != null ? labels.dateCourte(e.getDateHeureSortie()) : null,
                        e.getMontantPaye(),
                        e.getMontantPaye() != null ? labels.montant(e.getMontantPaye()) : null,
                        e.getDateHeureSortie() == null))
                .toList();
    }

    /** Mini-plan groupé par zone parlante ; immatriculation si la place est occupée. */
    private List<ZonePlan> planParZone() {
        Map<String, String> immatsParPlace = new HashMap<>();
        for (EntreeSortie e : entreeSortieRepository.findByDateHeureSortieIsNull()) {
            if (e.getPlace() != null && e.getVehicule() != null) {
                immatsParPlace.putIfAbsent(String.valueOf(e.getPlace().getId()), e.getVehicule().getImmatriculation());
            }
        }
        Map<String, List<PlacePlan>> parZone = new TreeMap<>();
        placeRepository.findAll(Sort.by("numero")).forEach(p -> {
            String zone = p.getZone() != null ? p.getZone() : "-";
            List<PlacePlan> places = parZone.computeIfAbsent(zone, z -> new ArrayList<>());
            if (places.size() < MAX_PLANS_PAR_ZONE) {
                String statut = p.getStatut() != null ? p.getStatut().name() : "HORS_SERVICE";
                places.add(new PlacePlan(
                        p.getId(), p.getNumero(), zone, labels.zone(zone),
                        p.getType() != null ? p.getType().name() : "-",
                        labels.typePlace(p.getType()),
                        statut,
                        labels.place(p.getStatut()),
                        lettre(statut),
                        p.getStatut() == StatutPlace.OCCUPEE
                                ? immatsParPlace.getOrDefault(String.valueOf(p.getId()), null) : null));
            }
        });
        List<ZonePlan> resultats = new ArrayList<>();
        parZone.forEach((zone, places) -> resultats.add(new ZonePlan(zone, labels.zone(zone), places)));
        return resultats;
    }

    private static String lettre(String statut) {
        return switch (statut) {
            case "LIBRE" -> "L";
            case "OCCUPEE" -> "O";
            case "RESERVEE" -> "R";
            default -> "X";
        };
    }
}
