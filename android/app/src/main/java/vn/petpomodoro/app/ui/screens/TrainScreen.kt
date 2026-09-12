package vn.petpomodoro.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.ceil
import kotlinx.coroutines.delay
import vn.petpomodoro.app.TimerMode
import vn.petpomodoro.app.TimerUi
import vn.petpomodoro.app.ui.components.BattleHUD
import vn.petpomodoro.app.ui.components.GoalDots
import vn.petpomodoro.app.ui.components.MemphisBlock
import vn.petpomodoro.app.ui.components.MemphisChip
import vn.petpomodoro.app.ui.components.PetTile
import vn.petpomodoro.app.ui.components.RetroButton
import vn.petpomodoro.app.ui.components.RetroVariant
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle
import vn.petpomodoro.game.Catalog
import vn.petpomodoro.game.GameState

/**
 * Màn ② "Tập" (spec §4-KB1 ②): pomodoro THẬT 25:00/05:00 với start/pause/abort,
 * pet + Battle HUD (tên + Lv + TT thanh xanh/vàng/đỏ), goal dots —
 * sushi, dots mục tiêu ngày.
 */
@Composable
fun TrainScreen(
    state: GameState,
    timer: TimerUi,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAbort: () -> Unit,
    onFeed: () -> Boolean,
    modifier: Modifier = Modifier,
) {
    val line = Catalog.line(state.speciesId)
    val stage = line.stageById(state.stageId) ?: line.stages.first()
    val sick = state.isSick
    // (chỉ số XP/Sức mạnh/Thể trạng đã chuyển sang màn Tiến hoá)

    var feedMsg by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(feedMsg) {
        if (feedMsg != null) {
            delay(2400)
            feedMsg = null
        }
    }

    // Hủy phiên 2 bước — tránh mis-click mất cả phiên 25 phút
    var confirmingAbort by remember { mutableStateOf(false) }
    LaunchedEffect(confirmingAbort) {
        if (confirmingAbort) {
            delay(3000)
            confirmingAbort = false
        }
    }
    fun onAbortConfirmed() {
        if (confirmingAbort) {
            confirmingAbort = false
            onAbort()
        } else {
            confirmingAbort = true
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Chips trạng thái
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MemphisChip(
                text = when {
                    sick -> "Ốm yếu vẫn tập chậm"
                    timer.mode == TimerMode.BREAK -> "Giải lao 5 phút"
                    timer.mode == TimerMode.FOCUS && timer.running -> "Đang tập trung"
                    timer.mode == TimerMode.FOCUS -> "Đã tạm dừng"
                    else -> "Sẵn sàng tập?"
                },
                bg = when {
                    sick -> MemphisColors.Red
                    timer.mode == TimerMode.FOCUS && timer.running -> MemphisColors.Olive
                    else -> MemphisColors.Mustard
                },
                fg = if (sick || (timer.mode == TimerMode.FOCUS && timer.running)) MemphisColors.Paper else MemphisColors.Ink,
                rotate = -1f,
            )
            MemphisChip(text = "Lv.${state.level}", bg = MemphisColors.Blue, fg = MemphisColors.Paper, rotate = 1f)
            MemphisChip(text = "🍣 × ${state.coins} sushi", bg = MemphisColors.Paper, rotate = 0.5f)
        }

        Spacer(Modifier.height(14.dp))

        // Đồng hồ pomodoro
        MemphisBlock(bg = MemphisColors.Blue, rotate = -1f, pad = 14.dp, modifier = Modifier.fillMaxWidth()) {
            val totalSec = ceil(timer.remainingMs / 1000.0).toInt().coerceAtLeast(0)
            val text = "%02d:%02d".format(totalSec / 60, totalSec % 60)
            Text(
                text,
                style = BungeeStyle.copy(fontSize = 54.sp),
                color = MemphisColors.Paper,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                if (timer.mode == TimerMode.BREAK) "GIÃI LAO 5 PHÚT · THƯỞNG SAU PHIÊN HOÀN THÀNH"
                else "POMODORO 25 PHÚT · MỤC TIÊU NGÀY ≥ 3 PHIÊN",
                style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Paper,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(14.dp))

        // Sân khấu pet + Battle HUD
        Box(Modifier.fillMaxWidth()) {
            PetTile(
                spriteName = stage.sprite,
                height = 300.dp,
                sick = sick,
                rotate = -0.6f,
                modifier = Modifier.align(Alignment.Center),
            )
            BattleHUD(
                name = stage.name,
                level = state.level,
                value = state.condition,
                critical = sick,
                modifier = Modifier.align(Alignment.BottomEnd).padding(top = 40.dp),
            )
            feedMsg?.let {
                MemphisChip(
                    text = it,
                    bg = MemphisColors.Olive,
                    fg = MemphisColors.Paper,
                    rotate = -1.5f,
                    modifier = Modifier.align(Alignment.TopStart),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        GoalDots(done = state.todaySessions, modifier = Modifier.fillMaxWidth())
        if (sick) {
            Spacer(Modifier.height(10.dp))
            MemphisBlock(bg = MemphisColors.Red, rotate = 0.6f, pad = 10.dp) {
                Text(
                    "⚠ LEVEL GIỮ NGUYÊN — NGOẠI HÌNH ỐM YẾU",
                    style = BungeeStyle.copy(fontSize = 13.sp),
                    color = MemphisColors.Paper,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${stage.name} vẫn Lv.${state.level} · XP không mất. Phục hồi: cho ăn (+15 thể trạng/lần) + ≥ 1 phiên/ngày.",
                    style = MonoStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Normal),
                    color = MemphisColors.Paper,
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Nút điều khiển timer
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            when {
                timer.mode == TimerMode.FOCUS && timer.running -> {
                    RetroButton("Tạm dừng", onPause, Modifier.weight(1f), RetroVariant.PURPLE)
                    RetroButton(
                        if (confirmingAbort) "Chắc bỏ phiên?" else "Bỏ phiên",
                        { onAbortConfirmed() },
                        Modifier.weight(1f),
                        if (confirmingAbort) RetroVariant.RED else RetroVariant.GHOST,
                    )
                }
                timer.mode == TimerMode.FOCUS -> {
                    RetroButton("Tiếp tục", onResume, Modifier.weight(1f), RetroVariant.PURPLE)
                    RetroButton(
                        if (confirmingAbort) "Chắc bỏ phiên?" else "Bỏ phiên",
                        { onAbortConfirmed() },
                        Modifier.weight(1f),
                        if (confirmingAbort) RetroVariant.RED else RetroVariant.GHOST,
                    )
                }
                timer.mode == TimerMode.BREAK -> {
                    RetroButton("Bỏ qua nghỉ →", onAbort, Modifier.fillMaxWidth(), RetroVariant.PURPLE)
                }
                else -> {
                    RetroButton("Bắt đầu 25 phút", onStart, Modifier.fillMaxWidth(), RetroVariant.PURPLE)
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        RetroButton(
            text = "Cho ăn · −1 🍣",
            onClick = {
                if (onFeed()) feedMsg = "thích lắm! +5 XP · +15 thể trạng"
            },
            modifier = Modifier.fillMaxWidth(),
            variant = RetroVariant.OLIVE,
            enabled = state.coins >= 1,
        )

        Spacer(Modifier.height(20.dp))
    }
}
