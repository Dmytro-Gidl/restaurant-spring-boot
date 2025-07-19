document.addEventListener('DOMContentLoaded', function() {
    const pos = localStorage.getItem('menuScrollPos');
    if (pos) {
        window.scrollTo(0, parseInt(pos));
        localStorage.removeItem('menuScrollPos');
    }
});
window.addEventListener('beforeunload', function() {
    localStorage.setItem('menuScrollPos', window.scrollY);
});
