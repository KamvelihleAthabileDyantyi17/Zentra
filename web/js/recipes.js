// Ingredients, example recipes, delivery areas and pickup times: the same data and rules as the
// Android app (Recipes.kt, PickupTimes.kt, DeliveryArea in Models.kt). Keep the two in step.
//
// Powers the staff "Sales & buying" page: the Daily Purchase Recommendation from Task 1
// (FR-10, FR-14) turns real orders into a shopping list, so Shannon stops buying by guesswork.

export const PRICE_SOURCE = "Checkers online reference prices, Nov 2026. Check in store before buying.";

// packSize is in the ingredient's unit; packPrice is the Checkers reference price (null = not found)
export const INGREDIENTS = [
  { id: "bread", name: "Bread", unit: "loaf", pack: "Albany Superior white bread 700 g", packSize: 1, packPrice: 17.99 },
  { id: "russians", name: "Smoked russians", unit: "kg", pack: "Smoked russians, per kg", packSize: 1, packPrice: 49.99 },
  { id: "cheese", name: "Cheese slices", unit: "slice", pack: "Crystal Valley cheddar slices 900 g (54)", packSize: 54, packPrice: 114.99 },
  { id: "potatoes", name: "Potatoes", unit: "kg", pack: "Potatoes 7 kg pocket", packSize: 7, packPrice: 69.99 },
  { id: "oil", name: "Sunflower oil", unit: "L", pack: "Checkers Housebrand sunflower oil 2 L", packSize: 2, packPrice: 84.99 },
  { id: "flour", name: "Cake flour", unit: "kg", pack: "Snowflake cake wheat flour 2.5 kg", packSize: 2.5, packPrice: 42.99 },
  { id: "yeast", name: "Instant yeast", unit: "g", pack: "NCP instant yeast sachet 10 g", packSize: 10, packPrice: 5.99 },
  { id: "sugar", name: "White sugar", unit: "kg", pack: "Huletts white sugar 2.5 kg", packSize: 2.5, packPrice: 66.99 },
  { id: "boerewors", name: "Boerewors", unit: "kg", pack: "Traditional thin boerewors, per kg", packSize: 1, packPrice: 109.99 },
  { id: "rolls", name: "Hotdog rolls", unit: "roll", pack: "Checkers white hotdog rolls 6-pack", packSize: 6, packPrice: 14.99 },
  { id: "wraps", name: "Tortilla wraps", unit: "wrap", pack: "Mezé tortilla wraps 6-pack", packSize: 6, packPrice: 57.99 },
  { id: "chicken", name: "Chicken breast fillets", unit: "kg", pack: "Chicken breast fillets, per kg", packSize: 1, packPrice: 89.99 },
  { id: "colddrink", name: "Cold drinks", unit: "can", pack: "Coca-Cola 6 x 330 ml cans", packSize: 6, packPrice: 74.99 },
  { id: "teabags", name: "Tea bags", unit: "bag", pack: "Black tea bags, 100-pack", packSize: 100, packPrice: null },
  { id: "rooibos", name: "Rooibos tea bags", unit: "bag", pack: "Freshpak rooibos 80 tagless", packSize: 80, packPrice: 54.99 },
  { id: "water", name: "Bottled water", unit: "bottle", pack: "Thirsti still water 6 x 500 ml", packSize: 6, packPrice: 42.99 },
];

// Example portions per menu item (ingredient id → amount in the ingredient's unit)
export const RECIPES = {
  k1: { bread: 0.25, potatoes: 0.2, russians: 0.125, cheese: 1, oil: 0.05 },
  k2: { bread: 0.125, potatoes: 0.12, russians: 0.0625, cheese: 1, oil: 0.03 },
  a1: { flour: 0.24, yeast: 3, sugar: 0.02, oil: 0.08 },
  s1: { potatoes: 0.3, oil: 0.06 },
  r1: { rolls: 1, russians: 0.125, cheese: 1, oil: 0.01 },
  r2: { rolls: 1, boerewors: 0.12 },
  r3: { wraps: 1, chicken: 0.1, oil: 0.01 },
  d1: { colddrink: 1 },
  d2: { teabags: 0.5, sugar: 0.02 },
  d3: { rooibos: 0.5, sugar: 0.015 },
  d4: { water: 1 },
};

/** Owner's delivery pricing: free inside Parklands, R50 anywhere else. */
export const DELIVERY_AREAS = [
  { id: "PARKLANDS", label: "In Parklands", fee: 0 },
  { id: "OUTSIDE", label: "Outside Parklands", fee: 50 },
];
export const deliveryArea = (id) => DELIVERY_AREAS.find((a) => a.id === id) || null;

/** Items + delivery fee. Works for orders saved by the app or the website. */
export function orderTotal(order) {
  const items = (order.lines || []).reduce((s, l) => s + l.quantity * l.unitPrice, 0);
  return items + (order.fulfilment === "DELIVERY" ? Number(order.deliveryFee || 0) : 0);
}

// ---------- pickup times (Task 1, FR-04) ----------
export const ASAP = "ASAP";
const OPEN = 6 * 60, CLOSE = 20 * 60, STEP = 15, LEAD = 15; // trading hours from Task 1, NFR-04
const fmt = (m) => String(Math.floor(m / 60)).padStart(2, "0") + ":" + String(m % 60).padStart(2, "0");

/** Quarter-hour slots from at least 15 minutes from now until closing. */
export function pickupSlots(nowMinutes) {
  const earliest = Math.max(nowMinutes + LEAD, OPEN);
  const slots = [];
  for (let m = Math.ceil(earliest / STEP) * STEP; m <= CLOSE; m += STEP) slots.push(fmt(m));
  return slots;
}
export const nowMinutes = () => { const d = new Date(); return d.getHours() * 60 + d.getMinutes(); };
export const timeLabel = (t) => (!t || t === ASAP ? "As soon as possible" : t);

// ---------- sales + Daily Purchase Recommendation ----------
export function salesSummary(orders, topN = 5) {
  const sold = {};
  orders.flatMap((o) => o.lines || []).forEach((l) => { sold[l.name] = (sold[l.name] || 0) + l.quantity; });
  const revenue = orders.reduce((s, o) => s + orderTotal(o), 0);
  return {
    orderCount: orders.length,
    revenue,
    itemsSold: Object.values(sold).reduce((a, b) => a + b, 0),
    averageOrder: orders.length ? revenue / orders.length : 0,
    topSellers: Object.entries(sold).sort((a, b) => b[1] - a[1]).slice(0, topN),
  };
}

export function usage(orders) {
  const used = {};
  orders.flatMap((o) => o.lines || []).forEach((l) => {
    Object.entries(RECIPES[l.itemId] || {}).forEach(([id, per]) => { used[id] = (used[id] || 0) + per * l.quantity; });
  });
  return used;
}

/** Shopping list for one day. days = how many days the orders cover; stock = { ingredientId: amount }. */
export function purchasePlan(orders, days = 1, stock = {}) {
  const used = usage(orders);
  return INGREDIENTS.filter((i) => used[i.id] !== undefined)
    .map((i) => {
      const needed = used[i.id] / Math.max(1, days);
      const onHand = stock[i.id] || 0;
      const shortfall = Math.max(0, needed - onHand);
      const packsToBuy = shortfall <= 1e-9 ? 0 : Math.ceil(shortfall / i.packSize - 1e-9);
      return { ingredient: i, needed, onHand, packsToBuy, estimatedCost: i.packPrice == null ? null : i.packPrice * packsToBuy };
    })
    .sort((a, b) => (b.estimatedCost || 0) - (a.estimatedCost || 0));
}

export const totalCost = (lines) => lines.reduce((s, l) => s + (l.estimatedCost || 0), 0);

/** 2, 0.5, 1.25 */
export const qty = (n) => (Number.isInteger(n) ? String(n) : String(Math.round(n * 100) / 100));
