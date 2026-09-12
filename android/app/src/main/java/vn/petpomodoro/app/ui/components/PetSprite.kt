package vn.petpomodoro.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import vn.petpomodoro.app.R
import vn.petpomodoro.app.ui.theme.MemphisColors

/**
 * Map tên sprite trong Catalog (cột `sprite`) → drawable.
 * Tất cả sprite v3 lấy từ design-demos/sprites/ref (direction-approved.md),
 * copy vào res/drawable-nodpi — KHÔNG dùng grid tay.
 */
fun spriteResId(name: String): Int = when (name) {
    "sprite_tan_lua" -> R.drawable.sprite_tan_lua
    "sprite_bong_bot" -> R.drawable.sprite_bong_bot
    "sprite_diem_long" -> R.drawable.sprite_diem_long
    "sprite_giot" -> R.drawable.sprite_giot
    "sprite_suoi_vot" -> R.drawable.sprite_suoi_vot
    "sprite_trieu_long" -> R.drawable.sprite_trieu_long
    "sprite_mam" -> R.drawable.sprite_mam
    "sprite_bup_xanh" -> R.drawable.sprite_bup_xanh
    "sprite_co_thu_linh" -> R.drawable.sprite_co_thu_linh
    else -> R.drawable.sprite_egg
}

/**
 * PetSprite — render sprite pixel: `FilterQuality.None` (nearest-neighbor)
 * giữ cạnh pixel sắc khi scale.
 *
 * Biến thể ỐM YẾU (thể trạng < 30, spec §2.4) — lựa chọn M3-prep (ghi trong báo cáo):
 * spec yêu cầu silhouette vẽ riêng theo grid; bản Android này dùng BIẾN ĐỔI CHƯƠNG TRÌNH
 * thay vì vẽ lại grid: nhợt màu (ColorMatrix saturation 0.3) + cúi/rũ hình
 * (graphicsLayer scaleY 0.9 + rotationZ + dịch xuống) + shiver rung ±1.5px ~400ms/lần
 * đúng nhịp §2.4. Silhouette vẽ riêng để làm mục tiêu sau (chỉ cần thêm
 * `sprite_<form>_om` vào drawable và 1 nhánh trong [spriteResId] — data-driven).
 */
@Composable
fun PetSprite(
    spriteName: String,
    modifier: Modifier = Modifier,
    sick: Boolean = false,
    shiver: Boolean = true,
    saturation: Float = 1f,
) {
    // ốm → nhợt (≤0.3); UI có thể ép grayscale (0f) cho form chưa đạt trong timeline
    val effective = if (sick) saturation.coerceAtMost(0.3f) else saturation
    val matrix = remember(effective) {
        ColorMatrix().apply { setToSaturation(effective) }
    }
    val shiverX by rememberInfiniteTransition(label = "shiver").animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 200, easing = LinearEasing),
        ),
        label = "shiverX",
    )

    val bitmap = ImageBitmap.imageResource(spriteResId(spriteName))
    val aspect = if (bitmap.height > 0) bitmap.width.toFloat() / bitmap.height else 1f
    Image(
        bitmap = bitmap,
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        filterQuality = FilterQuality.None,
        colorFilter = ColorFilter.colorMatrix(matrix),
        modifier = modifier.aspectRatio(aspect).graphicsLayer {
            if (sick) {
                scaleY = 0.9f
                rotationZ = 2.5f
                translationY = 6.dp.toPx()
                translationX = if (shiver) shiverX else 0f
            }
        },
    )
}

/**
 * PetTile — sân khấu Memphis cho sprite: thẻ giấy kem viền mực + bóng lệch,
 * xoay nhẹ (bản Compose của `.PetTile` demo).
 */
@Composable
fun PetTile(
    spriteName: String,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp,
    sick: Boolean = false,
    lite: Boolean = false,
    rotate: Float = 0f,
    pad: Dp = 10.dp,
) {
    Box(
        modifier = modifier
            .graphicsLayer { rotationZ = rotate }
            .memphisShadow(if (lite) 3.dp else 6.dp)
            .background(MemphisColors.Paper)
            .border(if (lite) 2.dp else 3.dp, MemphisColors.Ink)
            .padding(pad),
    ) {
        PetSprite(
            spriteName = spriteName,
            sick = sick,
            modifier = Modifier.height(height),
        )
    }
}
