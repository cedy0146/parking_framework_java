package com.tpe.parking.repository;

import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.enumeration.RoleUtilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<Utilisateur> findByNomContainingIgnoreCase(String nom, Pageable pageable);

    Page<Utilisateur> findByRole(RoleUtilisateur role, Pageable pageable);

    @Query("SELECT u FROM Utilisateur u WHERE " +
           "(:nom IS NULL OR LOWER(u.nom) LIKE LOWER(CONCAT('%', :nom, '%'))) AND " +
           "(:role IS NULL OR u.role = :role)")
    Page<Utilisateur> rechercher(@Param("nom") String nom,
                                  @Param("role") RoleUtilisateur role,
                                  Pageable pageable);
}
