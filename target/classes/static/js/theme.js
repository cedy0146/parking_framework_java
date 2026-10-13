/* ==========================================================================
   ParkFlow — Bascule de thème clair / sombre
   Le mode sombre est la valeur par défaut ; seul un choix enregistré
   (localStorage « parkflow-theme ») peut imposer le mode clair.
   Le thème est appliqué avant le rendu par le script inline du <head>
   (layout.html, login.html, error.html, 403.html, landing.html) : ce fichier
   gère ensuite le bouton, les changements en direct et la synchro entre onglets.
   ========================================================================== */
(function () {
    'use strict';

    var CLE = 'parkflow-theme';
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

    /* ---------- initialisation (icônes + métadonnées) ---------- */

    peindre(themeCourant());
})();
