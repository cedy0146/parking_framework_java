package com.tpe.parking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Données agrégées du tableau de bord /accueil, sérialisées telles quelles
 * pour /accueil/stats et injectées au rendu Thymeleaf initial.
 * Les champs *Libelle / *Css sont produits par web.Labels (libellés uniques).
 */
public record DashboardStatsDto(
        Places places,
        List<ZoneStat> parZone,
        Statuts reservations,
        Abonnements abonnements,
        Mouvements mouvements,
        Revenus revenus,
        Serie serie7j,
        SerieLongue serieReservations7j,
        List<ReservationRecente> reservationsRecentes,
        List<MouvementRecent> mouvementsRecents,
        List<ZonePlan> planParZone) {

    public record Places(long libres, long reservees, long occupees, long horsService, long total,
                         double tauxOccupation) {
    }

    public record ZoneStat(String zone, String libelle, long libres, long reservees, long occupees,
                           long horsService, long total) {
    }

    public record Statuts(long enAttente, long confirmees, long annulees, long terminees) {
    }

    public record Abonnements(long actifs, long expires, long resilies) {
    }

    /** enStationnementHier sert au calcul de la variation « vs veille ». */
    public record Mouvements(long entreesJour, long sortiesJour, long enStationnement,
                             long enStationnementHier) {
        public Long variationStationnement() {
            return enStationnement - enStationnementHier;
        }
    }

    /** Null quand le rôle connecté n'est pas autorisé à voir les montants.
     *  moyenneJour30 = moyenne quotidienne sur les 30 derniers jours. */
    public record Revenus(BigDecimal jour, BigDecimal hier, BigDecimal mois, BigDecimal moyenneJour30) {
        public BigDecimal variationJour() {
            if (jour == null || hier == null) return null;
            return jour.subtract(hier);
        }
    }

    /** Série 7 jours : libellés jj/MM + une valeur par jour. */
    public record Serie(List<String> labels, List<Long> entrees, List<Long> sorties, List<BigDecimal> revenus) {
    }

    public record SerieLongue(List<String> labels, List<Long> valeurs) {
    }

    public record ReservationRecente(Long id, String utilisateur, String vehicule, String place,
                                     String statut, String statutLibelle, String statutCss,
                                     LocalDateTime arriveePrevue, String arriveeLibelle) {
    }

    public record MouvementRecent(Long id, String immatriculation, String place, LocalDateTime entree,
                                  String entreeLibelle, LocalDateTime sortie, String sortieLibelle,
                                  BigDecimal montant, String montantLibelle, boolean enCours) {
    }

    /** Groupe du mini-plan, titré par une zone parlante. */
    public record ZonePlan(String zone, String libelle, List<PlacePlan> places) {
    }

    /** statut = LIBRE/OCCUPEE/… (filtre), statutLibelle = affichage ; immat si occupée. */
    public record PlacePlan(Long id, String numero, String zone, String zoneLibelle, String type,
                            String typeLibelle, String statut, String statutLibelle,
                            String lettre, String immatriculation) {
    }
}
