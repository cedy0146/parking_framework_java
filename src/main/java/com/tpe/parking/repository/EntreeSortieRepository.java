package com.tpe.parking.repository;

import com.tpe.parking.entity.EntreeSortie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EntreeSortieRepository extends JpaRepository<EntreeSortie, Long> {

    Optional<EntreeSortie> findByVehiculeIdAndDateHeureSortieIsNull(Long vehiculeId);

    List<EntreeSortie> findByPlaceIdAndDateHeureSortieIsNull(Long placeId);

    Page<EntreeSortie> findByVehiculeId(Long vehiculeId, Pageable pageable);

    Page<EntreeSortie> findByPlaceId(Long placeId, Pageable pageable);

    @Query("SELECT e FROM EntreeSortie e WHERE e.dateHeureEntree BETWEEN :debut AND :fin")
    Page<EntreeSortie> findByPeriode(@Param("debut") LocalDateTime debut,
                                      @Param("fin") LocalDateTime fin,
                                      Pageable pageable);

    @Query("SELECT COALESCE(SUM(e.montantPaye), 0) FROM EntreeSortie e WHERE e.dateHeureSortie IS NOT NULL " +
           "AND e.dateHeureEntree BETWEEN :debut AND :fin")
    BigDecimal calculerRevenuPeriode(@Param("debut") LocalDateTime debut, @Param("fin") LocalDateTime fin);

    @Query("SELECT COALESCE(SUM(e.montantPaye), 0) FROM EntreeSortie e WHERE e.dateHeureSortie IS NOT NULL")
    BigDecimal calculerRevenuTotal();

    @Query("SELECT e FROM EntreeSortie e WHERE " +
           "(:vehiculeId IS NULL OR e.vehicule.id = :vehiculeId) AND " +
           "(:placeId IS NULL OR e.place.id = :placeId) AND " +
           "(:enCours IS NULL OR (:enCours = true AND e.dateHeureSortie IS NULL) OR (:enCours = false AND e.dateHeureSortie IS NOT NULL))")
    Page<EntreeSortie> rechercher(@Param("vehiculeId") Long vehiculeId,
                                   @Param("placeId") Long placeId,
                                   @Param("enCours") Boolean enCours,
                                   Pageable pageable);

    long countByDateHeureSortieIsNull();

    long countByDateHeureEntreeAfter(LocalDateTime debut);

    long countByDateHeureSortieAfter(LocalDateTime debut);

    List<EntreeSortie> findByDateHeureEntreeGreaterThanEqual(LocalDateTime debut);

    List<EntreeSortie> findByDateHeureSortieGreaterThanEqual(LocalDateTime debut);

    /** Véhicules encore sur place (mouvements ouverts) — mini-plan, listes. */
    List<EntreeSortie> findByDateHeureSortieIsNull();

    /** Nombre de véhicules en stationnement à un instant donné (variation « vs veille »). */
    @Query("SELECT COUNT(e) FROM EntreeSortie e WHERE e.dateHeureEntree <= :moment " +
           "AND (e.dateHeureSortie IS NULL OR e.dateHeureSortie > :moment)")
    long compterEnStationnementA(@Param("moment") LocalDateTime moment);
}
