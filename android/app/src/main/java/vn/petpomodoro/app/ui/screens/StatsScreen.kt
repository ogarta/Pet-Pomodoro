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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import vn.petpomodoro.app.ui.components.MemphisBlock
import vn.petpomodoro.app.ui.components.MemphisChip
import vn.petpomodoro.app.ui.components.MemphisMeter
import vn.petpomodoro.app.ui.components.PetTile
import vn.petpomodoro.app.ui.components.RetroButton
import vn.petpomodoro.app.ui.components.RetroVariant
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle
import vn.petpomodoro.app.ui.theme.SerifItalic
import vn.petpomodoro.game.Catalog
import vn.petpomodoro.game.Formulas
import vn.petpomodoro.game.GameState

private val dayFormat = DateTimeFormatter.ofPattern("dd/MM")
private val timeFormat = "%02d:%02d"

/**
 * Màn ④ "Stats" (spec §4-KB1 ④): streak flames, hôm nay, lịch sử (từ DataStore),
 * thẻ pet + chip "Sắp tiến hoá", placeholder đồng bộ Google (M4 — KHÔNG Firebase ở M3).
 */
@Composable
fun StatsScreen(
    state: GameState,
    modifier: Modifier = Modifier,
) {
    val line = Catalog.line(state.speciesId)
    val stage = line.stageById(state.stageId) ?: line.stages.first()
    val nextStage = line.nextStage(state.level)
    val cap = Formulas.powerCap(state.level)
    val branchName = state.branchId?.let { id -> line.branches.firstOrNull { it.id == id }?.name }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemphisChip("SỔ TAY NUÔI BÉ", bg = MemphisColors.Blue, fg = MemphisColors.Paper, rotate = -1f)
            MemphisChip("🍣 × ${state.coins} sushi", bg = MemphisColors.Paper)
        }

        Spacer(Modifier.height(12.dp))

        // ── Streak ──
        MemphisBlock(bg = MemphisColors.Red, rotate = -0.9f, pad = 12.dp) {
            Text("Streak ${state.streak} ngày 🔥", style = BungeeStyle.copy(fontSize = 20.sp), color = MemphisColors.Paper)
            Spacer(Modifier.height(2.dp))
            Text(
                "${state.streak} ngày liên tiếp · đủ mục tiêu ≥ ${Formulas.GOAL_SESSIONS_PER_DAY} phiên/ngày",
                style = MonoStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Paper,
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val last7 = state.history.takeLast(7)
                repeat(7) { i ->
                    val rec = last7.getOrNull(i)
                    val met = rec?.goalMet == true
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .border(2.dp, MemphisColors.Paper, CircleShape)
                            .background(if (met) MemphisColors.Mustard else MemphisColors.Red2),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Hôm nay ──
        MemphisBlock(bg = MemphisColors.Mustard, rotate = 0.9f, pad = 12.dp) {
            Text("HÔM NAY", style = MonoStyle.copy(fontSize = 12.5.sp))
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${state.todayMinutes}", style = BungeeStyle.copy(fontSize = 34.sp))
                Text("phút", style = SerifItalic)
                Text("· ${state.todaySessions}", style = BungeeStyle.copy(fontSize = 24.sp), color = MemphisColors.Red)
                Text("phiên", style = SerifItalic)
                Spacer(Modifier.weight(1f))
                Text("+${state.todayXp} XP", style = MonoStyle)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Thẻ pet ──
        MemphisBlock(bg = MemphisColors.Cream2, rotate = -0.6f, pad = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PetTile(spriteName = stage.sprite, height = 84.dp, sick = state.isSick, lite = true, rotate = -1.5f, pad = 6.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${stage.name} · Lv.${state.level} · ${state.xp}/${Formulas.xpNeed(state.level)} XP", style = SerifItalic)
                    Spacer(Modifier.height(6.dp))
                    MemphisMeter(
                        label = "Sức mạnh",
                        value = state.power,
                        max = cap,
                        c1 = MemphisColors.Red,
                        c2 = MemphisColors.Red2,
                    )
                    Spacer(Modifier.height(6.dp))
                    MemphisMeter(
                        label = "Thể trạng · ${state.conditionBand.label}",
                        value = state.condition,
                        max = 100,
                        c1 = MemphisColors.Purple,
                        c2 = MemphisColors.Purple2,
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MemphisChip(
                    text = if (nextStage != null) "Sắp tiến hoá: Lv.${nextStage.atLevel} → ${nextStage.name}"
                    else "Form cuối${branchName?.let { " · nhánh $it" } ?: ""}",
                    bg = MemphisColors.Blue,
                    fg = MemphisColors.Paper,
                    rotate = -1.2f,
                )
                MemphisChip("TRẠNG THÁI: ${state.conditionBand.label.uppercase()}", rotate = 1f)
            }
            if (state.isSick) {
                Spacer(Modifier.height(10.dp))
                MemphisBlock(bg = MemphisColors.Red, rotate = 0.6f, pad = 10.dp) {
                    Text(
                        "⚠ LEVEL GIỮ NGUYÊN — NGOẠI HÌNH ỐM YẾU",
                        style = BungeeStyle.copy(fontSize = 13.sp),
                        color = MemphisColors.Paper,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Sức mạnh −6/ngày bỏ bê · Thể trạng −10/ngày không ăn & không phiên · XP & Lv không mất. Phục hồi: cho ăn mỗi ngày (+15/lần) + ≥ 1 phiên/ngày.",
                        style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                        color = MemphisColors.Paper,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Đồng bộ (placeholder — Firebase là milestone sau) ──
        MemphisBlock(bg = MemphisColors.Blue, rotate = -0.8f, pad = 12.dp) {
            Text("ĐỒNG BỘ THIẾT BỊ", style = BungeeStyle.copy(fontSize = 13.sp), color = MemphisColors.Paper)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(3.dp, MemphisColors.Ink, CircleShape)
                        .background(MemphisColors.Purple),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("PP", style = BungeeStyle.copy(fontSize = 14.sp), color = MemphisColors.Paper)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Chưa đồng bộ · offline", style = BungeeStyle.copy(fontSize = 14.sp), color = MemphisColors.Paper)
                    Text(
                        "Dữ liệu lưu trên máy (DataStore) — đăng nhập Google sẽ mở ở milestone sau",
                        style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                        color = MemphisColors.Paper,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            RetroButton(
                text = "Đăng nhập bằng Google — sắp có",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                variant = RetroVariant.MUSTARD,
                enabled = false,
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Lịch sử phiên ──
        MemphisBlock(bg = MemphisColors.Paper, rotate = 0.7f, pad = 12.dp) {
            Text("LỊCH SỬ PHIÊN GẦN ĐÂY", style = BungeeStyle.copy(fontSize = 13.sp))
            Spacer(Modifier.height(4.dp))
            val logs = state.sessionLog.takeLast(8).asReversed()
            if (logs.isEmpty()) {
                Text(
                    "Chưa có phiên nào — sang tab Tập và bấm \"Bắt đầu 25 phút\"!",
                    style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                    color = MemphisColors.Ink.copy(alpha = 0.75f),
                )
            } else {
                logs.forEachIndexed { i, log ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            timeFormat.format(log.minuteOfDay / 60, log.minuteOfDay % 60),
                            style = MonoStyle.copy(fontSize = 12.5.sp),
                        )
                        Text(
                            "Tập trung · ${log.minutes} phút",
                            style = MonoStyle.copy(fontSize = 12.5.sp, fontWeight = FontWeight.Normal),
                        )
                        Text("+${log.xp} XP", style = MonoStyle.copy(fontSize = 12.5.sp), color = MemphisColors.Olive)
                    }
                    if (i < logs.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(MemphisColors.Ink.copy(alpha = 0.35f)),
                        )
                    }
                }
            }
            if (state.history.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("LỊCH SỬ NGÀY", style = BungeeStyle.copy(fontSize = 12.sp))
                state.history.takeLast(5).asReversed().forEach { rec ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            LocalDate.ofEpochDay(rec.epochDay).format(dayFormat),
                            style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                        )
                        Text(
                            "${rec.minutes} phút · ${rec.sessions} phiên",
                            style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                        )
                        Text(
                            if (rec.goalMet) "ĐỦ ✓" else "THIẾU",
                            style = MonoStyle.copy(fontSize = 12.sp),
                            color = if (rec.goalMet) MemphisColors.Olive else MemphisColors.Red,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
    }
}
