package com.tpe.parking.config;

import com.tpe.parking.entity.*;
import com.tpe.parking.enumeration.*;
import com.tpe.parking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Random;

/**
 * Jeu de démonstration (--spring.profiles.active=demo) : historique de
 * mouvements sur 7 jours, réservations et abonnements pour animer le
 * tableau de bord (graphiques, listes, revenus).
 */
@Profile("demo")
@Order(1)
@Component
public class DemoDataSeeder implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final PlaceRepository placeRepository;
    private final EntreeSortieRepository entreeSortieRepository;
    private final ReservationRepository reservationRepository;
    private final AbonnementRepository abonnementRepository;

    public DemoDataSeeder(UtilisateurRepository utilisateurRepository, VehiculeRepository vehiculeRepository,
                          PlaceRepository placeRepository, EntreeSortieRepository entreeSortieRepository,
                          ReservationRepository reservationRepository, AbonnementRepository abonnementRepository) {
        this.utilisateurRepository = utilisateurRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.placeRepository = placeRepository;
        this.entreeSortieRepository = entreeSortieRepository;
        this.reservationRepository = reservationRepository;
        this.abonnementRepository = abonnementRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.count() == 0 || entreeSortieRepository.count() > 0) {
            return;
        }
        Random alea = new Random(20261011);
        List<Utilisateur> utilisateurs = utilisateurRepository.findAll();
        List<Vehicule> vehicules = vehiculeRepository.findAll();
        List<Place> places = placeRepository.findAll(Sort.by("numero"));

        if (vehicules.size() < 3 || places.size() < 6) {
            return;
        }

        /* --- Mouvements sur les 7 derniers jours (entrées + sorties facturées) --- */
        LocalDateTime maintenant = LocalDateTime.now();
        for (int joursAvant = 6; joursAvant >= 1; joursAvant--) {
            int mouvementsDuJour = 2 + alea.nextInt(4);
            for (int i = 0; i < mouvementsDuJour; i++) {
                Place place = places.get(alea.nextInt(places.size()));
                Vehicule vehicule = vehicules.get(alea.nextInt(vehicules.size()));
                LocalDateTime entree = maintenant.minusDays(joursAvant)
                        .with(LocalTime.of(7 + alea.nextInt(12), alea.nextInt(60)));
                LocalDateTime sortie = entree.plusHours(1 + alea.nextInt(6)).plusMinutes(alea.nextInt(55));
                if (sortie.isAfter(maintenant)) {
                    sortie = maintenant.minusHours(1 + alea.nextInt(3));
                }
                EntreeSortie mouvement = new EntreeSortie(place, vehicule);
                mouvement.setDateHeureEntree(entree);
                mouvement.setDateHeureSortie(sortie);
                int heures = Math.max(1, (int) java.time.Duration.between(entree, sortie).toHours());
                mouvement.setMontantPaye(BigDecimal.valueOf(heures * (100 + alea.nextInt(4) * 50)));
                place.setStatut(StatutPlace.LIBRE);
                entreeSortieRepository.save(mouvement);
            }
        }

        /* --- Jour courant : un véhicule en stationnement + deux sorties facturées --- */
        Place placeEnCours = places.get(0);
        EntreeSortie enCours = new EntreeSortie(placeEnCours, vehicules.get(0));
        enCours.setDateHeureEntree(maintenant.minusHours(2).minusMinutes(15));
        placeEnCours.setStatut(StatutPlace.OCCUPEE);
        entreeSortieRepository.save(enCours);

        for (int i = 0; i < 2; i++) {
            Place place = places.get(1 + i);
            Vehicule vehicule = vehicules.get((i + 1) % vehicules.size());
            LocalDateTime entree = maintenant.minusHours(5 - i).minusMinutes(30);
            LocalDateTime sortie = maintenant.minusMinutes(20 + i * 25);
            EntreeSortie mouvement = new EntreeSortie(place, vehicule);
            mouvement.setDateHeureEntree(entree);
            mouvement.setDateHeureSortie(sortie);
            int heures = Math.max(1, (int) java.time.Duration.between(entree, sortie).toHours());
            mouvement.setMontantPaye(BigDecimal.valueOf(heures * 150));
            place.setStatut(StatutPlace.LIBRE);
            entreeSortieRepository.save(mouvement);
        }

        /* --- Réservations aux quatre statuts, sur créneaux futurs --- */
        StatutReservation[] statuts = {StatutReservation.CONFIRMEE, StatutReservation.CONFIRMEE,
                StatutReservation.EN_ATTENTE, StatutReservation.TERMINEE, StatutReservation.ANNULEE};
        for (int i = 0; i < statuts.length; i++) {
            Utilisateur client = utilisateurs.get(alea.nextInt(utilisateurs.size()));
            Vehicule vehicule = vehicules.get(alea.nextInt(vehicules.size()));
            Place place = places.get(alea.nextInt(places.size()));
            LocalDateTime arrivee = maintenant.plusHours(2 + i * 6).withMinute(0).withSecond(0).withNano(0);
            Reservation reservation = new Reservation(arrivee, arrivee.plusHours(2), client, vehicule);
            reservation.setPlace(place);
            reservation.setStatut(statuts[i]);
            reservation.setDateCreation(maintenant.minusDays(alea.nextInt(5)).minusHours(alea.nextInt(10)));
            if (statuts[i] == StatutReservation.CONFIRMEE && place.getStatut() == StatutPlace.LIBRE) {
                place.setStatut(StatutPlace.RESERVEE);
            }
            reservationRepository.save(reservation);
        }

        /* --- Abonnements actifs et expirés --- */
        Abonnement actif = new Abonnement(TypeAbonnement.MENSUEL, LocalDate.now().minusDays(10),
                LocalDate.now().plusDays(20), new BigDecimal("45000"),
                utilisateurs.get(utilisateurs.size() - 1), vehicules.get(vehicules.size() - 1));
        abonnementRepository.save(actif);

        Abonnement expire = new Abonnement(TypeAbonnement.HEBDOMADAIRE, LocalDate.now().minusDays(20),
                LocalDate.now().minusDays(13), new BigDecimal("12000"),
                utilisateurs.get(0), vehicules.get(0));
        abonnementRepository.save(expire);
    }
}
