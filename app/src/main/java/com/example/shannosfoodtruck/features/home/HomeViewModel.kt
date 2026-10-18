package com.example.shannosfoodtruck.features.home

import androidx.lifecycle.ViewModel

// One featured item shown on the home screen
data class FeaturedItem(
    val name: String,
    val description: String,
    val price: String
)

class HomeViewModel : ViewModel() {

    // Hard-coded for now. Later this can come from the database.
    val featuredItems: List<FeaturedItem> = listOf(
        FeaturedItem("Amagwinya", "Fresh, fluffy fried dough with your choice of filling", "R20.00"),
        FeaturedItem("Kota Special", "Loaded quarter loaf, a local comfort-food favourite", "R55.00"),
        FeaturedItem("Iced Rooibos", "Cold, refreshing and lightly sweetened", "R20.00")
    )
}