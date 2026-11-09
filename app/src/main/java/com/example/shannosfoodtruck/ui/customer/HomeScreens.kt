package com.example.shannosfoodtruck.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shannosfoodtruck.data.toRand
import com.example.shannosfoodtruck.ui.components.AddButton
import com.example.shannosfoodtruck.ui.components.BackHeader
import com.example.shannosfoodtruck.ui.components.Badge
import com.example.shannosfoodtruck.ui.components.BrandMark
import com.example.shannosfoodtruck.ui.components.FoodTile
import com.example.shannosfoodtruck.ui.components.PillButton
import com.example.shannosfoodtruck.ui.components.SectionLabel
import com.example.shannosfoodtruck.ui.theme.DisplayFont
import com.example.shannosfoodtruck.ui.theme.Kasi

@Composable
fun HomeScreen(
    state: CustomerUiState,
    onOrderNow: () -> Unit,
    onPickTruck: () -> Unit,
    onTrack: () -> Unit,
    onAdd: (String) -> Unit,
    onStaff: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        // Header: brand + which truck you're ordering from
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BrandMark("Food Truck")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .clip(CircleShape)
                    .background(Kasi.Card)
                    .border(1.5.dp, Kasi.Line, CircleShape)
                    .clickable(role = Role.Button, onClickLabel = "Choose truck", onClick = onPickTruck)
                    .padding(horizontal = 12.dp),
            ) {
                val open = state.selectedTruck != null
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (open) Kasi.Green else Kasi.Faint))
                Text(state.selectedTruck?.name ?: "Closed", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Hero
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Kasi.Sign)
                .padding(horizontal = 22.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("10–15 MIN TURNAROUND", style = MaterialTheme.typography.labelMedium)
            Text(
                "KASI FLAVOUR.\nNO QUEUE.",
                fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 42.sp, lineHeight = 42.sp,
            )
            Text(
                "Order your kota, amagwinya and a cold one ahead. Collect it hot from the truck.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth(0.85f),
            )
            PillButton("Order now", onOrderNow, container = Kasi.Ink)
        }

        // Live order shortcut
        val order = state.activeOrder
        if (state.hasLiveOrder && order != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Kasi.Ink)
                    .clickable(role = Role.Button, onClick = onTrack)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("YOUR ORDER · ${order.number}", color = Kasi.Sign, style = MaterialTheme.typography.labelMedium)
                    Text(order.stage.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
                Text("Track", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Favourites
        if (state.favourites.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("KASI FAVOURITES", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = onOrderNow) { Text("Full menu", color = Kasi.Chilli, fontWeight = FontWeight.Bold) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(end = 8.dp)) {
                    items(state.favourites, key = { it.id }) { item ->
                        Column(
                            Modifier
                                .width(150.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .background(Kasi.Card),
                        ) {
                            FoodTile(item, size = 150.dp, fontSize = 28, modifier = Modifier.heightIn(max = 92.dp))
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.price.toRand(), fontWeight = FontWeight.Bold)
                                    if (item.available) AddButton(item.name, { onAdd(item.id) })
                                    else Badge("Sold out", Kasi.ChilliSoft, Kasi.ChilliDark)
                                }
                            }
                        }
                    }
                }
            }
        }

        // How it works
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("HOW IT WORKS", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("Pick your food", "Pay card or cash", "Collect it hot").forEachIndexed { i, step ->
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Kasi.Card)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("${i + 1}", fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 26.sp, color = Kasi.Chilli)
                        Text(step, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        TextButton(onClick = onStaff, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Staff login", color = Kasi.Muted)
        }
    }
}

@Composable
fun TruckPickerScreen(state: CustomerUiState, onPick: (String) -> Unit, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader("Choose your truck", onBack)
        Text("Your order goes to the truck you pick. Collect it there.", color = Kasi.Muted)
        if (state.trucks.isEmpty()) {
            Text("No trucks set up yet.", color = Kasi.Muted)
        }
        state.trucks.forEach { truck ->
            val selected = truck.id == state.selectedTruck?.id
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (selected) Kasi.SignSoft else Kasi.Card)
                    .border(if (selected) 2.dp else 1.5.dp, if (selected) Kasi.Ink else Kasi.Line, RoundedCornerShape(18.dp))
                    .clickable(enabled = truck.open, role = Role.RadioButton) { onPick(truck.id) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(Kasi.Ink),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(truck.name.filter { it.isDigit() }.ifEmpty { "S" }, color = Kasi.Sign, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 22.sp)
                }
                Column(Modifier.weight(1f)) {
                    Text(truck.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(truck.spot, color = Kasi.Muted, fontSize = 13.sp)
                }
                if (truck.open) Badge("Open", Kasi.GreenSoft, Kasi.GreenDark) else Badge("Closed", Kasi.Divider, Kasi.Muted)
            }
        }
        SectionLabel("Closed trucks can't take orders")
    }
}

@Composable
fun ThanksScreen(onHome: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(Kasi.Sign), contentAlignment = Alignment.Center) {
            Text("✓", fontSize = 40.sp, fontWeight = FontWeight.Black)
        }
        Text("SHARP, THANK YOU!", style = MaterialTheme.typography.headlineMedium)
        Text("Your feedback is with the team. See you at the truck.", color = Kasi.Muted)
        PillButton("Back home", onHome, container = Kasi.Ink)
    }
}
