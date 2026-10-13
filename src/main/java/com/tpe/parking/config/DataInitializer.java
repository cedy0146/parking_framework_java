package com.tpe.parking.config;

import com.tpe.parking.entity.*;
import com.tpe.parking.enumeration.*;
import com.tpe.parking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Order(0)
@Component
public class DataInitializer implements CommandLineRunner {

    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final PlaceRepository placeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UtilisateurRepository utilisateurRepository,
                            VehiculeRepository vehiculeRepository,
                            PlaceRepository placeRepository,
                            PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.placeRepository = placeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) {
            return;
        }

        Utilisateur admin = new Utilisateur("Admin", "Super", "admin@parking.com",
                passwordEncoder.encode("admin123"), RoleUtilisateur.ADMIN);
        admin.setTelephone("0600000001");
        utilisateurRepository.save(admin);

        Utilisateur manager = new Utilisateur("Manager", "Jean", "manager@parking.com",
                passwordEncoder.encode("manager123"), RoleUtilisateur.MANAGER);
        manager.setTelephone("0600000002");
        utilisateurRepository.save(manager);

        Utilisateur operator = new Utilisateur("Operator", "Pierre", "operator@parking.com",
                passwordEncoder.encode("operator123"), RoleUtilisateur.OPERATOR);
        operator.setTelephone("0600000003");
        utilisateurRepository.save(operator);

        Utilisateur user1 = new Utilisateur("Dupont", "Marie", "user@parking.com",
                passwordEncoder.encode("user123"), RoleUtilisateur.USER);
        user1.setTelephone("0600000004");
        utilisateurRepository.save(user1);

        Vehicule v1 = new Vehicule("AA-123-BB", "Renault", "Clio", admin);
        v1.setCouleur("Rouge");
        v1.setTypeVehicule("VOITURE");
        vehiculeRepository.save(v1);

        Vehicule v2 = new Vehicule("CC-456-DD", "Peugeot", "208", manager);
        v2.setCouleur("Bleu");
        v2.setTypeVehicule("VOITURE");
        vehiculeRepository.save(v2);

        Vehicule v3 = new Vehicule("EE-789-FF", "Yamaha", "MT-07", user1);
        v3.setCouleur("Noir");
        v3.setTypeVehicule("MOTO");
        vehiculeRepository.save(v3);

        String[] zones = {"A", "B", "C"};
        int compteur = 1;
        for (String zone : zones) {
            for (int i = 1; i <= 5; i++) {
                Place place = new Place(zone + String.format("%02d", compteur), zone, TypePlace.STANDARD);
                placeRepository.save(place);
                compteur++;
            }
        }

        for (int i = 1; i <= 3; i++) {
            Place place = new Place("PMR" + String.format("%02d", i), "P", TypePlace.PMR);
            placeRepository.save(place);
        }

        for (int i = 1; i <= 2; i++) {
            Place place = new Place("VIP" + String.format("%02d", i), "V", TypePlace.VIP);
            placeRepository.save(place);
        }

        for (int i = 1; i <= 2; i++) {
            Place place = new Place("AB" + String.format("%02d", i), "A", TypePlace.ABONNE);
            placeRepository.save(place);
        }
    }
}
