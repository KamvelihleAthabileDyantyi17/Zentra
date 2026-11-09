// Starting menu, trucks and stock. Same as the Android app's StarterData.kt.
// Used by demo mode, and by the staff "Load starter menu" button on an empty database.

export const STARTER_MENU = [
  { id: "k1", name: "Signature Kota", description: "Quarter loaf, chips, russian, cheese and our secret sauce", price: 55, category: "KOTAS", tag: "KOTA" },
  { id: "k2", name: "Half Kota", description: "All the flavour, half the size", price: 32, category: "KOTAS", tag: "½ KOTA" },
  { id: "a1", name: "Amagwinya (4 pack)", description: "Soft, fluffy, fried fresh", price: 25, category: "SIDES", tag: "AMA" },
  { id: "s1", name: "Slap Chips", description: "Salt and vinegar on request", price: 20, category: "SIDES", tag: "CHIPS" },
  { id: "r1", name: "Cheese Russian Roll", description: "Grilled russian with melted cheddar", price: 35, category: "ROLLS", tag: "RUSS" },
  { id: "r2", name: "Boerewors Roll", description: "Traditional boerewors with relish", price: 40, category: "ROLLS", tag: "WORS" },
  { id: "r3", name: "Chicken Wrap", description: "Grilled chicken, wrapped fresh", price: 45, category: "ROLLS", tag: "WRAP" },
  { id: "d1", name: "Cold Drink (330ml)", description: "Ask about today's flavours", price: 15, category: "DRINKS", tag: "COLD" },
  { id: "d2", name: "Homemade Ice Tea", description: "Brewed fresh daily", price: 18, category: "DRINKS", tag: "TEA" },
  { id: "d3", name: "Rooibos Iced Tea", description: "Local rooibos, served cold", price: 20, category: "DRINKS", tag: "ROOI" },
  { id: "d4", name: "Bottled Water", description: "Still, chilled", price: 12, category: "DRINKS", tag: "H2O" },
].map((item, i) => ({ ...item, available: true, sort: i }));

export const FAVOURITE_IDS = ["k1", "a1", "d3"];

// TODO(team): real locations for trucks 2 and 3
export const STARTER_TRUCKS = [
  { id: "truck1", name: "Truck 1", spot: "Parklands Main Stand", open: true },
  { id: "truck2", name: "Truck 2", spot: "[LOCATION]", open: true },
  { id: "truck3", name: "Truck 3", spot: "[LOCATION]", open: false },
];

export const STARTER_STOCK = [
  { id: "bread", name: "Bread loaves", onHand: 14, reorderAt: 10 },
  { id: "dough", name: "Vetkoek dough (batches)", onHand: 3, reorderAt: 5 },
  { id: "russians", name: "Russian sausages (packs)", onHand: 6, reorderAt: 4 },
  { id: "cheese", name: "Cheese slices (packs)", onHand: 2, reorderAt: 4 },
  { id: "potatoes", name: "Potatoes (kg)", onHand: 0, reorderAt: 8 },
  { id: "drinks", name: "Cold drinks (crates)", onHand: 9, reorderAt: 3 },
];
