/* ==========================================================================
   ParkFlow — Tableau de bord /accueil
   Graphiques Chart.js, sparklines, mini-plan groupé, rafraîchissement
   automatique (30 s), pause persistée, toasts, raccourcis clavier, état
   hors-ligne. Les couleurs sont lues dans les variables CSS du thème et
   suivent la bascule clair / sombre.
   ========================================================================== */
(function () {
    'use strict';

    var RACINE = document.documentElement;
    var miniPlan = document.querySelector('[data-dash-mini-plan]');
    if (!miniPlan && !document.querySelector('[data-chart]')) return;

    var INTERVALLE_MS = 30000;
    var CLE_PAUSE = 'parkflow-dash-pause';
    var minuteur = null;
    var pause = false;
    var enLigne = navigator.onLine !== false;
    var dernierRafraichissement = Date.now();
    var dernierToast = 0;
    var derniersIds = { reservations: null, mouvements: null };
    var valeursPrecedentes = {};
    var premierChargement = true;
    var motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

    /* ---------- utilitaires ---------- */

    function reduit() { return motionQuery.matches; }

    function couleur(nom, defaut) {
        var valeur = getComputedStyle(RACINE).getPropertyValue(nom);
        return (valeur && valeur.trim()) || defaut;
    }

    function palette() {
        return {
            accent: couleur('--parking-accent', '#4d8dff'),
            chart1: couleur('--chart-1', '#4d8dff'),
            chart2: couleur('--chart-2', '#b4ccff'),
            chart3: couleur('--chart-3', '#38bdf8'),
            chart4: couleur('--chart-4', 'rgba(77,141,255,.18)'),
            free: couleur('--status-free', '#4ade80'),
            busy: couleur('--status-busy', '#f87171'),
            booked: couleur('--status-booked', '#fbbf24'),
            off: couleur('--status-off', '#94a3b8'),
            muted: couleur('--parking-muted', '#93a3c4'),
            text: couleur('--parking-text', '#eef3ff'),
            surface: couleur('--parking-surface', '#0d1630'),
            border: couleur('--parking-border', 'rgba(148,170,255,.14)')
        };
    }

    var formatNombre = new Intl.NumberFormat('fr-FR');

    function formatMontant(valeur) {
        var nombre = typeof valeur === 'string' ? Number(valeur.replace(/\s/g, '')) : valeur;
        if (nombre === null || nombre === undefined || isNaN(nombre)) return '—';
        return formatNombre.format(Math.round(nombre)) + ' Ar';
    }

    function formatTaux(valeur) {
        if (valeur === null || valeur === undefined || isNaN(valeur)) return '—';
        return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 1 }).format(valeur) + ' %';
    }

    function texte(element, valeur) {
        if (element) element.textContent = valeur;
    }

    function easeOutCubic(t) { return 1 - Math.pow(1 - t, 3); }

    /* Comptage animé depuis la valeur précédente (1,2 s) + flash discret.
       Au premier chargement, les valeurs servies par le serveur sont déjà
       là et animations.js a joué le compteur : on ne bouge rien. */
    function compter(element, cible) {
        if (!element) return;
        if (premierChargement) {
            element.setAttribute('data-count-to', String(cible));
            return;
        }
        var brut = element.textContent.replace(/\s/g, '').trim();
        var depart = Number(brut);
        if (!isFinite(depart)) depart = cible;

        element.setAttribute('data-count-to', String(cible));
        if (reduit() || depart === cible) {
            texte(element, formatNombre.format(cible));
            return;
        }

        var debut = null;
        function frame(now) {
            if (debut === null) debut = now;
            var progression = Math.min((now - debut) / 1200, 1);
            var valeur = Math.round(depart + (cible - depart) * easeOutCubic(progression));
            texte(element, formatNombre.format(valeur));
            if (progression < 1) requestAnimationFrame(frame);
            else {
                texte(element, formatNombre.format(cible));
                var carte = element.closest('.card');
                if (carte) {
                    carte.classList.remove('metric-flash');
                    void carte.offsetWidth;
                    carte.classList.add('metric-flash');
                    window.setTimeout(function () { carte.classList.remove('metric-flash'); }, 900);
                }
            }
        }
        requestAnimationFrame(frame);
    }

    /* Ligne de variation : flèche + valeur, jamais la couleur seule.
       quand = null → rien (pas d'historique) ; sinon delta entier. */
    function rendreTrend(element, quand, suffixe) {
        if (!element) return;
        if (quand === null || quand === undefined) {
            element.classList.remove('is-up', 'is-down', 'is-flat');
            texte(element, '');
            return;
        }
        element.classList.remove('is-up', 'is-down', 'is-flat');
        if (quand > 0) {
            element.classList.add('is-up');
            texte(element, '▲ +' + quand + (suffixe || ''));
        } else if (quand < 0) {
            element.classList.add('is-down');
            texte(element, '▼ ' + quand + (suffixe || ''));
        } else {
            element.classList.add('is-flat');
            texte(element, '= ' + (suffixe ? suffixe.replace(/^\s*vs\s/, 'vs ') : 'stable'));
        }
    }

    /* Variation « vs veille » quand le serveur la fournit, sinon delta
       depuis la précédente mise à jour. */
    function majTrend(cle, valeur, deltaVeille) {
        var element = document.querySelector('[data-trend="' + cle + '"]');
        if (!element) return;
        if (deltaVeille !== undefined && deltaVeille !== null) {
            rendreTrend(element, deltaVeille, ' vs hier');
            return;
        }
        var precedente = valeursPrecedentes[cle];
        if (premierChargement || precedente === undefined || precedente === null) {
            texte(element, '');
            element.classList.remove('is-up', 'is-down', 'is-flat');
            return;
        }
        rendreTrend(element, valeur - precedente, '');
    }

    /* ---------- badge « En direct · il y a Xs » ---------- */

    var badgeLive = document.querySelector('[data-dash-live]');
    var badgeTexte = document.querySelector('[data-dash-live-text]');

    function heureCourte(date) {
        return String(date.getHours()).padStart(2, '0') + ':' + String(date.getMinutes()).padStart(2, '0');
    }

    function rafraichirBadge() {
        if (!badgeTexte || !badgeLive) return;
        badgeLive.classList.toggle('is-offline', !enLigne);
        badgeLive.classList.toggle('is-paused', pause && enLigne);
        if (!enLigne) {
            texte(badgeTexte, 'Hors ligne – dernière mise à jour à ' + heureCourte(new Date(dernierRafraichissement)));
            return;
        }
        if (pause) {
            texte(badgeTexte, 'En pause – dernière mise à jour à ' + heureCourte(new Date(dernierRafraichissement)));
            return;
        }
        var secondes = Math.round((Date.now() - dernierRafraichissement) / 1000);
        if (secondes < 10) texte(badgeTexte, 'En direct · il y a quelques secondes');
        else if (secondes < 60) texte(badgeTexte, 'En direct · il y a ' + secondes + ' s');
        else texte(badgeTexte, 'En direct · il y a ' + Math.round(secondes / 60) + ' min');
    }
    window.setInterval(rafraichirBadge, 1000);

    /* ---------- pause (persistée) ---------- */

    var boutonPause = document.querySelector('[data-dash-pause]');

    function appliquerPause(valeur) {
        pause = valeur;
        try { window.localStorage.setItem(CLE_PAUSE, pause ? '1' : '0'); } catch (e) { /* stockage indisponible */ }
        if (boutonPause) {
            boutonPause.setAttribute('aria-pressed', pause ? 'true' : 'false');
            var icone = boutonPause.querySelector('i');
            if (icone) icone.className = pause ? 'bi bi-play-fill' : 'bi bi-pause-fill';
            var titre = pause ? 'Reprendre le rafraîchissement automatique' : 'Mettre le rafraîchissement automatique en pause';
            boutonPause.setAttribute('title', titre);
        }
        rafraichirBadge();
        if (pause) arreterMinuteur(); else { rafraichir(); demarrerMinuteur(); }
    }

    if (boutonPause) {
        boutonPause.addEventListener('click', function () { appliquerPause(!pause); });
    }

    try {
        if (window.localStorage.getItem(CLE_PAUSE) === '1') pause = true;
    } catch (e) { /* stockage indisponible */ }

    /* ---------- sparklines (canvas, sans dépendance) ---------- */

    function dessinerSparkline(canvas, valeurs, couleurLigne) {
        if (!canvas || !valeurs || !valeurs.length) return;
        var toutesNulles = valeurs.every(function (v) { return !v; });
        canvas.hidden = toutesNulles; /* sans historique : rien à afficher */
        if (toutesNulles) return;

        var ratio = window.devicePixelRatio || 1;
        var largeur = canvas.clientWidth || 120;
        var hauteur = canvas.clientHeight || 28;
        canvas.width = largeur * ratio;
        canvas.height = hauteur * ratio;
        var ctx = canvas.getContext('2d');
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        ctx.clearRect(0, 0, largeur, hauteur);

        var max = Math.max.apply(null, valeurs);
        var min = Math.min.apply(null, valeurs);
        var amplitude = max - min || 1;
        var pas = valeurs.length > 1 ? largeur / (valeurs.length - 1) : largeur;
        var marge = 4;

        ctx.beginPath();
        valeurs.forEach(function (valeur, index) {
            var x = index * pas;
            var y = hauteur - marge - ((valeur - min) / amplitude) * (hauteur - marge * 2);
            if (index === 0) ctx.moveTo(x, y); else ctx.lineTo(x, y);
        });
        ctx.strokeStyle = couleurLigne;
        ctx.lineWidth = 2;
        ctx.lineJoin = 'round';
        ctx.lineCap = 'round';
        ctx.stroke();
    }

    /* ---------- graphiques Chart.js ---------- */

    var graphiques = {};

    function optionsGenerales(p) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: p.surface,
                    titleColor: p.text,
                    bodyColor: p.text,
                    borderColor: p.border,
                    borderWidth: 1,
                    padding: 10,
                    cornerRadius: 8,
                    displayColors: true
                }
            }
        };
    }

    function optionsBarres(p) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: {
                    display: true,
                    position: 'bottom',
                    labels: {
                        usePointStyle: true,
                        pointStyle: 'circle',
                        boxWidth: 8,
                        boxHeight: 8,
                        color: p.muted,
                        font: { size: 12 },
                        padding: 16
                    }
                },
                tooltip: {
                    backgroundColor: p.surface, titleColor: p.text, bodyColor: p.text,
                    borderColor: p.border, borderWidth: 1, padding: 10, cornerRadius: 8,
                    callbacks: {
                        label: function (context) {
                            var nom = context.dataset.label || '';
                            if (nom.indexOf('Revenus') !== -1) {
                                return ' ' + nom + ' : ' + formatNombre.format(context.parsed.y) + ' Ar';
                            }
                            return ' ' + nom + ' : ' + context.parsed.y;
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: { color: p.muted, font: { size: 11 } },
                    grid: { display: false },
                    border: { color: p.border }
                },
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Mouvements', color: p.muted, font: { size: 11 } },
                    ticks: { color: p.muted, precision: 0, font: { size: 11 } },
                    grid: { color: p.border },
                    border: { display: false }
                }
            }
        };
    }

    function optionsAire(p) {
        return {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    backgroundColor: p.surface, titleColor: p.text, bodyColor: p.text,
                    borderColor: p.border, borderWidth: 1, padding: 10, cornerRadius: 8,
                    callbacks: {
                        label: function (context) {
                            return ' Revenus : ' + formatNombre.format(context.parsed.y) + ' Ar';
                        }
                    }
                }
            },
            scales: {
                x: {
                    ticks: { color: p.muted, font: { size: 11 } },
                    grid: { display: false },
                    border: { color: p.border }
                },
                y: {
                    beginAtZero: true,
                    title: { display: true, text: 'Ar', color: p.muted, font: { size: 11 } },
                    ticks: {
                        color: p.muted, font: { size: 11 },
                        callback: function (valeur) { return formatNombre.format(valeur); }
                    },
                    grid: { color: p.border },
                    border: { display: false }
                }
            }
        };
    }

    function optionDonut(p) {
        var options = optionsGenerales(p);
        options.cutout = '68%';
        options.plugins.tooltip.callbacks = {
            label: function (context) {
                var total = context.dataset.data.reduce(function (a, b) { return a + b; }, 0);
                var part = total > 0 ? Math.round(context.parsed * 1000 / total) / 10 : 0;
                return ' ' + context.label + ' : ' + context.parsed + ' (' + formatTaux(part) + ')';
            }
        };
        return options;
    }

    function masquerSquelette(canvas) {
        var conteneur = canvas && canvas.closest('.dash-skeleton');
        if (conteneur) conteneur.classList.remove('dash-skeleton');
    }

    function creerGraphiques(stats) {
        if (typeof Chart === 'undefined') return;
        var p = palette();

        var occupation = document.querySelector('[data-chart="occupation"]');
        if (occupation) {
            graphiques.occupation = new Chart(occupation, {
                type: 'doughnut',
                data: {
                    labels: ['Libres', 'Réservées', 'Occupées', 'Hors service'],
                    datasets: [{
                        data: [stats.places.libres, stats.places.reservees, stats.places.occupees, stats.places.horsService],
                        backgroundColor: [p.free, p.booked, p.busy, p.off],
                        borderColor: p.surface,
                        borderWidth: 3,
                        hoverOffset: 6
                    }]
                },
                options: optionDonut(p)
            });
            masquerSquelette(occupation);
        }

        var activite = document.querySelector('[data-chart="activite"]');
        if (activite) {
            var avecRevenus = !!(stats.revenus && stats.serie7j.revenus);
            var donnees = [
                {
                    type: 'bar',
                    label: 'Entrées',
                    data: stats.serie7j.entrees,
                    backgroundColor: p.chart1,
                    borderRadius: 4,
                    maxBarThickness: 30,
                    yAxisID: 'y'
                },
                {
                    type: 'bar',
                    label: 'Sorties',
                    data: stats.serie7j.sorties,
                    backgroundColor: p.chart2,
                    borderRadius: 4,
                    maxBarThickness: 30,
                    yAxisID: 'y'
                }
            ];
            var options = optionsBarres(p);
            if (avecRevenus) {
                donnees.push({
                    type: 'line',
                    label: 'Revenus (Ar)',
                    data: stats.serie7j.revenus.map(function (v) { return Number(v); }),
                    borderColor: p.chart3,
                    backgroundColor: p.chart3,
                    borderWidth: 2,
                    tension: 0.35,
                    pointRadius: 3,
                    yAxisID: 'y1'
                });
                options.scales.y1 = {
                    position: 'right',
                    beginAtZero: true,
                    title: { display: true, text: 'Ar', color: p.muted, font: { size: 11 } },
                    ticks: {
                        color: p.muted, font: { size: 11 },
                        callback: function (valeur) { return formatNombre.format(valeur); }
                    },
                    grid: { display: false },
                    border: { display: false }
                };
            }
            graphiques.activite = new Chart(activite, {
                data: { labels: stats.serie7j.labels, datasets: donnees },
                options: options
            });
            masquerSquelette(activite);
        }

        var reservations = document.querySelector('[data-chart="reservations"]');
        if (reservations) {
            graphiques.reservations = new Chart(reservations, {
                type: 'doughnut',
                data: {
                    labels: ['En attente', 'Confirmées', 'Annulées', 'Terminées'],
                    datasets: [{
                        data: [stats.reservations.enAttente, stats.reservations.confirmees,
                            stats.reservations.annulees, stats.reservations.terminees],
                        backgroundColor: [p.booked, p.accent, p.busy, p.off],
                        borderColor: p.surface,
                        borderWidth: 3,
                        hoverOffset: 6
                    }]
                },
                options: optionDonut(p)
            });
            masquerSquelette(reservations);
        }

        var revenus = document.querySelector('[data-chart="revenus"]');
        if (revenus && stats.revenus && stats.serie7j.revenus) {
            var ctx = revenus.getContext('2d');
            var degradé = ctx.createLinearGradient(0, 0, 0, revenus.parentNode.clientHeight || 200);
            degradé.addColorStop(0, p.chart3);
            degradé.addColorStop(1, p.chart4);
            graphiques.revenus = new Chart(revenus, {
                type: 'line',
                data: {
                    labels: stats.serie7j.labels,
                    datasets: [{
                        label: 'Revenus',
                        data: stats.serie7j.revenus.map(function (v) { return Number(v); }),
                        borderColor: p.chart3,
                        backgroundColor: degradé,
                        fill: true,
                        tension: 0.35,
                        pointRadius: 3,
                        pointBackgroundColor: p.chart3
                    }]
                },
                options: optionsAire(p)
            });
            masquerSquelette(revenus);
        }
    }

    function recolorerGraphiques() {
        if (typeof Chart === 'undefined') return;
        var p = palette();
        if (graphiques.occupation) {
            graphiques.occupation.data.datasets[0].backgroundColor = [p.free, p.booked, p.busy, p.off];
            graphiques.occupation.data.datasets[0].borderColor = p.surface;
            graphiques.occupation.options.plugins.tooltip.backgroundColor = p.surface;
            graphiques.occupation.options.plugins.tooltip.titleColor = p.text;
            graphiques.occupation.options.plugins.tooltip.bodyColor = p.text;
            graphiques.occupation.options.plugins.tooltip.borderColor = p.border;
            graphiques.occupation.update('none');
        }
        if (graphiques.activite) {
            graphiques.activite.data.datasets.forEach(function (jeu) {
                if (jeu.type === 'line') {
                    jeu.borderColor = p.chart3;
                    jeu.backgroundColor = p.chart3;
                } else if (jeu.label === 'Entrées') {
                    jeu.backgroundColor = p.chart1;
                } else {
                    jeu.backgroundColor = p.chart2;
                }
            });
            var opt = graphiques.activite.options;
            opt.plugins.legend.labels.color = p.muted;
            opt.plugins.tooltip.backgroundColor = p.surface;
            opt.plugins.tooltip.titleColor = p.text;
            opt.plugins.tooltip.bodyColor = p.text;
            opt.plugins.tooltip.borderColor = p.border;
            opt.scales.x.ticks.color = p.muted;
            opt.scales.x.border.color = p.border;
            opt.scales.y.ticks.color = p.muted;
            opt.scales.y.title.color = p.muted;
            opt.scales.y.grid.color = p.border;
            if (opt.scales.y1) {
                opt.scales.y1.ticks.color = p.muted;
                opt.scales.y1.title.color = p.muted;
            }
            graphiques.activite.update('none');
        }
        if (graphiques.reservations) {
            graphiques.reservations.data.datasets[0].backgroundColor = [p.booked, p.accent, p.busy, p.off];
            graphiques.reservations.data.datasets[0].borderColor = p.surface;
            graphiques.reservations.options.plugins.tooltip.backgroundColor = p.surface;
            graphiques.reservations.options.plugins.tooltip.titleColor = p.text;
            graphiques.reservations.options.plugins.tooltip.bodyColor = p.text;
            graphiques.reservations.options.plugins.tooltip.borderColor = p.border;
            graphiques.reservations.update('none');
        }
        if (graphiques.revenus) {
            var jeu = graphiques.revenus.data.datasets[0];
            jeu.borderColor = p.chart3;
            var ctx = graphiques.revenus.ctx;
            var degradé = ctx.createLinearGradient(0, 0, 0, graphiques.revenus.height || 200);
            degradé.addColorStop(0, p.chart3);
            degradé.addColorStop(1, p.chart4);
            jeu.backgroundColor = degradé;
            jeu.pointBackgroundColor = p.chart3;
            var o = graphiques.revenus.options;
            o.plugins.tooltip.backgroundColor = p.surface;
            o.plugins.tooltip.titleColor = p.text;
            o.plugins.tooltip.bodyColor = p.text;
            o.plugins.tooltip.borderColor = p.border;
            o.scales.x.ticks.color = p.muted;
            o.scales.x.border.color = p.border;
            o.scales.y.ticks.color = p.muted;
            o.scales.y.title.color = p.muted;
            o.scales.y.grid.color = p.border;
            graphiques.revenus.update('none');
        }
        rafraichirSparklines();
    }

    /* ---------- application des données ---------- */

    function rafraichirSparklines(stats) {
        var p = palette();
        document.querySelectorAll('[data-spark]').forEach(function (canvas) {
            var cle = canvas.getAttribute('data-spark');
            var valeurs = null;
            var ligne = p.chart1;
            if (cle === 'reservations' && stats.serieReservations7j && stats.serieReservations7j.valeurs) {
                valeurs = stats.serieReservations7j.valeurs;
                ligne = p.chart3;
            } else if (cle === 'stationnement' && stats.serie7j && stats.serie7j.entrees) {
                valeurs = stats.serie7j.entrees;
                ligne = p.chart1;
            }
            if (valeurs) dessinerSparkline(canvas, valeurs, ligne);
        });
    }

    var LETTRES = { LIBRE: 'L', OCCUPEE: 'O', RESERVEE: 'R', HORS_SERVICE: 'X' };
    var CLASSES = {
        LIBRE: 'parking-slot--libre',
        OCCUPEE: 'parking-slot--occupee',
        RESERVEE: 'parking-slot--reservee',
        HORS_SERVICE: 'parking-slot--hors-service'
    };
    var TOUTES_CLASSES = [CLASSES.LIBRE, CLASSES.OCCUPEE, CLASSES.RESERVEE, CLASSES.HORS_SERVICE];

    function rafraichirMiniPlan(stats) {
        if (!miniPlan) return;
        var parId = {};
        (stats.planParZone || []).forEach(function (groupe) {
            (groupe.places || []).forEach(function (place) { parId[place.id] = place; });
        });
        miniPlan.querySelectorAll('[data-mini-slot]').forEach(function (case_) {
            var place = parId[case_.getAttribute('data-place-id')];
            if (!place) return;
            var statut = place.statut in LETTRES ? place.statut : 'HORS_SERVICE';
            var precedent = case_.getAttribute('data-statut');
            case_.classList.remove.apply(case_.classList, TOUTES_CLASSES);
            case_.classList.add(CLASSES[statut]);
            case_.setAttribute('data-statut', statut);
            texte(case_.querySelector('.slot-number'), place.numero);
            texte(case_.querySelector('.mini-letter'), place.lettre || LETTRES[statut]);
            case_.setAttribute('title',
                place.numero + ' · ' + (place.zoneLibelle || ('Zone ' + place.zone)) +
                ' · ' + (place.typeLibelle || place.type) + ' · ' + (place.statutLibelle || statut) +
                (place.immatriculation ? ' · ' + place.immatriculation : ''));
            /* flash quand le statut change */
            if (precedent && precedent !== statut && !reduit()) {
                case_.classList.remove('slot-flash');
                void case_.offsetWidth;
                case_.classList.add('slot-flash');
                window.setTimeout(function () { case_.classList.remove('slot-flash'); }, 800);
            }
        });
        appliquerFiltreLegende();
    }

    /* ---------- légende filtrante du mini-plan ---------- */

    var boutonsLegende = Array.prototype.slice.call(document.querySelectorAll('[data-legend-filter]'));

    function appliquerFiltreLegende() {
        var actifs = boutonsLegende
            .filter(function (b) { return b.getAttribute('aria-pressed') === 'true'; })
            .map(function (b) { return b.getAttribute('data-legend-filter'); });
        miniPlan.querySelectorAll('[data-mini-slot]').forEach(function (case_) {
            if (!actifs.length) { case_.hidden = false; return; }
            case_.hidden = actifs.indexOf(case_.getAttribute('data-statut')) === -1;
        });
    }

    boutonsLegende.forEach(function (bouton) {
        bouton.addEventListener('click', function () {
            var presse = bouton.getAttribute('aria-pressed') === 'true';
            bouton.setAttribute('aria-pressed', presse ? 'false' : 'true');
            appliquerFiltreLegende();
        });
    });

    /* ---------- listes ---------- */

    function rafraichirListes(stats) {
        var conteneurResa = document.querySelector('[data-dash-list="reservations"]');
        if (conteneurResa) {
            conteneurResa.innerHTML = stats.reservationsRecentes.map(function () {
                return '<li class="dash-list-row">' +
                    '<span class="dash-list-icon metric-dot metric-dot--warning" aria-hidden="true"><i class="bi bi-calendar-check-fill"></i></span>' +
                    '<span class="dash-list-main"><span class="dash-list-title"></span>' +
                    '<span class="dash-list-sub"></span></span>' +
                    '<span class="badge"></span></li>';
            }).join('');
            var lignes = conteneurResa.querySelectorAll('.dash-list-row');
            stats.reservationsRecentes.forEach(function (r, index) {
                var ligne = lignes[index];
                if (!ligne) return;
                texte(ligne.querySelector('.dash-list-title'), r.utilisateur);
                texte(ligne.querySelector('.dash-list-sub'),
                    r.place + ' · ' + r.vehicule + ' · ' + (r.arriveeLibelle || '—'));
                var badge = ligne.querySelector('.badge');
                badge.classList.add(r.statutCss || 'bg-secondary');
                texte(badge, r.statutLibelle || r.statut);
            });
            var vide = document.querySelector('[data-dash-empty="reservations"]');
            if (vide) vide.hidden = stats.reservationsRecentes.length > 0;
            conteneurResa.hidden = stats.reservationsRecentes.length === 0;
        }

        var conteneurMouv = document.querySelector('[data-dash-list="mouvements"]');
        if (conteneurMouv) {
            conteneurMouv.innerHTML = stats.mouvementsRecents.map(function () {
                return '<li class="dash-list-row">' +
                    '<span class="dash-list-icon metric-dot" aria-hidden="true"><i class="bi"></i></span>' +
                    '<span class="dash-list-main"><span class="dash-list-title"></span>' +
                    '<span class="dash-list-sub"></span></span>' +
                    '<span class="badge"></span></li>';
            }).join('');
            var lignesM = conteneurMouv.querySelectorAll('.dash-list-row');
            stats.mouvementsRecents.forEach(function (m, index) {
                var ligne = lignesM[index];
                if (!ligne) return;
                var icone = ligne.querySelector('.dash-list-icon');
                icone.classList.add(m.enCours ? 'metric-dot--info' : 'metric-dot--success');
                icone.querySelector('i').classList.add(m.enCours ? 'bi-box-arrow-in-right' : 'bi-box-arrow-right');
                texte(ligne.querySelector('.dash-list-title'), m.immatriculation);
                var sousTitre = 'Place ' + m.place + ' · entrée ' + (m.entreeLibelle || '—');
                if (m.sortieLibelle) sousTitre += ' · sortie ' + m.sortieLibelle;
                texte(ligne.querySelector('.dash-list-sub'), sousTitre);
                var badge = ligne.querySelector('.badge');
                badge.classList.add(m.enCours ? 'bg-info' : 'bg-success');
                texte(badge, m.enCours ? 'En cours' : (m.montantLibelle || 'Payé'));
            });
            var videM = document.querySelector('[data-dash-empty="mouvements"]');
            if (videM) videM.hidden = stats.mouvementsRecents.length > 0;
            conteneurMouv.hidden = stats.mouvementsRecents.length === 0;
        }

        var conteneurZones = document.querySelector('[data-dash-list="zones"]');
        if (conteneurZones) {
            conteneurZones.innerHTML = stats.parZone.map(function () {
                return '<li class="dash-zone-row">' +
                    '<span class="dash-zone-name"></span>' +
                    '<span class="dash-zone-bar" aria-hidden="true"><span class="dash-zone-fill"></span></span>' +
                    '<span class="dash-zone-count"><strong></strong>/<span></span> · <span></span></span></li>';
            }).join('');
            var lignesZ = conteneurZones.querySelectorAll('.dash-zone-row');
            stats.parZone.forEach(function (z, index) {
                var ligne = lignesZ[index];
                if (!ligne) return;
                texte(ligne.querySelector('.dash-zone-name'), z.libelle || ('Zone ' + z.zone));
                var pourcentage = z.total > 0 ? Math.round(z.occupees * 100 / z.total) : 0;
                var remplissage = ligne.querySelector('.dash-zone-fill');
                remplissage.style.width = pourcentage + '%';
                remplissage.classList.toggle('is-warn', pourcentage >= 80 && pourcentage < 95);
                remplissage.classList.toggle('is-crit', pourcentage >= 95);
                texte(ligne.querySelector('.dash-zone-count strong'), String(z.occupees));
                var spans = ligne.querySelectorAll('.dash-zone-count span');
                texte(spans[0], String(z.total));
                texte(spans[1], pourcentage + ' %');
            });
            var videZ = document.querySelector('[data-dash-empty="zones"]');
            if (videZ) videZ.hidden = stats.parZone.length > 0;
            conteneurZones.hidden = stats.parZone.length === 0;
        }
    }

    /* ---------- toasts (nouveaux éléments, sans voler le focus) ---------- */

    function toast(message, icone) {
        var maintenant = Date.now();
        if (maintenant - dernierToast < 5000) return; /* au plus 1 toutes les 5 s */
        dernierToast = maintenant;
        if (typeof Swal === 'undefined') return;
        Swal.fire({
            toast: true,
            position: 'bottom-end',
            icon: icone || 'info',
            title: message,
            showConfirmButton: false,
            timer: 4000,
            timerProgressBar: true,
            allowOutsideClick: false,
            didOpen: function (element) { element.blur(); } /* le focus reste sur la page */
        });
    }

    function detecterNouveautes(stats) {
        var premiereResa = stats.reservationsRecentes.length ? stats.reservationsRecentes[0].id : null;
        var premierMouv = stats.mouvementsRecents.length ? stats.mouvementsRecents[0].id : null;
        if (derniersIds.reservations !== null) {
            if (premiereResa !== null && premiereResa !== derniersIds.reservations) {
                toast('Nouvelle réservation : ' + stats.reservationsRecentes[0].utilisateur, 'success');
            }
            if (premierMouv !== null && premierMouv !== derniersIds.mouvements) {
                toast('Entrée enregistrée : ' + stats.mouvementsRecents[0].immatriculation, 'info');
            }
        }
        derniersIds.reservations = premiereResa;
        derniersIds.mouvements = premierMouv;
    }

    /* ---------- application ---------- */

    var sparklinesCrees = false;

    function appliquer(stats) {
        var libres = stats.places.libres;
        var occupees = stats.places.occupees;
        var actives = stats.reservations.confirmees;
        var stationnement = stats.mouvements.enStationnement;

        compter(document.querySelector('[data-dash="libres"]'), libres);
        compter(document.querySelector('[data-dash="occupees"]'), occupees);
        compter(document.querySelector('[data-dash="reservations-actives"]'), actives);
        compter(document.querySelector('[data-dash="stationnement"]'), stationnement);

        majTrend('libres', libres);
        majTrend('occupees', occupees);
        majTrend('reservations-actives', actives);
        majTrend('stationnement', stationnement,
            stats.mouvements.variationStationnement !== undefined ? stats.mouvements.variationStationnement : null);

        valeursPrecedentes.libres = libres;
        valeursPrecedentes.occupees = occupees;
        valeursPrecedentes['reservations-actives'] = actives;
        valeursPrecedentes.stationnement = stationnement;

        texte(document.querySelector('[data-dash="taux"]'), formatTaux(stats.places.tauxOccupation));
        texte(document.querySelector('[data-dash="total"]'), formatNombre.format(stats.places.total));
        texte(document.querySelector('[data-dash="total-resume"]'), formatNombre.format(stats.places.total));
        texte(document.querySelector('[data-dash="libres-resume"]'), formatNombre.format(libres));
        texte(document.querySelector('[data-dash="occupees-resume"]'), formatNombre.format(occupees));
        texte(document.querySelector('[data-dash="res-attente"]'), formatNombre.format(stats.reservations.enAttente));
        texte(document.querySelector('[data-dash="res-confirmees"]'), formatNombre.format(stats.reservations.confirmees));
        texte(document.querySelector('[data-dash="res-annulees"]'), formatNombre.format(stats.reservations.annulees));
        texte(document.querySelector('[data-dash="res-terminees"]'), formatNombre.format(stats.reservations.terminees));
        texte(document.querySelector('[data-dash="abo-actifs"]'), formatNombre.format(stats.abonnements.actifs));
        texte(document.querySelector('[data-dash="res-total"]'), formatNombre.format(
            stats.reservations.enAttente + stats.reservations.confirmees +
            stats.reservations.annulees + stats.reservations.terminees));

        if (stats.revenus) {
            texte(document.querySelector('[data-dash="rev-jour"]'), formatMontant(stats.revenus.jour));
            texte(document.querySelector('[data-dash="rev-mois"]'), formatMontant(stats.revenus.mois));
            texte(document.querySelector('[data-dash="rev-moyenne"]'), formatMontant(stats.revenus.moyenneJour30));
            var elementVar = document.querySelector('[data-trend="rev-jour"]');
            if (elementVar && stats.revenus.variationJour !== undefined && stats.revenus.variationJour !== null) {
                var variation = Number(stats.revenus.variationJour);
                elementVar.classList.remove('is-up', 'is-down', 'is-flat');
                if (variation > 0) {
                    elementVar.classList.add('is-up');
                    texte(elementVar, '▲ +' + formatMontant(variation) + ' vs hier');
                } else if (variation < 0) {
                    elementVar.classList.add('is-down');
                    texte(elementVar, '▼ ' + formatMontant(variation) + ' vs hier');
                } else {
                    elementVar.classList.add('is-flat');
                    texte(elementVar, '= vs hier');
                }
            }
        }

        var videOccupation = document.querySelector('[data-dash-empty="occupation"]');
        var wrapDonut = document.querySelector('[data-dash-occupation-chart]');
        if (videOccupation && wrapDonut) {
            var sansPlaces = stats.places.total === 0;
            videOccupation.hidden = !sansPlaces;
            wrapDonut.hidden = sansPlaces;
        }

        rafraichirSparklines(stats);
        rafraichirMiniPlan(stats);
        rafraichirListes(stats);
        detecterNouveautes(stats);
        premierChargement = false;

        if (!sparklinesCrees && typeof Chart !== 'undefined') {
            creerGraphiques(stats);
            sparklinesCrees = true;
        } else if (sparklinesCrees) {
            majGraphiques(stats);
        }
    }

    function majGraphiques(stats) {
        if (typeof Chart === 'undefined') return;
        if (graphiques.occupation) {
            graphiques.occupation.data.datasets[0].data = [stats.places.libres, stats.places.reservees,
                stats.places.occupees, stats.places.horsService];
            graphiques.occupation.update();
        }
        if (graphiques.activite) {
            graphiques.activite.data.datasets.forEach(function (jeu) {
                if (jeu.label === 'Entrées') jeu.data = stats.serie7j.entrees;
                else if (jeu.label === 'Sorties') jeu.data = stats.serie7j.sorties;
                else if (jeu.label === 'Revenus (Ar)' && stats.serie7j.revenus) {
                    jeu.data = stats.serie7j.revenus.map(function (v) { return Number(v); });
                }
            });
            graphiques.activite.update();
        }
        if (graphiques.reservations) {
            graphiques.reservations.data.datasets[0].data = [stats.reservations.enAttente,
                stats.reservations.confirmees, stats.reservations.annulees, stats.reservations.terminees];
            graphiques.reservations.update();
        }
        if (graphiques.revenus && stats.serie7j.revenus) {
            graphiques.revenus.data.datasets[0].data = stats.serie7j.revenus.map(function (v) { return Number(v); });
            graphiques.revenus.update();
        }
    }

    /* ---------- rafraîchissement périodique ---------- */

    function rafraichir() {
        if (!enLigne) return;
        fetch('/accueil/stats', { headers: { 'X-Requested-With': 'XMLHttpRequest' }, credentials: 'same-origin' })
            .then(function (reponse) {
                if (!reponse.ok) throw new Error('HTTP ' + reponse.status);
                return reponse.json();
            })
            .then(function (stats) {
                dernierRafraichissement = Date.now();
                rafraichirBadge();
                appliquer(stats);
            })
            .catch(function () { /* erreur réseau ponctuelle : on garde l'affichage courant */ });
    }

    function demarrerMinuteur() {
        if (minuteur || pause || !enLigne) return;
        minuteur = window.setInterval(rafraichir, INTERVALLE_MS);
    }

    function arreterMinuteur() {
        if (!minuteur) return;
        window.clearInterval(minuteur);
        minuteur = null;
    }

    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            arreterMinuteur();
        } else if (!pause && enLigne) {
            rafraichir();
            demarrerMinuteur();
        }
    });

    /* ---------- état hors-ligne ---------- */

    window.addEventListener('offline', function () {
        enLigne = false;
        arreterMinuteur();
        rafraichirBadge();
    });

    window.addEventListener('online', function () {
        enLigne = true;
        rafraichir();
        if (!pause) demarrerMinuteur();
        rafraichirBadge();
    });

    /* ---------- raccourcis clavier (N, R, ?) ---------- */

    function zoneSaisie(cible) {
        if (!cible) return false;
        var balise = cible.tagName;
        return balise === 'INPUT' || balise === 'TEXTAREA' || balise === 'SELECT' || cible.isContentEditable;
    }

    function modaleOuverte() {
        return !!document.querySelector('.modal.show');
    }

    function aideRaccourcis() {
        if (typeof Swal === 'undefined') return;
        Swal.fire({
            title: 'Raccourcis clavier',
            html:
                '<div class="shortcut-row"><span><span class="shortcut-key">N</span> Enregistrer une entrée</span></div>' +
                '<div class="shortcut-row"><span><span class="shortcut-key">R</span> Actualiser maintenant</span></div>' +
                '<div class="shortcut-row"><span><span class="shortcut-key">?</span> Afficher cette aide</span></div>',
            confirmButtonText: 'Fermer',
            confirmButtonColor: getComputedStyle(RACINE).getPropertyValue('--parking-accent').trim() || '#4d8dff',
            width: 420
        });
    }

    document.addEventListener('keydown', function (event) {
        if (zoneSaisie(event.target) || modaleOuverte()) return;
        if (event.ctrlKey || event.metaKey || event.altKey) return;
        if (event.key === 'n' || event.key === 'N') {
            var lien = document.querySelector('[data-crud-open][data-crud-title="Enregistrer une entrée"]');
            if (lien) { event.preventDefault(); lien.click(); }
        } else if (event.key === 'r' || event.key === 'R') {
            event.preventDefault();
            rafraichir();
        } else if (event.key === '?') {
            event.preventDefault();
            aideRaccourcis();
        }
    });

    /* ---------- projecteur de carte (curseur) ---------- */

    document.querySelectorAll('.dash-glow').forEach(function (carte) {
        carte.addEventListener('mousemove', function (event) {
            var rect = carte.getBoundingClientRect();
            carte.style.setProperty('--mx', (event.clientX - rect.left) + 'px');
            carte.style.setProperty('--my', (event.clientY - rect.top) + 'px');
        });
    });

    /* ---------- cascade du mini-plan (≤ 400 ms) ---------- */

    function casserMiniPlan() {
        if (!miniPlan || reduit()) return;
        var index = 0;
        miniPlan.querySelectorAll('[data-mini-slot]').forEach(function (case_) {
            case_.style.animationDelay = Math.min(index * 6, 240) + 'ms';
            index += 1;
        });
    }

    /* ---------- réaction à la bascule de thème ---------- */

    document.addEventListener('parkflow:themechange', recolorerGraphiques);
    document.addEventListener('parkflow:accentchange', recolorerGraphiques);

    if (typeof MutationObserver === 'function') {
        var observateur = new MutationObserver(function (mutations) {
            var change = mutations.some(function (mutation) {
                return mutation.attributeName === 'data-bs-theme';
            });
            if (change) recolorerGraphiques();
        });
        observateur.observe(RACINE, { attributes: true, attributeFilter: ['data-bs-theme'] });
    }

    /* ---------- amorçage ---------- */

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach(function (element) {
            if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) new bootstrap.Tooltip(element);
        });
        if (boutonPause) {
            boutonPause.setAttribute('aria-pressed', pause ? 'true' : 'false');
            var icone = boutonPause.querySelector('i');
            if (icone && pause) icone.className = 'bi bi-play-fill';
        }
        casserMiniPlan();
        rafraichirBadge();
        rafraichir();
        if (!pause) demarrerMinuteur();
    });
})();
