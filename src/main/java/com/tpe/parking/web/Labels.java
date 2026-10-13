package com.tpe.parking.web;

import com.tpe.parking.enumeration.StatutAbonnement;
import com.tpe.parking.enumeration.StatutPlace;
import com.tpe.parking.enumeration.StatutReservation;
import com.tpe.parking.enumeration.TypeAbonnement;
import com.tpe.parking.enumeration.TypePlace;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Libellés d'affichage uniques (Bastien & Scapin — critère 7) :
 * aucune valeur d'énumération brute n'atteint l'écran. Exposés aux
 * templates Thymeleaf sous la forme ${@labels.reservation(...)}.
 */
@Component("labels")
public class Labels {

    private static final String[] MOIS = {
            "janv.", "févr.", "mars", "avr.", "mai", "juin",
            "juil.", "août", "sept.", "oct.", "nov.", "déc."
    };

    private static final DecimalFormat MONTANT =
            new DecimalFormat("#,##0", new DecimalFormatSymbols(Locale.FRANCE));

    /* ---------- statuts de réservation ---------- */

    public String reservation(StatutReservation statut) {
        if (statut == null) return "-";
        return switch (statut) {
            case EN_ATTENTE -> "En attente";
            case CONFIRMEE -> "Confirmée";
            case ANNULEE -> "Annulée";
            case TERMINEE -> "Terminée";
        };
    }

    public String reservation(String statut) {
        if (statut == null || statut.isBlank()) return "-";
        try {
            return reservation(StatutReservation.valueOf(statut));
        } catch (IllegalArgumentException e) {
            return statut;
        }
    }

    public String reservationCss(StatutReservation statut) {
        if (statut == null) return "bg-secondary";
        return switch (statut) {
            case EN_ATTENTE -> "bg-warning";
            case CONFIRMEE -> "bg-primary";
            case ANNULEE -> "bg-danger";
            case TERMINEE -> "bg-secondary";
        };
    }

    /* ---------- statuts de place ---------- */

    public String place(StatutPlace statut) {
        if (statut == null) return "-";
        return switch (statut) {
            case LIBRE -> "Libre";
            case OCCUPEE -> "Occupée";
            case RESERVEE -> "Réservée";
            case HORS_SERVICE -> "Hors service";
        };
    }

    public String place(String statut) {
        if (statut == null || statut.isBlank()) return "-";
        try {
            return place(StatutPlace.valueOf(statut));
        } catch (IllegalArgumentException e) {
            return statut;
        }
    }

    public String placeCss(String statut) {
        if ("LIBRE".equals(statut)) return "bg-success";
        if ("OCCUPEE".equals(statut)) return "bg-danger";
        if ("RESERVEE".equals(statut)) return "bg-warning";
        return "bg-secondary";
    }

    /* ---------- types de place ---------- */

    public String typePlace(TypePlace type) {
        if (type == null) return "-";
        return switch (type) {
            case STANDARD -> "Standard";
            case PMR -> "PMR";
            case VIP -> "VIP";
            case ABONNE -> "Abonné";
        };
    }

    public String typePlace(String type) {
        if (type == null || type.isBlank()) return "-";
        try {
            return typePlace(TypePlace.valueOf(type));
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /* ---------- abonnements ---------- */

    public String abonnement(StatutAbonnement statut) {
        if (statut == null) return "-";
        return switch (statut) {
            case ACTIF -> "Actif";
            case EXPIRE -> "Expiré";
            case RESILIE -> "Résilié";
        };
    }

    public String abonnement(String statut) {
        if (statut == null || statut.isBlank()) return "-";
        try {
            return abonnement(StatutAbonnement.valueOf(statut));
        } catch (IllegalArgumentException e) {
            return statut;
        }
    }

    public String abonnementCss(StatutAbonnement statut) {
        if (statut == null) return "bg-secondary";
        return switch (statut) {
            case ACTIF -> "bg-success";
            case EXPIRE -> "bg-secondary";
            case RESILIE -> "bg-danger";
        };
    }

    public String typeAbonnement(TypeAbonnement type) {
        if (type == null) return "-";
        return switch (type) {
            case JOURNALIER -> "Journalier";
            case HEBDOMADAIRE -> "Hebdomadaire";
            case MENSUEL -> "Mensuel";
            case ANNUEL -> "Annuel";
        };
    }

    public String typeAbonnement(String type) {
        if (type == null || type.isBlank()) return "-";
        try {
            return typeAbonnement(TypeAbonnement.valueOf(type));
        } catch (IllegalArgumentException e) {
            return type;
        }
    }

    /* ---------- zones parlantes (le code P = PMR, V = VIP dans DataInitializer) ---------- */

    public String zone(String code) {
        if (code == null || code.isBlank()) return "-";
        return switch (code) {
            case "P" -> "Zone P (PMR)";
            case "V" -> "Zone V (VIP)";
            default -> "Zone " + code;
        };
    }

    public String zoneCourte(String code) {
        if (code == null || code.isBlank()) return "-";
        return switch (code) {
            case "P" -> "P · PMR";
            case "V" -> "V · VIP";
            default -> code;
        };
    }

    /* ---------- montants et dates ---------- */

    public String montant(BigDecimal valeur) {
        if (valeur == null) return "—";
        return MONTANT.format(valeur.longValue()) + " Ar";
    }

    public String montant(Object valeur) {
        if (valeur == null) return "—";
        if (valeur instanceof BigDecimal b) return montant(b);
        if (valeur instanceof Number n) return MONTANT.format(n.longValue()) + " Ar";
        return valeur + " Ar";
    }

    /** « 12 oct. 2026, 09:00 » */
    public String dateHeure(LocalDateTime moment) {
        if (moment == null) return "—";
        return moment.getDayOfMonth() + " " + MOIS[moment.getMonthValue() - 1] + " " + moment.getYear()
                + ", " + String.format("%02d:%02d", moment.getHour(), moment.getMinute());
    }

    /** « 12 oct., 09:00 » (listes compactes) */
    public String dateCourte(LocalDateTime moment) {
        if (moment == null) return "—";
        return moment.getDayOfMonth() + " " + MOIS[moment.getMonthValue() - 1]
                + ", " + String.format("%02d:%02d", moment.getHour(), moment.getMinute());
    }

    /** « 12/10/2026 » */
    public String date(java.time.LocalDate jour) {
        if (jour == null) return "—";
        return String.format("%02d/%02d/%d", jour.getDayOfMonth(), jour.getMonthValue(), jour.getYear());
    }

    /** « il y a 12 min » / « il y a 3 h » / « à l'instant » */
    public String depuis(LocalDateTime moment) {
        if (moment == null) return "—";
        long secondes = Math.max(0, ChronoUnit.SECONDS.between(moment, LocalDateTime.now()));
        if (secondes < 60) return "à l'instant";
        long minutes = secondes / 60;
        if (minutes < 60) return "il y a " + minutes + " min";
        long heures = minutes / 60;
        if (heures < 24) return "il y a " + heures + " h";
        long jours = heures / 24;
        return "il y a " + jours + " j";
    }
}
