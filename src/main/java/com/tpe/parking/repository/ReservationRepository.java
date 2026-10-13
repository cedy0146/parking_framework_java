package com.tpe.parking.repository;

import com.tpe.parking.entity.Reservation;
import com.tpe.parking.enumeration.StatutReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUtilisateurId(Long utilisateurId, Pageable pageable);

    Page<Reservation> findByStatut(StatutReservation statut, Pageable pageable);

    @Query("SELECT r FROM Reservation r WHERE r.place.id = :placeId AND r.statut IN ('CONFIRMEE', 'EN_ATTENTE') " +
           "AND r.dateHeureArriveePrevue < :dateFin AND r.dateHeureDepartPrevue > :dateDebut")
    List<Reservation> trouverChevauchements(@Param("placeId") Long placeId,
                                             @Param("dateDebut") LocalDateTime dateDebut,
                                             @Param("dateFin") LocalDateTime dateFin);

    @Query("SELECT r FROM Reservation r WHERE r.place.id = :placeId AND r.statut IN ('CONFIRMEE', 'EN_ATTENTE') " +
           "AND r.dateHeureArriveePrevue < :dateFin AND r.dateHeureDepartPrevue > :dateDebut AND r.id <> :excludeId")
    List<Reservation> trouverChevauchementsExcluant(@Param("placeId") Long placeId,
                                                     @Param("dateDebut") LocalDateTime dateDebut,
                                                     @Param("dateFin") LocalDateTime dateFin,
                                                     @Param("excludeId") Long excludeId);

    @Query("SELECT r FROM Reservation r WHERE " +
           "(:utilisateurId IS NULL OR r.utilisateur.id = :utilisateurId) AND " +
           "(:statut IS NULL OR r.statut = :statut) AND " +
           "(:placeId IS NULL OR r.place.id = :placeId)")
    Page<Reservation> rechercher(@Param("utilisateurId") Long utilisateurId,
                                  @Param("statut") StatutReservation statut,
                                  @Param("placeId") Long placeId,
                                  Pageable pageable);

    long countByStatut(StatutReservation statut);

    List<Reservation> findByDateCreationGreaterThanEqual(LocalDateTime debut);
}
