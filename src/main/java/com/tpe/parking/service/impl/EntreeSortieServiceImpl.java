package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.EntreeSortieRequestDTO;
import com.tpe.parking.dto.response.EntreeSortieResponseDTO;
import com.tpe.parking.dto.response.TarificationResponseDTO;
import com.tpe.parking.entity.*;
import com.tpe.parking.enumeration.RoleUtilisateur;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import com.tpe.parking.exception.BusinessException;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.exception.ResourceNotFoundException;
import com.tpe.parking.mapper.EntreeSortieMapper;
import com.tpe.parking.repository.*;
import com.tpe.parking.service.EntreeSortieService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class EntreeSortieServiceImpl implements EntreeSortieService {

    private final EntreeSortieRepository repository;
    private final VehiculeRepository vehiculeRepository;
    private final PlaceRepository placeRepository;
    private final AbonnementRepository abonnementRepository;

    private static final BigDecimal TARIF_STANDARD_HEURE = new BigDecimal("2.00");
    private static final BigDecimal TARIF_PMR_HEURE = new BigDecimal("1.50");
    private static final BigDecimal TARIF_VIP_HEURE = new BigDecimal("4.00");
    private static final BigDecimal TARIF_ABONNE_HEURE = new BigDecimal("1.50");
    private static final BigDecimal TARIF_MINIMUM = new BigDecimal("1.00");
    private static final BigDecimal MAJORATION_DEPASSEMENT = new BigDecimal("1.50");
    private static final BigDecimal REMISE_ABONNE = new BigDecimal("0.20");
    private static final BigDecimal FORFAIT_JOURNALIER = new BigDecimal("15.00");

    public EntreeSortieServiceImpl(EntreeSortieRepository repository,
                                    VehiculeRepository vehiculeRepository,
                                    PlaceRepository placeRepository,
                                    AbonnementRepository abonnementRepository) {
        this.repository = repository;
        this.vehiculeRepository = vehiculeRepository;
        this.placeRepository = placeRepository;
        this.abonnementRepository = abonnementRepository;
    }

    @Override
    public EntreeSortieResponseDTO enregistrerEntree(EntreeSortieRequestDTO dto) {
        Vehicule vehicule = vehiculeRepository.findById(dto.getVehiculeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", dto.getVehiculeId()));

        repository.findByVehiculeIdAndDateHeureSortieIsNull(vehicule.getId())
                .ifPresent(e -> {
                    throw new BusinessException("Ce véhicule est déjà stationné (place " + e.getPlace().getNumero() + ")");
                });

        Place place;
        if (dto.getPlaceId() != null) {
            place = placeRepository.findById(dto.getPlaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Place", dto.getPlaceId()));
        } else {
            place = attribuerPlaceAutomatique(vehicule);
        }

        if (place.getStatut() != StatutPlace.LIBRE) {
            throw new BusinessException("La place " + place.getNumero() + " n'est pas disponible");
        }

        EntreeSortie entreeSortie = new EntreeSortie(place, vehicule);
        entreeSortie.setDateHeureEntree(dto.getDateHeureEntree() != null ? dto.getDateHeureEntree() : LocalDateTime.now());

        place.setStatut(StatutPlace.OCCUPEE);
        placeRepository.save(place);

        entreeSortie = repository.save(entreeSortie);
        return EntreeSortieMapper.toResponse(entreeSortie);
    }

    @Override
    public EntreeSortieResponseDTO enregistrerSortie(Long vehiculeId) {
        EntreeSortie entreeSortie = repository.findByVehiculeIdAndDateHeureSortieIsNull(vehiculeId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée en cours pour le véhicule", vehiculeId));

        entreeSortie.setDateHeureSortie(LocalDateTime.now());

        BigDecimal montant = calculerMontant(entreeSortie);
        entreeSortie.setMontantPaye(montant);

        Place place = entreeSortie.getPlace();
        place.setStatut(StatutPlace.LIBRE);
        placeRepository.save(place);

        entreeSortie = repository.save(entreeSortie);
        return EntreeSortieMapper.toResponse(entreeSortie);
    }

    @Override
    @Transactional(readOnly = true)
    public EntreeSortieResponseDTO recupererParId(Long id) {
        EntreeSortie entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EntreeSortie", id));
        return EntreeSortieMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public EntreeSortieResponseDTO recupererEntreeEnCours(Long vehiculeId) {
        EntreeSortie entity = repository.findByVehiculeIdAndDateHeureSortieIsNull(vehiculeId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrée en cours pour le véhicule", vehiculeId));
        return EntreeSortieMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EntreeSortieResponseDTO> trouverTous(Long vehiculeId, Long placeId, Boolean enCours, Pageable pageable) {
        return repository.rechercher(vehiculeId, placeId, enCours, pageable)
                .map(EntreeSortieMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public TarificationResponseDTO calculerTarification(Long entreeSortieId) {
        EntreeSortie entreeSortie = repository.findById(entreeSortieId)
                .orElseThrow(() -> new ResourceNotFoundException("EntreeSortie", entreeSortieId));
        return calculerTarificationPourEntreeSortie(entreeSortie);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerRevenuPeriode(LocalDateTime debut, LocalDateTime fin) {
        return repository.calculerRevenuPeriode(debut, fin);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculerRevenuTotal() {
        return repository.calculerRevenuTotal();
    }

    @Override
    @Transactional(readOnly = true)
    public long compterEnCours() {
        return repository.countByDateHeureSortieIsNull();
    }

    private BigDecimal calculerMontant(EntreeSortie entreeSortie) {
        long minutes = Duration.between(entreeSortie.getDateHeureEntree(), entreeSortie.getDateHeureSortie()).toMinutes();
        BigDecimal heures = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 4, RoundingMode.CEILING);

        Place place = entreeSortie.getPlace();
        Vehicule vehicule = entreeSortie.getVehicule();
        RoleUtilisateur role = vehicule.getUtilisateur().getRole();

        BigDecimal tarifHoraire = getTarifHoraire(place.getType(), role);

        boolean aAbonnement = abonnementRepository.trouverActifPourDate(
                vehicule.getId(), entreeSortie.getDateHeureEntree().toLocalDate()).isPresent();

        BigDecimal montant = tarifHoraire.multiply(heures).setScale(2, RoundingMode.CEILING);

        if (montant.compareTo(TARIF_MINIMUM) < 0) {
            montant = TARIF_MINIMUM;
        }

        if (aAbonnement) {
            BigDecimal remise = montant.multiply(REMISE_ABONNE).setScale(2, RoundingMode.CEILING);
            montant = montant.subtract(remise);
        }

        return montant;
    }

    private BigDecimal getTarifHoraire(TypePlace typePlace, RoleUtilisateur role) {
        if (role == RoleUtilisateur.ADMIN || role == RoleUtilisateur.MANAGER) {
            return TARIF_VIP_HEURE;
        }
        return switch (typePlace) {
            case VIP -> TARIF_VIP_HEURE;
            case PMR -> TARIF_PMR_HEURE;
            case ABONNE -> TARIF_ABONNE_HEURE;
            default -> TARIF_STANDARD_HEURE;
        };
    }

    private Place attribuerPlaceAutomatique(Vehicule vehicule) {
        RoleUtilisateur role = vehicule.getUtilisateur().getRole();
        TypePlace typeRequis;

        switch (role) {
            case ADMIN:
            case MANAGER:
                typeRequis = TypePlace.VIP;
                break;
            case OPERATOR:
                typeRequis = TypePlace.ABONNE;
                break;
            default:
                typeRequis = TypePlace.STANDARD;
                break;
        }

        List<Place> placesDisponibles = placeRepository.trouverDisponiblesParType(StatutPlace.LIBRE, typeRequis);

        if (placesDisponibles.isEmpty() && typeRequis != TypePlace.STANDARD) {
            placesDisponibles = placeRepository.trouverDisponiblesParType(StatutPlace.LIBRE, TypePlace.STANDARD);
        }

        if (placesDisponibles.isEmpty()) {
            throw new BusinessException("Aucune place disponible pour ce véhicule");
        }

        return placesDisponibles.get(0);
    }

    private TarificationResponseDTO calculerTarificationPourEntreeSortie(EntreeSortie entreeSortie) {
        TarificationResponseDTO dto = new TarificationResponseDTO();
        dto.setEntreeSortieId(entreeSortie.getId());
        dto.setPlaceNumero(entreeSortie.getPlace().getNumero());
        dto.setVehiculeImmatriculation(entreeSortie.getVehicule().getImmatriculation());

        LocalDateTime dateSortie = entreeSortie.getDateHeureSortie() != null ?
                entreeSortie.getDateHeureSortie() : LocalDateTime.now();
        long minutes = Duration.between(entreeSortie.getDateHeureEntree(), dateSortie).toMinutes();
        dto.setDureeMinutes(minutes);

        long heures = minutes / 60;
        long mins = minutes % 60;
        dto.setDureeFormatee(heures + "h" + (mins > 0 ? String.format("%02d", mins) : "00"));

        Place place = entreeSortie.getPlace();
        Vehicule vehicule = entreeSortie.getVehicule();
        RoleUtilisateur role = vehicule.getUtilisateur().getRole();
        BigDecimal tarifHoraire = getTarifHoraire(place.getType(), role);
        dto.setTarifHoraire(tarifHoraire);

        boolean aAbonnement = abonnementRepository.trouverActifPourDate(
                vehicule.getId(), entreeSortie.getDateHeureEntree().toLocalDate()).isPresent();
        dto.setAAbonnement(aAbonnement);

        BigDecimal montant = calculerMontant(entreeSortie);
        dto.setMontantTotal(montant);

        StringBuilder details = new StringBuilder();
        details.append("Durée: ").append(dto.getDureeFormatee());
        details.append(" | Tarif horaire: ").append(tarifHoraire).append(" Ar");
        if (aAbonnement) {
            details.append(" | Abonné: -20%");
        }
        details.append(" | Montant: ").append(montant).append(" Ar");
        dto.setDetailsCalcul(details.toString());

        return dto;
    }
}
