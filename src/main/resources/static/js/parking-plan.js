document.addEventListener('DOMContentLoaded', () => {
    const slots = [...document.querySelectorAll('[data-parking-slot]')];
    const filter = document.querySelector('[data-zone-filter]');
    if (!slots.length) return;

    /* Scroll vers la place ciblée par l'ancre (#place-12) et flash. */
    const cible = window.location.hash ? document.querySelector(window.location.hash) : null;
    if (cible && cible.matches('[data-parking-slot]')) {
        cible.scrollIntoView({ block: 'center' });
        cible.classList.add('slot-flash');
        window.setTimeout(() => cible.classList.remove('slot-flash'), 900);
    }

    if (!filter) return;

    /* Libellés de zone parlants fournis par le rendu serveur (Labels.zone). */
    const libelles = new Map();
    slots.forEach((slot) => {
        if (slot.dataset.zone && !libelles.has(slot.dataset.zone)) {
            libelles.set(slot.dataset.zone, slot.dataset.zoneLabel || `Zone ${slot.dataset.zone}`);
        }
    });
    [...libelles.keys()].sort().forEach((zone) => {
        filter.add(new Option(libelles.get(zone), zone));
    });
    filter.addEventListener('change', () => {
        slots.forEach((slot) => { slot.hidden = filter.value && slot.dataset.zone !== filter.value; });
    });
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach((element) => new bootstrap.Tooltip(element));
});
