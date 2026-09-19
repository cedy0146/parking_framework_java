package com.tpe.parking.repository;

import com.tpe.parking.entity.Abonnement;
import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.TypeAbonnement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AbonnementRepository extends JpaRepository<Abonnement, Long> {

    Page<Abonnement> findByUtilisateurId(Long utilisateurId, Pageable pageable);

    Page<Abonnement> findByStatut(StatutAbonnement statut, Pageable pageable);

    Optional<Abonnement> findByVehiculeIdAndStatut(Long vehiculeId, StatutAbonnement statut);

    List<Abonnement> findByVehiculeIdAndStatutIn(Long vehiculeId, List<StatutAbonnement> statuts);

    @Query("SELECT a FROM Abonnement a WHERE a.vehicule.id = :vehiculeId AND a.statut = 'ACTIF' " +
           "AND a.dateDebut <= :date AND a.dateFin >= :date")
    Optional<Abonnement> trouverActifPourDate(@Param("vehiculeId") Long vehiculeId, @Param("date") LocalDate date);

    @Query("SELECT a FROM Abonnement a WHERE " +
           "(:utilisateurId IS NULL OR a.utilisateur.id = :utilisateurId) AND " +
           "(:statut IS NULL OR a.statut = :statut) AND " +
           "(:type IS NULL OR a.type = :type)")
    Page<Abonnement> rechercher(@Param("utilisateurId") Long utilisateurId,
                                 @Param("statut") StatutAbonnement statut,
                                 @Param("type") TypeAbonnement type,
                                 Pageable pageable);

    long countByStatut(StatutAbonnement statut);
}
