/* ==========================================================================
   Shanon's Food Truck — shared front-end logic
   Cart state is kept in localStorage so it persists across pages.
   This is a Task 1 prototype: no backend, everything runs client-side.
   ========================================================================== */

const CART_KEY = 'shanons_cart';

function getCart() {
  try {
    return JSON.parse(localStorage.getItem(CART_KEY)) || [];
  } catch (e) {
    return [];
  }
}

function saveCart(cart) {
  localStorage.setItem(CART_KEY, JSON.stringify(cart));
  updateCartBadges();
}

function addToCart(item) {
  const cart = getCart();
  const existing = cart.find((line) => line.id === item.id);
  if (existing) {
    existing.qty += 1;
  } else {
    cart.push({ ...item, qty: 1 });
  }
  saveCart(cart);
}

function changeQty(id, delta) {
  let cart = getCart();
  cart = cart
    .map((line) => (line.id === id ? { ...line, qty: line.qty + delta } : line))
    .filter((line) => line.qty > 0);
  saveCart(cart);
  renderCartPage();
}

function removeLine(id) {
  const cart = getCart().filter((line) => line.id !== id);
  saveCart(cart);
  renderCartPage();
}

function cartCount() {
  return getCart().reduce((sum, line) => sum + line.qty, 0);
}

function cartTotal() {
  return getCart().reduce((sum, line) => sum + line.qty * line.price, 0);
}

function formatR(amount) {
  return 'R' + amount.toFixed(2);
}

function updateCartBadges() {
  const count = cartCount();
  document.querySelectorAll('[data-cart-count]').forEach((el) => {
    el.textContent = count;
  });
}

/* ---------- Menu page: add-to-cart buttons ---------- */

function initMenuPage() {
  document.querySelectorAll('[data-add-item]').forEach((btn) => {
    btn.addEventListener('click', () => {
      addToCart({
        id: btn.dataset.addItem,
        name: btn.dataset.name,
        price: parseFloat(btn.dataset.price),
      });
      btn.textContent = '✓';
      setTimeout(() => (btn.textContent = '+'), 700);
    });
  });

  const tabs = document.querySelectorAll('.category-tab');
  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      tabs.forEach((t) => t.setAttribute('aria-selected', 'false'));
      tab.setAttribute('aria-selected', 'true');
      const group = tab.dataset.category;
      document.querySelectorAll('[data-item-category]').forEach((item) => {
        const show = group === 'all' || item.dataset.itemCategory === group;
        item.style.display = show ? '' : 'none';
      });
    });
  });
}

/* ---------- Cart page ---------- */

function renderCartPage() {
  const list = document.getElementById('cart-list');
  if (!list) return;
  const cart = getCart();

  if (cart.length === 0) {
    list.innerHTML = '';
    document.getElementById('cart-empty').style.display = 'block';
    document.getElementById('cart-summary').style.display = 'none';
    return;
  }

  document.getElementById('cart-empty').style.display = 'none';
  document.getElementById('cart-summary').style.display = 'block';

  list.innerHTML = cart
    .map(
      (line) => `
    <div class="line-row">
      <div>
        <h3 style="margin:0 0 2px;font-size:1rem;">${line.name}</h3>
        <span style="font-size:0.85rem;color:var(--ink-soft);">${formatR(line.price)} each</span>
      </div>
      <div style="display:flex;align-items:center;gap:14px;">
        <div class="qty-control">
          <button type="button" aria-label="Decrease quantity" onclick="changeQty('${line.id}', -1)">−</button>
          <span>${line.qty}</span>
          <button type="button" aria-label="Increase quantity" onclick="changeQty('${line.id}', 1)">+</button>
        </div>
        <strong style="font-family:var(--font-display);min-width:64px;text-align:right;">${formatR(
          line.price * line.qty
        )}</strong>
      </div>
    </div>`
    )
    .join('');

  document.getElementById('cart-subtotal').textContent = formatR(cartTotal());
  const deliveryFee = 25;
  document.getElementById('cart-delivery').textContent = formatR(deliveryFee);
  document.getElementById('cart-grand-total').textContent = formatR(cartTotal() + deliveryFee);
}

/* ---------- Checkout page ---------- */

function renderCheckoutSummary() {
  const el = document.getElementById('checkout-order-summary');
  if (!el) return;
  const cart = getCart();
  if (cart.length === 0) {
    el.innerHTML = '<p>Your cart is empty. <a href="menu.html">Browse the menu</a> to add something tasty first.</p>';
    return;
  }
  el.innerHTML = cart
    .map(
      (line) =>
        `<div class="line-row"><span>${line.qty} × ${line.name}</span><span>${formatR(
          line.price * line.qty
        )}</span></div>`
    )
    .join('');
  const deliveryFee = 25;
  el.innerHTML += `<div class="summary-total"><span>Total</span><span>${formatR(
    cartTotal() + deliveryFee
  )}</span></div>`;
}

function initCheckoutForm() {
  const form = document.getElementById('checkout-form');
  if (!form) return;
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    if (cartCount() === 0) {
      alert('Add something from the menu before checking out.');
      return;
    }
    const orderNumber = 'SFT-' + Math.floor(1000 + Math.random() * 9000);
    localStorage.setItem('shanons_last_order', orderNumber);
    localStorage.setItem(CART_KEY, '[]');
    window.location.href = 'order-status.html?order=' + orderNumber;
  });
}

/* ---------- Order status page ---------- */

function initOrderStatusPage() {
  const el = document.getElementById('order-number');
  if (!el) return;
  const params = new URLSearchParams(window.location.search);
  const order = params.get('order') || localStorage.getItem('shanons_last_order') || 'SFT-DEMO';
  el.textContent = order;
}

/* ---------- Feedback page ---------- */

function initFeedbackForm() {
  const form = document.getElementById('feedback-form');
  if (!form) return;
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    document.getElementById('feedback-form-wrap').style.display = 'none';
    document.getElementById('feedback-thanks').style.display = 'block';
  });
}

/* ---------- Admin: login (demo only, no real auth) ---------- */

function initAdminLogin() {
  const form = document.getElementById('admin-login-form');
  if (!form) return;
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    window.location.href = 'dashboard.html';
  });
}

/* ---------- Admin: menu management demo table ---------- */

function initMenuManagement() {
  document.querySelectorAll('[data-remove-row]').forEach((btn) => {
    btn.addEventListener('click', () => {
      btn.closest('tr').remove();
    });
  });
}

/* ---------- Init on load ---------- */

document.addEventListener('DOMContentLoaded', () => {
  updateCartBadges();
  initMenuPage();
  renderCartPage();
  renderCheckoutSummary();
  initCheckoutForm();
  initOrderStatusPage();
  initFeedbackForm();
  initAdminLogin();
  initMenuManagement();
});
