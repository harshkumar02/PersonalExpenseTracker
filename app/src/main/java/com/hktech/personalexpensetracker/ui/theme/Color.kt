package com.hktech.personalexpensetracker.ui.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors (tweak as you like)
val BluePrimary = Color(0xFF2962FF)
val BlueOnPrimary = Color(0xFFFFFFFF)
val BlueContainer = Color(0xFFDEE5FF)
val BlueOnContainer = Color(0xFF001A43)

val Secondary = Color(0xFF455A64)
val OnSecondary = Color(0xFFFFFFFF)

val Tertiary = Color(0xFF00BFA6)
val OnTertiary = Color(0xFF00201A)

val Error = Color(0xFFB00020)
val OnError = Color(0xFFFFFFFF)

val Background = Color(0xFFF7F9FC)
val OnBackground = Color(0xFF101316)

val Surface = Color(0xFFFFFFFF)
val OnSurface = Color(0xFF101316)

// Direction colors
val CreditGreen = Color(0xFFE8F5E9)
val CreditGreenText = Color(0xFF2E7D32)
val DebitRed = Color(0xFFFFEBEE)
val DebitRedText = Color(0xFFC62828)

// Category chip colors
val CategoryColors: Map<String, Color> = mapOf(
    "Food" to Color(0xFFFFB74D),
    "Groceries" to Color(0xFF81C784),
    "Transport" to Color(0xFF64B5F6),
    "Shopping" to Color(0xFFBA68C8),
    "Utilities" to Color(0xFF4DD0E1),
    "Fuel" to Color(0xFFFF8A65),
    "Rent" to Color(0xFFA1887F),
    "Education" to Color(0xFF7986CB),
    "Transfers" to Color(0xFF90A4AE),
    "Income" to Color(0xFF81C784),
    "Wallet" to Color(0xFFFFD54F),
    "UPI" to Color(0xFF9575CD),
    "Uncategorized" to Color(0xFFBDBDBD)
).withDefault { Color(0xFFBDBDBD) }

fun safeCategoryColor(category: String?): Color {
    return category?.let { CategoryColors[it] } ?: Color(0xFFBDBDBD)
}

fun safeParseColor(colorString: String?): Color {
    if (colorString.isNullOrBlank()) return Color(0xFFFF5722)
    return try {
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color(0xFFFF5722)
    }
}
