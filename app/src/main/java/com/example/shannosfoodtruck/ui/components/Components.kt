package com.example.shannosfoodtruck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shannosfoodtruck.data.MenuItem
import com.example.shannosfoodtruck.ui.theme.DisplayFont
import com.example.shannosfoodtruck.ui.theme.Kasi

/** Coloured square with a short word on it ("KOTA"). Stands in for a food photo. */
@Composable
fun FoodTile(item: MenuItem, size: Dp, modifier: Modifier = Modifier, fontSize: Int = 18) {
    val (bg, fg) = Kasi.tile(item.category)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(bg),
        contentAlignment = Alignment.Center,
    ) {
        Text(item.tag, color = fg, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = fontSize.sp, textAlign = TextAlign.Center)
    }
}

/** Big pill button used for the main action on a screen. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = Kasi.Chilli,
    content: Color = Color.White,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.heightIn(min = 54.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = content, strokeWidth = 2.5.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
    }
}

/** Round black "+" button. */
@Composable
fun AddButton(itemName: String, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (enabled) Kasi.Ink else Kasi.Line),
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add $itemName", tint = Color.White)
    }
}

/** − 2 + control. */
@Composable
fun QtyStepper(quantity: Int, itemName: String, onMinus: () -> Unit, onPlus: () -> Unit, filled: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .then(if (filled) Modifier.background(Kasi.Sign) else Modifier.border(1.5.dp, Kasi.Line, CircleShape)),
    ) {
        StepperButton("−", "One less $itemName", onMinus)
        Text("$quantity", fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
        StepperButton("+", "One more $itemName", onPlus)
    }
}

@Composable
private fun StepperButton(symbol: String, description: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
    ) { Text(symbol, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Kasi.Ink) }
}

/** Big tappable option card, e.g. "Collect at truck" vs "Deliver to me". */
@Composable
fun ChoiceCard(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .heightIn(min = 60.dp)
            .clip(shape)
            .background(if (selected) Kasi.SignSoft else Kasi.Card)
            .border(if (selected) BorderStroke(2.dp, Kasi.Ink) else BorderStroke(1.5.dp, Kasi.Line), shape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(12.dp),
    ) { Text(text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = MaterialTheme.typography.headlineMedium, modifier = modifier)
}

@Composable
fun SectionLabel(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = Kasi.Muted)
}

@Composable
fun BackHeader(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Kasi.Card)
                .border(1.5.dp, Kasi.Line, CircleShape),
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
        ScreenTitle(title)
    }
}

@Composable
fun KasiTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardType: KeyboardType = KeyboardType.Text,
    error: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    Column(modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
            singleLine = singleLine,
            minLines = minLines,
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Kasi.Card,
                unfocusedContainerColor = Kasi.Card,
                focusedBorderColor = Kasi.Ink,
                unfocusedBorderColor = Kasi.Line,
                focusedLabelColor = Kasi.Ink,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** White rounded panel. */
@Composable
fun Panel(modifier: Modifier = Modifier, padding: Dp = 16.dp, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Kasi.Card)
            .padding(padding),
    ) { content() }
}

/** Small status badge ("Open", "Low", ...). */
@Composable
fun Badge(text: String, background: Color, content: Color) {
    Text(
        text,
        color = content,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun BrandMark(subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Kasi.Sign),
        ) { Text("S", fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 22.sp, color = Kasi.Ink) }
        Column {
            Text("SHANNOS", fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 19.sp, letterSpacing = 0.5.sp)
            Text(subtitle, fontSize = 12.sp, color = Kasi.Muted)
        }
    }
}

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.heightIn(min = height))
