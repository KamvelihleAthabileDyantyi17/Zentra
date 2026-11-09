// Staff dashboard for the website: the same features as the app's staff screen.
//  - Sign in with Google (SSO) or email; the account must be on the Firestore `staff` list.
//  - Live order board per truck: one click moves an order to the next stage, and the
//    customer's phone or browser updates instantly.
//  - Mark menu items sold out; see stock levels.

import { getBackend } from "./backend.js";
import { CATEGORIES, esc, log, nextStage, orderTotal, rand, stageInfo, toast } from "./util.js";

const { data, auth, demo } = await getBackend();
const root = document.getElementById("staff-app");

const state = {
  signedIn: null, // null = checking
  signingIn: false,
  error: null,
  user: null,
  section: "orders",
  trucks: [],
  truckId: null,
  orders: [],
  menu: [],
  stock: [],
  busy: false,
};

let unsubscribers = [];
let truckSubs = [];

function stopAll() {
  [...unsubscribers, ...truckSubs].forEach((stop) => stop?.());
  unsubscribers = []; truckSubs = [];
}

function startListening() {
  stopAll();
  unsubscribers.push(data.onTrucks((trucks) => {
    state.trucks = trucks;
    if (!state.truckId && trucks.length) selectTruck(trucks[0].id);
    render();
  }));
  unsubscribers.push(data.onMenu((menu) => { state.menu = menu; render(); }));
}

function selectTruck(id) {
  state.truckId = id;
  truckSubs.forEach((stop) => stop?.());
  let firstLoad = true;
  truckSubs = [
    data.onOpenOrders(id, (orders) => {
      const newOnes = orders.filter((o) => o.stage === "RECEIVED" && !state.orders.some((p) => p.id === o.id));
      if (!firstLoad && newOnes.length) toast(`New order: ${newOnes.map((o) => o.number).join(", ")}`);
      firstLoad = false;
      state.orders = orders;
      render();
    }),
    data.onStock(id, (stock) => { state.stock = stock; render(); }),
  ];
  render();
}

// Session: Firebase remembers who is signed in, so staff go straight to the dashboard.
auth.onUser(async (u) => {
  state.user = u;
  const staff = await auth.isStaff();
  if (staff && !state.signedIn) startListening();
  if (!staff) stopAll();
  state.signedIn = staff;
  render();
});

// ------------------------------------------------------------------ screens

function loginScreen() {
  return `
    <div class="login-wrap">
      <div class="login-card">
        <a class="brand" href="index.html"><span class="brand-mark" aria-hidden="true">S</span>
          <span><span class="brand-name">SHANNOS</span><span class="brand-sub">Staff login</span></span></a>
        <p class="muted" style="margin:0">For the truck team only. Customers don't need an account to order.</p>
        <button class="btn btn-google btn-block" data-action="google" ${state.signingIn ? "disabled" : ""}><span class="g">G</span> Continue with Google</button>
        <span class="label" style="text-align:center">or use your staff email</span>
        <form id="email-form" class="stack" novalidate>
          <label class="field">Email<input type="email" name="email" autocomplete="username" required></label>
          <label class="field">Password<input type="password" name="password" autocomplete="current-password" required></label>
          ${state.error ? `<p class="error" role="alert" style="color:var(--chilli);margin:0">${esc(state.error)}</p>` : ""}
          <button class="btn btn-ink btn-block" type="submit" ${state.signingIn ? "disabled" : ""}>${state.signingIn ? "Signing in…" : "Sign in"}</button>
        </form>
        ${demo ? `<p class="muted small" style="margin:0">Demo mode: any email and a 6+ character password works.</p>` : ""}
        <a href="index.html" class="small">← Back to the shop</a>
      </div>
    </div>`;
}

function ordersSection() {
  const cols = [
    { stage: "RECEIVED", title: "New", badge: "background:var(--ink);color:#fff", btn: "btn-ink" },
    { stage: "COOKING", title: "On the pan", badge: "background:var(--sign);color:var(--ink)", btn: "btn-yellow" },
    { stage: "READY", title: "Ready to collect", badge: "background:var(--green);color:#fff", btn: "btn-ink" },
  ];
  const takings = state.orders.reduce((s, o) => s + orderTotal(o), 0);
  return `
    <div class="stats">
      ${cols.map((c) => `<div class="stat"><span class="label">${c.title}</span><div class="num">${state.orders.filter((o) => o.stage === c.stage).length}</div></div>`).join("")}
      <div class="stat" style="background:var(--sign)"><span class="label" style="color:var(--ink)">Open order value</span><div class="num">${rand(takings)}</div></div>
    </div>
    <div class="board">
      ${cols.map((c) => {
        const list = state.orders.filter((o) => o.stage === c.stage);
        return `
          <section class="panel-soft stack" aria-label="${c.title}">
            <div class="spread"><h2 style="font-size:20px">${c.title}</h2><span class="badge" style="${c.badge}">${list.length}</span></div>
            ${list.length ? "" : `<div class="muted small" style="text-align:center;padding:16px;border:1.5px dashed var(--line);border-radius:14px">Nothing here</div>`}
            ${list.map((o) => `
              <article class="ticket">
                <div class="spread"><strong>${esc(o.number)}</strong><span class="muted small">${o.fulfilment === "DELIVERY" ? "Delivery" : "Collect"}</span></div>
                <span>${esc(o.customerName)} · ${esc(o.phone)}</span>
                <span class="muted small">${esc(o.lines.map((l) => (l.quantity > 1 ? `${l.quantity} × ` : "") + l.name).join(", "))}</span>
                ${o.note ? `<strong class="small">Note: ${esc(o.note)}</strong>` : ""}
                ${o.fulfilment === "DELIVERY" ? `<span class="small">Deliver to: ${esc(o.address)}</span>` : ""}
                <div class="spread" style="padding-top:6px">
                  <span><strong>${rand(orderTotal(o))}</strong><br><span class="muted small">${o.payment === "CARD" ? "Card" : "Cash at truck"}</span></span>
                  <button class="btn ${c.btn}" data-action="advance" data-id="${esc(o.id)}" data-stage="${esc(o.stage)}" ${state.busy ? "disabled" : ""}>${esc(stageInfo(o.stage).action)}</button>
                </div>
              </article>`).join("")}
          </section>`;
      }).join("")}
    </div>`;
}

function menuSection() {
  if (!state.menu.length) {
    return `<div class="panel stack"><strong>No menu yet</strong>
      <span class="muted">Load the starter menu, trucks and stock list. Existing data is never overwritten.</span>
      <button class="btn btn-ink" data-action="starter" ${state.busy ? "disabled" : ""}>Load starter menu</button></div>`;
  }
  const catLabel = (id) => CATEGORIES.find((c) => c.id === id)?.label || id;
  return `
    <span class="label">Click to mark sold out. Customers see it instantly on the app and website.</span>
    <div class="table-wrap"><table>
      <thead><tr><th scope="col">Item</th><th scope="col">Category</th><th scope="col">Price</th><th scope="col">Status</th></tr></thead>
      <tbody>
        ${state.menu.map((m) => `
          <tr>
            <td><div class="row"><span class="tile tile-${esc(m.category)}" style="flex:0 0 48px;height:48px;font-size:13px" aria-hidden="true">${esc(m.tag)}</span><strong>${esc(m.name)}</strong></div></td>
            <td>${esc(catLabel(m.category))}</td>
            <td><strong>${rand(m.price)}</strong></td>
            <td><button class="toggle ${m.available ? "on" : "off"}" role="switch" aria-checked="${m.available}" aria-label="${esc(m.name)} available"
                  data-action="toggle" data-id="${esc(m.id)}" data-available="${m.available}">${m.available ? "Available" : "Sold out"}</button></td>
          </tr>`).join("")}
      </tbody>
    </table></div>`;
}

function stockSection() {
  if (!state.stock.length) return `<p class="muted">No stock list for this truck yet. Use "Load starter menu" under Menu.</p>`;
  const status = (s) => (s.onHand <= 0 ? ["Out", "badge-red"] : s.onHand < s.reorderAt ? ["Low", "badge-amber"] : ["OK", "badge-green"]);
  return `
    <div class="table-wrap"><table>
      <thead><tr><th scope="col">Item</th><th scope="col">On hand</th><th scope="col">Reorder at</th><th scope="col">Status</th></tr></thead>
      <tbody>${state.stock.map((s) => { const [label, cls] = status(s); return `
        <tr><td><strong>${esc(s.name)}</strong></td><td>${s.onHand}</td><td>${s.reorderAt}</td><td><span class="badge ${cls}">${label}</span></td></tr>`; }).join("")}
      </tbody>
    </table></div>
    <span class="muted small">Next step: stock drops automatically as orders come in.</span>`;
}

const SECTIONS = {
  orders: { title: "Live orders", sub: "Click the button on a ticket to move it along. The customer sees it update.", body: ordersSection },
  menu: { title: "Menu", sub: "Mark items sold out so customers can't order them.", body: menuSection },
  stock: { title: "Stock", sub: "What's on the truck right now.", body: stockSection },
};

function dashboard() {
  const s = SECTIONS[state.section];
  return `
    <div class="dash">
      <aside class="side">
        <a class="brand" href="index.html" style="color:#fff"><span class="brand-mark" aria-hidden="true">S</span>
          <span><span class="brand-name">SHANNOS</span><span class="brand-sub">Truck owner</span></span></a>
        <nav aria-label="Dashboard">
          ${Object.entries(SECTIONS).map(([id, sec]) => `<button data-action="section" data-id="${id}" ${state.section === id ? 'aria-current="page"' : ""}>${id === "orders" ? "Orders" : sec.title}</button>`).join("")}
        </nav>
        <div class="who">
          <span>Signed in as<br><strong style="color:#fff">${esc(state.user?.name || state.user?.email || "staff")}</strong></span>
          <button data-action="sign-out">Sign out</button>
        </div>
      </aside>
      <main class="content">
        <div class="spread" style="align-items:flex-end">
          <div><h1>${s.title}</h1><span class="muted">${s.sub}</span></div>
          ${state.trucks.length ? `<div class="segmented" role="group" aria-label="Truck">
            ${state.trucks.map((t) => `<button data-action="truck" data-id="${esc(t.id)}" aria-pressed="${state.truckId === t.id}">${esc(t.name)}</button>`).join("")}
          </div>` : ""}
        </div>
        ${state.error ? `<div class="panel spread" role="alert"><span style="color:var(--chilli)">${esc(state.error)}</span><button class="link-btn" data-action="dismiss">OK</button></div>` : ""}
        ${s.body()}
      </main>
    </div>`;
}

function render() {
  // Don't redraw the login form while someone is typing in it
  if (state.signedIn === false && root.contains(document.activeElement) && document.activeElement.tagName === "INPUT") return;
  root.innerHTML = state.signedIn === null ? `<div class="spinner" role="status" aria-label="Loading"></div>`
    : state.signedIn ? dashboard() : loginScreen();
}

// ------------------------------------------------------------------ actions

async function attemptSignIn(fn) {
  state.signingIn = true; state.error = null; render();
  try {
    await fn();
    if (await auth.isStaff()) {
      state.user = auth.currentUser();
      if (!state.signedIn) startListening();
      state.signedIn = true;
      log.i("Staff", "Dashboard opened");
    }
  } catch (err) {
    log.w("Staff", "Staff sign-in failed", err);
    if (err.code !== "auth/popup-closed-by-user") state.error = err.message || "Sign-in failed";
  } finally {
    state.signingIn = false;
    document.activeElement?.blur(); // let render() redraw the form
    render();
  }
}

async function runAction(fn) {
  state.busy = true; render();
  try { await fn(); }
  catch (err) { log.e("Staff", "Action failed", err); state.error = err.message || "Something went wrong"; }
  finally { state.busy = false; render(); }
}

document.addEventListener("click", async (e) => {
  const el = e.target.closest("[data-action]");
  if (!el) return;
  const { action, id } = el.dataset;
  switch (action) {
    case "google": await attemptSignIn(() => auth.signInStaffGoogle()); break;
    case "section": state.section = id; render(); break;
    case "truck": selectTruck(id); break;
    case "advance": {
      const to = nextStage(el.dataset.stage);
      log.d("Staff", `Moving order to ${to}`);
      await runAction(() => data.setStage(id, to));
      break;
    }
    case "toggle": await runAction(() => data.setAvailable(id, el.dataset.available !== "true")); break;
    case "starter": await runAction(() => data.loadStarterData()); break;
    case "dismiss": state.error = null; render(); break;
    case "sign-out": await auth.signOut(); toast("Signed out"); break;
  }
});

document.addEventListener("submit", async (e) => {
  if (e.target.id !== "email-form") return;
  e.preventDefault();
  const form = new FormData(e.target);
  await attemptSignIn(() => auth.signInStaffEmail(String(form.get("email") || ""), String(form.get("password") || "")));
});

if (demo) {
  document.getElementById("demo-banner").innerHTML =
    `<div class="demo-banner">Demo mode: data stays in this browser. Place an order on <a href="index.html" target="_blank" style="color:#fff">the shop</a> in another tab and watch it arrive here.</div>`;
}

render();
