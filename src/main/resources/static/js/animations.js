/* ==========================================================================
   ParkFlow — animations complémentaires (ce que le CSS ne peut pas faire)
   - cascade des éléments (variable CSS --i)
   - révélation au scroll (IntersectionObserver, une seule fois)
   - compteurs animés (easeOutCubic, 1,2 s, valeur finale exacte)
   - remplissage de la barre d'occupation
   - page de connexion (fondu/zoom, cascade des champs, Ken Burns)
   - héros de la landing (cascade + tracé de l'ellipse .hl)
   - fondu de sortie vers la page suivante
   Respecte prefers-reduced-motion : aucun mouvement, valeurs finales directes.
   ========================================================================== */
(function () {
  'use strict';

  var STAGGER_STEP = 60;
  var STAGGER_MAX = 12;
  var COUNT_DURATION = 1200;
  var EXIT_DURATION = 160;
  var EASE = 'cubic-bezier(0.22, 1, 0.36, 1)';

  var motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)');

  function reduced() {
    return motionQuery.matches;
  }

  function easeOutCubic(t) {
    return 1 - Math.pow(1 - t, 3);
  }

  function each(list, fn) {
    Array.prototype.forEach.call(list, fn);
  }

  /* ---------------------------------------------------------------- a) Cascade */
  function initStagger() {
    var scope = document.getElementById('main-content');
    if (!scope) return;

    var items = scope.querySelectorAll(
      '.card, .status-tile, .quick-action, .parking-slot, ' +
        '.table tbody tr, .list-group-item, .empty-state'
    );
    var index = 0;

    each(items, function (el) {
      if (el.closest('.reveal')) return;
      if (el.classList.contains('card') && el.querySelector('table')) return;
      if (!el.offsetParent) return;

      el.style.setProperty('--i', String(Math.min(index, STAGGER_MAX - 1)));
      el.classList.add('stagger-item');
      index += 1;
    });
  }

  /* ------------------------------------------------------- b) Révélation au scroll */
  function initReveal() {
    var items = document.querySelectorAll('.reveal');
    if (!items.length) return;

    if (reduced() || !('IntersectionObserver' in window)) {
      each(items, function (el) {
        el.classList.add('is-visible');
      });
      return;
    }

    var observer = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (!entry.isIntersecting) return;
          entry.target.classList.add('is-visible');
          observer.unobserve(entry.target);
        });
      },
      { threshold: 0.12, rootMargin: '0px 0px -32px 0px' }
    );

    each(items, function (el) {
      observer.observe(el);
    });
  }

  /* --------------------------------------------------------------- c) Compteurs */
  function initCounters() {
    if (reduced()) return;

    var items = document.querySelectorAll('.metric-value, .summary-value');

    each(items, function (el) {
      var raw = el.textContent.replace(/ /g, ' ').trim();
      var digits = raw.replace(/\s/g, '');
      if (!/^-?\d+([.,]\d+)?$/.test(digits)) return;

      var target = parseFloat(digits.replace(',', '.'));
      if (!isFinite(target) || target === 0) return;

      var decimals = (digits.split(/[.,]/)[1] || '').length;
      var grouped = /\s/.test(raw);
      var start = null;

      function format(value) {
        var parts = value.toFixed(decimals).split('.');
        if (grouped) parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ' ');
        return decimals > 0 ? parts[0] + ',' + parts[1] : parts[0];
      }

      function frame(now) {
        if (start === null) start = now;
        var progress = Math.min((now - start) / COUNT_DURATION, 1);
        el.textContent = format(target * easeOutCubic(progress));
        if (progress < 1) {
          requestAnimationFrame(frame);
        } else {
          el.textContent = raw;
        }
      }

      el.textContent = format(0);
      requestAnimationFrame(frame);
    });
  }

  /* -------------------------------------------- d) Barre d'occupation (remplissage) */
  function initProgress() {
    if (reduced()) return;

    var bars = document.querySelectorAll('.progress-bar');

    each(bars, function (bar) {
      if (!bar.style.width) return;

      bar.classList.add('is-primed');
      void bar.offsetWidth;
      bar.classList.add('is-filling');
      bar.classList.remove('is-primed');

      window.setTimeout(function () {
        bar.classList.remove('is-filling');
      }, COUNT_DURATION);
    });
  }

  /* ------------------------------------------- j) Héros de la landing
     Cascade des éléments du héros + tracé de l'ellipse autour du mot en
     emphase (.hl). Le flottement de l'illustration vit en CSS (.float-slow). */
  function initHero() {
    var hero = document.querySelector('[data-anim="hero"]');
    /* Tracé de l'ellipse : héros de la landing + titres de page (.page-title .hl) */
    var marks = document.querySelectorAll('.hl-mark');
    each(marks, function (mark) {
      mark.classList.add('is-drawn');
    });
    if (!hero) return;

    if (reduced()) return;

    var items = hero.querySelectorAll('[data-hero-item]');
    each(items, function (el, i) {
      if (!el.animate) return;
      el.animate(
        [
          { opacity: 0, transform: 'translateY(18px)' },
          { opacity: 1, transform: 'none' }
        ],
        {
          duration: 600,
          delay: 120 + Math.min(i, STAGGER_MAX - 1) * STAGGER_STEP,
          easing: EASE,
          fill: 'both'
        }
      );
    });
  }

  /* ------------------------------------------- k) Page de connexion (hors layout) */
  function isLoginPage() {
    return !!document.querySelector('[data-anim="card"]');
  }

  function initLogin() {
    if (!isLoginPage() || reduced()) return;

    var card = document.querySelector('[data-anim="card"]');
    if (card && card.animate) {
      card.animate(
        [
          { opacity: 0, transform: 'scale(0.97)' },
          { opacity: 1, transform: 'none' }
        ],
        { duration: 500, easing: EASE, fill: 'both' }
      );
    }

    var form = document.querySelector('main form');
    if (form && form.children[0] && form.children[0].animate) {
      each(form.children, function (child, i) {
        if (i >= STAGGER_MAX) return;
        child.animate(
          [
            { opacity: 0, transform: 'translateY(12px)' },
            { opacity: 1, transform: 'none' }
          ],
          {
            duration: 450,
            delay: 320 + i * STAGGER_STEP,
            easing: EASE,
            fill: 'both'
          }
        );
      });
    }

    var image = document.querySelector('[data-anim="kenburns"]');
    if (image && image.animate) {
      image.animate(
        [
          { transform: 'scale(1)' },
          { transform: 'scale(1.06)' }
        ],
        {
          duration: 20000,
          direction: 'alternate',
          iterations: Infinity,
          easing: 'ease-in-out',
          fill: 'both'
        }
      );
    }

    var mark = document.querySelector('main a[aria-label] span');
    if (mark && mark.animate) {
      mark.animate(
        [
          { transform: 'translateY(0)' },
          { transform: 'translateY(-3px)' }
        ],
        {
          duration: 4000,
          direction: 'alternate',
          iterations: Infinity,
          easing: 'ease-in-out'
        }
      );
    }
  }

  /* ------------------------------------------------ n) Fondu de sortie de page */
  function initPageExit() {
    document.addEventListener('click', function (event) {
      if (
        event.defaultPrevented ||
        event.button !== 0 ||
        event.metaKey ||
        event.ctrlKey ||
        event.shiftKey ||
        event.altKey
      ) {
        return;
      }

      var link =
        event.target && event.target.closest
          ? event.target.closest('a[href]')
          : null;
      if (!link || link.classList.contains('skip-link')) return;
      if (link.target && link.target !== '_self') return;
      if (link.hasAttribute('download') || link.hasAttribute('data-bs-toggle'))
        return;
      /* Opérations CRUD : le formulaire s'ouvre en modale, pas de changement de page. */
      if (link.hasAttribute('data-crud-open') || link.hasAttribute('data-crud-report')) return;
      if (link.closest('#crudModal')) return;

      var href = link.getAttribute('href');
      if (!href || href.charAt(0) === '#') return;
      if (/^(mailto:|tel:|javascript:)/i.test(href)) return;

      var url;
      try {
        url = new URL(href, window.location.href);
      } catch (error) {
        return;
      }
      if (url.origin !== window.location.origin) return;
      if (url.pathname === window.location.pathname && url.hash) return;

      event.preventDefault();

      if (reduced()) {
        window.location.href = url.href;
        return;
      }

      document.body.classList.add('page-leaving');
      window.setTimeout(function () {
        window.location.href = url.href;
      }, EXIT_DURATION);
    });

    window.addEventListener('pageshow', function () {
      document.body.classList.remove('page-leaving');
    });

    /* Bouton « Réessayer » des pages d'erreur : aucun JavaScript en ligne. */
    document.addEventListener('click', function (event) {
      var bouton =
        event.target && event.target.closest
          ? event.target.closest('[data-retry]')
          : null;
      if (!bouton) return;
      event.preventDefault();
      window.location.reload();
    });
  }

  function init() {
    if (!reduced()) document.documentElement.classList.add('anim-js');
    initStagger();
    initReveal();
    initCounters();
    initProgress();
    initHero();
    initLogin();
    initPageExit();
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
