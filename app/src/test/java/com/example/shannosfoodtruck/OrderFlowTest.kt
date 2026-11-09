package com.example.shannosfoodtruck

import com.example.shannosfoodtruck.data.DemoAuthService
import com.example.shannosfoodtruck.data.DemoRepository
import com.example.shannosfoodtruck.data.InMemoryActiveOrderStore
import com.example.shannosfoodtruck.data.OrderStage
import com.example.shannosfoodtruck.ui.customer.CustomerViewModel
import com.example.shannosfoodtruck.ui.staff.StaffViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** End-to-end: a customer orders, staff move it along, the customer sees it live. */
@OptIn(ExperimentalCoroutinesApi::class)
class OrderFlowTest {

    private lateinit var repo: DemoRepository
    private lateinit var store: InMemoryActiveOrderStore
    private lateinit var customer: CustomerViewModel
    private val dispatcher = StandardTestDispatcher()

    /** Let every pending coroutine run, so state is settled before we check it. */
    private fun idle() = dispatcher.scheduler.advanceUntilIdle()

    private fun signedInStaff(): StaffViewModel {
        val staff = StaffViewModel(repo, DemoAuthService())
        idle()
        staff.signIn("owner@shannos.co.za", "secret123")
        idle()
        return staff
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repo = DemoRepository()
        store = InMemoryActiveOrderStore()
        customer = CustomerViewModel(repo, DemoAuthService(), store)
        idle()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun menuAndTrucksLoad() {
        val s = customer.state.value
        assertFalse(s.loadingMenu)
        assertEquals(11, s.menu.size)
        assertEquals("truck1", s.selectedTruck?.id) // first open truck by default
    }

    @Test
    fun categoryFilter() {
        customer.selectCategory(com.example.shannosfoodtruck.data.Category.DRINKS)
        idle()
        assertTrue(customer.state.value.visibleMenu.all { it.category.label == "Drinks" })
        assertEquals(4, customer.state.value.visibleMenu.size)
    }

    @Test
    fun invalidCheckoutDoesNotPlaceOrder() {
        customer.add("k1")
        var placed = false
        customer.placeOrder { placed = true }
        idle()
        assertFalse(placed)
        assertTrue(customer.state.value.showFormErrors)
    }

    @Test
    fun fullOrderJourney() {
        customer.add("k1")
        customer.add("k1")
        customer.add("d1")
        customer.updateForm { it.copy(name = "Thando", phone = "082 123 4567") }

        var placed = false
        customer.placeOrder { placed = true }
        idle()

        assertTrue(placed)
        val afterPlace = customer.state.value
        assertTrue(afterPlace.cart.isEmpty)
        assertNotNull(afterPlace.activeOrder)
        assertEquals(125.0, afterPlace.activeOrder!!.total, 0.001)
        assertEquals(OrderStage.RECEIVED, afterPlace.activeOrder!!.stage)
        assertEquals(afterPlace.activeOrderId, store.get())

        // Staff side
        val staff = signedInStaff()
        assertEquals(true, staff.state.value.signedIn)
        assertEquals(1, staff.state.value.orders.size)

        staff.advance(staff.state.value.orders.first())
        idle()
        assertEquals(OrderStage.COOKING, customer.state.value.activeOrder!!.stage)

        staff.advance(staff.state.value.orders.first())
        idle()
        staff.advance(staff.state.value.orders.first())
        idle()
        assertEquals(OrderStage.COLLECTED, customer.state.value.activeOrder!!.stage)
        assertTrue(staff.state.value.orders.isEmpty()) // collected orders leave the board

        // Feedback clears the active order
        var done = false
        customer.submitFeedback(5, "Lekker!") { done = true }
        idle()
        assertTrue(done)
        assertNull(customer.state.value.activeOrder)
        assertNull(store.get())
        assertEquals(1, repo.feedback.size)
    }

    @Test
    fun soldOutItemCannotBeAdded() {
        val staff = signedInStaff()
        val chips = staff.state.value.menu.first { it.id == "s1" }
        staff.toggleAvailable(chips)
        idle()

        customer.add("s1")
        assertEquals(0, customer.state.value.cart.quantityOf("s1"))
    }

    @Test
    fun googleSignInKeepsGuestOrdersAndShowsHistory() {
        // Order as a guest first
        customer.add("k1")
        customer.updateForm { it.copy(name = "Guest", phone = "0821234567") }
        customer.placeOrder { }
        idle()
        assertFalse(customer.state.value.isSignedIn)

        // Then sign in with Google (demo token): same account, so the order shows in history
        customer.signInWithGoogle("demo-token")
        idle()
        val s = customer.state.value
        assertTrue(s.isSignedIn)
        assertEquals("Thando Demo", s.user?.name)
        assertEquals(1, s.myOrders.size)
        assertEquals(55.0, s.myOrders.first().total, 0.001)
    }

    @Test
    fun signOutEndsSessionAndStopsTracking() {
        customer.signInWithGoogle("demo-token")
        idle()
        customer.add("d1")
        customer.updateForm { it.copy(phone = "0821234567") } // name pre-filled from Google
        customer.placeOrder { }
        idle()
        assertNotNull(customer.state.value.activeOrder)

        customer.signOut()
        idle()
        val s = customer.state.value
        assertFalse(s.isSignedIn)
        assertNull(s.activeOrder)
        assertTrue(s.myOrders.isEmpty())
        assertNull(store.get())
    }

    @Test
    fun staffCanUseGoogleSignIn() {
        val staff = StaffViewModel(repo, DemoAuthService())
        idle()
        staff.signInWithGoogle("demo-token")
        idle()
        assertEquals(true, staff.state.value.signedIn)
    }
}
