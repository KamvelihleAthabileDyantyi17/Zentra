package com.example.shannosfoodtruck.admin

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.shannosfoodtruck.R
import com.example.shannosfoodtruck.data.MenuRepository
import com.example.shannosfoodtruck.features.menu.MenuItem
import com.google.firebase.firestore.ListenerRegistration

class AdminMenuActivity : AppCompatActivity() {
    private var reg: ListenerRegistration? = null
    private var items: List<MenuItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_menu)

        val name = findViewById<EditText>(R.id.inputItemName)
        val desc = findViewById<EditText>(R.id.inputItemDesc)
        val price = findViewById<EditText>(R.id.inputItemPrice)
        val category = findViewById<EditText>(R.id.inputItemCategory)
        val emoji = findViewById<EditText>(R.id.inputItemEmoji)
        val list = findViewById<ListView>(R.id.listMenu)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        list.adapter = adapter

        reg = MenuRepository.listen({ newItems ->
            items = newItems
            adapter.clear()
            adapter.addAll(newItems.map { "${it.emoji} ${it.name}  -  R%.2f  (${it.category})".format(it.price) })
        }, { msg -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show() })

        findViewById<Button>(R.id.buttonAddItem).setOnClickListener {
            val p = price.text.toString().toDoubleOrNull()
            if (name.text.isBlank() || p == null || p <= 0) {
                Toast.makeText(this, "Enter a name and a valid price", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MenuRepository.add(name.text.toString().trim(), desc.text.toString().trim(), p,
                category.text.toString().trim().ifBlank { "Mains" },
                emoji.text.toString().trim().ifBlank { "🍽️" }) { ok ->
                if (ok) { name.text.clear(); desc.text.clear(); price.text.clear() }
                else Toast.makeText(this, "Could not add item", Toast.LENGTH_SHORT).show()
            }
        }

        list.setOnItemLongClickListener { _, _, pos, _ ->
            val item = items.getOrNull(pos) ?: return@setOnItemLongClickListener true
            AlertDialog.Builder(this).setTitle("Delete ${item.name}?")
                .setPositiveButton("Delete") { _, _ -> MenuRepository.delete(item.docId) {} }
                .setNegativeButton("Cancel", null).show()
            true
        }
    }

    override fun onDestroy() { reg?.remove(); super.onDestroy() }
}