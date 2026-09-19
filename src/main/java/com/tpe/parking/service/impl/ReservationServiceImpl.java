package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.entity.Place;
import com.tpe.parking.entity.Reservation;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.entity.Vehicule;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.enumeration.TypePlace;
import com.tpe.parking.exception.BusinessException;
import com.tpe.parking.exception.ConflictException;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.exception.ResourceNotFoundException;
import com.tpe.parking.mapper.ReservationMapper;
import com.tpe.parking.repository.PlaceRepository;
import com.tpe.parking.repository.ReservationRepository;
import com.tpe.parking.repository.UtilisateurRepository;
import com.tpe.parking.repository.VehiculeRepository;
import com.tpe.parking.service.ReservationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ReservationServiceImpl implements ReservationService {

    private final ReservationRepository repository;
    private final UtilisateurRepository utilisateurRepository;
    private final VehiculeRepository vehiculeRepository;
    private final PlaceRepository placeRepository;

    public ReservationServiceImpl(ReservationRepository repository,
                                   UtilisateurRepository utilisateurRepository,
                                   VehiculeRepository vehiculeRepository,
                                   PlaceRepository placeRepository) {
        this.repository = repository;
        this.utilisateurRepository = utilisateurRepository;
        this.vehiculeRepository = vehiculeRepository;
        this.placeRepository = placeRepository;
    }

    @Override
    public ReservationResponseDTO creer(ReservationRequestDTO dto) {
        if (dto.getDateHeureArriveePrevue().isAfter(dto.getDateHeureDepartPrevue())) {
            throw new BusinessException("La date d'arrivée doit être antérieure à la date de départ");
        }

        if (dto.getDateHeureArriveePrevue().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("La date d'arrivée ne peut pas être dans le passé");
        }

        Utilisateur utilisateur = utilisateurRepository.findById(dto.getUtilisateurId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", dto.getUtilisateurId()));

        Vehicule vehicule = vehiculeRepository.findById(dto.getVehiculeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicule", dto.getVehiculeId()));

        Place place;
        if (dto.getPlaceId() != null) {
            place = placeRepository.findById(dto.getPlaceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Place", dto.getPlaceId()));

            if (place.getStatut() != StatutPlace.LIBRE) {
                throw new BusinessException("La place " + place.getNumero() + " n'est pas disponible");
            }
        } else {
            place = attribuerPlaceAutomatique(utilisateur, dto.getDateHeureArriveePrevue(), dto.getDateHeureDepartPrevue());
        }

        verifierChevauchement(place.getId(), dto.getDateHeureArriveePrevue(), dto.getDateHeureDepartPrevue(), null);

        Reservation reservation = ReservationMapper.toEntity(dto);
        reservation.setUtilisateur(utilisateur);
        reservation.setVehicule(vehicule);
        reservation.setPlace(place);
        reservation.setStatut(StatutReservation.CONFIRMEE);

        place.setStatut(StatutPlace.RESERVEE);
        placeRepository.save(place);

        reservation = repository.save(reservation);
        return ReservationMapper.toResponse(reservation);
    }

    @Override
    @Transactional(readOnly = true)
    public ReservationResponseDTO recupererParId(Long id) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        return ReservationMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReservationResponseDTO> trouverTous(Long utilisateurId, StatutReservation statut, Long placeId, Pageable pageable) {
        return repository.rechercher(utilisateurId, statut, placeId, pageable)
                .map(ReservationMapper::toResponse);
    }

    @Override
    public ReservationResponseDTO confirmer(Long id) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (entity.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new InvalidOperationException("Seules les réservations en attente peuvent être confirmées");
        }

        entity.setStatut(StatutReservation.CONFIRMEE);
        entity = repository.save(entity);
        return ReservationMapper.toResponse(entity);
    }

    @Override
    public ReservationResponseDTO annuler(Long id) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (entity.getStatut() == StatutReservation.ANNULEE || entity.getStatut() == StatutReservation.TERMINEE) {
            throw new InvalidOperationException("Cette réservation ne peut plus être annulée");
        }

        entity.setStatut(StatutReservation.ANNULEE);

        if (entity.getPlace() != null) {
            Place place = entity.getPlace();
            boolean aUneAutreReservation = repository.trouverChevauchements(
                    place.getId(),
                    entity.getDateHeureArriveePrevue(),
                    entity.getDateHeureDepartPrevue()
            ).stream().anyMatch(r -> !r.getId().equals(id) && r.getStatut() != StatutReservation.ANNULEE);

            if (!aUneAutreReservation) {
                place.setStatut(StatutPlace.LIBRE);
                placeRepository.save(place);
            }
        }

        entity = repository.save(entity);
        return ReservationMapper.toResponse(entity);
    }

    @Override
    public ReservationResponseDTO terminer(Long id) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));

        if (entity.getStatut() != StatutReservation.CONFIRMEE) {
            throw new InvalidOperationException("Seules les réservations confirmées peuvent être terminées");
        }

        entity.setStatut(StatutReservation.TERMINEE);

        if (entity.getPlace() != null) {
            Place place = entity.getPlace();
            boolean aUneAutreReservation = repository.trouverChevauchements(
                    place.getId(),
                    java.time.LocalDateTime.now().minusMinutes(1),
                    java.time.LocalDateTime.now().plusMinutes(1)
            ).stream().anyMatch(r -> !r.getId().equals(id) && r.getStatut() == StatutReservation.CONFIRMEE);

            if (!aUneAutreReservation) {
                place.setStatut(StatutPlace.LIBRE);
                placeRepository.save(place);
            }
        }

        entity = repository.save(entity);
        return ReservationMapper.toResponse(entity);
    }

    @Override
    public void supprimer(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Reservation", id);
        }
        repository.deleteById(id);
    }

    @Override
    public ReservationResponseDTO confirmerSiProprietaire(Long id, Long utilisateurId) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        verifierProprietaire(entity, utilisateurId);
        return confirmer(id);
    }

    @Override
    public ReservationResponseDTO annulerSiProprietaire(Long id, Long utilisateurId) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        verifierProprietaire(entity, utilisateurId);
        return annuler(id);
    }

    @Override
    public ReservationResponseDTO terminerSiProprietaire(Long id, Long utilisateurId) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        verifierProprietaire(entity, utilisateurId);
        return terminer(id);
    }

    @Override
    public void supprimerSiProprietaire(Long id, Long utilisateurId) {
        Reservation entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reservation", id));
        verifierProprietaire(entity, utilisateurId);
        supprimer(id);
    }

    private void verifierProprietaire(Reservation entity, Long utilisateurId) {
        if (!entity.getUtilisateur().getId().equals(utilisateurId)) {
            throw new InvalidOperationException("Vous n'êtes pas autorisé à modifier cette réservation");
        }
    }

    private Place attribuerPlaceAutomatique(Utilisateur utilisateur,
                                             java.time.LocalDateTime dateDebut,
                                             java.time.LocalDateTime dateFin) {
        TypePlace typeRequis;

        switch (utilisateur.getRole()) {
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
            throw new BusinessException("Aucune place disponible pour le créneau demandé");
        }

        Place placeChoisie = placesDisponibles.get(0);

        for (Place place : placesDisponibles) {
            List<Reservation> chevauchements = repository.trouverChevauchements(
                    place.getId(), dateDebut, dateFin);
            if (chevauchements.isEmpty()) {
                return place;
            }
        }

        throw new BusinessException("Aucune place disponible sans chevauchement pour le créneau demandé");
    }

    private void verifierChevauchement(Long placeId, java.time.LocalDateTime dateDebut,
                                         java.time.LocalDateTime dateFin, Long excludeId) {
        List<Reservation> chevauchements;
        if (excludeId != null) {
            chevauchements = repository.trouverChevauchementsExcluant(placeId, dateDebut, dateFin, excludeId);
        } else {
            chevauchements = repository.trouverChevauchements(placeId, dateDebut, dateFin);
        }

        if (!chevauchements.isEmpty()) {
            throw new ConflictException("La place est déjà réservée pour ce créneau horaire");
        }
    }
}
