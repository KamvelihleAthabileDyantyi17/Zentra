package com.example.shannosfoodtruck

import com.example.shannosfoodtruck.database.PasswordHasher
import com.example.shannosfoodtruck.features.cart.CartViewModel
import com.example.shannosfoodtruck.features.home.HomeViewModel
import com.example.shannosfoodtruck.features.menu.MenuViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelsUnitTest {

    @Test
    fun testHomeViewModelFeaturedItemsNotEmpty() {
        val viewModel = HomeViewModel()
        assertTrue(viewModel.featuredItems.isNotEmpty())
        assertEquals("Amagwinya", viewModel.featuredItems[0].name)
    }

    @Test
    fun testMenuViewModelInitialState() {
        val viewModel = MenuViewModel()
        val state = viewModel.state.value
        assertEquals(6, state.items.size)
        assertEquals(2, state.categories.size)
    }

    @Test
    fun testMenuViewModelFilterCategory() {
        val viewModel = MenuViewModel()
        viewModel.selectCategory("Drinks")
        val state = viewModel.state.value
        assertEquals(2, state.items.size)
        assertTrue(state.items.all { it.category == "Drinks" })
    }

    @Test
    fun testMenuViewModelSearchQuery() {
        val viewModel = MenuViewModel()
        viewModel.onQueryChange("Kota")
        val state = viewModel.state.value
        assertEquals(2, state.items.size)
    }

    @Test
    fun testCartViewModelCalculation() {
        val viewModel = CartViewModel()
        val total = viewModel.calculateTotal()
        assertTrue(total > 0.0)
    }

    @Test
    fun testPasswordHasher() {
        val pass1 = PasswordHasher.hashPassword("Secret123")
        val pass2 = PasswordHasher.hashPassword("Secret123")
        val pass3 = PasswordHasher.hashPassword("Different")

        assertEquals(pass1, pass2)
        assertNotEquals(pass1, pass3)
        assertEquals(64, pass1.length) // SHA-256 hex length
    }
}
