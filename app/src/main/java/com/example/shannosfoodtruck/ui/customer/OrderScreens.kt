package com.example.shannosfoodtruck.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shannosfoodtruck.data.Category
import com.example.shannosfoodtruck.data.Fulfilment
import com.example.shannosfoodtruck.data.OrderStage
import com.example.shannosfoodtruck.data.PaymentMethod
import com.example.shannosfoodtruck.data.toRand
import com.example.shannosfoodtruck.ui.components.AddButton
import com.example.shannosfoodtruck.ui.components.BackHeader
import com.example.shannosfoodtruck.ui.components.Badge
import com.example.shannosfoodtruck.ui.components.ChoiceCard
import com.example.shannosfoodtruck.ui.components.FoodTile
import com.example.shannosfoodtruck.ui.components.KasiTextField
import com.example.shannosfoodtruck.ui.components.Panel
import com.example.shannosfoodtruck.ui.components.PillButton
import com.example.shannosfoodtruck.ui.components.QtyStepper
import com.example.shannosfoodtruck.ui.components.ScreenTitle
import com.example.shannosfoodtruck.ui.components.SectionLabel
import com.example.shannosfoodtruck.ui.theme.DisplayFont
import com.example.shannosfoodtruck.ui.theme.Kasi

// ======================= MENU =======================

@Composable
fun MenuScreen(
    state: CustomerUiState,
    onCategory: (Category?) -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ScreenTitle("The menu")
                Text("Collecting from ${state.selectedTruck?.name ?: "—"}", color = Kasi.Muted, fontSize = 13.sp)
            }
        }
        item {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CategoryChip("All", state.category == null) { onCategory(null) }
                Category.entries.forEach { c -> CategoryChip(c.label, state.category == c) { onCategory(c) } }
            }
        }
        when {
            state.loadingMenu -> item {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kasi.Ink) }
            }
            state.menu.isEmpty() -> item {
                Panel {
                    Text("The menu is empty", fontWeight = FontWeight.Bold)
                    Text("Staff: open Staff login on the Home screen and tap \"Load starter menu\".", color = Kasi.Muted)
                }
            }
        }
        items(state.visibleMenu, key = { it.id }) { item ->
            val qty = state.cart.quantityOf(item.id)
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Kasi.Card)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FoodTile(item, 76.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (item.description.isNotBlank()) Text(item.description, color = Kasi.Muted, fontSize = 13.sp, lineHeight = 17.sp)
                    Text(item.price.toRand(), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                when {
                    !item.available -> Badge("Sold out", Kasi.ChilliSoft, Kasi.ChilliDark)
                    qty == 0 -> AddButton(item.name, { onAdd(item.id) })
                    else -> QtyStepper(qty, item.name, onMinus = { onRemove(item.id) }, onPlus = { onAdd(item.id) }, filled = true)
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(if (selected) Kasi.Ink else Kasi.Card)
            .border(1.5.dp, if (selected) Kasi.Ink else Kasi.Line, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = if (selected) Color.White else Kasi.Ink, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

// ======================= CART =======================

@Composable
fun CartScreen(
    state: CustomerUiState,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onBrowse: () -> Unit,
    onCheckout: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Your order")
        if (state.cart.isEmpty) {
            EmptyState("NOTHING HERE YET", "Your kota is waiting.", "Browse the menu", onBrowse)
            return@Column
        }
        Panel(padding = 16.dp) {
            state.cartLines.forEach { line ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(line.name, fontWeight = FontWeight.Bold)
                        Text("${line.unitPrice.toRand()} each", color = Kasi.Muted, fontSize = 13.sp)
                    }
                    QtyStepper(line.quantity, line.name, onMinus = { onRemove(line.itemId) }, onPlus = { onAdd(line.itemId) })
                    Text(line.lineTotal.toRand(), fontWeight = FontWeight.Bold, modifier = Modifier.width(68.dp), textAlign = TextAlign.End)
                }
                HorizontalDivider(color = Kasi.Divider)
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(state.cartTotal.toRand(), fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        }
        KasiTextField(
            label = "Note for the kitchen (optional)",
            value = state.form.note,
            onValueChange = onNoteChange,
            placeholder = "e.g. extra sauce, no cheese",
            singleLine = false,
            minLines = 2,
        )
        PillButton("Checkout · ${state.cartTotal.toRand()}", onCheckout, Modifier.fillMaxWidth())
    }
}

// ======================= CHECKOUT =======================

@Composable
fun CheckoutScreen(
    state: CustomerUiState,
    onFormChange: ((CheckoutForm) -> CheckoutForm) -> Unit,
    onBack: () -> Unit,
    onPlace: () -> Unit,
) {
    val form = state.form
    val errors = if (state.showFormErrors) form.errors() else emptyMap()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackHeader("Checkout", onBack)

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("How do you want it?")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Fulfilment.entries.forEach { f ->
                    ChoiceCard(f.label, form.fulfilment == f, { onFormChange { it.copy(fulfilment = f) } }, Modifier.weight(1f))
                }
            }
            if (form.fulfilment == Fulfilment.PICKUP) {
                val truck = state.selectedTruck
                Panel(padding = 14.dp) {
                    Text(if (truck != null) "Collect from ${truck.name}, ${truck.spot}" else "No truck is open right now", fontSize = 14.sp)
                }
            } else {
                KasiTextField("Delivery address", form.address, { v -> onFormChange { it.copy(address = v) } }, placeholder = "Street, area", error = errors[CheckoutFormFields.ADDRESS])
                Text("Delivery fee: [SET BY OWNER]", color = Kasi.Muted, fontSize = 13.sp)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Your details")
            KasiTextField("Name", form.name, { v -> onFormChange { it.copy(name = v) } }, placeholder = "So we can call your order", error = errors[CheckoutFormFields.NAME])
            KasiTextField("Cellphone number", form.phone, { v -> onFormChange { it.copy(phone = v) } }, placeholder = "082 000 0000", keyboardType = KeyboardType.Phone, error = errors[CheckoutFormFields.PHONE])
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionLabel("Pay with")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PaymentMethod.entries.forEach { p ->
                    ChoiceCard(p.label, form.payment == p, { onFormChange { it.copy(payment = p) } }, Modifier.weight(1f))
                }
            }
            if (form.payment == PaymentMethod.CARD) {
                Text("Card payment is a placeholder for now: the truck takes card at collection.", color = Kasi.Muted, fontSize = 13.sp)
            }
        }

        Panel(padding = 16.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${state.cartCount} ${if (state.cartCount == 1) "item" else "items"}", fontWeight = FontWeight.Bold)
                Text(state.cartTotal.toRand(), fontWeight = FontWeight.Bold)
            }
        }
        PillButton(
            "Place order",
            onPlace,
            Modifier.fillMaxWidth(),
            enabled = !state.cart.isEmpty && state.selectedTruck != null,
            loading = state.placing,
        )
    }
}

// ======================= TRACK =======================

@Composable
fun TrackScreen(state: CustomerUiState, onBrowse: () -> Unit, onRate: () -> Unit) {
    val order = state.activeOrder
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (order == null) {
            ScreenTitle("Track")
            if (state.activeOrderId != null) {
                Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kasi.Ink) }
            } else {
                EmptyState("NO ORDER YET", "Place an order and follow it here, live.", "Browse the menu", onBrowse)
            }
            return@Column
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Kasi.Ink)
                .padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("ORDER ${order.number}", color = Kasi.Sign, style = MaterialTheme.typography.labelMedium)
            Text(order.stage.label.uppercase(), color = Color.White, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 34.sp, lineHeight = 36.sp)
            Text(order.stage.hint, color = Kasi.Line, fontSize = 14.sp)
        }

        Panel(padding = 18.dp) {
            OrderStage.entries.forEach { stage ->
                val done = stage.ordinal < order.stage.ordinal
                val current = stage == order.stage
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        done -> Kasi.Green
                                        current -> Kasi.Sign
                                        else -> Kasi.Divider
                                    }
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${stage.ordinal + 1}",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                color = when {
                                    done -> Color.White
                                    current -> Kasi.Ink
                                    else -> Kasi.Muted
                                },
                            )
                        }
                        if (stage != OrderStage.COLLECTED) {
                            Box(Modifier.width(3.dp).height(28.dp).background(if (done) Kasi.Green else Kasi.Divider))
                        }
                    }
                    Text(
                        stage.label,
                        fontWeight = FontWeight.Bold,
                        color = if (done || current) Kasi.Ink else Kasi.Faint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        Panel(padding = 18.dp) {
            SectionLabel(if (order.fulfilment == Fulfilment.PICKUP) "Collect at truck · ${order.payment.label}" else "Delivery · ${order.payment.label}")
            order.lines.forEach { line ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${line.quantity} × ${line.name}", fontSize = 14.sp)
                    Text(line.lineTotal.toRand(), fontSize = 14.sp)
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Kasi.Divider)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.Bold)
                Text(order.total.toRand(), fontWeight = FontWeight.Bold)
            }
        }

        if (order.stage == OrderStage.COLLECTED) {
            PillButton("Rate your food", onRate, Modifier.fillMaxWidth(), container = Kasi.Sign, content = Kasi.Ink)
        }
    }
}

// ======================= FEEDBACK =======================

@Composable
fun FeedbackScreen(onBack: () -> Unit, onSend: (rating: Int, comment: String) -> Unit) {
    var rating by rememberSaveable { mutableIntStateOf(0) }
    var comment by rememberSaveable { mutableStateOf("") }
    var sending by rememberSaveable { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        BackHeader("How was it?", onBack)
        Text("We read every one. Tell us what hit and what missed.", color = Kasi.Muted)
        Row {
            (1..5).forEach { n ->
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .clickable(role = Role.RadioButton) { rating = n }
                        .semantics { contentDescription = if (n == 1) "1 star" else "$n stars" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = if (n <= rating) Kasi.Sign else Kasi.Line, modifier = Modifier.size(40.dp))
                }
            }
        }
        KasiTextField("Anything to add?", comment, { comment = it }, placeholder = "The kota was…", singleLine = false, minLines = 4)
        PillButton(
            "Send feedback",
            { sending = true; onSend(rating, comment) },
            Modifier.fillMaxWidth(),
            container = Kasi.Ink,
            enabled = rating > 0,
            loading = sending,
        )
    }
}

// ======================= shared =======================

@Composable
private fun EmptyState(title: String, body: String, action: String, onAction: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Kasi.Card)
            .padding(horizontal = 20.dp, vertical = 44.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(body, color = Kasi.Muted, textAlign = TextAlign.Center)
        PillButton(action, onAction, container = Kasi.Ink)
    }
}
