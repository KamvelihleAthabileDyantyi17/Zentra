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

export const STARTER_TRUCKS = [
  {
    id: "truck1", name: "Truck 1", spot: "Cnr Sandown Rd & Discovery Dr, Rivergate", open: true, comingSoon: false,
    address: "Corner of Sandown Road and Discovery Drive, Rivergate, Cape Farms, Cape Town, 7441",
  },
  // Locations for trucks 2 and 3 are still being decided
  { id: "truck2", name: "Truck 2", spot: "Location coming soon", open: false, comingSoon: true, address: "" },
  { id: "truck3", name: "Truck 3", spot: "Location coming soon", open: false, comingSoon: true, address: "" },
];

// Starting stock per truck, in each ingredient's unit (see recipes.js). Example numbers.
export const STARTER_STOCK = [
  { id: "bread", name: "Bread", onHand: 6, reorderAt: 8, unit: "loaf" },
  { id: "potatoes", name: "Potatoes", onHand: 10, reorderAt: 7, unit: "kg" },
  { id: "russians", name: "Smoked russians", onHand: 1.5, reorderAt: 1, unit: "kg" },
  { id: "cheese", name: "Cheese slices", onHand: 20, reorderAt: 30, unit: "slice" },
  { id: "oil", name: "Sunflower oil", onHand: 3, reorderAt: 2, unit: "L" },
  { id: "flour", name: "Cake flour", onHand: 0, reorderAt: 2.5, unit: "kg" },
  { id: "rolls", name: "Hotdog rolls", onHand: 12, reorderAt: 12, unit: "roll" },
  { id: "colddrink", name: "Cold drinks", onHand: 24, reorderAt: 12, unit: "can" },
];
