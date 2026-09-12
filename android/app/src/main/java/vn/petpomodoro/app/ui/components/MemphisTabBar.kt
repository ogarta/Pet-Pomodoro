package vn.petpomodoro.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import vn.petpomodoro.app.ui.theme.BungeeStyle
import vn.petpomodoro.app.ui.theme.MemphisColors

/** Tab dưới đáy màn hình — Trứng · Tập · Tiến hoá · Stats · Cài đặt */
enum class AppTab(val label: String, val activeBg: Color, val activeFg: Color) {
    EGG("Trứng", MemphisColors.Mustard, MemphisColors.Ink),
    TRAIN("Tập", MemphisColors.Red, MemphisColors.Paper),
    EVOLVE("Tiến hoá", MemphisColors.Purple, MemphisColors.Paper),
    STATS("Stats", MemphisColors.Blue, MemphisColors.Paper),
    SET("Cài đặt", MemphisColors.Olive, MemphisColors.Paper),
}

@Composable
fun MemphisTabBar(
    selected: AppTab,
    onSelect: (AppTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
        AppTab.entries.forEach { tab ->
            val active = tab == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 3.dp)
                    .clickable { onSelect(tab) }
                    .then(
                        if (active) {
                            Modifier
                                .graphicsLayer { translationY = -4.dp.toPx() }
                                .memphisShadow(3.dp)
                        } else {
                            Modifier
                        },
                    )
                    .background(if (active) tab.activeBg else MemphisColors.Cream2)
                    .border(3.dp, MemphisColors.Ink)
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    tab.label,
                    style = BungeeStyle.copy(fontSize = BungeeStyle.fontSize * 0.85f),
                    color = if (active) tab.activeFg else MemphisColors.Ink.copy(alpha = 0.75f),
                )
            }
        }
    }
}
