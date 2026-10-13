package com.tpe.parking.repository;

import com.tpe.parking.entity.Place;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.TypePlace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaceRepository extends JpaRepository<Place, Long> {

    Optional<Place> findByNumero(String numero);

    boolean existsByNumero(String numero);

    Page<Place> findByStatut(StatutPlace statut, Pageable pageable);

    Page<Place> findByType(TypePlace type, Pageable pageable);

    Page<Place> findByZone(String zone, Pageable pageable);

    List<Place> findByStatutAndType(StatutPlace statut, TypePlace type);

    @Query("SELECT p FROM Place p WHERE p.statut = :statut AND p.type = :type ORDER BY p.numero")
    List<Place> trouverDisponiblesParType(@Param("statut") StatutPlace statut, @Param("type") TypePlace type);

    @Query("SELECT p FROM Place p WHERE " +
           "(:numero IS NULL OR LOWER(p.numero) LIKE LOWER(CONCAT('%', :numero, '%'))) AND " +
           "(:zone IS NULL OR p.zone = :zone) AND " +
           "(:type IS NULL OR p.type = :type) AND " +
           "(:statut IS NULL OR p.statut = :statut)")
    Page<Place> rechercher(@Param("numero") String numero,
                            @Param("zone") String zone,
                            @Param("type") TypePlace type,
                            @Param("statut") StatutPlace statut,
                            Pageable pageable);

    long countByStatut(StatutPlace statut);

    long countByStatutAndZone(StatutPlace statut, String zone);

    @Query("SELECT p.zone, p.statut, COUNT(p) FROM Place p GROUP BY p.zone, p.statut")
    List<Object[]> compterParZoneEtStatut();
}
