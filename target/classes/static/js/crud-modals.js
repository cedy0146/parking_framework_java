/*
 * ParkFlow — opérations CRUD en modale.
 *
 * Principe : aucune opération n'ouvre une nouvelle page.
 *  - [data-crud-open]           : lien GET → fragment de formulaire injecté dans #crudModal ;
 *                                 sans JS, l'href reste une page complète.
 *  - form[data-crud-confirm]    : POST → confirmation SweetAlert2 puis envoi en fetch ;
 *                                 sans JS, le formulaire est soumis normalement (flash).
 *  - [data-crud-report]         : GET JSON → récapitulatif affiché en alerte.
 * Toutes les réponses serveur attendues sont JSON : {"success":bool,"message":str},
 * sauf la validation (422) qui renvoie le fragment HTML du formulaire.
 * En cas d'absence de Bootstrap/Swal, aucun interception : le site reste natif.
 */
(function () {
    'use strict';

    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    var modalEl = document.getElementById('crudModal');
    var bodyEl = document.getElementById('crudModalBody');
    var titleEl = document.getElementById('crudModalLabel');
    var loaderHTML = bodyEl ? bodyEl.innerHTML : '';
    var modal = null;

    var canEnhance = Boolean(
        window.bootstrap && window.Swal && modalEl && bodyEl && titleEl
    );

    /* ---------- utilitaires ---------- */

    function meta(name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.content : null;
    }

    /* Entête X-Requested-With + jeton CSRF (balises <meta> du layout). */
    function headers() {
        var h = { 'X-Requested-With': 'XMLHttpRequest' };
        var token = meta('_csrf');
        var name = meta('_csrf_header');
        if (token && name) h[name] = token;
        return h;
    }

    function escapeHtml(value) {
        return String(value === null || value === undefined ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    /* Animations Swal neutralisées si l'utilisateur limite les mouvements. */
    function motion(extra) {
        var base = extra || {};
        if (reduceMotion) {
            base.showClass = { popup: '' };
            base.hideClass = { popup: '' };
        }
        return base;
    }

    function toast(icon, message) {
        return window.Swal.fire(motion({
            toast: true,
            position: 'top-end',
            icon: icon,
            title: message,
            showConfirmButton: false,
            timer: 3200,
            timerProgressBar: true
        }));
    }

    function erreurHttp(fallback) {
        return function (response) {
            if (response.ok) return response;
            return response.json()
                .catch(function () { return null; })
                .then(function (data) {
                    toast('error', (data && data.message) || fallback);
                    throw new Error('HTTP ' + response.status);
                });
        };
    }

    function recharger() {
        window.location.reload();
    }

    /* Message lisible quand la réponse n'est pas le JSON attendu (403, 405…). */
    function messageStatut(status) {
        if (status === 401) return 'Session expirée : reconnectez-vous.';
        if (status === 403) return 'Accès refusé : vos droits ne permettent pas cette action.';
        if (status === 404) return 'Ressource introuvable.';
        if (status === 405) return 'Action non autorisée.';
        if (status === 500) return 'Erreur serveur : veuillez réessayer plus tard.';
        return 'Une erreur est survenue (' + status + ').';
    }

    /* ---------- modale ---------- */

    function instance() {
        if (!modal) modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
        return modal;
    }

    function afficherModale(titre) {
        titleEl.textContent = titre || 'Formulaire';
        bodyEl.innerHTML = loaderHTML;
        var loader = bodyEl.querySelector('#crudLoader');
        if (loader) loader.hidden = false;
        instance().show();
    }

    function focusChamp() {
        var champ = bodyEl.querySelector('input:not([type="hidden"]), select, textarea, button');
        if (champ) champ.focus();
    }

    function ouvrirFormulaire(trigger) {
        var url = trigger.getAttribute('href');
        if (!url) return;
        afficherModale(trigger.getAttribute('data-crud-title'));
        fetch(url, { headers: headers(), credentials: 'same-origin' })
            .then(erreurHttp('Impossible de charger le formulaire.'))
            .then(function (response) { return response.text(); })
            .then(function (html) {
                bodyEl.innerHTML = html;
                focusChamp();
            })
            .catch(function () {
                instance().hide();
            });
    }

    /* Soumission du formulaire de la modale : 422 → fragment, sinon JSON. */
    function soumettreFormulaire(form) {
        var bouton = form.querySelector('[type="submit"]');
        if (bouton) bouton.disabled = true;

        fetch(form.action, {
            method: (form.method || 'POST').toUpperCase(),
            headers: headers(),
            body: new FormData(form),
            credentials: 'same-origin'
        })
            .then(function (response) {
                var type = response.headers.get('content-type') || '';
                if (type.indexOf('text/html') !== -1) {
                    return response.text().then(function (html) {
                        return { fragment: html, status: response.status };
                    });
                }
                return response.json().then(function (data) {
                    return { data: data, status: response.status };
                });
            })
            .then(function (resultat) {
                if (resultat.fragment) {
                    if (resultat.status === 422) {
                        // Validation KO : on réaffiche le formulaire avec ses erreurs.
                        bodyEl.innerHTML = resultat.fragment;
                        focusChamp();
                        return;
                    }
                    toast('error', messageStatut(resultat.status));
                    return;
                }
                if (resultat.data && resultat.data.success) {
                    toast('success', resultat.data.message || 'Opération effectuée');
                    instance().hide();
                    window.setTimeout(recharger, 1300);
                    return;
                }
                toast('error', (resultat.data && resultat.data.message) || 'Opération impossible.');
            })
            .catch(function () {
                toast('error', 'Une erreur est survenue. Veuillez réessayer.');
            })
            .then(function () {
                if (bouton) bouton.disabled = false;
            });
    }

    /* ---------- actions de ligne (POST avec confirmation) ---------- */

    function confirmerEtEnvoyer(form) {
        var message = form.getAttribute('data-crud-confirm') || 'Confirmer cette opération ?';
        window.Swal.fire(motion({
            icon: 'warning',
            title: message,
            showCancelButton: true,
            confirmButtonText: 'Confirmer',
            cancelButtonText: 'Annuler'
        })).then(function (resultat) {
            if (resultat.isConfirmed) envoyer(form);
        });
    }

    function envoyer(form) {
        var bouton = form.querySelector('[type="submit"]');
        if (bouton) bouton.disabled = true;

        fetch(form.action, {
            method: 'POST',
            headers: headers(),
            body: new FormData(form),
            credentials: 'same-origin'
        })
            .then(function (response) {
                var type = response.headers.get('content-type') || '';
                if (type.indexOf('application/json') !== -1) {
                    return response.json().then(function (data) { return { data: data }; });
                }
                return { status: response.status };
            })
            .then(function (resultat) {
                if (!resultat.data) {
                    toast('error', messageStatut(resultat.status || 500));
                    return;
                }
                if (resultat.data.success) {
                    toast('success', resultat.data.message || 'Opération effectuée');
                    window.setTimeout(recharger, 1300);
                    return;
                }
                toast('error', resultat.data.message || 'Opération impossible.');
            })
            .catch(function () {
                toast('error', 'Réponse invalide du serveur.');
            })
            .then(function () {
                if (bouton) bouton.disabled = false;
            });
    }

    /* ---------- récapitulatif (facturation) ---------- */

    var LIBELLES = {
        vehiculeImmatriculation: 'Véhicule',
        placeNumero: 'Place',
        duree: 'Durée',
        tarifHoraire: 'Tarif horaire',
        montantTotal: 'Montant total',
        aAbonnement: 'Abonnement actif',
        detailsCalcul: 'Détail du calcul'
    };

    function afficherRapport(trigger) {
        var url = trigger.getAttribute('href');
        if (!url) return;
        fetch(url, { headers: headers(), credentials: 'same-origin' })
            .then(erreurHttp('Facturation indisponible.'))
            .then(function (response) { return response.json(); })
            .then(function (data) {
                if (!data || data.success === false) {
                    toast('error', (data && data.message) || 'Facturation indisponible.');
                    return;
                }
                var lignes = '';
                Object.keys(LIBELLES).forEach(function (cle) {
                    var valeur = data[cle];
                    if (valeur === null || valeur === undefined || valeur === '') return;
                    if (cle === 'aAbonnement') valeur = valeur ? 'Oui' : 'Non';
                    lignes += '<div class="crud-report-row">'
                        + '<dt>' + escapeHtml(LIBELLES[cle]) + '</dt>'
                        + '<dd>' + escapeHtml(valeur) + '</dd>'
                        + '</div>';
                });
                window.Swal.fire(motion({
                    icon: 'info',
                    title: 'Facturation',
                    html: '<dl class="crud-report">' + (lignes || '<div class="crud-report-row"><dd>Aucune information.</dd></div>') + '</dl>',
                    confirmButtonText: 'Fermer'
                }));
            })
            .catch(function () {
                toast('error', 'Facturation indisponible.');
            });
    }

    /* ---------- messages de redirection (flash) ---------- */

    function messagesFlash() {
        [['flash-success', 'success'], ['flash-error', 'error']].forEach(function (bloc) {
            var el = document.getElementById(bloc[0]);
            if (!el) return;
            var texte = (el.querySelector('span') || el).textContent.trim();
            el.classList.add('d-none');
            if (texte) toast(bloc[1], texte);
        });
    }

    /* ---------- écouteurs ---------- */

    if (canEnhance) {
        document.addEventListener('click', function (event) {
            var ouvrir = event.target.closest('[data-crud-open]');
            if (ouvrir) {
                event.preventDefault();
                ouvrirFormulaire(ouvrir);
                return;
            }
            var rapport = event.target.closest('[data-crud-report]');
            if (rapport) {
                event.preventDefault();
                afficherRapport(rapport);
                return;
            }
            // Tout lien à l'intérieur de la modale (Annuler) la referme.
            if (event.target.closest('#crudModal a')) {
                event.preventDefault();
                instance().hide();
            }
        });

        document.addEventListener('submit', function (event) {
            var form = event.target;
            if (form.matches('form[data-crud-confirm]')) {
                event.preventDefault();
                confirmerEtEnvoyer(form);
                return;
            }
            if (modalEl.contains(form)) {
                event.preventDefault();
                soumettreFormulaire(form);
            }
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            bodyEl.innerHTML = loaderHTML;
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', messagesFlash);
    } else {
        messagesFlash();
    }
})();
