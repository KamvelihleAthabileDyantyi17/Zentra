package com.example.shannosfoodtruck.data

/**
 * The starting menu, trucks and stock list. Used by the offline demo, and by the
 * staff "Load starter menu" button to fill an empty Firestore database.
 * No fake orders or feedback are ever written to the live database.
 */
object StarterData {

    val menu = listOf(
        MenuItem("k1", "Signature Kota", "Quarter loaf, chips, russian, cheese and our secret sauce", 55.0, Category.KOTAS, "KOTA"),
        MenuItem("k2", "Half Kota", "All the flavour, half the size", 32.0, Category.KOTAS, "½ KOTA"),
        MenuItem("a1", "Amagwinya (4 pack)", "Soft, fluffy, fried fresh", 25.0, Category.SIDES, "AMA"),
        MenuItem("s1", "Slap Chips", "Salt and vinegar on request", 20.0, Category.SIDES, "CHIPS"),
        MenuItem("r1", "Cheese Russian Roll", "Grilled russian with melted cheddar", 35.0, Category.ROLLS, "RUSS"),
        MenuItem("r2", "Boerewors Roll", "Traditional boerewors with relish", 40.0, Category.ROLLS, "WORS"),
        MenuItem("r3", "Chicken Wrap", "Grilled chicken, wrapped fresh", 45.0, Category.ROLLS, "WRAP"),
        MenuItem("d1", "Cold Drink (330ml)", "Ask about today's flavours", 15.0, Category.DRINKS, "COLD"),
        MenuItem("d2", "Homemade Ice Tea", "Brewed fresh daily", 18.0, Category.DRINKS, "TEA"),
        MenuItem("d3", "Rooibos Iced Tea", "Local rooibos, served cold", 20.0, Category.DRINKS, "ROOI"),
        MenuItem("d4", "Bottled Water", "Still, chilled", 12.0, Category.DRINKS, "H2O"),
    )

    /** Ids of the items shown under "Kasi favourites" on the home screen. */
    val favouriteIds = listOf("k1", "a1", "d3")

    // TODO(team): replace [LOCATION] with the real spots for trucks 2 and 3.
    val trucks = listOf(
        Truck("truck1", "Truck 1", "Parklands Main Stand", open = true),
        Truck("truck2", "Truck 2", "[LOCATION]", open = true),
        Truck("truck3", "Truck 3", "[LOCATION]", open = false),
    )

    val stock = listOf(
        StockItem("bread", "Bread loaves", 14, 10),
        StockItem("dough", "Vetkoek dough (batches)", 3, 5),
        StockItem("russians", "Russian sausages (packs)", 6, 4),
        StockItem("cheese", "Cheese slices (packs)", 2, 4),
        StockItem("potatoes", "Potatoes (kg)", 0, 8),
        StockItem("drinks", "Cold drinks (crates)", 9, 3),
    )
}
