package com.tpe.parking.repository;

import com.tpe.parking.entity.Vehicule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VehiculeRepository extends JpaRepository<Vehicule, Long> {

    Optional<Vehicule> findByImmatriculation(String immatriculation);

    boolean existsByImmatriculation(String immatriculation);

    Page<Vehicule> findByUtilisateurId(Long utilisateurId, Pageable pageable);

    Page<Vehicule> findByMarqueContainingIgnoreCase(String marque, Pageable pageable);

    @Query("SELECT v FROM Vehicule v WHERE " +
           "(:immatriculation IS NULL OR LOWER(v.immatriculation) LIKE LOWER(CONCAT('%', :immatriculation, '%'))) AND " +
           "(:marque IS NULL OR LOWER(v.marque) LIKE LOWER(CONCAT('%', :marque, '%'))) AND " +
           "(:utilisateurId IS NULL OR v.utilisateur.id = :utilisateurId)")
    Page<Vehicule> rechercher(@Param("immatriculation") String immatriculation,
                               @Param("marque") String marque,
                               @Param("utilisateurId") Long utilisateurId,
                               Pageable pageable);
}
