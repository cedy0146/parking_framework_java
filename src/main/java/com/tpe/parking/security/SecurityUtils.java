package com.tpe.parking.security;

import com.tpe.parking.entity.Utilisateur;
import com.tpe.parking.enumeration.RoleUtilisateur;
import com.tpe.parking.repository.UtilisateurRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityUtils {

    private final UtilisateurRepository utilisateurRepository;

    public SecurityUtils(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Optional<String> getEmailConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return Optional.empty();
        }
        return Optional.ofNullable(auth.getName());
    }

    public Optional<Utilisateur> getUtilisateurConnecte() {
        return getEmailConnecte()
                .flatMap(utilisateurRepository::findByEmail);
    }

    public Long getUtilisateurConnecteId() {
        return getUtilisateurConnecte()
                .map(Utilisateur::getId)
                .orElseThrow(() -> new IllegalStateException("Aucun utilisateur connecté"));
    }

    public boolean aLeRole(RoleUtilisateur role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_" + role.name()));
    }

    public boolean estUSER() {
        return aLeRole(RoleUtilisateur.USER);
    }

    public boolean estAdminOuManager() {
        return aLeRole(RoleUtilisateur.ADMIN) || aLeRole(RoleUtilisateur.MANAGER);
    }
}
