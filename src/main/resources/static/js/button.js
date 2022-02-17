window.onload = function () {
    if (localStorage.data_inputs) {
        let obj = JSON.parse(localStorage.data_inputs);
        for (key in obj) {
            document.querySelector(`input[name="${key}"]`).value = obj[key];
        }
    }
    const totalPrice = calcOrder();
    document.querySelector('.total-price').innerHTML = totalPrice;
}

function calcOrder() {
    let result = 0;
    const menus = document.querySelectorAll('.single-menu');
    for (let i = 0; i < menus.length; i++) {
        const price = Number(menus[i].querySelector('.price').innerText);
        const qty = Number(menus[i].querySelector('input').value);
        result += price * qty;
    }
    return result;
}

function stepperDecrement(btn) {
    const inputEl = btn.nextElementSibling;
    const calcStep = inputEl.step;
    const newValue = parseInt(inputEl.value) - calcStep;
    if (newValue >= inputEl.min && newValue <= inputEl.max) {
        inputEl.value = newValue;
    }
    const totalPrice = calcOrder(); // calculate sum
    document.querySelector('.total-price').innerHTML = totalPrice;
    saveLocalStorage()
}

function stepperIncrement(btn) {
    const inputEl = btn.previousElementSibling;
    const calcStep = inputEl.step * 1;
    const newValue = parseInt(inputEl.value) + calcStep;
    if (newValue >= inputEl.min && newValue <= inputEl.max) {
        inputEl.value = newValue;
    }
    const totalPrice = calcOrder(); // calculate sum
    document.querySelector('.total-price').innerHTML = totalPrice;
    saveLocalStorage()
}

const objForLocalStorage = {}
function saveLocalStorage() {
    document.querySelectorAll('input[name^="dishIdQuantityMap"]').forEach(el => {
        objForLocalStorage[el.getAttribute('name')] = el.value
    })
    localStorage.setItem('data_inputs', JSON.stringify(objForLocalStorage))
}
function clearLocalStorage() {
    localStorage.clear();
}