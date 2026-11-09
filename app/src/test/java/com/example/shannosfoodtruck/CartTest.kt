package com.example.shannosfoodtruck

import com.example.shannosfoodtruck.data.Cart
import com.example.shannosfoodtruck.data.OrderStage
import com.example.shannosfoodtruck.data.StarterData
import com.example.shannosfoodtruck.data.StockItem
import com.example.shannosfoodtruck.data.StockStatus
import com.example.shannosfoodtruck.data.toRand
import com.example.shannosfoodtruck.ui.customer.CheckoutForm
import com.example.shannosfoodtruck.data.Fulfilment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CartTest {
    private val menu = StarterData.menu // Signature Kota k1 = R55, Cold Drink d1 = R15

    @Test
    fun startsEmpty() {
        val cart = Cart()
        assertTrue(cart.isEmpty)
        assertEquals(0.0, cart.total(menu), 0.001)
    }

    @Test
    fun addingTheSameItemIncreasesQuantity() {
        val cart = Cart().add("k1").add("k1").add("d1")
        assertEquals(2, cart.quantityOf("k1"))
        assertEquals(3, cart.itemCount)
        assertEquals(55.0 * 2 + 15.0, cart.total(menu), 0.001)
    }

    @Test
    fun removingLastOneDropsTheLine() {
        val cart = Cart().add("k1").removeOne("k1")
        assertTrue(cart.isEmpty)
        assertEquals(0, cart.lines(menu).size)
    }

    @Test
    fun quantityIsCapped() {
        val cart = Cart().setQuantity("k1", 500)
        assertEquals(Cart.MAX_PER_ITEM, cart.quantityOf("k1"))
    }

    @Test
    fun soldOutItemsAreDroppedFromCart() {
        val cart = Cart().add("k1").add("s1")
        val menuWithChipsSoldOut = menu.map { if (it.id == "s1") it.copy(available = false) else it }
        val cleaned = cart.keepOnly(menuWithChipsSoldOut)
        assertEquals(1, cleaned.quantityOf("k1"))
        assertEquals(0, cleaned.quantityOf("s1"))
    }

    @Test
    fun linesUseCurrentMenuPrice() {
        val cart = Cart().add("k1")
        val pricier = menu.map { if (it.id == "k1") it.copy(price = 60.0) else it }
        assertEquals(60.0, cart.total(pricier), 0.001)
    }

    @Test
    fun randFormatting() {
        assertEquals("R55.00", 55.0.toRand())
        assertEquals("R7.50", 7.5.toRand())
    }

    @Test
    fun orderStagesMoveForwardAndStop() {
        assertEquals(OrderStage.COOKING, OrderStage.RECEIVED.next())
        assertEquals(OrderStage.READY, OrderStage.COOKING.next())
        assertEquals(OrderStage.COLLECTED, OrderStage.READY.next())
        assertEquals(OrderStage.COLLECTED, OrderStage.COLLECTED.next())
    }

    @Test
    fun stockStatus() {
        assertEquals(StockStatus.OUT, StockItem("a", "A", 0, 5).status)
        assertEquals(StockStatus.LOW, StockItem("a", "A", 3, 5).status)
        assertEquals(StockStatus.OK, StockItem("a", "A", 5, 5).status)
    }

    @Test
    fun saPhoneNumbers() {
        assertTrue(CheckoutForm.isValidSaPhone("0821234567"))
        assertTrue(CheckoutForm.isValidSaPhone("082 123 4567"))
        assertTrue(CheckoutForm.isValidSaPhone("+27 82 123 4567"))
        assertFalse(CheckoutForm.isValidSaPhone("12345"))
        assertFalse(CheckoutForm.isValidSaPhone(""))
    }

    @Test
    fun deliveryNeedsAnAddress() {
        val form = CheckoutForm(name = "Thando", phone = "0821234567", fulfilment = Fulfilment.DELIVERY)
        assertTrue(form.errors().containsKey("address"))
        assertTrue(form.copy(address = "12 Main Rd").errors().isEmpty())
    }
}
