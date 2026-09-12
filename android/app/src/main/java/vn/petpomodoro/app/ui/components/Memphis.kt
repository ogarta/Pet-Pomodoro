package vn.petpomodoro.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle

/** Bóng đổ lệch cứng chất Memphis (hard offset shadow) */
fun Modifier.memphisShadow(offset: Dp = 5.dp, color: Color = MemphisColors.Ink): Modifier =
    drawBehind {
        val o = offset.toPx()
        drawRect(color = color, topLeft = Offset(o, o), size = size)
    }

/** Nền sọc chéo 45° hai màu (repeating-linear-gradient của web, vẽ bằng Canvas) */
fun Modifier.memphisStripes(c1: Color, c2: Color): Modifier = drawBehind {
    val stripe = 7.dp.toPx()
    clipRect {
        rotate(45f) {
            var x = -size.height
            var i = 0
            while (x < size.width + size.height) {
                drawRect(
                    color = if (i % 2 == 0) c1 else c2,
                    topLeft = Offset(x, -size.height),
                    size = Size(stripe, size.height * 3),
                )
                x += stripe
                i++
            }
        }
    }
}

/**
 * Block — thẻ giấy Memphis: nền màu, viền mực 3dp, bóng lệch, xoay nhẹ tùy ý.
 * Ngôn ngữ hình ảnh của `.Block` trong demo KB1, viết mới cho Compose.
 */
@Composable
fun MemphisBlock(
    modifier: Modifier = Modifier,
    bg: Color = MemphisColors.Paper,
    rotate: Float = 0f,
    pad: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .graphicsLayer { rotationZ = rotate } // xoá toàn bộ thẻ (nền+viền+nội dung)
            .memphisShadow()
            .background(bg)
            .border(3.dp, MemphisColors.Ink)
            .padding(pad),
        content = content,
    )
}

/** Chip — nhãn nhỏ viền mực (trạng thái / Lv / sushi) */
@Composable
fun MemphisChip(
    text: String,
    modifier: Modifier = Modifier,
    bg: Color = MemphisColors.Cream2,
    fg: Color = MemphisColors.Ink,
    rotate: Float = 0f,
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotate }
            .memphisShadow(3.dp)
            .background(bg)
            .border(3.dp, MemphisColors.Ink)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MonoStyle, color = fg)
    }
}

enum class RetroVariant(val bg: Color, val fg: Color) {
    RED(MemphisColors.Red, MemphisColors.Paper),
    PURPLE(MemphisColors.Purple, MemphisColors.Paper),
    OLIVE(MemphisColors.Olive, MemphisColors.Paper),
    BLUE(MemphisColors.Blue, MemphisColors.Paper),
    MUSTARD(MemphisColors.Mustard, MemphisColors.Ink),
    GHOST(MemphisColors.Cream, MemphisColors.Ink),
}

/** Nút retro Memphis: nền đậm, viền mực, bóng lệch — như `.rbtn` demo */
@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: RetroVariant = RetroVariant.RED,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.memphisShadow(4.dp),
        enabled = enabled,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(3.dp, MemphisColors.Ink),
        colors = ButtonDefaults.buttonColors(
            containerColor = variant.bg,
            contentColor = variant.fg,
            disabledContainerColor = variant.bg.copy(alpha = 0.45f),
            disabledContentColor = variant.fg.copy(alpha = 0.6f),
        ),
    ) {
        Text(text, style = MonoStyle)
    }
}

/** Thanh sọc in (striped meter) — nền giấy, viền mực, fill sọc 45° */
@Composable
fun StripedBar(
    fraction: Float,
    c1: Color,
    c2: Color,
    modifier: Modifier = Modifier,
    barHeight: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .border(3.dp, MemphisColors.Ink)
            .background(MemphisColors.Paper)
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .memphisStripes(c1, c2),
        )
    }
}

/** Meter nhãn + số (như `.Meter` demo): "Sức mạnh  62/64" + thanh sọc */
@Composable
fun MemphisMeter(
    label: String,
    value: Int,
    max: Int,
    c1: Color,
    c2: Color,
    modifier: Modifier = Modifier,
    caption: String? = null,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MonoStyle)
            Text("$value/$max", style = MonoStyle)
        }
        Spacer(Modifier.height(5.dp))
        StripedBar(
            fraction = if (max <= 0) 0f else value.toFloat() / max,
            c1 = c1, c2 = c2,
        )
        if (caption != null) {
            Text(
                caption,
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = MemphisColors.Ink.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/**
 * CapMeter — thang 0–100 có VẠCH CAP theo level (spec §3):
 * cap(L) = min(100, 40 + 6×L) — vạch mực đứng tại cap.
 */
@Composable
fun CapMeter(
    label: String,
    value: Int,
    cap: Int,
    c1: Color,
    c2: Color,
    caption: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MonoStyle)
            Text("$value/$cap", style = MonoStyle)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(25.dp)) {
            StripedBar(
                fraction = value / 100f,
                c1 = c1, c2 = c2,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            // vạch cap: nằm ở cap% thang 0–100
            Box(Modifier.fillMaxWidth((cap / 100f).coerceIn(0f, 1f))) {
                Box(
                    Modifier
                        .align(Alignment.CenterEnd)
                        .width(4.dp)
                        .height(25.dp)
                        .background(MemphisColors.Ink),
                )
            }
        }
        caption?.let {
            Text(
                it,
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                color = MemphisColors.Ink.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** Battle HUD Gen-1: tên + Lv + thanh TT 10 ô xanh lá→vàng→đỏ (spec §2.5) */
@Composable
fun BattleHUD(
    name: String,
    level: Int,
    value: Int,
    max: Int = 100,
    critical: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val segments = 10
    val pct = if (max <= 0) 0f else value.toFloat() / max
    val filled = (pct * segments).toInt().coerceIn(0, segments)
    val hpColor = when {
        critical -> MemphisColors.HpRed
        pct >= 0.5f -> MemphisColors.HpGreen
        pct >= 0.2f -> MemphisColors.HpYellow
        else -> MemphisColors.HpRed
    }

    val blink by rememberInfiniteTransition(label = "hudBlink").animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hudBlinkAlpha",
    )

    Column(
        modifier
            .memphisShadow(4.dp)
            .background(MemphisColors.Paper)
            .border(3.dp, MemphisColors.Ink)
            .padding(horizontal = 9.dp, vertical = 5.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name.uppercase(), style = MonoStyle.copy(fontSize = 13.5.sp, letterSpacing = 1.sp))
            Text("Lv.$level", style = MonoStyle.copy(fontSize = 11.sp))
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TT", style = MonoStyle.copy(fontSize = 11.sp))
            Spacer(Modifier.width(6.dp))
            Row(
                Modifier
                    .weight(1f)
                    .border(2.dp, MemphisColors.Ink)
                    .background(MemphisColors.Cream2)
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                repeat(segments) { i ->
                    val isFilled = i < filled
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .alpha(if (critical && isFilled) blink else 1f)
                            .background(if (isFilled) hpColor else MemphisColors.Ink.copy(alpha = 0.12f)),
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            "$value/$max",
            style = MonoStyle.copy(fontSize = 10.5.sp),
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Dots mục tiêu ngày: ≥ 3 phiên (spec §3) */
@Composable
fun GoalDots(done: Int, total: Int = 3, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("Mục tiêu ngày", style = MonoStyle.copy(fontSize = 11.sp))
        Spacer(Modifier.width(8.dp))
        repeat(total) { i ->
            Box(
                Modifier
                    .padding(end = 5.dp)
                    .size(16.dp)
                    .clip(CircleShape)
                    .border(2.dp, MemphisColors.Ink, CircleShape)
                    .background(if (i < done) MemphisColors.Olive else MemphisColors.Cream2),
            )
        }
        Text(
            "≥ $total phiên",
            style = MonoStyle.copy(fontSize = 11.sp),
            color = MemphisColors.Ink.copy(alpha = 0.7f),
        )
    }
}
