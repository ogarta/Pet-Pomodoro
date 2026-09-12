<script setup lang="ts">
/**
 * Màn CÀI ĐẶT: chỉnh phút tập trung · nghỉ ngắn · nghỉ dài · số phiên trước nghỉ dài.
 * Đổi cài đặt chỉ áp dụng cho phiên KẾ TIẾP — phiên đang chạy giữ nguyên độ dài.
 * Persist tự động qua updateSettings (doc `state/settings`).
 */
import { computed, ref } from 'vue'
import {
  SETTING_LIMITS,
  breakMsOf,
  clampSetting,
  defaultSettings,
  focusMsOf,
  type SettingKey,
} from '#shared/game/formulas'
import { useGame } from '~/composables/useGame'
import { ensureNotifyPermission, unlockAudio } from '~/utils/alerts'

const game = useGame()
const cfg = computed(() => game.settings.value ?? defaultSettings())

type AlertKey = 'sound' | 'notify'

const ALERT_ROWS: { key: AlertKey; label: string; emoji: string; hint: string; bg: string }[] = [
  {
    key: 'sound',
    label: 'ÂM THANH',
    emoji: '🔔',
    hint: 'Chuông báo khi hết giờ, cho ăn và tiến hoá',
    bg: 'var(--mustardInk)',
  },
  {
    key: 'notify',
    label: 'THÔNG BÁO',
    emoji: '📬',
    hint: 'Báo ngoài màn hình khi hết giờ mà bạn đang ở tab khác',
    bg: 'var(--blue)',
  },
]

/** Trạng thái quyền thông báo của trình duyệt — quyết định dòng gợi ý bên dưới. */
const notifyPerm = computed<'granted' | 'denied' | 'default' | 'unsupported'>(() => {
  if (import.meta.server || typeof window === 'undefined' || !('Notification' in window)) {
    return 'unsupported'
  }
  return Notification.permission
})

const NOTIFY_HINT: Record<'granted' | 'denied' | 'default' | 'unsupported', string> = {
  granted: 'Đã được phép hiện thông báo — hết giờ sẽ được gọi cả khi tab đang ẩn.',
  default: 'Quyền sẽ được xin khi bạn bật thông báo hoặc bắt đầu phiên đầu tiên.',
  denied: 'Bị trình duyệt chặn — mở quyền thông báo của trang web để dùng được.',
  unsupported: 'Trình duyệt không hỗ trợ Notification — chỉ dùng được chuông trong trang.',
}

function toggleAlert(key: AlertKey): void {
  if (key === 'sound') unlockAudio()
  const next = !game.alerts.value[key]
  game.updateAlerts({ [key]: next })
  if (key === 'notify' && next) void ensureNotifyPermission()
}

/* ---------- bản lưu pet (xuất/nhập JSON) ---------- */
const fileInput = ref<HTMLInputElement | null>(null)
const confirmingImport = ref(false)
let importRevert: ReturnType<typeof setTimeout> | null = null

function onExport(): void {
  const docs = game.exportDocs()
  if (!docs) return
  try {
    const blob = new Blob([JSON.stringify(docs, null, 2)], { type: 'application/json' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `petpomodoro-backup-${new Date().toISOString().slice(0, 10)}.json`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
    game.notify('Đã xuất bản lưu — giữ file này ở nơi an toàn nhé!')
  } catch {
    game.notify('Không xuất được bản lưu — thử lại nhé.')
  }
}

/** Nhập 2 bước: bấm lần 1 hiện cảnh báo ghi đè, lần 2 mới mở file picker. */
function triggerImport(): void {
  if (!confirmingImport.value) {
    confirmingImport.value = true
    importRevert = setTimeout(() => {
      confirmingImport.value = false
    }, 3000)
    return
  }
  if (importRevert) clearTimeout(importRevert)
  confirmingImport.value = false
  fileInput.value?.click()
}

async function onFile(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = '' // chọn lại đúng file cũ vẫn fires change
  if (!file) return
  try {
    const docs = JSON.parse(await file.text())
    if (game.importDocs(docs)) game.notify('Đã nhập bản lưu — chào mừng bạn và bé trở lại!')
    else game.notify('File không đúng bản lưu của PetPomodoro — chưa đổi gì.')
  } catch {
    game.notify('File đọc không được — chưa đổi gì.')
  }
}

const ROWS: {
  key: SettingKey
  label: string
  hint: string
  unit: string
  emoji: string
  bg: string
}[] = [
  {
    key: 'focusMinutes',
    label: 'TẬP TRUNG',
    hint: 'Một phiên tập · mỗi phút tập được +1 XP',
    unit: 'phút',
    emoji: '🎯',
    bg: 'var(--red)',
  },
  {
    key: 'breakMinutes',
    label: 'NGHỈ NGẮN',
    hint: 'Nghỉ ngắn giữa các phiên tập',
    unit: 'phút',
    emoji: '☕',
    bg: 'var(--blue)',
  },
  {
    key: 'longBreakMinutes',
    label: 'NGHỈ DÀI',
    hint: 'Nghỉ lớn sau khi đủ chuỗi phiên tập',
    unit: 'phút',
    emoji: '🛋️',
    bg: 'var(--purple)',
  },
  {
    key: 'sessionsBeforeLongBreak',
    label: 'PHIÊN TRƯỚC NGHỈ DÀI',
    hint: 'Tập đủ số phiên này → lần nghỉ kế tiếp là nghỉ dài',
    unit: 'phiên',
    emoji: '🔁',
    bg: 'var(--olive)',
  },
]

function bump(key: SettingKey, dir: 1 | -1): void {
  const { step, min, max } = SETTING_LIMITS[key]
  game.updateSettings({ [key]: clampSetting(key, cfg.value[key] + dir * step) })
}

function atMin(key: SettingKey): boolean {
  return cfg.value[key] <= SETTING_LIMITS[key].min
}

function atMax(key: SettingKey): boolean {
  return cfg.value[key] >= SETTING_LIMITS[key].max
}

function resetDefaults(): void {
  game.updateSettings(defaultSettings())
  game.notify('Đã khôi phục mặc định: 25 tập · 5 nghỉ · 15 nghỉ dài · 4 phiên')
}
</script>

<template>
  <div>
    <h2 class="f-bungee screen-title">CÀI ĐẶT POMODORO</h2>
    <p class="f-mono screen-sub">
      Tuỳ chỉnh nhịp tập của bé linh thú. Đổi cài đặt áp dụng cho phiên kế tiếp — phiên đang
      chạy giữ nguyên độ dài.
    </p>

    <!-- từng dòng cài đặt: − giá trị + -->
    <div
      v-for="(row, i) in ROWS"
      :key="row.key"
      class="mcard"
      :style="{ '--rot': `${i % 2 === 0 ? -0.6 : 0.7}deg`, marginTop: i === 0 ? '4px' : '14px' }"
    >
      <div style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap">
        <div style="flex: 1; min-width: 170px">
          <div class="f-bungee" :style="{ fontSize: '12px', color: row.bg }">
            {{ row.emoji }} {{ row.label }}
          </div>
          <div class="f-mono" style="font-size: 10.5px; opacity: 0.7; margin-top: 3px">
            {{ row.hint }}
          </div>
        </div>
        <div style="display: flex; align-items: center; gap: 8px">
          <button
            class="rbtn rbtn-sm"
            :disabled="atMin(row.key)"
            aria-label="Giảm"
            @click="bump(row.key, -1)"
          >−</button>
          <div
            class="f-bungee"
            style="
              min-width: 96px;
              text-align: center;
              border: 3px solid var(--ink);
              background: var(--cream2);
              padding: 7px 6px;
              font-size: 15px;
              box-shadow: inset 2px 2px 0 rgba(38, 32, 29, 0.12);
            "
            :aria-label="`${row.label} hiện tại`"
          >
            {{ cfg[row.key] }} <span class="f-mono" style="font-size: 10px">{{ row.unit }}</span>
          </div>
          <button
            class="rbtn rbtn-sm"
            :disabled="atMax(row.key)"
            aria-label="Tăng"
            @click="bump(row.key, 1)"
          >+</button>
        </div>
      </div>
    </div>

    <!-- âm thanh & thông báo -->
    <div class="mcard" style="--rot: -0.4deg; margin-top: 20px">
      <div class="f-bungee" style="font-size: 12px">🔔 ÂM THANH &amp; THÔNG BÁO</div>
      <div
        v-for="row in ALERT_ROWS"
        :key="row.key"
        style="display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-top: 10px"
      >
        <div style="flex: 1; min-width: 170px">
          <div class="f-bungee" :style="{ fontSize: '12px', color: row.bg }">
            {{ row.emoji }} {{ row.label }}
          </div>
          <div class="f-mono" style="font-size: 10.5px; opacity: 0.7; margin-top: 3px">
            {{ row.hint }}
          </div>
        </div>
        <button
          class="rbtn rbtn-sm"
          :class="game.alerts.value[row.key] ? 'rbtn-olive' : 'rbtn-ghost'"
          :aria-pressed="game.alerts.value[row.key]"
          @click="toggleAlert(row.key)"
        >
          {{ game.alerts.value[row.key] ? 'Đang bật' : 'Đang tắt' }}
        </button>
      </div>
      <div class="f-mono" style="font-size: 10px; opacity: 0.65; margin-top: 10px">
        {{ NOTIFY_HINT[notifyPerm] }}
      </div>
    </div>

    <!-- xem trước chu kỳ -->
    <div class="mcard tape" style="--rot: 0.8deg; margin-top: 20px; text-align: center">
      <div class="f-bungee" style="font-size: 12px">CHU KỲ CỦA BẠN</div>
      <div class="f-mono" style="font-size: 13px; margin-top: 8px; line-height: 1.7">
        <span class="chip" style="--rot: -1.5deg; background: var(--red); color: var(--paper)">
          🎯 tập {{ game.mmss(focusMsOf(cfg)) }}
        </span>
        →
        <span class="chip" style="--rot: 1deg; background: var(--blue); color: var(--paper)">
          ☕ nghỉ {{ game.mmss(breakMsOf(cfg, false)) }}
        </span>
        <span style="opacity: 0.75">× {{ cfg.sessionsBeforeLongBreak }} lần</span>
        <br />
        →
        <span class="chip" style="--rot: -1deg; background: var(--purple); color: var(--paper)">
          🛋️ nghỉ dài {{ game.mmss(breakMsOf(cfg, true)) }}
        </span>
      </div>
      <div class="f-mono" style="font-size: 10.5px; opacity: 0.65; margin-top: 8px">
        Một vòng chu kỳ ≈
        {{
          cfg.sessionsBeforeLongBreak * cfg.focusMinutes +
            (cfg.sessionsBeforeLongBreak - 1) * cfg.breakMinutes +
            cfg.longBreakMinutes
        }}
        phút
      </div>
    </div>

    <!-- bản lưu pet -->
    <div class="mcard tape" style="--rot: 0.6deg; margin-top: 20px">
      <div class="f-bungee" style="font-size: 12px">💾 BẢN LƯU PET</div>
      <div class="f-mono" style="font-size: 10.5px; opacity: 0.7; margin-top: 4px">
        Bé sống trong trình duyệt này — xuất file để phòng lúc xoá dữ liệu hoặc chuyển máy.
      </div>
      <div style="display: flex; gap: 8px; margin-top: 10px; flex-wrap: wrap">
        <button class="rbtn rbtn-blue rbtn-sm" style="flex: 1; min-width: 150px" @click="onExport">
          ⬇ Xuất bản lưu
        </button>
        <button
          class="rbtn rbtn-sm"
          :class="confirmingImport ? 'rbtn-red' : 'rbtn-ghost'"
          style="flex: 1; min-width: 150px"
          @click="triggerImport"
        >
          {{ confirmingImport ? '! Ghi đè bé hiện tại?' : '⬆ Nhập bản lưu' }}
        </button>
        <input
          ref="fileInput"
          type="file"
          accept="application/json,.json"
          style="display: none"
          @change="onFile"
        />
      </div>
    </div>

    <div style="display: flex; gap: 8px; margin-top: 16px; flex-wrap: wrap">
      <button class="rbtn rbtn-ghost rbtn-sm" style="flex: 1" @click="resetDefaults">
        ↺ Khôi phục mặc định (25 · 5 · 15 · 4)
      </button>
    </div>
  </div>
</template>
