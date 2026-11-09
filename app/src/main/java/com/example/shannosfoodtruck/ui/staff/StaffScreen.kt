package com.example.shannosfoodtruck.ui.staff

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.shannosfoodtruck.data.Fulfilment
import com.example.shannosfoodtruck.data.Order
import com.example.shannosfoodtruck.data.OrderStage
import com.example.shannosfoodtruck.data.PickupTimes
import com.example.shannosfoodtruck.data.PurchasePlanner
import com.example.shannosfoodtruck.data.Recipes
import com.example.shannosfoodtruck.data.toQty
import com.example.shannosfoodtruck.data.StockStatus
import com.example.shannosfoodtruck.data.toRand
import com.example.shannosfoodtruck.ui.components.BackHeader
import com.example.shannosfoodtruck.ui.components.Badge
import com.example.shannosfoodtruck.ui.components.BrandMark
import com.example.shannosfoodtruck.ui.auth.rememberGoogleSignIn
import com.example.shannosfoodtruck.ui.components.FoodTile
import com.example.shannosfoodtruck.ui.components.GoogleButton
import com.example.shannosfoodtruck.ui.components.KasiTextField
import com.example.shannosfoodtruck.ui.components.Panel
import com.example.shannosfoodtruck.ui.components.PillButton
import com.example.shannosfoodtruck.ui.components.ScreenTitle
import com.example.shannosfoodtruck.ui.components.SectionLabel
import com.example.shannosfoodtruck.ui.theme.DisplayFont
import com.example.shannosfoodtruck.ui.theme.Kasi

@Composable
fun StaffScreen(vm: StaffViewModel, onExit: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    when (state.signedIn) {
        null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Kasi.Ink) }
        false -> {
            val googleSignIn = rememberGoogleSignIn(onToken = vm::signInWithGoogle, onError = vm::showError)
            StaffLogin(state, onSignIn = vm::signIn, onGoogle = googleSignIn, onBack = onExit)
        }
        true -> StaffDashboard(state, vm, onExit)
    }
}

@Composable
private fun StaffLogin(state: StaffUiState, onSignIn: (String, String) -> Unit, onGoogle: () -> Unit, onBack: () -> Unit) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader("Staff login", onBack)
        Text("For the truck team only. Customers don't need an account to order.", color = Kasi.Muted)
        GoogleButton(onGoogle, Modifier.fillMaxWidth(), loading = state.signingIn)
        SectionLabel("or use your staff email")
        KasiTextField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        state.error?.let { Text(it, color = Kasi.Chilli) }
        PillButton("Sign in", { onSignIn(email, password) }, Modifier.fillMaxWidth(), container = Kasi.Ink, loading = state.signingIn)
    }
}

@Composable
private fun StaffDashboard(state: StaffUiState, vm: StaffViewModel, onExit: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            BrandMark("Truck owner")
            Row {
                TextButton(onClick = { vm.signOut(); onExit() }) { Text("Sign out", color = Kasi.Muted) }
                TextButton(onClick = onExit) { Text("Close", color = Kasi.Ink, fontWeight = FontWeight.Bold) }
            }
        }

        // Section tabs
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StaffSection.entries.forEach { s ->
                Toggle(s.label, state.section == s, dark = true) { vm.selectSection(s) }
            }
        }

        // Truck switcher
        if (state.trucks.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.trucks.forEach { t -> Toggle(t.name, state.truckId == t.id, dark = false) { vm.selectTruck(t.id) } }
            }
        }

        state.error?.let {
            Panel(padding = 14.dp) {
                Text(it, color = Kasi.Chilli)
                TextButton(onClick = vm::dismissError) { Text("OK") }
            }
        }

        when (state.section) {
            StaffSection.ORDERS -> OrdersBoard(state, vm::advance)
            StaffSection.REPORT -> ReportSection(state, vm::selectBasis)
            StaffSection.MENU -> MenuManager(state, vm)
            StaffSection.STOCK -> StockList(state)
        }
    }
}

@Composable
private fun Toggle(label: String, selected: Boolean, dark: Boolean, onClick: () -> Unit) {
    val bg = when {
        selected && dark -> Kasi.Sign
        selected -> Kasi.Ink
        else -> Kasi.Card
    }
    val fg = if (selected && !dark) Color.White else Kasi.Ink
    Box(
        Modifier
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.5.dp, if (selected) Color.Transparent else Kasi.Line, CircleShape)
            .clickable(role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

// ---------- Orders ----------

@Composable
private fun OrdersBoard(state: StaffUiState, onAdvance: (Order) -> Unit) {
    val columns = listOf(
        Triple(OrderStage.RECEIVED, "New", Kasi.Ink to Color.White),
        Triple(OrderStage.COOKING, "On the pan", Kasi.Sign to Kasi.Ink),
        Triple(OrderStage.READY, "Ready to collect", Kasi.Green to Color.White),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        columns.forEach { (stage, label, colors) ->
            val count = state.orders.count { it.stage == stage }
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Kasi.Card)
                    .padding(12.dp),
            ) {
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Kasi.Muted)
                Text("$count", fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 30.sp, color = if (stage == OrderStage.COOKING) Kasi.Ink else colors.first)
            }
        }
    }

    columns.forEach { (stage, label, colors) ->
        val list = state.orders.filter { it.stage == stage }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Kasi.Panel)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label.uppercase(), style = MaterialTheme.typography.titleLarge)
                Badge("${list.size}", colors.first, colors.second)
            }
            if (list.isEmpty()) Text("Nothing here", color = Kasi.Muted, fontSize = 14.sp)
            list.forEach { order -> OrderTicket(order, colors, onAdvance) }
        }
    }
}

@Composable
private fun OrderTicket(order: Order, colors: Pair<Color, Color>, onAdvance: (Order) -> Unit) {
    Panel(padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(order.number, fontWeight = FontWeight.Bold)
            Text(
                (if (order.fulfilment == Fulfilment.PICKUP) "Collect" else "Delivery") + " · " +
                    (if (order.pickupTime == PickupTimes.ASAP) "ASAP" else order.pickupTime),
                color = if (order.pickupTime == PickupTimes.ASAP) Kasi.Muted else Kasi.Chilli,
                fontSize = 13.sp, fontWeight = FontWeight.Bold,
            )
        }
        Text("${order.customerName} · ${order.phone}", fontSize = 14.sp)
        Text(order.lines.joinToString(", ") { if (it.quantity > 1) "${it.quantity} × ${it.name}" else it.name }, color = Kasi.Muted, fontSize = 14.sp)
        if (order.note.isNotBlank()) Text("Note: ${order.note}", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        if (order.fulfilment == Fulfilment.DELIVERY) {
            Text("Deliver to: ${order.address} (${order.deliveryArea?.label ?: ""}, fee ${order.deliveryFee.toRand()})", fontSize = 13.sp)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(order.total.toRand(), fontWeight = FontWeight.Bold)
                Text(order.payment.label, color = Kasi.Muted, fontSize = 12.sp)
            }
            order.stage.staffAction?.let { action ->
                PillButton(action, { onAdvance(order) }, container = colors.first, content = colors.second)
            }
        }
    }
}

// ---------- Menu ----------

@Composable
private fun MenuManager(state: StaffUiState, vm: StaffViewModel) {
    if (state.menu.isEmpty()) {
        Panel {
            Text("No menu yet", fontWeight = FontWeight.Bold)
            Text("Load the starter menu, trucks and stock list. Existing data is never overwritten.", color = Kasi.Muted)
            PillButton("Load starter menu", vm::loadStarterData, Modifier.padding(top = 12.dp), container = Kasi.Ink, loading = state.busy)
        }
        return
    }
    SectionLabel("Tap to mark sold out. Customers see it instantly.")
    Panel(padding = 12.dp) {
        state.menu.forEachIndexed { i, item ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FoodTile(item, 48.dp, fontSize = 13)
                Column(Modifier.weight(1f)) {
                    Text(item.name, fontWeight = FontWeight.Bold)
                    Text("${item.category.label} · ${item.price.toRand()}", color = Kasi.Muted, fontSize = 13.sp)
                }
                Box(
                    Modifier
                        .heightIn(min = 44.dp)
                        .width(112.dp)
                        .clip(CircleShape)
                        .background(if (item.available) Kasi.GreenSoft else Kasi.ChilliSoft)
                        .border(1.5.dp, if (item.available) Kasi.Green else Kasi.Chilli, CircleShape)
                        .clickable(role = Role.Switch) { vm.toggleAvailable(item) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (item.available) "Available" else "Sold out",
                        color = if (item.available) Kasi.GreenDark else Kasi.ChilliDark,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    )
                }
            }
            if (i < state.menu.lastIndex) HorizontalDivider(color = Kasi.Divider)
        }
    }
}

// ---------- Stock ----------

@Composable
private fun StockList(state: StaffUiState) {
    ScreenTitle("Stock")
    if (state.stock.isEmpty()) {
        Text("No stock list for this truck yet. Use \"Load starter menu\" under Menu.", color = Kasi.Muted)
        return
    }
    Panel(padding = 12.dp) {
        state.stock.forEachIndexed { i, s ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(s.name, fontWeight = FontWeight.Bold)
                    Text("On hand ${s.onHand.toQty()} ${s.unit} · reorder at ${s.reorderAt.toQty()} ${s.unit}", color = Kasi.Muted, fontSize = 13.sp)
                }
                when (s.status) {
                    StockStatus.OUT -> Badge("Out", Kasi.ChilliSoft, Kasi.ChilliDark)
                    StockStatus.LOW -> Badge("Low", Kasi.AmberSoft, Kasi.AmberDark)
                    StockStatus.OK -> Badge("OK", Kasi.GreenSoft, Kasi.GreenDark)
                }
            }
            if (i < state.stock.lastIndex) HorizontalDivider(color = Kasi.Divider)
        }
    }
    Text("See Sales & buying for what to buy, worked out from orders.", color = Kasi.Muted, fontSize = 13.sp)
}

// ---------- Sales & buying (Task 1: FR-10 Daily Purchase Recommendation, FR-12 sales) ----------

@Composable
private fun ReportSection(state: StaffUiState, onBasis: (ReportBasis) -> Unit) {
    val sales = state.sales
    val plan = state.purchasePlan
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ReportBasis.entries.forEach { b -> Toggle(b.label, state.reportBasis == b, dark = false) { onBasis(b) } }
    }

    // Sales
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Orders", "${sales.orderCount}", Modifier.weight(1f))
        StatCard("Sales", sales.revenue.toRand(), Modifier.weight(1.4f), highlight = true)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatCard("Items sold", "${sales.itemsSold}", Modifier.weight(1f))
        StatCard("Average order", sales.averageOrder.toRand(), Modifier.weight(1.4f))
    }
    if (state.reportBasis == ReportBasis.WEEK) {
        Text("Sales above are for the whole 7 days. The shopping list below is for one typical day.", color = Kasi.Muted, fontSize = 13.sp)
    }
    if (sales.topSellers.isNotEmpty()) {
        Panel(padding = 14.dp) {
            SectionLabel("Top sellers")
            sales.topSellers.forEachIndexed { i, (name, qty) ->
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${i + 1}. $name")
                    Text("$qty sold", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Daily Purchase Recommendation
    Text("WHAT TO BUY", style = MaterialTheme.typography.titleLarge)
    if (plan.isEmpty()) {
        Text("No orders in this period yet, so there's nothing to recommend.", color = Kasi.Muted)
        return
    }
    val toBuy = plan.filter { it.packsToBuy > 0 }
    Panel(padding = 12.dp) {
        plan.forEachIndexed { i, line ->
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(line.ingredient.name, fontWeight = FontWeight.Bold)
                    Text(
                        "Need ${line.needed.toQty()} ${line.ingredient.unit} · have ${line.onHand.toQty()}",
                        color = Kasi.Muted, fontSize = 13.sp,
                    )
                    if (line.packsToBuy > 0) Text(line.ingredient.pack, color = Kasi.Faint, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (line.packsToBuy == 0) Badge("Enough", Kasi.GreenSoft, Kasi.GreenDark)
                    else {
                        Text("Buy ${line.packsToBuy}", fontWeight = FontWeight.Bold)
                        Text(line.estimatedCost?.toRand() ?: "check price", color = Kasi.Muted, fontSize = 13.sp)
                    }
                }
            }
            if (i < plan.lastIndex) HorizontalDivider(color = Kasi.Divider)
        }
    }
    Panel(padding = 16.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Estimated shopping cost", fontWeight = FontWeight.Bold)
            Text(PurchasePlanner.totalCost(toBuy).toRand(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Text(
            "${toBuy.size} item(s) to buy. ${Recipes.PRICE_SOURCE} Portions are example recipes; adjust them in Recipes.kt.",
            color = Kasi.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (highlight) Kasi.Sign else Kasi.Card)
            .padding(14.dp),
    ) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = if (highlight) Kasi.Ink else Kasi.Muted)
        Text(value, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 26.sp)
    }
}
