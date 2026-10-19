package com.example.shannosfoodtruck.data

import com.google.firebase.firestore.FirebaseFirestore

object SeedData {
    fun seedIfEmpty() {
        val db = FirebaseFirestore.getInstance()
        db.collection("menu_items").limit(1).get().addOnSuccessListener { snap ->
            if (!snap.isEmpty) return@addOnSuccessListener
            val batch = db.batch()
            val menu = listOf(
                Triple("Signature Kota", 55.0, "Mains"), Triple("Half Kota", 32.0, "Mains"),
                Triple("Amagwinya (4 pack)", 25.0, "Mains"), Triple("Slap Chips", 20.0, "Mains"),
                Triple("Chicken Wrap", 45.0, "Mains"), Triple("Beef Burger", 60.0, "Mains"),
                Triple("Vienna Roll", 28.0, "Mains"), Triple("Cold Drink 330ml", 15.0, "Drinks"),
                Triple("Homemade Ice Tea", 18.0, "Drinks"), Triple("Bottled Water", 12.0, "Drinks")
            )
            menu.forEachIndexed { i, (name, price, cat) ->
                batch.set(db.collection("menu_items").document("item_${i + 1}"),
                    mapOf("id" to i + 1, "name" to name, "price" to price, "category" to cat))
            }
            val statuses = listOf("Received", "Preparing", "Ready", "Collected")
            for (i in 1..10) {
                batch.set(db.collection("orders").document("order_$i"), mapOf(
                    "id" to i, "customer" to "Customer $i",
                    "total" to 20.0 + i * 7, "status" to statuses[i % 4]))
                batch.set(db.collection("feedback").document("fb_$i"), mapOf(
                    "id" to i, "rating" to (3 + i % 3), "comment" to "Sample feedback $i"))
            }
            batch.commit()
        }
    }
}