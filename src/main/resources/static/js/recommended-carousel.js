document.addEventListener('DOMContentLoaded', function () {
    const wrapper = document.querySelector('.rec-wrapper');
    const carousel = document.querySelector('.recommended-carousel');
    if (!wrapper || !carousel) return;
    const prev = document.querySelector('.rec-prev');
    const next = document.querySelector('.rec-next');
    const toggle = document.querySelector('.rec-toggle');
    const hint = document.querySelector('.rec-hint');
    const scrollAmount = 300;
    const storageKey = 'recOpen';

    function restoreOpenState() {
        const open = localStorage.getItem(storageKey);
        const icon = toggle ? toggle.querySelector('i') : null;
        if (open === 'false') {
            carousel.classList.add('collapsed');
            if (icon) {
                icon.classList.add('fa-chevron-down');
                icon.classList.remove('fa-chevron-up');
            }
            if (hint) hint.style.display = '';
        } else if (open === 'true') {
            carousel.classList.remove('collapsed');
            if (icon) {
                icon.classList.remove('fa-chevron-down');
                icon.classList.add('fa-chevron-up');
            }
            if (hint) hint.style.display = 'none';
            updateNavVisibility();
        }
    }

    function updateNavVisibility() {
        if (!prev || !next) return;
        if (wrapper.scrollWidth <= wrapper.clientWidth) {
            prev.style.display = 'none';
            next.style.display = 'none';
        } else {
            prev.style.display = '';
            next.style.display = '';
        }
    }

    prev.addEventListener('click', function () {
        wrapper.scrollBy({ left: -scrollAmount, behavior: 'smooth' });
    });
    next.addEventListener('click', function () {
        wrapper.scrollBy({ left: scrollAmount, behavior: 'smooth' });
    });

    if (toggle) {
        toggle.addEventListener('click', function () {
            carousel.classList.toggle('collapsed');
            const open = !carousel.classList.contains('collapsed');
            const icon = toggle.querySelector('i');
            if (icon) {
                icon.classList.toggle('fa-chevron-down');
                icon.classList.toggle('fa-chevron-up');
            }
            localStorage.setItem(storageKey, String(open));
            if (hint) hint.style.display = open ? 'none' : '';
            if (open) {
                updateNavVisibility();
            }
        });
    }

    restoreOpenState();

    updateNavVisibility();
    window.addEventListener('resize', updateNavVisibility);
    window.updateRecNavVisibility = updateNavVisibility;
});
