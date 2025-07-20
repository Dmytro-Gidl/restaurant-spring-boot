window.onload = function () {
    // Load cart from localStorage and initialize dish inputs
    loadCart();
    initializeDishInputs();
    // Update total and render pinned cart
    document.querySelector('.total-price').innerHTML = calcOrder();
    renderBottomRightCart();
};

var cart = {}; // Global cart object

function loadCart() {
    let savedCart = localStorage.getItem('cart');
    if (savedCart) {
        cart = JSON.parse(savedCart);
    }
}

function saveCart() {
    localStorage.setItem('cart', JSON.stringify(cart));
}

function initializeDishInputs() {
    // For each dish card, set its quantity based on the stored cart
    document.querySelectorAll('.single-menu').forEach(menu => {
        const dishId = menu.getAttribute('data-dish-id');
        if (cart[dishId]) {
            const qty = cart[dishId].quantity;
            const inputEl = menu.querySelector('input[type="number"]');
            if (inputEl) {
                inputEl.value = qty;
            }
        }
    });
}

function calcOrder() {
    // Calculate total order from the cart object
    let total = 0;
    Object.keys(cart).forEach(dishId => {
        const item = cart[dishId];
        total += item.price * item.quantity;
    });
    return total.toFixed(2);
}

function stepperIncrement(btn) {
    const inputEl = btn.previousElementSibling;
    const step = parseInt(inputEl.step) || 1;
    let newVal = parseInt(inputEl.value) + step;
    if (newVal > 100) newVal = 100;
    inputEl.value = newVal;

    updateCartForDish(btn, newVal);

    document.querySelector('.total-price').innerHTML = calcOrder();
    renderBottomRightCart();
}

function stepperDecrement(btn) {
    const inputEl = btn.nextElementSibling;
    const step = parseInt(inputEl.step) || 1;
    let newVal = parseInt(inputEl.value) - step;
    if (newVal < 0) newVal = 0;
    inputEl.value = newVal;

    updateCartForDish(btn, newVal);

    document.querySelector('.total-price').innerHTML = calcOrder();
    renderBottomRightCart();
}

function updateCartForDish(btn, newQuantity) {
    // Get dish details from the dish card container
    const menu = btn.closest('.single-menu');
    const dishId = menu.getAttribute('data-dish-id');
    const dishName = menu.getAttribute('data-dish-name') ||
        (menu.querySelector('h4').innerText.split('\n')[0].trim());
    const dishPrice = parseFloat(menu.getAttribute('data-dish-price')) ||
        parseFloat(menu.querySelector('.price').innerText);

    // Update the cart based on the new quantity
    if (newQuantity <= 0) {
        delete cart[dishId];
    } else {
        cart[dishId] = {
            name: dishName,
            price: dishPrice,
            quantity: newQuantity
        };
    }
    saveCart();
}

function removeDish(dishId) {
    // Remove dish from cart and update its input field to 0
    const menu = document.querySelector(`.single-menu[data-dish-id="${dishId}"]`);
    if (!menu) return;
    const inputEl = menu.querySelector('input[type="number"]');
    if (inputEl) {
        inputEl.value = 0;
    }
    delete cart[dishId];
    saveCart();
    document.querySelector('.total-price').innerHTML = calcOrder();
    renderBottomRightCart();
}

function clearLocalStorage() {
    localStorage.removeItem('cart');
}

/**
 * Renders the pinned cart on the bottom-right using the cart object.
 */
function renderBottomRightCart() {
    const cartItemsEl = document.getElementById('cart-items-list');
    const cartTotalEl = document.getElementById('bottomCartTotal');
    if (!cartItemsEl || !cartTotalEl) return;

    cartItemsEl.innerHTML = '';
    let total = 0;
    Object.keys(cart).forEach(dishId => {
        const item = cart[dishId];
        const lineTotal = item.price * item.quantity;
        total += lineTotal;
        const lineItem = document.createElement('div');
        lineItem.classList.add('cart-line-item');
        lineItem.innerHTML = `
            <strong>${item.name}</strong> x ${item.quantity} = ${lineTotal.toFixed(2)} ₴
            <button class="remove-btn" onclick="removeDish('${dishId}')">&times;</button>
        `;
        cartItemsEl.appendChild(lineItem);
    });
    cartTotalEl.textContent = total.toFixed(2);
}

// The following functions remain unchanged if used for server updates
function updateCartItem(itemId, newQty) {
    if (newQty < 1) {
        removeCartItem(itemId);
        return;
    }
    fetch('/cart/update?itemId=' + itemId, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ quantity: newQty })
    })
    .then(() => {
        document.querySelector('.total-price').innerHTML = calcOrder();
        renderBottomRightCart();
    })
    .catch(err => console.error('Update cart error:', err));
}

function removeCartItem(itemId) {
    fetch('/cart/remove?itemId=' + itemId, { method: 'POST' })
    .then(() => {
        document.querySelector('.total-price').innerHTML = calcOrder();
        renderBottomRightCart();
    })
    .catch(err => console.error('Remove cart item error:', err));
}

window.onload = function () {
    loadCart();
    initializeDishInputs();
    document.querySelector('.total-price').innerHTML = calcOrder();
    renderBottomRightCart();
    document.getElementById('bottom-right-cart').style.visibility = 'visible';
};
