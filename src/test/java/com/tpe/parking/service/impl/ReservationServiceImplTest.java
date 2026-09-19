package com.tpe.parking.service.impl;

import com.tpe.parking.dto.request.ReservationRequestDTO;
import com.tpe.parking.dto.response.ReservationResponseDTO;
import com.tpe.parking.entity.Place;
import com.tpe.parking.entity.Reservation;
import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.entity.Vehicule;
import com.tpe.parking.enumeration.RoleUtilisateur;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.enumeration.TypePlace;
import com.tpe.parking.exception.ConflictException;
import com.tpe.parking.exception.InvalidOperationException;
import com.tpe.parking.repository.PlaceRepository;
import com.tpe.parking.repository.ReservationRepository;
import com.tpe.parking.repository.UtilisateurRepository;
import com.tpe.parking.repository.VehiculeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceImplTest {

    private static final LocalDateTime ARRIVEE = LocalDateTime.now().plusDays(1).withSecond(0).withNano(0);
    private static final LocalDateTime DEPART = ARRIVEE.plusHours(2);

    @Mock private ReservationRepository reservationRepository;
    @Mock private UtilisateurRepository utilisateurRepository;
    @Mock private VehiculeRepository vehiculeRepository;
    @Mock private PlaceRepository placeRepository;

    @InjectMocks private ReservationServiceImpl service;

    @Test
    void creer_refuseLeCreneauQuandUneReservationSeChevauche() {
        ReservationRequestDTO request = demandePourPlace(3L);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur(1L)));
        when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(vehicule(2L)));
        when(placeRepository.findById(3L)).thenReturn(Optional.of(place(3L, StatutPlace.LIBRE)));
        when(reservationRepository.trouverChevauchements(3L, ARRIVEE, DEPART))
                .thenReturn(List.of(reservation(99L, StatutReservation.CONFIRMEE, 1L, 3L)));

        assertThatThrownBy(() -> service.creer(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà réservée");

        verify(reservationRepository).trouverChevauchements(3L, ARRIVEE, DEPART);
        verify(reservationRepository, never()).save(any(Reservation.class));
        verify(placeRepository, never()).save(any(Place.class));
    }

    @Test
    void creer_confirmeEtReserveLaPlaceQuandLeCreneauEstLibre() {
        ReservationRequestDTO request = demandePourPlace(3L);
        Place place = place(3L, StatutPlace.LIBRE);
        when(utilisateurRepository.findById(1L)).thenReturn(Optional.of(utilisateur(1L)));
        when(vehiculeRepository.findById(2L)).thenReturn(Optional.of(vehicule(2L)));
        when(placeRepository.findById(3L)).thenReturn(Optional.of(place));
        when(reservationRepository.trouverChevauchements(3L, ARRIVEE, DEPART)).thenReturn(List.of());
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        ReservationResponseDTO result = service.creer(request);

        ArgumentCaptor<Reservation> reservationCaptor = ArgumentCaptor.forClass(Reservation.class);
        verify(reservationRepository).save(reservationCaptor.capture());
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(reservationCaptor.getValue().getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        assertThat(reservationCaptor.getValue().getPlace()).isSameAs(place);
        assertThat(place.getStatut()).isEqualTo(StatutPlace.RESERVEE);
        verify(placeRepository).save(place);
    }

    @Test
    void confirmer_accepteUniquementUneReservationEnAttente() {
        Reservation reservation = reservation(10L, StatutReservation.EN_ATTENTE, 1L, 3L);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponseDTO result = service.confirmer(10L);

        assertThat(result.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
        verify(reservationRepository).save(reservation);
    }

    @Test
    void confirmer_refuseUneReservationAnnulee() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(10L, StatutReservation.ANNULEE, 1L, 3L)));

        assertThatThrownBy(() -> service.confirmer(10L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("en attente");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void annuler_libereLaPlaceSIlNyAPasDAutreReservationActive() {
        Reservation reservation = reservation(10L, StatutReservation.CONFIRMEE, 1L, 3L);
        Place place = reservation.getPlace();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.trouverChevauchements(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponseDTO result = service.annuler(10L);

        assertThat(result.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(place.getStatut()).isEqualTo(StatutPlace.LIBRE);
        verify(placeRepository).save(place);
    }

    @Test
    void terminer_libereLaPlaceQuandAucuneAutreReservationConfirmeeNExiste() {
        Reservation reservation = reservation(10L, StatutReservation.CONFIRMEE, 1L, 3L);
        Place place = reservation.getPlace();
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.trouverChevauchements(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponseDTO result = service.terminer(10L);

        assertThat(result.getStatut()).isEqualTo(StatutReservation.TERMINEE);
        assertThat(place.getStatut()).isEqualTo(StatutPlace.LIBRE);
        verify(placeRepository).save(place);
    }

    @Test
    void actionProprietaire_refuseLUtilisateurQuiNePossedePasLaReservation() {
        when(reservationRepository.findById(10L))
                .thenReturn(Optional.of(reservation(10L, StatutReservation.EN_ATTENTE, 1L, 3L)));

        assertThatThrownBy(() -> service.confirmerSiProprietaire(10L, 42L))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("pas autorisé");

        verify(reservationRepository, never()).save(any(Reservation.class));
    }

    @Test
    void actionProprietaire_autoriseLeProprietaire() {
        Reservation reservation = reservation(10L, StatutReservation.EN_ATTENTE, 1L, 3L);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponseDTO result = service.confirmerSiProprietaire(10L, 1L);

        assertThat(result.getStatut()).isEqualTo(StatutReservation.CONFIRMEE);
    }

    @Test
    void annulerSiProprietaire_annuleLaReservationDuProprietaire() {
        Reservation reservation = reservation(10L, StatutReservation.CONFIRMEE, 1L, 3L);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.trouverChevauchements(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponseDTO result = service.annulerSiProprietaire(10L, 1L);

        assertThat(result.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(reservation.getPlace().getStatut()).isEqualTo(StatutPlace.LIBRE);
    }

    @Test
    void terminerSiProprietaire_termineLaReservationConfirmeeDuProprietaire() {
        Reservation reservation = reservation(10L, StatutReservation.CONFIRMEE, 1L, 3L);
        when(reservationRepository.findById(10L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.trouverChevauchements(eq(3L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(reservation)).thenReturn(reservation);

        ReservationResponseDTO result = service.terminerSiProprietaire(10L, 1L);

        assertThat(result.getStatut()).isEqualTo(StatutReservation.TERMINEE);
        assertThat(reservation.getPlace().getStatut()).isEqualTo(StatutPlace.LIBRE);
    }

    private ReservationRequestDTO demandePourPlace(Long placeId) {
        ReservationRequestDTO request = new ReservationRequestDTO(ARRIVEE, DEPART, 1L, 2L);
        request.setPlaceId(placeId);
        return request;
    }

    private Utilisateur utilisateur(Long id) {
        Utilisateur utilisateur = new Utilisateur("Doe", "Jane", "jane@example.test", "secret", RoleUtilisateur.USER);
        utilisateur.setId(id);
        return utilisateur;
    }

    private Vehicule vehicule(Long id) {
        Vehicule vehicule = new Vehicule("1234TAA", "Toyota", "Yaris", utilisateur(1L));
        vehicule.setId(id);
        return vehicule;
    }

    private Place place(Long id, StatutPlace statut) {
        Place place = new Place("A01", "A", TypePlace.STANDARD);
        place.setId(id);
        place.setStatut(statut);
        return place;
    }

    private Reservation reservation(Long id, StatutReservation statut, Long utilisateurId, Long placeId) {
        Reservation reservation = new Reservation(ARRIVEE, DEPART, utilisateur(utilisateurId), vehicule(2L));
        reservation.setId(id);
        reservation.setPlace(place(placeId, StatutPlace.RESERVEE));
        reservation.setStatut(statut);
        return reservation;
    }
}
