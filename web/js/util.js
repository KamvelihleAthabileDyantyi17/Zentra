// Small helpers shared by the customer site and the staff dashboard.

/**
 * Logging. Every message starts with [Shannos] so it's easy to filter in the browser's
 * DevTools console (F12 → Console → type "Shannos" in the filter box).
 * Same levels as the Android app's AppLog: d = normal flow, i = sign-in/out, w = recovered, e = failed.
 * Never log passwords, tokens or phone numbers.
 */
export const log = {
  d: (where, msg, ...extra) => console.debug(`[Shannos][${where}] ${msg}`, ...extra),
  i: (where, msg, ...extra) => console.info(`[Shannos][${where}] ${msg}`, ...extra),
  w: (where, msg, ...extra) => console.warn(`[Shannos][${where}] ${msg}`, ...extra),
  e: (where, msg, ...extra) => console.error(`[Shannos][${where}] ${msg}`, ...extra),
};

/** Escape text before putting it into HTML, so names or notes typed by users can't inject code. */
export function esc(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

/** R55.00 */
export const rand = (n) => "R" + Number(n || 0).toFixed(2);

/** Same rule as the app: 0821234567, 082 123 4567 or +27821234567 */
export function isValidSaPhone(raw) {
  const digits = String(raw || "").replace(/[^\d+]/g, "");
  return /^(0\d{9}|\+27\d{9})$/.test(digits);
}

export const CATEGORIES = [
  { id: "KOTAS", label: "Kotas" },
  { id: "SIDES", label: "Sides" },
  { id: "ROLLS", label: "Rolls" },
  { id: "DRINKS", label: "Drinks" },
];

/** Order stages: identical names to the Android app's OrderStage enum, so both read the same data. */
export const STAGES = [
  { id: "RECEIVED", label: "Order received", hint: "The truck has your order. We'll let you know when it hits the pan.", action: "Start cooking" },
  { id: "COOKING", label: "On the pan", hint: "The team is cooking it now. Usually 8–12 minutes.", action: "Mark ready" },
  { id: "READY", label: "Ready to collect", hint: "Come grab it while it's hot. Show your order number at the window.", action: "Collected" },
  { id: "COLLECTED", label: "Collected", hint: "Enjoy! Tell us how it was.", action: null },
];
export const stageIndex = (id) => Math.max(0, STAGES.findIndex((s) => s.id === id));
export const stageInfo = (id) => STAGES[stageIndex(id)];
export const nextStage = (id) => STAGES[Math.min(STAGES.length - 1, stageIndex(id) + 1)].id;

export { orderTotal } from "./recipes.js"; // items + delivery fee
export const orderCount = (order) => (order.lines || []).reduce((sum, l) => sum + l.quantity, 0);

/** Short toast message at the bottom of the screen. */
export function toast(text) {
  let el = document.getElementById("toast");
  if (!el) {
    el = document.createElement("div");
    el.id = "toast";
    el.className = "toast";
    el.setAttribute("role", "status");
    document.body.appendChild(el);
  }
  el.textContent = text;
  el.classList.add("show");
  clearTimeout(toast._t);
  toast._t = setTimeout(() => el.classList.remove("show"), 3200);
}
