package com.example.shannosfoodtruck.ui.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shannosfoodtruck.BuildConfig
import com.example.shannosfoodtruck.data.Order
import com.example.shannosfoodtruck.data.OrderStage
import com.example.shannosfoodtruck.data.toRand
import com.example.shannosfoodtruck.ui.auth.clearGoogleCredentialState
import com.example.shannosfoodtruck.ui.auth.rememberGoogleSignIn
import com.example.shannosfoodtruck.ui.components.Badge
import com.example.shannosfoodtruck.ui.components.GoogleButton
import com.example.shannosfoodtruck.ui.components.Panel
import com.example.shannosfoodtruck.ui.components.ScreenTitle
import com.example.shannosfoodtruck.ui.components.SectionLabel
import com.example.shannosfoodtruck.ui.theme.DisplayFont
import com.example.shannosfoodtruck.ui.theme.Kasi
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Account tab: Google single sign-on, the signed-in profile, order history and staff login.
 * Signing in is optional: guests can still order. Signing in keeps your details and
 * your orders on any phone, and on the Shannos website with the same Google account.
 */
@Composable
fun AccountScreen(vm: CustomerViewModel, state: CustomerUiState, onStaff: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSignIn = rememberGoogleSignIn(onToken = vm::signInWithGoogle, onError = vm::showMessage)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your account?") },
            text = { Text("This permanently deletes your Shannos login. Your past orders stay with the truck but are no longer linked to you. This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteAccount { scope.launch { clearGoogleCredentialState(context) } }
                }) { Text("Delete", color = Kasi.Chilli, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep my account", color = Kasi.Ink) } },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        ScreenTitle("Account")

        val user = state.user
        if (user == null) {
            Panel(padding = 20.dp) {
                Text("Order faster next time", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Text(
                    "Sign in to save your details and see your orders on any phone, and on our website.",
                    color = Kasi.Muted,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
                GoogleButton(googleSignIn, Modifier.fillMaxWidth(), loading = state.signingIn)
                Text(
                    "No account needed to order. You can always continue as a guest.",
                    color = Kasi.Muted, fontSize = 13.sp,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        } else {
            Panel(padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape).background(Kasi.Sign),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (user.name ?: user.email ?: "?").take(1).uppercase(),
                            fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 26.sp,
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(user.name ?: "Signed in", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        user.email?.let { Text(it, color = Kasi.Muted, fontSize = 14.sp) }
                        Text("Signed in with Google", color = Kasi.Green, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = {
                        vm.signOut()
                        scope.launch { clearGoogleCredentialState(context) }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                ) { Text("Sign out", color = Kasi.Ink) }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete my account", color = Kasi.Chilli)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionLabel("My orders")
                if (state.myOrders.isEmpty()) {
                    Text("No orders yet. Your next order will show up here.", color = Kasi.Muted)
                } else {
                    Panel(padding = 14.dp) {
                        state.myOrders.forEachIndexed { i, order ->
                            OrderHistoryRow(order)
                            if (i < state.myOrders.lastIndex) HorizontalDivider(color = Kasi.Divider)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionLabel("Work at the truck?")
            TextButton(onClick = onStaff) { Text("Staff login", color = Kasi.Ink, fontWeight = FontWeight.Bold) }
        }

        Text(
            "Shannos Food Truck v${BuildConfig.VERSION_NAME}" + if (BuildConfig.USE_FIREBASE) "" else " · demo data",
            color = Kasi.Faint, fontSize = 12.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun OrderHistoryRow(order: Order) {
    val date = remember(order.createdAt) { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(order.createdAt)) }
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("${order.number} · ${order.total.toRand()}", fontWeight = FontWeight.Bold)
            Text("$date · ${order.lines.joinToString(", ") { it.name }}", color = Kasi.Muted, fontSize = 13.sp, maxLines = 1)
        }
        if (order.stage == OrderStage.COLLECTED) Badge("Collected", Kasi.Divider, Kasi.Muted)
        else Badge(order.stage.label, Kasi.AmberSoft, Kasi.AmberDark)
    }
}
