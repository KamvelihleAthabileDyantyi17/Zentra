// Customer website: home, menu, cart, checkout, live tracking, feedback and account (Google SSO).
// It mirrors the Android app screen for screen and shares the same data, so an order placed
// here shows up on the staff dashboard (website or app) straight away.
//
// How the page works:
//  - `state` holds everything the page shows.
//  - Each screen is a function that returns HTML for the current state.
//  - render() redraws the current screen; route comes from the URL hash (#/menu, #/cart, ...).
//  - Clicks are handled in one place via data-action attributes.

import { getBackend } from "./backend.js";
import { FAVOURITE_IDS } from "./starter-data.js";
import {
  CATEGORIES, STAGES, esc, isValidSaPhone, log, nextStage, orderTotal, rand, stageIndex, stageInfo, toast,
} from "./util.js";

const { data, auth, demo } = await getBackend();
const app = document.getElementById("app");

const PLUS = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" aria-hidden="true"><path d="M12 5v14M5 12h14"/></svg>';
const STAR = '<svg viewBox="0 0 24 24" stroke-linejoin="round" aria-hidden="true"><path d="M12 2.8l2.8 5.7 6.3.9-4.55 4.43 1.07 6.27L12 17.15 6.38 20.1l1.07-6.27L2.9 9.4l6.3-.9z"/></svg>';

// ------------------------------------------------------------------ state

const state = {
  menu: [],
  menuLoaded: false,
  trucks: [],
  truckId: localStorage.getItem("shannos-truck"),
  category: null,
  cart: JSON.parse(sessionStorage.getItem("shannos-cart") || "{}"), // { itemId: quantity }
  form: { name: "", phone: "", fulfilment: "PICKUP", address: "", payment: "CARD", note: "" },
  showErrors: false,
  placing: false,
  activeOrderId: localStorage.getItem("shannos-active-order"),
  activeOrder: null,
  user: null, // signed-in (non-guest) user
  myOrders: [],
  rating: 0,
  signingIn: false,
};

const selectedTruck = () =>
  state.trucks.find((t) => t.id === state.truckId && t.open) || state.trucks.find((t) => t.open) || null;
const itemById = (id) => state.menu.find((m) => m.id === id);
const cartLines = () =>
  state.menu.filter((m) => state.cart[m.id] > 0).map((m) => ({ itemId: m.id, name: m.name, quantity: state.cart[m.id], unitPrice: m.price }));
const cartCount = () => Object.values(state.cart).reduce((a, b) => a + b, 0);
const cartTotal = () => cartLines().reduce((s, l) => s + l.quantity * l.unitPrice, 0);

function saveCart() {
  sessionStorage.setItem("shannos-cart", JSON.stringify(state.cart));
}

function setQty(id, qty) {
  const item = itemById(id);
  if (qty > 0 && item && !item.available) return toast(`${item.name} is sold out`);
  if (qty <= 0) delete state.cart[id];
  else state.cart[id] = Math.min(qty, 20);
  saveCart();
  render();
}

function formErrors() {
  const f = state.form, errors = {};
  if (!f.name.trim()) errors.name = "Add your name so we can call your order";
  if (!isValidSaPhone(f.phone)) errors.phone = "Enter a valid cellphone number, e.g. 082 000 0000";
  if (f.fulfilment === "DELIVERY" && !f.address.trim()) errors.address = "Where should we deliver?";
  return errors;
}

// ------------------------------------------------------------------ live data

data.onMenu((menu) => {
  state.menu = menu;
  state.menuLoaded = true;
  // Drop anything that just went sold out from the cart
  let changed = false;
  for (const id of Object.keys(state.cart)) {
    if (!menu.find((m) => m.id === id && m.available)) { delete state.cart[id]; changed = true; }
  }
  if (changed) { saveCart(); toast("Something in your order just sold out and was removed"); }
  log.d("Customer", `Menu loaded: ${menu.length} items, ${menu.filter((m) => !m.available).length} sold out`);
  render();
});

data.onTrucks((trucks) => { state.trucks = trucks; render(); });

let stopOrder = null;
function watchOrder(id) {
  stopOrder?.();
  state.activeOrderId = id;
  localStorage.setItem("shannos-active-order", id);
  stopOrder = data.onOrder(id, (order) => {
    if (!order) return clearActiveOrder();
    if (state.activeOrder && state.activeOrder.stage !== order.stage) {
      toast(`Order ${order.number}: ${stageInfo(order.stage).label}`);
    }
    log.d("Customer", `Order ${order.number} is now ${order.stage}`);
    state.activeOrder = order;
    render();
  });
}
function clearActiveOrder() {
  stopOrder?.();
  stopOrder = null;
  state.activeOrderId = null;
  state.activeOrder = null;
  localStorage.removeItem("shannos-active-order");
  render();
}
if (state.activeOrderId) watchOrder(state.activeOrderId);

let stopHistory = null;
auth.onUser((u) => {
  const signedIn = u && !u.isGuest ? u : null;
  const changedUser = signedIn?.uid !== state.user?.uid;
  state.user = signedIn;
  if (signedIn?.name && !state.form.name) state.form.name = signedIn.name;
  if (changedUser) {
    stopHistory?.();
    stopHistory = null;
    state.myOrders = [];
    if (signedIn) stopHistory = data.onCustomerOrders(signedIn.uid, (orders) => { state.myOrders = orders; render(); });
  }
  render();
});

// ------------------------------------------------------------------ screens

const tile = (item, extra = "") => `<div class="tile tile-${esc(item.category)} ${extra}" aria-hidden="true">${esc(item.tag)}</div>`;

function addControl(item, filled = true) {
  if (!item.available) return `<span class="badge badge-red">Sold out</span>`;
  const q = state.cart[item.id] || 0;
  if (q === 0) return `<button class="round" data-action="add" data-id="${esc(item.id)}" aria-label="Add ${esc(item.name)}">${PLUS}</button>`;
  return `<span class="stepper ${filled ? "filled" : ""}">
      <button data-action="remove" data-id="${esc(item.id)}" aria-label="One less ${esc(item.name)}">−</button>
      <span>${q}</span>
      <button data-action="add" data-id="${esc(item.id)}" aria-label="One more ${esc(item.name)}">+</button>
    </span>`;
}

function homeScreen() {
  const truck = selectedTruck();
  const order = state.activeOrder;
  const favs = FAVOURITE_IDS.map(itemById).filter(Boolean);
  return `
    <section class="hero">
      <span class="label" style="color:var(--ink)">10–15 min turnaround</span>
      <h1>Kasi flavour.<br>No queue.</h1>
      <p>Order your kota, amagwinya and a cold one ahead. Collect it hot from the truck.</p>
      <div class="row" style="flex-wrap:wrap">
        <a class="btn btn-ink" href="#/menu">Order now</a>
        <a class="btn btn-ghost" href="#/trucks" style="border-color:var(--ink)">
          <span class="badge ${truck ? "badge-green" : "badge-grey"}">${truck ? "Open" : "Closed"}</span>
          ${truck ? `${esc(truck.name)} · ${esc(truck.spot)}` : "Choose a truck"}
        </a>
      </div>
    </section>

    ${order && order.stage !== "COLLECTED" ? `
      <a class="live-order" href="#/track">
        <span><span class="label">Your order · ${esc(order.number)}</span><br><strong style="font-size:18px">${esc(stageInfo(order.stage).label)}</strong></span>
        <strong>Track</strong>
      </a>` : ""}

    ${favs.length ? `
      <section class="stack">
        <div class="spread"><h2>Kasi favourites</h2><a class="link-btn" href="#/menu" style="text-decoration:none;display:inline-flex;align-items:center">Full menu</a></div>
        <div class="fav-row">
          ${favs.map((f) => `
            <article class="fav">
              ${tile(f)}
              <div class="stack" style="padding:14px;gap:8px">
                <strong>${esc(f.name)}</strong>
                <div class="spread"><strong>${rand(f.price)}</strong>${addControl(f)}</div>
              </div>
            </article>`).join("")}
        </div>
      </section>` : ""}

    <section class="stack">
      <h2>How it works</h2>
      <div class="steps3">
        <div class="step3"><div class="n">1</div><strong>Pick your food</strong><div class="muted small">Kotas, amagwinya, rolls and cold drinks.</div></div>
        <div class="step3"><div class="n">2</div><strong>Pay card or cash</strong><div class="muted small">Collect at the truck or get it delivered.</div></div>
        <div class="step3"><div class="n">3</div><strong>Collect it hot</strong><div class="muted small">Track it live until it's ready.</div></div>
      </div>
    </section>`;
}

function trucksScreen() {
  const current = selectedTruck();
  return `
    <div class="narrow">
      <h1>Choose your truck</h1>
      <p class="muted" style="margin:0">Your order goes to the truck you pick. Collect it there.</p>
      ${state.trucks.length ? "" : `<p class="muted">No trucks set up yet.</p>`}
      <div class="stack" role="radiogroup" aria-label="Trucks">
        ${state.trucks.map((t) => `
          <button class="choice spread" role="radio" aria-checked="${t.id === current?.id}" data-action="truck" data-id="${esc(t.id)}" ${t.open ? "" : "disabled"} style="text-align:left;padding:16px">
            <span class="row"><span class="brand-mark" style="background:var(--ink);color:var(--sign)">${esc(t.name.replace(/\D/g, "") || "S")}</span>
              <span><strong>${esc(t.name)}</strong><br><span class="muted small">${esc(t.spot)}</span></span></span>
            <span class="badge ${t.open ? "badge-green" : "badge-grey"}">${t.open ? "Open" : "Closed"}</span>
          </button>`).join("")}
      </div>
    </div>`;
}

function menuScreen() {
  if (!state.menuLoaded) return `<div class="spinner" role="status" aria-label="Loading menu"></div>`;
  const items = state.menu.filter((m) => !state.category || m.category === state.category);
  const truck = selectedTruck();
  return `
    <div class="spread">
      <div><h1>The menu</h1><span class="muted small">Collecting from ${truck ? esc(truck.name) : "—"} · <a href="#/trucks">change</a></span></div>
    </div>
    <div class="chips" role="group" aria-label="Categories">
      <button class="chip" data-action="category" data-id="" aria-pressed="${!state.category}">All</button>
      ${CATEGORIES.map((c) => `<button class="chip" data-action="category" data-id="${c.id}" aria-pressed="${state.category === c.id}">${c.label}</button>`).join("")}
    </div>
    ${state.menu.length === 0 ? `<div class="panel"><strong>The menu is empty</strong><p class="muted">Staff: sign in on the staff page and tap "Load starter menu".</p></div>` : ""}
    <div class="menu-grid">
      ${items.map((m) => `
        <article class="item">
          ${tile(m)}
          <div class="item-body">
            <span class="item-name">${esc(m.name)}</span>
            <span class="item-desc">${esc(m.description)}</span>
            <strong>${rand(m.price)}</strong>
          </div>
          ${addControl(m)}
        </article>`).join("")}
    </div>`;
}

function cartScreen() {
  const lines = cartLines();
  if (!lines.length) {
    return `<div class="narrow"><h1>Your order</h1>
      <div class="empty"><span class="display" style="font-size:26px">Nothing here yet</span><span class="muted">Your kota is waiting.</span><a class="btn btn-ink" href="#/menu">Browse the menu</a></div></div>`;
  }
  return `
    <div class="narrow">
      <h1>Your order</h1>
      <div class="panel">
        ${lines.map((l) => `
          <div class="line">
            <div class="grow"><strong>${esc(l.name)}</strong><br><span class="muted small">${rand(l.unitPrice)} each</span></div>
            ${addControl(itemById(l.itemId), false)}
            <span class="amount">${rand(l.quantity * l.unitPrice)}</span>
          </div>`).join("")}
        <div class="total"><span>Total</span><span>${rand(cartTotal())}</span></div>
      </div>
      <label class="field">Note for the kitchen (optional)
        <textarea rows="2" data-field="note" placeholder="e.g. extra sauce, no cheese">${esc(state.form.note)}</textarea>
      </label>
      <a class="btn btn-red btn-block" href="#/checkout">Checkout · ${rand(cartTotal())}</a>
    </div>`;
}

function field(label, key, { type = "text", placeholder = "", autocomplete = "" } = {}) {
  const error = state.showErrors ? formErrors()[key] : null;
  const id = `f-${key}`;
  return `<label class="field" for="${id}">${label}
      <input id="${id}" type="${type}" data-field="${key}" value="${esc(state.form[key])}" placeholder="${esc(placeholder)}"
        ${autocomplete ? `autocomplete="${autocomplete}"` : ""} ${error ? `aria-invalid="true" aria-describedby="${id}-err"` : ""}>
      ${error ? `<span class="error" id="${id}-err">${esc(error)}</span>` : ""}
    </label>`;
}

function checkoutScreen() {
  if (!cartCount()) { location.hash = "#/cart"; return ""; }
  const f = state.form;
  const truck = selectedTruck();
  return `
    <div class="narrow">
      <h1>Checkout</h1>
      ${state.user ? "" : `<div class="panel spread"><span class="muted">Have a Google account? Sign in to save your details and see your orders.</span>
        <button class="btn btn-google" data-action="google"><span class="g">G</span> Continue with Google</button></div>`}

      <div class="stack">
        <span class="label">How do you want it?</span>
        <div class="choices" role="radiogroup" aria-label="Collect or delivery">
          <button class="choice" role="radio" aria-checked="${f.fulfilment === "PICKUP"}" data-action="fulfilment" data-id="PICKUP">Collect at truck</button>
          <button class="choice" role="radio" aria-checked="${f.fulfilment === "DELIVERY"}" data-action="fulfilment" data-id="DELIVERY">Deliver to me</button>
        </div>
        ${f.fulfilment === "PICKUP"
          ? `<div class="panel">${truck ? `Collect from <strong>${esc(truck.name)}</strong>, ${esc(truck.spot)}` : "No truck is open right now"}</div>`
          : `${field("Delivery address", "address", { placeholder: "Street, area", autocomplete: "street-address" })}
             <span class="muted small">Delivery fee: [SET BY OWNER]</span>`}
      </div>

      <div class="stack">
        <span class="label">Your details</span>
        ${field("Name", "name", { placeholder: "So we can call your order", autocomplete: "name" })}
        ${field("Cellphone number", "phone", { type: "tel", placeholder: "082 000 0000", autocomplete: "tel" })}
      </div>

      <div class="stack">
        <span class="label">Pay with</span>
        <div class="choices" role="radiogroup" aria-label="Payment">
          <button class="choice" role="radio" aria-checked="${f.payment === "CARD"}" data-action="payment" data-id="CARD">Card</button>
          <button class="choice" role="radio" aria-checked="${f.payment === "CASH"}" data-action="payment" data-id="CASH">Cash at truck</button>
        </div>
        ${f.payment === "CARD" ? `<span class="muted small">Card payment is a placeholder for now: the truck takes card at collection.</span>` : ""}
      </div>

      <div class="panel spread"><strong>${cartCount()} ${cartCount() === 1 ? "item" : "items"}</strong><strong>${rand(cartTotal())}</strong></div>
      <button class="btn btn-red btn-block" data-action="place" ${state.placing || !truck ? "disabled" : ""}>${state.placing ? "Placing order…" : "Place order"}</button>
    </div>`;
}

function trackScreen() {
  const order = state.activeOrder;
  if (!order) {
    return `<div class="narrow"><h1>Track</h1>
      ${state.activeOrderId ? `<div class="spinner" role="status" aria-label="Loading order"></div>`
        : `<div class="empty"><span class="display" style="font-size:26px">No order yet</span><span class="muted">Place an order and follow it here, live.</span><a class="btn btn-ink" href="#/menu">Browse the menu</a></div>`}</div>`;
  }
  const current = stageIndex(order.stage);
  return `
    <div class="narrow">
      <section class="status-card" aria-live="polite">
        <span class="label">Order ${esc(order.number)}</span>
        <span class="display">${esc(stageInfo(order.stage).label)}</span>
        <p>${esc(stageInfo(order.stage).hint)}</p>
      </section>
      <div class="panel">
        <ol class="timeline">
          ${STAGES.map((s, i) => `
            <li class="${i < current ? "done" : i === current ? "current" : ""}">
              <span class="dot-col"><span class="dot">${i + 1}</span>${i < STAGES.length - 1 ? `<span class="bar"></span>` : ""}</span>
              <span class="text">${esc(s.label)}</span>
            </li>`).join("")}
        </ol>
      </div>
      <div class="panel">
        <span class="label">${order.fulfilment === "PICKUP" ? "Collect at truck" : "Delivery"} · ${order.payment === "CARD" ? "Card" : "Cash at truck"}</span>
        ${order.lines.map((l) => `<div class="spread" style="padding-top:8px"><span>${l.quantity} × ${esc(l.name)}</span><span>${rand(l.quantity * l.unitPrice)}</span></div>`).join("")}
        <div class="total" style="border-top:1px solid var(--divider);margin-top:10px"><span>Total</span><span>${rand(orderTotal(order))}</span></div>
      </div>
      ${demo && order.stage !== "COLLECTED" ? `<button class="btn btn-ghost" data-action="demo-advance" style="border-style:dashed">Demo: move to next step (or use the staff page)</button>` : ""}
      ${order.stage === "COLLECTED" ? `<a class="btn btn-yellow btn-block" href="#/feedback">Rate your food</a>` : ""}
    </div>`;
}

function feedbackScreen() {
  return `
    <div class="narrow">
      <h1>How was it?</h1>
      <p class="muted" style="margin:0">We read every one. Tell us what hit and what missed.</p>
      <div class="stars" role="radiogroup" aria-label="Rating">
        ${[1, 2, 3, 4, 5].map((n) => `<button class="star ${n <= state.rating ? "lit" : ""}" role="radio" aria-checked="${n === state.rating}" aria-label="${n} star${n > 1 ? "s" : ""}" data-action="rate" data-id="${n}">${STAR}</button>`).join("")}
      </div>
      <label class="field">Anything to add?<textarea id="comment" rows="4" placeholder="The kota was…"></textarea></label>
      <button class="btn btn-ink btn-block" data-action="send-feedback" ${state.rating ? "" : "disabled"}>Send feedback</button>
    </div>`;
}

function thanksScreen() {
  return `<div class="narrow"><div class="empty">
    <div class="avatar" style="width:88px;height:88px">✓</div>
    <span class="display" style="font-size:32px">Sharp, thank you!</span>
    <span class="muted">Your feedback is with the team. See you at the truck.</span>
    <a class="btn btn-ink" href="#/">Back home</a></div></div>`;
}

function accountScreen() {
  const u = state.user;
  if (!u) {
    return `
      <div class="narrow">
        <h1>Account</h1>
        <div class="panel stack">
          <strong style="font-size:18px">Order faster next time</strong>
          <span class="muted">Sign in to save your details and see your orders here and in the Shannos app, with the same Google account.</span>
          <button class="btn btn-google btn-block" data-action="google" ${state.signingIn ? "disabled" : ""}><span class="g">G</span> ${state.signingIn ? "Signing in…" : "Continue with Google"}</button>
          <span class="muted small">No account needed to order. You can always continue as a guest.</span>
        </div>
        <p class="muted small">Work at the truck? <a href="staff.html">Staff login</a></p>
      </div>`;
  }
  return `
    <div class="narrow">
      <h1>Account</h1>
      <div class="panel stack">
        <div class="row">
          <span class="avatar" aria-hidden="true">${esc((u.name || u.email || "?").slice(0, 1).toUpperCase())}</span>
          <span><strong style="font-size:18px">${esc(u.name || "Signed in")}</strong><br>
            ${u.email ? `<span class="muted">${esc(u.email)}</span><br>` : ""}
            <span style="color:var(--green);font-weight:700;font-size:13px">Signed in with Google</span></span>
        </div>
        <button class="btn btn-ghost btn-block" data-action="sign-out">Sign out</button>
      </div>
      <div class="stack">
        <span class="label">My orders</span>
        ${state.myOrders.length ? `<div class="panel">${state.myOrders.map((o) => `
          <div class="line">
            <div class="grow"><strong>${esc(o.number)} · ${rand(orderTotal(o))}</strong><br>
              <span class="muted small">${new Date(o.createdAt).toLocaleString("en-ZA", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })} · ${esc(o.lines.map((l) => l.name).join(", "))}</span></div>
            <span class="badge ${o.stage === "COLLECTED" ? "badge-grey" : "badge-amber"}">${esc(stageInfo(o.stage).label)}</span>
          </div>`).join("")}</div>`
        : `<p class="muted" style="margin:0">No orders yet. Your next order will show up here.</p>`}
      </div>
      <p class="muted small">Work at the truck? <a href="staff.html">Staff login</a></p>
    </div>`;
}

const screens = {
  home: homeScreen, trucks: trucksScreen, menu: menuScreen, cart: cartScreen, checkout: checkoutScreen,
  track: trackScreen, feedback: feedbackScreen, thanks: thanksScreen, account: accountScreen,
};
const routeName = () => (location.hash.replace(/^#\/?/, "").split("/")[0] || "home");

let lastRoute = null;
function render() {
  const route = screens[routeName()] ? routeName() : "home";
  // Keep typed text: don't redraw a form while someone is typing in it
  const active = document.activeElement;
  if (route === lastRoute && active?.dataset?.field) return updateChrome(route);
  app.innerHTML = screens[route]();
  if (route !== lastRoute) { window.scrollTo(0, 0); app.focus({ preventScroll: true }); }
  lastRoute = route;
  updateChrome(route);
}

function updateChrome(route) {
  document.querySelectorAll(".nav a").forEach((a) => {
    if (a.dataset.route === route || (route === "checkout" && a.dataset.route === "cart")) a.setAttribute("aria-current", "page");
    else a.removeAttribute("aria-current");
  });
  const count = cartCount();
  const badge = document.getElementById("cart-count");
  badge.hidden = count === 0;
  badge.textContent = count;
  document.getElementById("account-link").textContent = state.user ? (state.user.name || "Account").split(" ")[0] : "Account";
}

// ------------------------------------------------------------------ actions

async function signInWithGoogle() {
  state.signingIn = true; render();
  try {
    const u = await auth.signInWithGoogle();
    toast(`Signed in as ${u.name || u.email || "you"}`);
  } catch (err) {
    log.e("Customer", "Google sign-in failed", err);
    if (err.code !== "auth/popup-closed-by-user" && err.code !== "auth/cancelled-popup-request") {
      toast("Sign-in didn't work. " + (err.message || ""));
    }
  } finally {
    state.signingIn = false; render();
  }
}

async function placeOrder() {
  if (state.placing || !cartCount()) return;
  if (Object.keys(formErrors()).length) {
    state.showErrors = true;
    lastRoute = null; // force redraw to show errors
    render();
    log.d("Customer", "Checkout blocked: " + Object.keys(formErrors()).join(", "));
    document.querySelector('[aria-invalid="true"]')?.focus();
    return;
  }
  const truck = selectedTruck();
  if (!truck) return toast("No truck is open right now");
  state.placing = true; lastRoute = null; render();
  try {
    const uid = await auth.ensureCustomer();
    const f = state.form;
    const lines = cartLines();
    // Same fields as the Android app writes (FirestoreRepository.kt → Order.toMap)
    const order = {
      number: "SFT-" + Math.floor(10000 + Math.random() * 89999),
      truckId: truck.id,
      customerUid: uid,
      customerName: f.name.trim(),
      phone: f.phone.trim(),
      fulfilment: f.fulfilment,
      address: f.fulfilment === "DELIVERY" ? f.address.trim() : "",
      payment: f.payment,
      note: f.note.trim(),
      lines,
      total: lines.reduce((s, l) => s + l.quantity * l.unitPrice, 0),
      stage: "RECEIVED",
      createdAt: Date.now(),
    };
    const id = await data.placeOrder(order);
    log.d("Customer", `Order ${order.number} placed`);
    state.cart = {}; saveCart();
    state.form.note = "";
    state.showErrors = false;
    state.activeOrder = { ...order, id };
    watchOrder(id);
    location.hash = "#/track";
  } catch (err) {
    log.e("Customer", "Placing order failed", err);
    toast("Order didn't go through. " + (err.message || ""));
  } finally {
    state.placing = false; lastRoute = null; render();
  }
}

document.addEventListener("click", async (e) => {
  const el = e.target.closest("[data-action]");
  if (!el) return;
  const { action, id } = el.dataset;
  switch (action) {
    case "add": setQty(id, (state.cart[id] || 0) + 1); break;
    case "remove": setQty(id, (state.cart[id] || 0) - 1); break;
    case "category": state.category = id || null; render(); break;
    case "truck":
      state.truckId = id; localStorage.setItem("shannos-truck", id);
      location.hash = "#/"; break;
    case "fulfilment": state.form.fulfilment = id; lastRoute = null; render(); break;
    case "payment": state.form.payment = id; lastRoute = null; render(); break;
    case "place": await placeOrder(); break;
    case "google": await signInWithGoogle(); break;
    case "sign-out":
      await auth.signOut();
      clearActiveOrder(); // the tracked order belongs to the signed-out account
      state.form = { ...state.form, name: "", phone: "" };
      toast("Signed out");
      break;
    case "rate": state.rating = Number(id); lastRoute = null; render(); break;
    case "send-feedback": {
      const comment = document.getElementById("comment")?.value.trim() || "";
      try { await data.submitFeedback({ orderId: state.activeOrderId || "", rating: state.rating, comment }); }
      catch (err) { log.w("Customer", "Feedback not saved", err); }
      state.rating = 0;
      clearActiveOrder();
      location.hash = "#/thanks";
      break;
    }
    case "demo-advance":
      if (state.activeOrder) await data.setStage(state.activeOrder.id, nextStage(state.activeOrder.stage));
      break;
  }
});

// Typing updates the form without redrawing (so the cursor stays put)
document.addEventListener("input", (e) => {
  const key = e.target.dataset?.field;
  if (key) state.form[key] = e.target.value;
});

window.addEventListener("hashchange", render);

if (demo) {
  document.getElementById("demo-banner").innerHTML =
    `<div class="demo-banner">Demo mode: data stays in this browser. Open <a href="staff.html" target="_blank" style="color:#fff">the staff page</a> in another tab to move orders along. <button data-action="reset-demo" id="reset-demo">Reset demo</button></div>`;
  document.getElementById("reset-demo").addEventListener("click", async () => {
    const { data: demoData } = await import("./demo-backend.js");
    demoData.reset();
    clearActiveOrder();
    toast("Demo data reset");
  });
}

render();
