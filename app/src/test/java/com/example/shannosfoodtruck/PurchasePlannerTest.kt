package com.example.shannosfoodtruck

import com.example.shannosfoodtruck.data.DeliveryArea
import com.example.shannosfoodtruck.data.Fulfilment
import com.example.shannosfoodtruck.data.Order
import com.example.shannosfoodtruck.data.OrderLine
import com.example.shannosfoodtruck.data.PaymentMethod
import com.example.shannosfoodtruck.data.PickupTimes
import com.example.shannosfoodtruck.data.PurchasePlanner
import com.example.shannosfoodtruck.data.Recipes
import com.example.shannosfoodtruck.data.StarterData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sales report, Daily Purchase Recommendation (Task 1 FR-10/12/14), delivery fees and pickup times. */
class PurchasePlannerTest {

    private fun order(vararg lines: Pair<String, Int>, fulfilment: Fulfilment = Fulfilment.PICKUP, area: DeliveryArea? = null) = Order(
        number = "SFT-1",
        truckId = "truck1",
        customerName = "Test",
        phone = "0821234567",
        fulfilment = fulfilment,
        deliveryArea = area,
        payment = PaymentMethod.CASH,
        lines = lines.map { (id, qty) ->
            val item = StarterData.menu.first { it.id == id }
            OrderLine(id, item.name, qty, item.price)
        },
    )

    @Test
    fun everyMenuItemHasARecipeAndEveryRecipeIngredientExists() {
        StarterData.menu.forEach { assertTrue("No recipe for ${it.id}", Recipes.recipes.containsKey(it.id)) }
        Recipes.recipes.values.flatMap { it.keys }.forEach { assertTrue("Unknown ingredient $it", Recipes.ingredient(it) != null) }
    }

    @Test
    fun salesSummaryAddsUpOrdersAndRanksTopSellers() {
        val orders = listOf(order("k1" to 2, "d1" to 1), order("k1" to 1), order("a1" to 1))
        val s = PurchasePlanner.summary(orders)
        assertEquals(3, s.orderCount)
        assertEquals(55.0 * 3 + 15 + 25, s.revenue, 0.001)
        assertEquals(5, s.itemsSold)
        assertEquals("Signature Kota" to 3, s.topSellers.first())
        assertEquals(s.revenue / 3, s.averageOrder, 0.001)
    }

    @Test
    fun usageMultipliesRecipesByQuantity() {
        val used = PurchasePlanner.usage(listOf(order("k1" to 4, "s1" to 2)))
        assertEquals(1.0, used["bread"]!!, 1e-9)            // 4 × quarter loaf
        assertEquals(4 * 0.20 + 2 * 0.30, used["potatoes"]!!, 1e-9)
        assertEquals(4.0, used["cheese"]!!, 1e-9)
    }

    @Test
    fun planRoundsUpToWholePacksAndSubtractsStock() {
        // 30 Signature Kotas = 7.5 loaves; 2 on the truck → 5.5 short → buy 6 loaves
        val plan = PurchasePlanner.plan(listOf(order("k1" to 30)), stock = mapOf("bread" to 2.0))
        val bread = plan.first { it.ingredient.id == "bread" }
        assertEquals(7.5, bread.needed, 1e-9)
        assertEquals(6, bread.packsToBuy)
        assertEquals(6 * 17.99, bread.estimatedCost!!, 0.001)
        // 30 cheese slices from a 54-slice pack → 1 pack
        assertEquals(1, plan.first { it.ingredient.id == "cheese" }.packsToBuy)
    }

    @Test
    fun enoughStockMeansBuyNothing() {
        val plan = PurchasePlanner.plan(listOf(order("d1" to 3)), stock = mapOf("colddrink" to 24.0))
        assertEquals(0, plan.single().packsToBuy)
        assertEquals(0.0, PurchasePlanner.totalCost(plan), 0.001)
    }

    @Test
    fun weekBasisAveragesPerDay() {
        val week = List(7) { order("r2" to 6) }               // 42 boerewors rolls over 7 days
        val rolls = PurchasePlanner.plan(week, days = 7).first { it.ingredient.id == "rolls" }
        assertEquals(6.0, rolls.needed, 1e-9)                 // 6 per typical day
        assertEquals(1, rolls.packsToBuy)                     // one 6-pack
    }

    @Test
    fun unpricedIngredientHasNoCost() {
        val tea = PurchasePlanner.plan(listOf(order("d2" to 2))).first { it.ingredient.id == "teabags" }
        assertEquals(1, tea.packsToBuy)
        assertNull(tea.estimatedCost)
    }

    @Test
    fun deliveryFeeIsFreeInParklandsAndR50Outside() {
        assertEquals(55.0, order("k1" to 1, fulfilment = Fulfilment.DELIVERY, area = DeliveryArea.PARKLANDS).total, 0.001)
        assertEquals(105.0, order("k1" to 1, fulfilment = Fulfilment.DELIVERY, area = DeliveryArea.OUTSIDE).total, 0.001)
        assertEquals(55.0, order("k1" to 1, fulfilment = Fulfilment.PICKUP, area = DeliveryArea.OUTSIDE).total, 0.001)
    }

    @Test
    fun pickupSlotsStartAQuarterHourAheadAndStopAtClosing() {
        val at1402 = PickupTimes.slots(14 * 60 + 2)
        assertEquals("14:30", at1402.first())                 // 14:02 + 15 min = 14:17 → next quarter hour
        assertEquals("20:00", at1402.last())
        assertEquals("06:00", PickupTimes.slots(5 * 60).first()) // before opening: first slot is opening time
        assertTrue(PickupTimes.slots(19 * 60 + 50).isEmpty())  // too late for a slot today
    }
}
