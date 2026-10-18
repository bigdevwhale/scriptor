package app.scriptor.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.scriptor.R

/**
 * A scriptorium desk: parchment and vellum, iron-gall ink that browns as it fades, cinnabar for
 * rubrics, lapis for the initials, verdigris for vines, gold leaf and red sealing wax.
 */
@Immutable
data class Palette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val faint: Color,
    val cinnabar: Color,
    val cinnabarSoft: Color,
    val lapis: Color,
    val lapisSoft: Color,
    val gold: Color,
    val goldDeep: Color,
    val goldSoft: Color,
    val verdigris: Color,
    val verdigrisSoft: Color,
    val leather: Color,
    val wax: Color,
    val vellum: Color,
    val night: Color,
    val candle: Color,
    val danger: Color,
)

val Parchment = Palette(
    bg = Color(0xFFEFE4CC),
    surface = Color(0xFFF7EFDC),
    surface2 = Color(0xFFE6D7B6),
    line = Color(0xFFD8C59F),
    ink = Color(0xFF2A1F17),
    muted = Color(0xFF6D5B45),
    faint = Color(0xFFA48F70),
    cinnabar = Color(0xFFB0372A),
    cinnabarSoft = Color(0xFFF1D3C3),
    lapis = Color(0xFF2B4785),
    lapisSoft = Color(0xFFD7DDEC),
    gold = Color(0xFFCDA33E),
    goldDeep = Color(0xFF9C7623),
    goldSoft = Color(0xFFF3E3AE),
    verdigris = Color(0xFF3E7A5E),
    verdigrisSoft = Color(0xFFD4E4D6),
    leather = Color(0xFF6B3A22),
    wax = Color(0xFF9B2320),
    vellum = Color(0xFFFBF6E9),
    night = Color(0xFF231A14),
    candle = Color(0xFFF2B640),
    danger = Color(0xFFB0372A),
)

val LocalPalette = staticCompositionLocalOf { Parchment }

/** The book hand: everything written, and the titles. */
val Garamond = FontFamily(
    Font(R.font.garamond_regular, FontWeight.Normal),
    Font(R.font.garamond_medium, FontWeight.Medium),
    Font(R.font.garamond_semibold, FontWeight.SemiBold),
    Font(R.font.garamond_italic, FontWeight.Normal, FontStyle.Italic),
)

/** The plain hand of the desk: buttons, counts, small labels. */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
)

/** A quick hand for what is scribbled on the margin. */
val Hand = FontFamily(Font(R.font.caveat, FontWeight.Normal))

object U {
    val c: Palette @Composable get() = LocalPalette.current

    val RXl = RoundedCornerShape(24.dp)
    val RLg = RoundedCornerShape(18.dp)
    val RMd = RoundedCornerShape(12.dp)
    val RSm = RoundedCornerShape(8.dp)
}

object UType {
    val Display: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.Medium, fontSize = 32.sp, lineHeight = 37.sp, color = U.c.ink)
    val Title: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.Medium, fontSize = 25.sp, lineHeight = 30.sp, color = U.c.ink)
    val Name: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 23.sp, color = U.c.ink)
    /** The page itself. */
    val Script: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontSize = 19.sp, lineHeight = 27.sp, color = U.c.ink)
    val Italic: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontStyle = FontStyle.Italic, fontSize = 17.sp, lineHeight = 23.sp, color = U.c.muted)
    val Rubric: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontStyle = FontStyle.Italic, fontSize = 16.sp, lineHeight = 21.sp, color = U.c.cinnabar)
    val Sub: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontSize = 14.5.sp, lineHeight = 21.sp, color = U.c.muted)
    val Eyebrow: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = 0.16.em, color = U.c.cinnabar)
    val Strong: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = U.c.ink)
    val Body: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontSize = 15.sp, lineHeight = 22.sp, color = U.c.ink)
    val Muted: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontSize = 13.sp, lineHeight = 18.sp, color = U.c.muted)
    val Small: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = U.c.faint)
    val Number: TextStyle @Composable get() = TextStyle(fontFamily = Garamond, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = U.c.ink)
    val Hand: TextStyle @Composable get() = TextStyle(fontFamily = app.scriptor.ui.theme.Hand, fontSize = 20.sp, lineHeight = 23.sp, color = U.c.cinnabar)
    val Button: TextStyle @Composable get() = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp)
}

@Composable
fun ScriptorTheme(content: @Composable () -> Unit) {
    val p = Parchment
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = p.lapis,
                onPrimary = p.vellum,
                primaryContainer = p.lapisSoft,
                onPrimaryContainer = p.ink,
                secondary = p.cinnabar,
                secondaryContainer = p.cinnabarSoft,
                onSecondaryContainer = p.ink,
                background = p.bg,
                surface = p.surface,
                surfaceContainer = p.surface,
                surfaceContainerHigh = p.surface,
                surfaceContainerHighest = p.surface2,
                surfaceContainerLow = p.surface2,
                onBackground = p.ink,
                onSurface = p.ink,
                onSurfaceVariant = p.muted,
                outline = p.line,
                outlineVariant = p.line,
                error = p.danger,
            ),
            typography = MaterialTheme.typography.copy(
                bodyLarge = UType.Body,
                bodyMedium = UType.Body.copy(fontSize = 14.sp),
                labelLarge = UType.Button,
            ),
            content = content,
        )
    }
}
