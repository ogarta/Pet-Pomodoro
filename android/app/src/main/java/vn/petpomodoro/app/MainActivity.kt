package vn.petpomodoro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import vn.petpomodoro.app.ui.components.MemphisChip
import vn.petpomodoro.app.ui.components.MemphisTabBar
import vn.petpomodoro.app.ui.components.PetTile
import vn.petpomodoro.app.ui.components.RetroButton
import vn.petpomodoro.app.ui.components.RetroVariant
import vn.petpomodoro.app.ui.components.AppTab
import vn.petpomodoro.app.ui.screens.EvolveScreen
import vn.petpomodoro.app.ui.screens.OnboardingScreen
import vn.petpomodoro.app.ui.screens.SettingsScreen
import vn.petpomodoro.app.ui.screens.StatsScreen
import vn.petpomodoro.app.ui.screens.TrainScreen
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle
import vn.petpomodoro.app.ui.theme.PetPomodoroTheme
import vn.petpomodoro.game.Catalog
import vn.petpomodoro.game.GameEvent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifications.createChannel(this) // kênh báo hết giờ pomodoro
        setContent {
            PetPomodoroTheme {
                PetPomodoroApp()
            }
        }
    }
}

@Composable
fun PetPomodoroApp(vm: PetViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    var evolution by remember { mutableStateOf<GameEvent.Evolved?>(null) }

    // Đóng ngày bỏ lỡ khi app quay lại foreground (decay §3)
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.advanceDay()
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Event bus → overlay khoảnh khắc tiến hoá (+ fanfare)
    LaunchedEffect(vm) {
        vm.events.collect { ev ->
            if (ev is GameEvent.Evolved) {
                evolution = ev
                vm.playEvolveFanfare()
            }
        }
    }

    val current = state
    if (current == null) {
        OnboardingScreen(
            currentSpeciesId = null,
            onAdopt = vm::adopt,
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
        )
        return
    }

    var tab by remember { mutableStateOf(AppTab.TRAIN) }
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                AppTab.EGG -> OnboardingScreen(
                    currentSpeciesId = current.speciesId,
                    onAdopt = vm::adopt,
                    modifier = Modifier.fillMaxSize(),
                )
                AppTab.TRAIN -> TrainScreen(
                    state = current,
                    timer = timer,
                    onStart = vm::startFocus,
                    onPause = vm::pauseTimer,
                    onResume = vm::resumeTimer,
                    onAbort = vm::abortTimer,
                    onFeed = vm::feed,
                    modifier = Modifier.fillMaxSize(),
                )
                AppTab.EVOLVE -> EvolveScreen(current, Modifier.fillMaxSize())
                AppTab.STATS -> StatsScreen(current, Modifier.fillMaxSize())
                AppTab.SET -> SettingsScreen(vm, Modifier.fillMaxSize())
            }
        }
        MemphisTabBar(selected = tab, onSelect = { tab = it })
    }

    evolution?.let { ev ->
        EvolutionOverlay(evolved = ev, onDismiss = { evolution = null })
    }
}

/** Khoảnh khắc tiến hoá: lóe sáng + pop form mới (sự kiện `Evolved` từ event bus) */
@Composable
fun EvolutionOverlay(evolved: GameEvent.Evolved, onDismiss: () -> Unit) {
    val line = remember(evolved) { Catalog.lineBySpeciesStage(evolved.toStageId) }
    val stage = line.stageById(evolved.toStageId) ?: return
    val branchText = evolved.branchId?.let { id ->
        line.branches.firstOrNull { it.id == id }?.let { " — nhánh ${it.name}!" } ?: ""
    } ?: ""

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    listOf(
                        Color(0xFFFFFDF6).copy(alpha = 0.97f),
                        MemphisColors.Mustard.copy(alpha = 0.85f),
                        MemphisColors.Cream.copy(alpha = 0.0f),
                    ),
                ),
            )
            .background(MemphisColors.Ink.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(24.dp)
                .background(MemphisColors.Paper)
                .border(3.dp, MemphisColors.Ink)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MemphisChip("TIẾN HOÁ!", bg = MemphisColors.Red, fg = MemphisColors.Paper, rotate = -2f)
            Spacer(Modifier.height(12.dp))
            PetTile(spriteName = stage.sprite, height = 170.dp, rotate = -0.6f)
            Spacer(Modifier.height(12.dp))
            Text(
                "${nameOf(evolved.fromStageId)} đã tiến hoá thành ${stage.name}$branchText",
                style = BungeeStyle.copy(fontSize = 16.sp),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Dáng ĐỔI HẲN · level giữ nguyên",
                style = MonoStyle.copy(fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                RetroButton("Tuyệt vời!", onDismiss, Modifier.width(200.dp), RetroVariant.OLIVE)
            }
        }
    }
}

private fun nameOf(stageId: String): String =
    Catalog.lines.firstOrNull { l -> l.stages.any { it.id == stageId } }
        ?.stageById(stageId)?.name ?: stageId
