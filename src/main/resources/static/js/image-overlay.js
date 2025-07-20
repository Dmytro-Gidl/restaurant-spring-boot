var overlayOpen = false;
window.addEventListener('DOMContentLoaded', function() {
    var overlay = document.getElementById('imageOverlay');
    var content = document.getElementById('overlayContent');
    var prevBtn = document.getElementById('overlayPrev');
    var nextBtn = document.getElementById('overlayNext');
    var images = [];
    var index = 0;
    var dishName = '';

    document.querySelectorAll('.single-menu img').forEach(function(img) {
        img.addEventListener('click', function(event) {
            var menu = event.target.closest('.single-menu');
            if(!menu) return;
            var imagesAttr = menu.getAttribute('data-images');
            if(!imagesAttr) return;
            dishName = menu.getAttribute('data-dish-name') || '';
            images = imagesAttr.split(',');
            index = 0;
            show();
            overlay.style.display = 'flex';
            overlayOpen = true;
    });
});

    function show() {
        content.innerHTML = '';
        if(images.length === 0) return;
        var img = document.createElement('img');
        img.src = images[index];
        img.classList.add('overlay-image');
        content.appendChild(img);
        var counter = document.createElement('div');
        counter.classList.add('image-counter');
        counter.textContent = (index + 1) + '/' + images.length;
        content.appendChild(counter);

        if(prevBtn) prevBtn.style.visibility = index > 0 ? 'visible' : 'hidden';
        if(nextBtn) nextBtn.style.visibility = index < images.length - 1 ? 'visible' : 'hidden';
    }

    document.getElementById('overlayPrev').addEventListener('click', function(e){
        e.stopPropagation();
        if(images.length === 0) return;
        index = (index - 1 + images.length) % images.length;
        show();
    });

    document.getElementById('overlayNext').addEventListener('click', function(e){
        e.stopPropagation();
        if(images.length === 0) return;
        index = (index + 1) % images.length;
        show();
    });

    overlay.addEventListener('click', hideOverlay);

    document.addEventListener('keydown', function(e) {
        if(!overlayOpen) return;
        if(e.key === 'ArrowLeft') {
            if(index > 0) { index--; show(); }
        } else if(e.key === 'ArrowRight') {
            if(index < images.length - 1) { index++; show(); }
        } else if(e.key === 'Escape') {
            hideOverlay();
        }
    });
});

function hideOverlay() {
    var overlay = document.getElementById('imageOverlay');
    if(overlay) overlay.style.display = 'none';
    overlayOpen = false;
}
