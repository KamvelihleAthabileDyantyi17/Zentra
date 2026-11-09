package com.example.shannosfoodtruck.data

/**
 * Ingredients, the shop pack each one is bought in, and an example recipe per menu item.
 * This powers the Daily Purchase Recommendation (Task 1, FR-10 and FR-14): Shannon buys stock
 * from what customers actually ordered instead of by guesswork.
 *
 * PRICES: reference prices from Checkers online (checkers.co.za), looked up in November 2026.
 *   Prices change and differ per store and on special, so the app always says "estimate".
 * RECIPES: example portions for the prototype. Shannon should check them against her real
 *   portions; change a number here (and in web/js/recipes.js) and every recommendation updates.
 */
data class Ingredient(
    val id: String,
    val name: String,
    /** The unit recipes and stock are counted in, e.g. "loaf", "kg", "slice". */
    val unit: String,
    /** How it's sold, e.g. "Albany Superior white bread 700 g". */
    val pack: String,
    /** How many [unit]s are in one pack (a 7 kg bag of potatoes = 7.0 kg). */
    val packSize: Double,
    /** Checkers reference price for one pack, or null if we couldn't find one. */
    val packPrice: Double?,
)

object Recipes {

    const val PRICE_SOURCE = "Checkers online reference prices, Nov 2026. Check in store before buying."

    val ingredients = listOf(
        Ingredient("bread", "Bread", "loaf", "Albany Superior white bread 700 g", 1.0, 17.99),
        Ingredient("russians", "Smoked russians", "kg", "Smoked russians, per kg", 1.0, 49.99),
        Ingredient("cheese", "Cheese slices", "slice", "Crystal Valley cheddar slices 900 g (54)", 54.0, 114.99),
        Ingredient("potatoes", "Potatoes", "kg", "Potatoes 7 kg pocket", 7.0, 69.99),
        Ingredient("oil", "Sunflower oil", "L", "Checkers Housebrand sunflower oil 2 L", 2.0, 84.99),
        Ingredient("flour", "Cake flour", "kg", "Snowflake cake wheat flour 2.5 kg", 2.5, 42.99),
        Ingredient("yeast", "Instant yeast", "g", "NCP instant yeast sachet 10 g", 10.0, 5.99),
        Ingredient("sugar", "White sugar", "kg", "Huletts white sugar 2.5 kg", 2.5, 66.99),
        Ingredient("boerewors", "Boerewors", "kg", "Traditional thin boerewors, per kg", 1.0, 109.99),
        Ingredient("rolls", "Hotdog rolls", "roll", "Checkers white hotdog rolls 6-pack", 6.0, 14.99),
        Ingredient("wraps", "Tortilla wraps", "wrap", "Mezé tortilla wraps 6-pack", 6.0, 57.99),
        Ingredient("chicken", "Chicken breast fillets", "kg", "Chicken breast fillets, per kg", 1.0, 89.99),
        Ingredient("colddrink", "Cold drinks", "can", "Coca-Cola 6 x 330 ml cans", 6.0, 74.99),
        Ingredient("teabags", "Tea bags", "bag", "Black tea bags, 100-pack", 100.0, null),
        Ingredient("rooibos", "Rooibos tea bags", "bag", "Freshpak rooibos 80 tagless", 80.0, 54.99),
        Ingredient("water", "Bottled water", "bottle", "Thirsti still water 6 x 500 ml", 6.0, 42.99),
    )

    /** Menu item id → (ingredient id → amount per item, in the ingredient's unit). */
    val recipes: Map<String, Map<String, Double>> = mapOf(
        // Signature Kota: quarter loaf, chips, a russian, a cheese slice, frying oil
        "k1" to mapOf("bread" to 0.25, "potatoes" to 0.20, "russians" to 0.125, "cheese" to 1.0, "oil" to 0.05),
        "k2" to mapOf("bread" to 0.125, "potatoes" to 0.12, "russians" to 0.0625, "cheese" to 1.0, "oil" to 0.03),
        // Amagwinya 4-pack: about 60 g dough each
        "a1" to mapOf("flour" to 0.24, "yeast" to 3.0, "sugar" to 0.02, "oil" to 0.08),
        "s1" to mapOf("potatoes" to 0.30, "oil" to 0.06),
        "r1" to mapOf("rolls" to 1.0, "russians" to 0.125, "cheese" to 1.0, "oil" to 0.01),
        "r2" to mapOf("rolls" to 1.0, "boerewors" to 0.12),
        "r3" to mapOf("wraps" to 1.0, "chicken" to 0.10, "oil" to 0.01),
        "d1" to mapOf("colddrink" to 1.0),
        "d2" to mapOf("teabags" to 0.5, "sugar" to 0.02),
        "d3" to mapOf("rooibos" to 0.5, "sugar" to 0.015),
        "d4" to mapOf("water" to 1.0),
    )

    fun ingredient(id: String): Ingredient? = ingredients.firstOrNull { it.id == id }
}

/** Sales numbers for a set of orders. */
data class SalesSummary(
    val orderCount: Int,
    val revenue: Double,
    val deliveryFees: Double,
    val itemsSold: Int,
    val topSellers: List<Pair<String, Int>>,
) {
    val averageOrder: Double get() = if (orderCount == 0) 0.0 else revenue / orderCount
}

/** One row of the shopping list. */
data class PurchaseLine(
    val ingredient: Ingredient,
    /** Amount needed for the period, in the ingredient's unit. */
    val needed: Double,
    val onHand: Double,
    /** Whole packs to buy (0 if there's enough on the truck). */
    val packsToBuy: Int,
    /** packsToBuy × reference price, or null if there's no price. */
    val estimatedCost: Double?,
)

/**
 * Turns orders into sales numbers and a shopping list. Pure Kotlin, no Android or Firebase,
 * so it is fully unit-tested (see PurchasePlannerTest).
 */
object PurchasePlanner {

    fun summary(orders: List<Order>, topN: Int = 5): SalesSummary {
        val sold = mutableMapOf<String, Int>()
        orders.flatMap { it.lines }.forEach { sold[it.name] = (sold[it.name] ?: 0) + it.quantity }
        return SalesSummary(
            orderCount = orders.size,
            revenue = orders.sumOf { it.total },
            deliveryFees = orders.sumOf { it.deliveryFee },
            itemsSold = sold.values.sum(),
            topSellers = sold.entries.sortedByDescending { it.value }.take(topN).map { it.key to it.value },
        )
    }

    /** Total ingredient use for these orders: ingredient id → amount. */
    fun usage(orders: List<Order>): Map<String, Double> {
        val used = mutableMapOf<String, Double>()
        for (line in orders.flatMap { it.lines }) {
            val recipe = Recipes.recipes[line.itemId] ?: continue
            for ((ingredientId, perItem) in recipe) {
                used[ingredientId] = (used[ingredientId] ?: 0.0) + perItem * line.quantity
            }
        }
        return used
    }

    /**
     * The shopping list for one day.
     * @param orders the orders to base it on
     * @param days how many days those orders cover; usage is divided by this to get "a typical day"
     * @param stock what's on the truck now (ingredient id → amount); missing means zero
     */
    fun plan(orders: List<Order>, days: Int = 1, stock: Map<String, Double> = emptyMap()): List<PurchaseLine> {
        val perDay = usage(orders).mapValues { it.value / days.coerceAtLeast(1) }
        return Recipes.ingredients.mapNotNull { ingredient ->
            val needed = perDay[ingredient.id] ?: return@mapNotNull null
            val onHand = stock[ingredient.id] ?: 0.0
            val shortfall = (needed - onHand).coerceAtLeast(0.0)
            // Small tolerance so 2.0000001 packs doesn't become 3
            val packs = if (shortfall <= 1e-9) 0 else Math.ceil(shortfall / ingredient.packSize - 1e-9).toInt()
            PurchaseLine(ingredient, needed, onHand, packs, ingredient.packPrice?.let { it * packs })
        }.sortedByDescending { it.estimatedCost ?: 0.0 }
    }

    /** Sum of the priced lines. Unpriced lines are listed separately in the UI. */
    fun totalCost(lines: List<PurchaseLine>): Double = lines.sumOf { it.estimatedCost ?: 0.0 }
}
