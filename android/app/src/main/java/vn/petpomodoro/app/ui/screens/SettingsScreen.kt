package vn.petpomodoro.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import vn.petpomodoro.app.PetViewModel
import vn.petpomodoro.app.ui.components.MemphisBlock
import vn.petpomodoro.app.ui.components.MemphisChip
import vn.petpomodoro.app.ui.components.RetroButton
import vn.petpomodoro.app.ui.components.RetroVariant
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors
import vn.petpomodoro.app.ui.theme.MonoStyle
import java.time.LocalDate

/**
 * Màn CÀI ĐẶT: âm thanh + thông báo, bản lưu pet (xuất/nhập JSON dùng chéo với web),
 * ghi chú đồng bộ đám mây (sắp có).
 */
@Composable
fun SettingsScreen(vm: PetViewModel, modifier: Modifier = Modifier) {
    val alerts by vm.alerts.collectAsStateWithLifecycle()

    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2600)
            toast = null
        }
    }

    // Xuất: SAF CreateDocument → ghi file. Nhập: OpenDocument → parse + ghi đè (2 bước confirm).
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            toast = if (vm.writeBackup(uri)) "Đã xuất bản lưu — giữ file ở nơi an toàn nhé!"
            else "Không ghi được file — thử lại nhé."
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            toast = when (vm.readBackup(uri)) {
                true -> "Đã nhập bản lưu — chào mừng bạn và bé trở lại!"
                false -> "File không đúng bản lưu PetPomodoro — chưa đổi gì."
                null -> "File đọc không được — chưa đổi gì."
            }
        }
    }

    // Quyền thông báo (Android 13+) — xin đúng lúc user bật toggle
    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    var confirmingImport by remember { mutableStateOf(false) }
    LaunchedEffect(confirmingImport) {
        if (confirmingImport) {
            delay(3000)
            confirmingImport = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        MemphisChip(text = "CÀI ĐẶT", bg = MemphisColors.Olive, fg = MemphisColors.Paper, rotate = -1f)
        Spacer(Modifier.height(14.dp))

        // ── Âm thanh & thông báo ──────────────────────────────────────
        MemphisBlock(bg = MemphisColors.Paper, rotate = -0.6f, modifier = Modifier.fillMaxWidth()) {
            Text("🔔 ÂM THANH & THÔNG BÁO", style = BungeeStyle.copy(fontSize = 13.sp))
            Spacer(Modifier.height(10.dp))
            AlertRow(
                label = "Âm thanh",
                hint = "Chuông báo khi hết giờ, cho ăn và tiến hoá",
                on = alerts.sound,
                onToggle = vm::toggleSound,
            )
            Spacer(Modifier.height(10.dp))
            AlertRow(
                label = "Thông báo",
                hint = "Báo trên màn hình khi hết giờ mà app đang ở nền",
                on = alerts.notify,
                onToggle = {
                    vm.toggleNotify()
                    if (!alerts.notify && Build.VERSION.SDK_INT >= 33) {
                        notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Chuông cuối giờ phát cả khi app đang ở nền; nếu process bị đóng, " +
                    "báo thức hệ thống vẫn kêu và phiên vẫn được tính khi bạn mở lại.",
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.75f),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Bản lưu pet ───────────────────────────────────────────────
        MemphisBlock(bg = MemphisColors.Cream2, rotate = 0.6f, modifier = Modifier.fillMaxWidth()) {
            Text("💾 BẢN LƯU PET", style = BungeeStyle.copy(fontSize = 13.sp))
            Spacer(Modifier.height(6.dp))
            Text(
                "Bé sống trong bộ nhớ máy này — xuất file để phòng lúc xoá ứng dụng hoặc chuyển máy. " +
                    "File dùng chéo được với bản web.",
                style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RetroButton(
                    text = "⬇ Xuất bản lưu",
                    onClick = {
                        exportLauncher.launch("petpomodoro-backup-${LocalDate.now()}.json")
                    },
                    modifier = Modifier.weight(1f),
                    variant = RetroVariant.BLUE,
                )
                RetroButton(
                    text = if (confirmingImport) "Chắc ghi đè bé?" else "⬆ Nhập bản lưu",
                    onClick = {
                        if (confirmingImport) {
                            confirmingImport = false
                            importLauncher.launch(arrayOf("application/json", "text/plain"))
                        } else {
                            confirmingImport = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    variant = if (confirmingImport) RetroVariant.RED else RetroVariant.GHOST,
                )
            }
        }

        toast?.let {
            Spacer(Modifier.height(12.dp))
            MemphisChip(text = it, bg = MemphisColors.Olive, fg = MemphisColors.Paper, rotate = -1f)
        }

        Spacer(Modifier.height(16.dp))
        MemphisBlock(bg = MemphisColors.Cream2, rotate = -0.4f, modifier = Modifier.fillMaxWidth()) {
            Text(
                "☁ ĐỒNG BỘ ĐÁM MÂY — SẮP CÓ",
                style = BungeeStyle.copy(fontSize = 12.sp),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Đăng nhập Google để đồng bộ 2 nền tảng sẽ mở ở bước sau. " +
                    "Dữ liệu bé hiện đang lưu an toàn trên máy này.",
                style = MonoStyle.copy(fontSize = 11.5.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AlertRow(label: String, hint: String, on: Boolean, onToggle: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MonoStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Text(
                hint,
                style = MonoStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Normal),
                color = MemphisColors.Ink.copy(alpha = 0.7f),
            )
        }
        RetroButton(
            text = if (on) "Đang bật" else "Đang tắt",
            onClick = onToggle,
            variant = if (on) RetroVariant.OLIVE else RetroVariant.GHOST,
        )
    }
}
