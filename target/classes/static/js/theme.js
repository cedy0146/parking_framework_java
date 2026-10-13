/* ==========================================================================
   ParkFlow — Bascule de thème clair / sombre + sélecteur de couleur d'accent
   Le mode sombre est la valeur par défaut ; seul un choix enregistré
   (localStorage « parkflow-theme ») peut imposer le mode clair.
   La couleur d'accent est stockée dans « parkflow-accent » et appliquée
   en surchargeant les variables CSS sur :root.
   ========================================================================== */
(function () {
    'use strict';

    var CLE = 'parkflow-theme';
    var CLE_ACCENT = 'parkflow-accent';
    var racine = document.documentElement;

    var mediaMouvement = window.matchMedia('(prefers-reduced-motion: reduce)');

    /* ---------- lecture / écriture du choix utilisateur ---------- */

    function lireChoix() {
        try {
            return window.localStorage.getItem(CLE);
        } catch (e) {
            return null; /* navigation privée, stockage refusé */
        }
    }

    function ecrireChoix(theme) {
        try {
            window.localStorage.setItem(CLE, theme);
        } catch (e) {
            /* sans stockage, le thème reste valable pour cette page */
        }
    }

    function themeCourant() {
        return racine.getAttribute('data-bs-theme') === 'light' ? 'light' : 'dark';
    }

    /* ---------- application visuelle ---------- */

    var metaTheme = document.querySelector('meta[name="theme-color"]');

    function peindre(theme) {
        racine.setAttribute('data-bs-theme', theme);

        /* Notification pour les scripts qui peignent des couleurs dynamiques
           (graphiques du tableau de bord, canvases…) */
        document.dispatchEvent(new CustomEvent('parkflow:themechange', { detail: { theme: theme } }));

        if (metaTheme) {
            /* teinte des surfaces principales, pour la barre du navigateur */
            metaTheme.setAttribute('content', theme === 'light' ? '#f4f7ff' : '#070d1c');
        }

        var bascules = document.querySelectorAll('[data-theme-toggle]');
        for (var i = 0; i < bascules.length; i++) {
            var bouton = bascules[i];
            /* Deux icônes par bouton (lune / soleil) : on montre celle du thème actif.
               L'attribut hidden est natif : aucun style CSS ni dépendance d'icônes. */
            var iconeClair = bouton.querySelector('[data-theme-icon="light"]');
            var iconeSombre = bouton.querySelector('[data-theme-icon="dark"]');
            if (iconeClair) iconeClair.hidden = theme !== 'light';
            if (iconeSombre) iconeSombre.hidden = theme !== 'dark';
            /* état courant pour les lecteurs d'écran ; le libellé reste « Changer de thème » */
            bouton.setAttribute('aria-pressed', theme === 'dark' ? 'true' : 'false');
            bouton.setAttribute('title', theme === 'dark' ? 'Mode clair' : 'Mode sombre');
        }

        /* Les couleurs dérivées de l'accent dépendent du thème (lisibilité) */
        if (typeof appliquerAccent === 'function' && accentCourant) {
            appliquerAccent(accentCourant);
        }
    }

    /* ---------- bascule déclenchée par le bouton ---------- */

    function basculer(bouton) {
        var suivant = themeCourant() === 'dark' ? 'light' : 'dark';
        ecrireChoix(suivant);

        var anime = !mediaMouvement.matches;
        if (anime && bouton) {
            /* icône : pivot + fondu pendant la transition des couleurs */
            bouton.classList.add('is-switching');
            racine.classList.add('theme-anim');
            window.setTimeout(function () {
                bouton.classList.remove('is-switching');
            }, 260);
            window.setTimeout(function () {
                racine.classList.remove('theme-anim');
            }, 420);
        }

        peindre(suivant);
    }

    /* Un seul écouteur pour tous les boutons (y compris ceux injectés en modale) */
    document.addEventListener('click', function (evenement) {
        var bouton = evenement.target.closest && evenement.target.closest('[data-theme-toggle]');
        if (!bouton) return;
        evenement.preventDefault();
        basculer(bouton);
    });

    /* ---------- synchronisation entre onglets ---------- */

    window.addEventListener('storage', function (evenement) {
        if (evenement.key !== CLE) return;
        peindre(lireChoix() === 'light' ? 'light' : 'dark');
    });

    /* ---------- sélection de couleur d'accent ---------- */

    var COULEURS = {
        '#4d8dff': { rgb: '77, 141, 255', hover: '#75a6ff', contrast: '#04122e' },
        '#a78bfa': { rgb: '167, 139, 250', hover: '#c4b5fd', contrast: '#1e1040' },
        '#f472b6': { rgb: '244, 114, 182', hover: '#f9a8d4', contrast: '#3d0a24' },
        '#fb923c': { rgb: '251, 146, 60', hover: '#fdba74', contrast: '#3b1501' },
        '#4ade80': { rgb: '74, 222, 128', hover: '#86efac', contrast: '#052e16' },
        '#38bdf8': { rgb: '56, 189, 248', hover: '#7dd3fc', contrast: '#082f49' },
        '#e879f9': { rgb: '232, 121, 249', hover: '#f0abfc', contrast: '#3b0764' },
        '#facc15': { rgb: '250, 204, 21', hover: '#fde047', contrast: '#422006' }
    };

    function lireAccent() {
        try {
            return window.localStorage.getItem(CLE_ACCENT);
        } catch (e) {
            return null;
        }
    }

    function ecrireAccent(couleur) {
        try {
            window.localStorage.setItem(CLE_ACCENT, couleur);
        } catch (e) { /* sans stockage */ }
    }

    /* Génère une couleur dérivée (éclaircie ou assombrie) d'une teinte hex */
    function derive(hex, ratio) {
        var r = parseInt(hex.slice(1, 3), 16);
        var g = parseInt(hex.slice(3, 5), 16);
        var b = parseInt(hex.slice(5, 7), 16);
        if (ratio >= 0) {
            r = Math.round(r + (255 - r) * ratio);
            g = Math.round(g + (255 - g) * ratio);
            b = Math.round(b + (255 - b) * ratio);
        } else {
            var k = 1 + ratio;
            r = Math.round(r * k);
            g = Math.round(g * k);
            b = Math.round(b * k);
        }
        return '#' + [r, g, b].map(function (v) {
            return ('0' + Math.max(0, Math.min(255, v)).toString(16)).slice(-2);
        }).join('');
    }

    function rgbDe(hex) {
        return parseInt(hex.slice(1, 3), 16) + ', ' + parseInt(hex.slice(3, 5), 16) + ', ' + parseInt(hex.slice(5, 7), 16);
    }

    var accentCourant = '#4d8dff';

    function appliquerAccent(couleur) {
        if (!couleur || !COULEURS[couleur]) return;
        accentCourant = couleur;
        var info = COULEURS[couleur];
        var estClair = themeCourant() === 'light';

        /* Couleurs dérivées de l'accent — éclaircies en mode sombre,
           assombries en mode clair pour rester lisibles. */
        var ton1 = derive(couleur, estClair ? -0.3 : 0.35);
        var ton2 = derive(couleur, estClair ? -0.5 : 0.55);
        var rgb1 = rgbDe(ton1);
        var rgb2 = rgbDe(ton2);

        racine.style.setProperty('--parking-accent', couleur);
        racine.style.setProperty('--parking-accent-hover', info.hover);
        racine.style.setProperty('--parking-accent-soft', 'rgba(' + info.rgb + ', 0.14)');
        racine.style.setProperty('--parking-accent-border', 'rgba(' + info.rgb + ', 0.35)');
        racine.style.setProperty('--parking-accent-contrast', info.contrast);
        racine.style.setProperty('--parking-accent-rgb', info.rgb);
        racine.style.setProperty('--parking-glow', 'rgba(' + info.rgb + ', 0.35)');
        racine.style.setProperty('--parking-on-filled', info.contrast);

        /* Séries de graphiques — dérivées de l'accent */
        racine.style.setProperty('--chart-1', couleur);
        racine.style.setProperty('--chart-2', ton1);
        racine.style.setProperty('--chart-3', ton2);
        racine.style.setProperty('--chart-4', 'rgba(' + info.rgb + ', 0.18)');

        /* Statut « libre » — même famille que l'accent */
        racine.style.setProperty('--status-free', ton1);
        racine.style.setProperty('--status-free-soft', 'rgba(' + rgb1 + ', 0.14)');
        racine.style.setProperty('--status-free-border', 'rgba(' + rgb1 + ', 0.32)');
        racine.style.setProperty('--parking-success', ton1);
        racine.style.setProperty('--parking-success-soft', 'rgba(' + rgb1 + ', 0.14)');
        racine.style.setProperty('--parking-success-border', 'rgba(' + rgb1 + ', 0.32)');
        racine.style.setProperty('--parking-success-hover', ton2);
        racine.style.setProperty('--parking-success-rgb', rgb1);
        racine.style.setProperty('--bs-success-rgb', rgb1);

        /* Info — version encore plus claire (ou plus sombre en mode clair) */
        racine.style.setProperty('--parking-info', ton2);
        racine.style.setProperty('--parking-info-soft', 'rgba(' + rgb2 + ', 0.14)');
        racine.style.setProperty('--parking-info-border', 'rgba(' + rgb2 + ', 0.32)');
        racine.style.setProperty('--bs-info', ton2);
        racine.style.setProperty('--bs-info-rgb', rgb2);

        /* Bootstrap */
        racine.style.setProperty('--bs-primary', couleur);
        racine.style.setProperty('--bs-primary-rgb', info.rgb);
        racine.style.setProperty('--bs-focus-ring-color', 'rgba(' + info.rgb + ', 0.3)');
        racine.style.setProperty('--bs-focus-ring-rgb', info.rgb);

        /* Mettre à jour l'état actif des swatches */
        var swatches = document.querySelectorAll('.accent-swatch');
        for (var i = 0; i < swatches.length; i++) {
            swatches[i].classList.toggle('is-active', swatches[i].getAttribute('data-accent') === couleur);
        }

        document.dispatchEvent(new CustomEvent('parkflow:accentchange', { detail: { accent: couleur } }));
    }

    /* Ouvrir / fermer le popup */
    document.addEventListener('click', function (e) {
        var btnToggle = e.target.closest && e.target.closest('[data-accent-toggle]');
        var popup = document.querySelector('[data-accent-popup]');
        if (!popup) return;

        if (btnToggle) {
            e.preventDefault();
            e.stopPropagation();
            var ouvert = !popup.hidden;
            popup.hidden = ouvert;
            btnToggle.setAttribute('aria-expanded', String(!ouvert));
            return;
        }

        /* Choisir une couleur */
        var swatch = e.target.closest && e.target.closest('.accent-swatch');
        if (swatch) {
            e.preventDefault();
            var couleur = swatch.getAttribute('data-accent');
            ecrireAccent(couleur);
            appliquerAccent(couleur);
            popup.hidden = true;
            var toggleBtn = document.querySelector('[data-accent-toggle]');
            if (toggleBtn) toggleBtn.setAttribute('aria-expanded', 'false');
            return;
        }

        /* Fermer si on clique ailleurs */
        popup.hidden = true;
        var toggleBtn2 = document.querySelector('[data-accent-toggle]');
        if (toggleBtn2) toggleBtn2.setAttribute('aria-expanded', 'false');
    });

    /* Initialiser la couleur au chargement */
    appliquerAccent(lireAccent() || '#4d8dff');

    /* Synchronisation entre onglets pour l'accent */
    window.addEventListener('storage', function (e) {
        if (e.key === CLE_ACCENT) appliquerAccent(e.newValue || '#4d8dff');
    });

    /* ---------- initialisation (icônes + métadonnées) ---------- */

    peindre(themeCourant());
})();
