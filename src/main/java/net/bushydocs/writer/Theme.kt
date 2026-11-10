package net.bushydocs.writer

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Palette — sampled from the live Bushy Writer site (emerald brand + zinc).
// ---------------------------------------------------------------------------
val Brand50 = Color(0xFFECFDF5)
val Brand100 = Color(0xFFD1FAE5)
val Brand400 = Color(0xFF34D399)
val Brand500 = Color(0xFF10B981)
val Brand600 = Color(0xFF059669)
val Brand700 = Color(0xFF047857)

val Zinc50 = Color(0xFFFAFAFA)
val Zinc100 = Color(0xFFF4F4F5)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc500 = Color(0xFF71717A)
val Zinc600 = Color(0xFF52525B)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)
val Zinc900 = Color(0xFF18181B)
val Zinc950 = Color(0xFF09090B)

val Rose500 = Color(0xFFF43F5E)
val Rose600 = Color(0xFFE11D48)
val Emerald400 = Color(0xFF34D399)
val Emerald950 = Color(0xFF022C22)
val Blue500 = Color(0xFF3B82F6)

// ---------------------------------------------------------------------------
// Type families bundled with the app so it stays completely offline.
// ---------------------------------------------------------------------------
val Jakarta = FontFamily(
    Font(R.font.plus_jakarta_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_bold, FontWeight.Bold),
)

val SourceSerif = FontFamily(
    Font(R.font.source_serif_regular, FontWeight.Normal),
    Font(R.font.source_serif_semibold, FontWeight.SemiBold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
    labelMedium = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontFamily = Jakarta, fontWeight = FontWeight.Medium, fontSize = 10.sp),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(10.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

@Composable
fun BushyTheme(dark: Boolean, content: @Composable () -> Unit) {
    val scheme = if (dark) {
        darkColorScheme(
            primary = Brand500,
            onPrimary = Color.White,
            primaryContainer = Emerald950,
            onPrimaryContainer = Brand100,
            secondary = Brand400,
            background = Zinc950,
            onBackground = Zinc100,
            surface = Zinc900,
            onSurface = Zinc100,
            surfaceVariant = Zinc800,
            onSurfaceVariant = Zinc400,
            outline = Zinc700,
            error = Rose500,
        )
    } else {
        lightColorScheme(
            primary = Brand600,
            onPrimary = Color.White,
            primaryContainer = Brand50,
            onPrimaryContainer = Brand700,
            secondary = Brand500,
            background = Zinc50,
            onBackground = Zinc800,
            surface = Color.White,
            onSurface = Zinc800,
            surfaceVariant = Zinc100,
            onSurfaceVariant = Zinc500,
            outline = Zinc200,
            error = Rose600,
        )
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
