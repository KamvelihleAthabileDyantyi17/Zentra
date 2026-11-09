package com.example.shannosfoodtruck.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.shannosfoodtruck.data.Category

/** Kasi street-signage palette — same values as the design prototype. */
object Kasi {
    val Ink = Color(0xFF1A1612)
    val Paper = Color(0xFFF4EFE6)
    val Card = Color(0xFFFFFFFF)
    val Panel = Color(0xFFEAE3D7)
    val Sign = Color(0xFFFFC531)        // sign yellow
    val SignSoft = Color(0xFFFFF4D1)
    val Chilli = Color(0xFFC73A1D)      // main call-to-action red
    val ChilliSoft = Color(0xFFFBE3DD)
    val ChilliDark = Color(0xFF8E2410)
    val Green = Color(0xFF1F7A4D)
    val GreenSoft = Color(0xFFDDF0E5)
    val GreenDark = Color(0xFF145233)
    val Rolls = Color(0xFF1F6B45)
    val Blue = Color(0xFF1D4E89)
    val AmberSoft = Color(0xFFFFF0C2)
    val AmberDark = Color(0xFF6B4A00)
    val Muted = Color(0xFF5E554B)
    val Faint = Color(0xFF8A7F72)
    val Line = Color(0xFFD9D0C2)
    val Divider = Color(0xFFEEE7DB)

    /** Background and text colour for each category's food tile. */
    fun tile(category: Category): Pair<Color, Color> = when (category) {
        Category.KOTAS -> Sign to Ink
        Category.SIDES -> Chilli to Color.White
        Category.ROLLS -> Rolls to Color.White
        Category.DRINKS -> Blue to Color.White
    }
}

/**
 * Headline font. The prototype uses "Anton" (a tall, condensed signage face).
 * To match it exactly: download Anton-Regular.ttf from Google Fonts, save it as
 * app/src/main/res/font/anton_regular.ttf, then change this line to
 *   val DisplayFont = FontFamily(Font(R.font.anton_regular))
 */
val DisplayFont: FontFamily = FontFamily.SansSerif

private val AppTypography = Typography(
    displaySmall = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = 0.5.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 30.sp, lineHeight = 32.sp, letterSpacing = 0.5.sp),
    titleLarge = TextStyle(fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = 0.5.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp),
)

private val AppColors = lightColorScheme(
    primary = Kasi.Ink,
    onPrimary = Color.White,
    secondary = Kasi.Sign,
    onSecondary = Kasi.Ink,
    tertiary = Kasi.Chilli,
    onTertiary = Color.White,
    background = Kasi.Paper,
    onBackground = Kasi.Ink,
    surface = Kasi.Card,
    onSurface = Kasi.Ink,
    surfaceVariant = Kasi.Panel,
    onSurfaceVariant = Kasi.Muted,
    outline = Kasi.Line,
    error = Kasi.Chilli,
)

@Composable
fun ShannosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
