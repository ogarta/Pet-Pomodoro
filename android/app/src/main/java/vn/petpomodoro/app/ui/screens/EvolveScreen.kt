package vn.petpomodoro.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.petpomodoro.app.ui.components.CapMeter
import vn.petpomodoro.app.ui.components.MemphisBlock
import vn.petpomodoro.app.ui.components.MemphisChip
import vn.petpomodoro.app.ui.components.MemphisMeter
import vn.petpomodoro.app.ui.components.PetSprite
import vn.petpomodoro.app.ui.components.PetTile
import vn.petpomodoro.app.ui.components.StripedBar
import vn.petpomodoro.app.ui.components.memphisShadow
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle
import vn.petpomodoro.app.ui.theme.SerifItalic
import vn.petpomodoro.game.BranchDef
import vn.petpomodoro.game.Catalog
import vn.petpomodoro.game.Formulas
import vn.petpomodoro.game.GameState
import vn.petpomodoro.game.HybridDef
import vn.petpomodoro.game.LeafProgress

/**
 * Màn ③ "Tiến hoá" (spec §4-KB1 ③):
 *  · timeline 4 form của line đang nuôi — render từ CATALOG
 *  · thẻ điều kiện nhánh Stage 3 (Hiền ∨ Chiến Binh) — render từ registry COND
 *  · panel form lai hiếm liên quan (điều kiện kép power≥85 ∧ streak≥10)
 * Khoảnh khắc tiến hoá thật (overlay) kích hoạt bởi sự kiện `Evolved` từ event bus.
 */
@Composable
fun EvolveScreen(
    state: GameState,
    modifier: Modifier = Modifier,
) {
    val line = Catalog.line(state.speciesId)
    val nextStage = line.nextStage(state.level)
    val current = line.stageById(state.stageId) ?: line.stages.first()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemphisChip("KHOẢNH KHẮC TIẾN HOÁ", bg = MemphisColors.Purple, fg = MemphisColors.Paper, rotate = -1.2f)
            MemphisChip(
                text = if (nextStage != null) "Lv.${state.level} · mốc kế Lv.${nextStage.atLevel}" else "Lv.${state.level} · form cuối",
                rotate = 0.8f,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Sân khấu form hiện tại ──
        MemphisBlock(bg = MemphisColors.Mustard, rotate = -1f, pad = 18.dp, modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                PetTile(
                    spriteName = current.sprite,
                    height = 160.dp,
                    sick = state.isSick,
                    rotate = -0.6f,
                    modifier = Modifier.align(Alignment.Center),
                )
                MemphisChip(
                    text = current.name.uppercase(),
                    bg = MemphisColors.Red,
                    fg = MemphisColors.Paper,
                    rotate = -6f,
                    modifier = Modifier.align(Alignment.TopStart).offset(x = 0.dp, y = (-6).dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "${current.name} · ${current.desc}",
                style = SerifItalic,
                color = MemphisColors.Ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Chỉ số hiện tại (chuyển từ màn Tập) — XP/Sức mạnh/Thể trạng ──
        MemphisBlock(bg = MemphisColors.Paper, rotate = -0.7f, pad = 12.dp) {
            MemphisMeter(
                label = "XP · Lv.${state.level}",
                value = state.xp,
                max = Formulas.xpNeed(state.level),
                c1 = MemphisColors.Mustard,
                c2 = MemphisColors.Mustard2,
            )
            Spacer(Modifier.height(12.dp))
            CapMeter(
                label = "Sức mạnh",
                value = state.power,
                cap = Formulas.powerCap(state.level),
                c1 = MemphisColors.Red,
                c2 = MemphisColors.Red2,
            )
            Spacer(Modifier.height(12.dp))
            MemphisMeter(
                label = "Thể trạng · ${state.conditionBand.label}",
                value = state.condition,
                max = 100,
                c1 = MemphisColors.Purple,
                c2 = MemphisColors.Purple2,
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Timeline 4 form render từ catalog ──
        MemphisBlock(bg = MemphisColors.Paper, rotate = 0.7f, pad = 12.dp) {
            Text("HÀNH TRÌNH CỦA LINE ${line.name.uppercase()}", style = BungeeStyle.copy(fontSize = 13.sp))
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TimelineTile("Trứng", "sprite_egg", reached = true, isCurrent = false, height = 56.dp)
                line.stages.forEachIndexed { i, s ->
                    Text("→", style = BungeeStyle.copy(fontSize = 14.sp), color = MemphisColors.Ink.copy(alpha = 0.6f))
                    TimelineTile(
                        name = s.name,
                        sprite = s.sprite,
                        reached = state.level >= s.atLevel,
                        isCurrent = s.id == state.stageId,
                        height = (64 + i * 18).dp,
                        badge = "Lv.${s.atLevel}",
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                if (nextStage != null)
                    "Sắp tiến hoá: Lv.${nextStage.atLevel} → ${nextStage.name} — dáng ĐỔI HẲN, level giữ nguyên"
                else
                    "${line.stages.last().name} là form cuối — nhánh Stage 3 chọn ở dưới",
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.8f),
            )
        }

        Spacer(Modifier.height(18.dp))

        // ── Nhánh Stage 3 — thẻ điều kiện render từ registry ──
        MemphisBlock(bg = MemphisColors.Mustard, rotate = -0.8f, pad = 12.dp) {
            Text("NHÁNH STAGE 3 · CHỌN 1 TRONG 2 + MẶC ĐỊNH", style = BungeeStyle.copy(fontSize = 13.sp))
            Spacer(Modifier.height(4.dp))
            Text(
                "Khi đạt Lv.${Catalog.LEVEL_STAGE_3}, nhánh đầu tiên đủ điều kiện sẽ tự chọn; không nhánh nào → form thường.",
                style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                line.branches.forEachIndexed { i, b ->
                    BranchCard(
                        branch = b,
                        stage3Name = line.stages.last().name,
                        chosen = state.branchId == b.id,
                        state = state,
                        rotate = if (i % 2 == 0) -1f else 1f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Form lai hiếm liên quan (hệ collection — spec §1.2) ──
        MemphisBlock(bg = MemphisColors.Paper, rotate = 0.8f, pad = 12.dp) {
            Text("FORM LAI HIẾM · LINE ${line.name.uppercase()}", style = BungeeStyle.copy(fontSize = 13.sp))
            Spacer(Modifier.height(4.dp))
            Text(
                "Điều kiện KÉP — mở khóa vào bộ sưu tầm khi đủ (hệ collection đọc registry).",
                style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
            )
            Spacer(Modifier.height(12.dp))
            Catalog.hybridsFor(line.id).forEach { h ->
                HybridCard(h, state)
                Spacer(Modifier.height(10.dp))
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun TimelineTile(
    name: String,
    sprite: String,
    reached: Boolean,
    isCurrent: Boolean,
    height: androidx.compose.ui.unit.Dp,
    badge: String? = null,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
            PetSprite(
                spriteName = sprite,
                modifier = Modifier.height(height),
                saturation = if (reached) 1f else 0f,
            )
            if (isCurrent) {
                Box(
                    Modifier
                        .matchParentSize()
                        .border(3.dp, MemphisColors.Red),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            name,
            style = MonoStyle.copy(fontSize = 10.5.sp),
            color = if (reached) MemphisColors.Ink else MemphisColors.Ink.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
        )
        if (badge != null) {
            Text(
                badge,
                style = MonoStyle.copy(fontSize = 9.5.sp),
                color = if (reached) MemphisColors.Olive else MemphisColors.Ink.copy(alpha = 0.45f),
            )
        }
    }
}

/** Thẻ điều kiện nhánh — thanh tiến độ từng lá điều kiện (như demo KB1) */
@Composable
private fun BranchCard(
    branch: BranchDef,
    stage3Name: String,
    chosen: Boolean,
    state: GameState,
    rotate: Float,
    modifier: Modifier = Modifier,
) {
    val ok = branch.cond.evaluate(state)
    val c1 = if (ok) MemphisColors.Olive else MemphisColors.Red
    val c2 = if (ok) MemphisColors.Olive2 else MemphisColors.Red2

    Box(modifier.graphicsLayerRot(rotate)) {
        Column(
            Modifier
                .memphisShadow(4.dp)
                .background(if (ok) MemphisColors.CardMet else MemphisColors.Paper)
                .border(3.dp, MemphisColors.Ink)
                .padding(11.dp),
        ) {
            if (ok || chosen) {
                MemphisChip(
                    text = if (chosen) "ĐÃ CHỌN" else "ĐẠT",
                    bg = MemphisColors.Olive,
                    fg = MemphisColors.Paper,
                    modifier = Modifier.align(Alignment.End),
                )
                Spacer(Modifier.height(6.dp))
            }
            Text(branch.name, style = BungeeStyle.copy(fontSize = 15.sp))
            Spacer(Modifier.height(3.dp))
            Text(
                "${branch.name} — ${branch.short}",
                style = MonoStyle.copy(fontSize = 12.sp),
                color = MemphisColors.Red,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "điều kiện registry: ${branch.cond.describe()}",
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(8.dp))
            branch.cond.leaves(state).forEach { leaf ->
                LeafBar(leaf, c1, c2)
                Spacer(Modifier.height(6.dp))
            }
            Text(
                "form: $stage3Name ${branch.formSuffix}",
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun HybridCard(h: HybridDef, state: GameState) {
    val unlocked = h.id in state.unlockedFormIds
    val ok = h.cond.evaluate(state)
    val c1 = if (ok) MemphisColors.Olive else MemphisColors.Mustard
    val c2 = if (ok) MemphisColors.Olive2 else MemphisColors.Mustard2
    Column(
        Modifier
            .fillMaxWidth()
            .memphisShadow(3.dp)
            .background(if (unlocked) MemphisColors.CardMet else MemphisColors.Cream)
            .border(3.dp, MemphisColors.Ink)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(h.name, style = BungeeStyle.copy(fontSize = 14.sp))
            Spacer(Modifier.width(8.dp))
            MemphisChip("HIẾM", bg = MemphisColors.Pink, fg = MemphisColors.Ink, rotate = -2f)
            Spacer(Modifier.weight(1f))
            MemphisChip(
                if (unlocked) "ĐÃ MỞ" else "CHƯA MỞ",
                bg = if (unlocked) MemphisColors.Olive else MemphisColors.Cream2,
                fg = if (unlocked) MemphisColors.Paper else MemphisColors.Ink,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "nguyên tố: ${h.from.joinToString(" × ") { Catalog.line(it).name }} · ${h.cond.describe()}",
            style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
        )
        Spacer(Modifier.height(6.dp))
        h.cond.leaves(state).forEach { LeafBar(it, c1, c2) }
    }
}

@Composable
private fun LeafBar(leaf: LeafProgress, c1: Color, c2: Color) {
    Column {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(leaf.label, style = MonoStyle.copy(fontSize = 11.5.sp))
            Text("${leaf.current}/${leaf.target}", style = MonoStyle.copy(fontSize = 11.5.sp))
        }
        Spacer(Modifier.height(3.dp))
        StripedBar(fraction = leaf.fraction, c1 = c1, c2 = c2, barHeight = 10.dp)
    }
}

private fun Modifier.graphicsLayerRot(degrees: Float): Modifier =
    this.then(Modifier.graphicsLayer { rotationZ = degrees })
