package vn.petpomodoro.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Memphis Maximalism palette (adapt từ demo KB1 `kich-ban-1-*.html` — KHÔNG copy web code):
 * nền kem, viền mực dày, bóng đổ lệch cứng, màu đậm chặn nhau.
 */
object MemphisColors {
    val Cream = Color(0xFFF2E9D8)
    val Cream2 = Color(0xFFE7DCC3)
    val Paper = Color(0xFFFBF5E9)
    val PaperWhite = Color(0xFFFFFDF6)
    val Ink = Color(0xFF26201D)

    val Red = Color(0xFFBC3A28)
    val Red2 = Color(0xFF9A2F20)
    val Mustard = Color(0xFFE3A72F)
    val Mustard2 = Color(0xFFC98F1D)
    val Blue = Color(0xFF2B54A3)
    val Blue2 = Color(0xFF1F3F7E)
    val Purple = Color(0xFF6B4C9A)
    val Purple2 = Color(0xFF553A7E)
    val Olive = Color(0xFF5F6B2F)
    val Olive2 = Color(0xFF4C5624)
    val Pink = Color(0xFFE89BB0)

    // Thanh TT kiểu Pokémon (spec §2.5): xanh lá → vàng → đỏ
    val HpGreen = Color(0xFF4CA83D)
    val HpYellow = Color(0xFFE8B830)
    val HpRed = Color(0xFFD84028)

    val CreamPicked = Color(0xFFFBF0D2)
    val CardMet = Color(0xFFEAF0DC)
}

private val MemphisScheme = androidx.compose.material3.lightColorScheme(
    primary = MemphisColors.Red,
    onPrimary = MemphisColors.Paper,
    secondary = MemphisColors.Mustard,
    onSecondary = MemphisColors.Ink,
    tertiary = MemphisColors.Blue,
    onTertiary = MemphisColors.Paper,
    background = MemphisColors.Cream,
    onBackground = MemphisColors.Ink,
    surface = MemphisColors.Paper,
    onSurface = MemphisColors.Ink,
    surfaceVariant = MemphisColors.Cream2,
    onSurfaceVariant = MemphisColors.Ink,
    error = MemphisColors.Red,
    onError = MemphisColors.Paper,
)

/** Chữ "Bungee-like": sans đậm, giãn chữ cho heading Memphis */
val BungeeStyle = TextStyle(
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Black,
    fontSize = 15.sp,
    letterSpacing = 0.6.sp,
)

val MonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
    fontSize = 12.5.sp,
)

val SerifItalic = TextStyle(
    fontFamily = FontFamily.Serif,
    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
    fontSize = 16.sp,
)

@Composable
fun PetPomodoroTheme(content: @Composable () -> Unit) {
    // Ứng dụng luôn dùng palette Memphis sáng (không có dark mode ở M3-prep)
    MaterialTheme(
        colorScheme = MemphisScheme,
        typography = Typography(),
        content = content,
    )
}
