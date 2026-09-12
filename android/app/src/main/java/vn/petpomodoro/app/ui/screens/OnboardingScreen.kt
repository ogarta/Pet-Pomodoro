package vn.petpomodoro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.petpomodoro.app.ui.components.MemphisBlock
import vn.petpomodoro.app.ui.components.MemphisChip
import vn.petpomodoro.app.ui.components.PetTile
import vn.petpomodoro.app.ui.components.RetroButton
import vn.petpomodoro.app.ui.components.RetroVariant
import vn.petpomodoro.app.ui.components.memphisShadow
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle
import vn.petpomodoro.game.Catalog
import vn.petpomodoro.game.SpeciesDef

/**
 * Màn ① — "Chọn quả trứng" (spec §4-KB1 ①): 3 trứng nguyên tố render từ CATALOG.
 * Lần đầu = onboarding; sau đó tab Trứng cho phép xem lại / chọn lại (reset có confirm).
 */
@Composable
fun OnboardingScreen(
    currentSpeciesId: String?,
    onAdopt: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var picked by remember(currentSpeciesId) { mutableStateOf(currentSpeciesId ?: Catalog.lines.first().id) }
    var confirmReset by remember { mutableStateOf(false) }
    val line = Catalog.line(picked)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        MemphisBlock(bg = MemphisColors.Mustard, rotate = -1.2f, pad = 14.dp) {
            Text("Chọn quả trứng", style = BungeeStyle.copy(fontSize = 26.sp), color = MemphisColors.Ink)
            Spacer(Modifier.height(4.dp))
            Text(
                "3 line nguyên tố · mỗi trứng nở thành một người bạn pixel",
                style = MonoStyle.copy(fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink,
            )
        }

        Spacer(Modifier.height(20.dp))
        val rotations = listOf(-1.6f, 1.3f, -0.9f)
        Catalog.lines.forEachIndexed { i, l ->
            EggCard(
                line = l,
                selected = l.id == picked,
                isCurrent = l.id == currentSpeciesId,
                rotate = rotations[i % rotations.size],
                onClick = { picked = l.id },
            )
            Spacer(Modifier.height(16.dp))
        }

        Spacer(Modifier.height(20.dp))
        RetroButton(
            text = if (currentSpeciesId == null) "Nhận ${line.stages[0].name} về nhà"
            else "Chọn lại trứng — nhận ${line.stages[0].name} về nhà",
            onClick = { if (currentSpeciesId == null) onAdopt(picked) else confirmReset = true },
            modifier = Modifier.fillMaxWidth(),
            variant = RetroVariant.RED,
        )
        if (currentSpeciesId != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "Chọn trứng khác = BẮT ĐẦU LẠI · tiến trình hiện tại sẽ mất",
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Bắt đầu lại với ${line.stages[0].name}?", style = BungeeStyle) },
            text = {
                Text(
                    "Tiến trình với ${currentSpeciesId?.let { Catalog.line(it).stages[0].name } ?: "pet hiện tại"} sẽ bị xóa (level, sushi, streak…).",
                    style = MonoStyle.copy(fontWeight = FontWeight.Normal),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onAdopt(picked)
                }) { Text("Chọn lại trứng", style = MonoStyle) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Thôi", style = MonoStyle) }
            },
        )
    }
}

@Composable
private fun EggCard(
    line: SpeciesDef,
    selected: Boolean,
    isCurrent: Boolean,
    rotate: Float,
    onClick: () -> Unit,
) {
    val tint = Color(line.colorArgb)
    Box(modifier = Modifier.graphicsLayer { rotationZ = rotate }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .memphisShadow(4.dp)
                .background(if (selected) MemphisColors.CreamPicked else MemphisColors.Paper)
                .border(3.dp, if (selected || isCurrent) tint else MemphisColors.Ink)
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PetTile(spriteName = "sprite_egg", height = 64.dp, lite = true, pad = 5.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "${line.element} Trứng ${line.name}",
                    style = BungeeStyle.copy(fontSize = 15.sp),
                )
                Spacer(Modifier.height(3.dp))
                val s = line.stages[0]
                Text(
                    "nở thành ${s.name} · ${s.desc}",
                    style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                )
            }
        }
        if (isCurrent) {
            MemphisChip(
                text = "ĐANG NUÔI",
                bg = tint,
                fg = MemphisColors.Paper,
                rotate = -2f,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 0.dp, y = (-10).dp),
            )
        } else if (selected) {
            MemphisChip(
                text = "ĐÃ CHỌN",
                bg = MemphisColors.Red,
                fg = MemphisColors.Paper,
                rotate = -2f,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 0.dp, y = (-10).dp),
            )
        }
    }
}
