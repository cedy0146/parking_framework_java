document.addEventListener('DOMContentLoaded', () => {
    const slots = [...document.querySelectorAll('[data-parking-slot]')];
    const filter = document.querySelector('[data-zone-filter]');
    if (!filter || !slots.length) return;

    [...new Set(slots.map((slot) => slot.dataset.zone).filter(Boolean))].sort().forEach((zone) => {
        const option = new Option(`Zone ${zone}`, zone);
        filter.add(option);
    });
    filter.addEventListener('change', () => {
        slots.forEach((slot) => { slot.hidden = filter.value && slot.dataset.zone !== filter.value; });
    });
    document.querySelectorAll('[data-bs-toggle="tooltip"]').forEach((element) => new bootstrap.Tooltip(element));
});
