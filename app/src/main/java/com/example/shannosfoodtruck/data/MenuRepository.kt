package com.example.shannosfoodtruck.data

import com.example.shannosfoodtruck.features.menu.MenuItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object MenuRepository {
    private val col get() = FirebaseFirestore.getInstance().collection("menu_items")

    /** Calls onChange every time the menu changes in Firestore, in real time. */
    fun listen(onChange: (List<MenuItem>) -> Unit, onError: (String) -> Unit = {}): ListenerRegistration =
        col.addSnapshotListener { snap, e ->
            if (e != null || snap == null) { onError(e?.message ?: "Could not load menu"); return@addSnapshotListener }
            onChange(snap.documents.map { d ->
                MenuItem(
                    id = d.id.hashCode(),
                    name = d.getString("name") ?: "",
                    description = d.getString("description") ?: "",
                    price = d.getDouble("price") ?: 0.0,
                    category = d.getString("category") ?: "Mains",
                    emoji = d.getString("emoji") ?: "🍽️",
                    docId = d.id
                )
            }.sortedBy { it.name })
        }

    fun add(name: String, description: String, price: Double, category: String, emoji: String,
            onDone: (Boolean) -> Unit) {
        col.add(mapOf("name" to name, "description" to description, "price" to price,
            "category" to category, "emoji" to emoji))
            .addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
    }

    fun delete(docId: String, onDone: (Boolean) -> Unit) {
        col.document(docId).delete()
            .addOnSuccessListener { onDone(true) }.addOnFailureListener { onDone(false) }
    }
}