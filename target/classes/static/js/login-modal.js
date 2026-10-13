/* ==========================================================================
   ParkFlow — Modale de connexion (landing)
   Ouvre #loginModal depuis tout élément [data-login-open] et depuis les
   paramètres d'URL (?login, ?login&error, ?login&logout, ?login&expired).
   La soumission se fait en AJAX (fetch, redirect « follow ») : l'URL de la
   page ne change pas tant que la connexion n'a pas réussi.
   ========================================================================== */
(function () {
  'use strict';

  var modalEl = document.getElementById('loginModal');
  if (!modalEl) return;

  var form = document.getElementById('loginModalForm');
  var usernameInput = document.getElementById('login-username');
  var passwordInput = document.getElementById('login-password');
  var pwToggle = document.getElementById('loginPwToggle');
  var alertBox = document.getElementById('loginModalAlert');
  var submitBtn = document.getElementById('loginModalSubmit');
  var submitLabel = submitBtn ? submitBtn.querySelector('.login-modal-submit-label') : null;
  var spinner = submitBtn ? submitBtn.querySelector('.login-modal-spinner') : null;

  var bsModal = null;
  var lastTrigger = null;
  var busy = false;

  function getModal() {
    if (!bsModal && window.bootstrap && bootstrap.Modal) {
      bsModal = bootstrap.Modal.getOrCreateInstance(modalEl);
    }
    return bsModal;
  }

  var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

  /* ---------- ouverture / fermeture ---------- */

  function openModal(trigger) {
    lastTrigger = trigger || null;
    var m = getModal();
    if (m) m.show();
  }

  function closeModal() {
    var m = getModal();
    if (m) m.hide();
  }

  /* Tout élément [data-login-open] ouvre la modale (aucun lien vers /login). */
  document.addEventListener('click', function (e) {
    var trigger = e.target && e.target.closest ? e.target.closest('[data-login-open]') : null;
    if (!trigger) return;
    e.preventDefault();
    openModal(trigger);
  });

  /* Focus : e-mail à l'ouverture, rendu au déclencheur à la fermeture. */
  modalEl.addEventListener('shown.bs.modal', function () {
    if (usernameInput) {
      try { usernameInput.focus({ preventScroll: true }); } catch (err) { usernameInput.focus(); }
    }
  });

  modalEl.addEventListener('hidden.bs.modal', function () {
    if (lastTrigger && document.contains(lastTrigger) && lastTrigger.focus) {
      try { lastTrigger.focus({ preventScroll: true }); } catch (err) { lastTrigger.focus(); }
    }
    lastTrigger = null;
  });

  /* ---------- œil afficher / masquer ---------- */

  if (pwToggle && passwordInput) {
    pwToggle.addEventListener('click', function () {
      var show = passwordInput.type === 'password';
      passwordInput.type = show ? 'text' : 'password';
      pwToggle.setAttribute('aria-label', show ? 'Masquer le mot de passe' : 'Afficher le mot de passe');
      pwToggle.setAttribute('title', show ? 'Masquer le mot de passe' : 'Afficher le mot de passe');
      var icon = pwToggle.querySelector('i');
      if (icon) icon.className = show ? 'bi bi-eye-slash' : 'bi bi-eye';
    });
  }

  /* ---------- état d'erreur en ligne (shake) ---------- */

  function showError(message) {
    if (!alertBox) return;
    alertBox.textContent = message;
    alertBox.hidden = false;
    alertBox.classList.remove('is-shaking');
    /* forcer un reflow pour rejouer l'animation */
    void alertBox.offsetWidth;
    alertBox.classList.add('is-shaking');
    if (passwordInput) passwordInput.value = '';
    if (usernameInput) {
      try { usernameInput.focus({ preventScroll: true }); } catch (err) { usernameInput.focus(); }
    }
  }

  function clearError() {
    if (!alertBox) return;
    alertBox.hidden = true;
    alertBox.textContent = '';
    alertBox.classList.remove('is-shaking');
  }

  /* ---------- état de chargement ---------- */

  function setBusy(on) {
    busy = on;
    if (submitBtn) submitBtn.disabled = on;
    if (spinner) spinner.hidden = !on;
    if (submitLabel) submitLabel.textContent = on ? 'Connexion en cours…' : 'Se connecter';
    if (usernameInput) usernameInput.readOnly = on;
    if (passwordInput) passwordInput.readOnly = on;
  }

  /* ---------- toasts SweetAlert2 ---------- */

  function toast(icon, title) {
    if (!window.Swal) return Promise.resolve();
    return Swal.fire({
      icon: icon,
      title: title,
      showConfirmButton: false,
      timer: icon === 'success' ? 900 : 2600,
      timerProgressBar: true
    });
  }

  /* ---------- soumission AJAX ---------- */

  if (form) {
    form.addEventListener('submit', function (e) {
      e.preventDefault();
      if (busy) return;
      clearError();
      setBusy(true);

      var data = new FormData(form);
      var body = new URLSearchParams();
      data.forEach(function (value, key) {
        body.append(key, value);
      });

      fetch(form.action, {
        method: 'POST',
        body: body,
        credentials: 'same-origin',
        redirect: 'follow',
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      })
        .then(function (response) {
          var finalUrl = response.url || '';
          /* Échec : retour sur /login ou paramètre nu ?login&error (sans "="). */
          var failed = !response.ok || /\/login([/?]|$)|[?&](error|expired)([=&]|$)/.test(finalUrl);
          if (failed) {
            setBusy(false);
            showError('E-mail ou mot de passe incorrect');
            return null;
          }
          /* Succès : toast puis navigation vers l'URL finale (/accueil). */
          return toast('success', 'Connexion réussie').then(function () {
            window.location.href = finalUrl;
          });
        })
        .catch(function () {
          setBusy(false);
          showError('Connexion impossible, réessayez plus tard');
        });
    });
  }

  /* ---------- paramètres d'URL (?login…) ---------- */

  function handleLoginParams() {
    var params = new URLSearchParams(window.location.search);
    if (!params.has('login')) return;

    openModal(null);

    if (params.has('error')) {
      showError('E-mail ou mot de passe incorrect');
    } else if (params.has('expired')) {
      showError('Session expirée, veuillez vous reconnecter');
    } else if (params.has('logout')) {
      toast('success', 'Vous avez été déconnecté');
    }

    /* Nettoyer l'URL sans recharger. */
    params.delete('login');
    params.delete('error');
    params.delete('logout');
    params.delete('expired');
    var qs = params.toString();
    history.replaceState(null, '', window.location.pathname + (qs ? '?' + qs : '') + window.location.hash);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', handleLoginParams);
  } else {
    handleLoginParams();
  }
})();
