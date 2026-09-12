<script setup lang="ts">
/**
 * Màn TẬP (home — KB1 ②): timer pomodoro thật (độ dài theo màn Cài đặt), pet sprite +
 * BattleHUD, Tập = pomodoro + linh thú: timer, pet + HUD, goal dots (chỉ số ở màn Tiến hoá),
 * cho ăn (−1 sushi · +5 XP · +15 TT — cho ăn khi ốm vẫn hồi phục).
 * Sau N phiên focus (settings) → nút nghỉ chuyển thành NGHỈ DÀI.
 */
import { computed, ref } from 'vue'
import { lineById, nextStage, stageById } from '#shared/game/catalog'
import { condView } from '#shared/game/conditions'
import {
  GOAL_SESSIONS,
  breakMsOf,
  defaultSettings,
  focusMsOf,
  statusBand,
  STATUS_LABEL,
} from '#shared/game/formulas'
import { useGame } from '~/composables/useGame'
import { ensureNotifyPermission, unlockAudio } from '~/utils/alerts'

const game = useGame()
const state = computed(() => game.state.value as NonNullable<typeof game.state.value>)
const cfg = computed(() => game.settings.value ?? defaultSettings())
const line = computed(() => lineById(state.value.pet.speciesId))
const stage = computed(() => stageById(line.value, state.value.pet.stageId))
const band = computed(() => statusBand(state.value.pet.condition))
const sick = computed(() => band.value === 'omyeu')
const view = computed(() => condView(state.value))
const upNext = computed(() => nextStage(line.value, state.value.pet.level))

const hudName = computed(() => {
  const branch = state.value.systems.evolution.branchId
  if (branch && state.value.pet.level >= 16) {
    const b = line.value.branches.find((x) => x.id === branch)
    if (b) return b.formName.toUpperCase()
  }
  return stage.value.name.toUpperCase()
})

/* timer display — total bám phiên đang chạy (totalMs), idle bám settings */
const remaining = computed(() => game.remainingMs.value)
const t = computed(() => game.timer.value)
const displayMs = computed(() => (t.value ? (remaining.value ?? 0) : focusMsOf(cfg.value)))
const timeText = computed(() => game.mmss(displayMs.value))
const phaseLabel = computed(() => {
  if (!t.value) return 'SẴN SÀNG TẬP?'
  if (t.value.phase === 'focus') return 'TẬP TRUNG'
  // nghỉ dài đang chạy: streak đã reset nên dò qua độ dài phiên
  return t.value.totalMs === breakMsOf(cfg.value, true) ? 'NGHỈ DÀI' : 'NGHỈ XÌN'
})
const phaseColor = computed(() => (!t.value ? 'var(--olive)' : t.value.phase === 'focus' ? 'var(--red)' : 'var(--blue)'))
const progressPct = computed(() => {
  const total = t.value ? t.value.totalMs : focusMsOf(cfg.value)
  return Math.max(0, Math.min(100, 100 - (displayMs.value / total) * 100))
})
/* chu kỳ nghỉ dài: còn bao nhiêu phiên focus nữa đến lượt nghỉ dài */
const untilLong = computed(() => {
  const n = cfg.value.sessionsBeforeLongBreak
  return n - (game.focusStreak.value % n)
})

/* feed fx */
const hearts = ref(0)
function onFeed(): void {
  if (game.feedPet()) {
    hearts.value++
    setTimeout(() => hearts.value--, 1600)
  } else {
    game.notify('Hết sushi! Hoàn thành phiên để nhận thêm.')
  }
}

/* nút bấm có gắn mở khóa âm thanh (autoplay policy) + xin quyền thông báo đúng lúc */
function onStart(): void {
  unlockAudio()
  if (game.alerts.value.notify) void ensureNotifyPermission()
  game.startFocus()
}
function onResume(): void {
  unlockAudio()
  game.resumeTimer()
}
function onBreak(): void {
  unlockAudio()
  game.startBreak(game.longBreakDue.value)
}

/* hủy phiên 2 bước — tránh mis-click mất cả phiên 25 phút */
const confirmingAbort = ref(false)
let abortRevert: ReturnType<typeof setTimeout> | null = null
function onAbort(): void {
  if (!confirmingAbort.value) {
    confirmingAbort.value = true
    abortRevert = setTimeout(() => {
      confirmingAbort.value = false
    }, 3000)
    return
  }
  if (abortRevert) clearTimeout(abortRevert)
  confirmingAbort.value = false
  game.abortTimer()
}
</script>

<template>
  <div>
    <div style="display: flex; gap: 7px; flex-wrap: wrap; margin-bottom: 10px">
      <span class="chip" style="--rot: -1.2deg; background: var(--mustard)">
        {{ line.emoji }} {{ stage.name }} · {{ line.elementLabel }}
      </span>
      <span
        class="chip"
        :style="{
          '--rot': '1deg',
          background: band === 'khoemanh' ? 'var(--olive)' : band === 'omyeu' ? 'var(--red)' : 'var(--cream2)',
          color: band === 'binhthuong' ? 'var(--ink)' : 'var(--paper)',
        }"
      >
        {{ STATUS_LABEL[band] }}
      </span>
      <span class="chip" style="--rot: -0.6deg; background: var(--paper)">🍣 sushi × {{ state.pet.coins }}</span>
    </div>

    <!-- banner ốm -->
    <div
      v-if="sick"
      class="mcard"
      style="--rot: -0.8deg; background: var(--red); color: var(--paper); margin-bottom: 12px"
    >
      <div class="f-bungee" style="font-size: 12.5px">ỐM YẾU — NGOẠI HÌNH RÙ XUỐNG</div>
      <div class="f-mono" style="font-size: 11.5px; margin-top: 3px">
        {{ stage.name }} vẫn giữ Lv.{{ state.pet.level }} (XP không mất) nhưng thể trạng {{ state.pet.condition }}/100.
        Cho ăn (+15) và duy trì phiên mỗi ngày để hồi phục.
      </div>
    </div>

    <!-- pet + HUD -->
    <div style="display: flex; flex-direction: column; align-items: center; gap: 12px; position: relative">
      <div style="position: relative">
        <PetStage
          :src="stage.sprite"
          :alt="stage.name"
          :nat-w="stage.natW"
          :nat-h="stage.natH"
          :h="230"
          :sick="sick"
          :bounce="!sick"
          :rotate="0.8"
        />
        <span
          v-for="i in hearts"
          :key="i"
          class="f-bungee"
          :style="{
            position: 'absolute',
            top: `${18 - i * 6}px`,
            right: '14%',
            fontSize: '20px',
            color: 'var(--red)',
            zIndex: 8,
            animation: 'heartFloat 1.5s ease-out both',
            pointerEvents: 'none',
          }"
        >♥</span>
      </div>
      <BattleHud :name="hudName" :level="state.pet.level" :value="state.pet.condition" :critical="sick" />
    </div>

    <!-- timer thật -->
    <div class="mcard" style="--rot: 0.5deg; margin-top: 16px; text-align: center; position: relative">
      <div class="f-bungee" :style="{ fontSize: '13px', color: phaseColor, letterSpacing: 1.5 }">
        {{ phaseLabel }}
      </div>
      <div
        class="f-mono"
        style="font-size: 56px; font-weight: 700; line-height: 1.05; font-variant-numeric: tabular-nums"
        :aria-label="`Thời gian còn lại ${timeText}`"
      >
        {{ timeText }}
      </div>
      <div class="track" style="height: 10px; margin: 4px 6px 10px">
        <div class="fill" :style="{ width: `${progressPct}%`, background: phaseColor }" />
      </div>
      <div style="display: flex; gap: 8px; justify-content: center; flex-wrap: wrap">
        <button v-if="!t" class="rbtn rbtn-red rbtn-sm" @click="onStart">
          ▶ Bắt đầu {{ game.mmss(focusMsOf(cfg)) }}
        </button>
        <template v-else-if="t.running">
          <button class="rbtn rbtn-sm" @click="game.pauseTimer()">Tạm dừng</button>
          <button
            class="rbtn rbtn-sm"
            :class="confirmingAbort ? 'rbtn-red' : 'rbtn-ghost'"
            @click="onAbort"
          >
            {{ confirmingAbort ? '! Chắc hủy? Bấm lần nữa' : '✕ Hủy phiên' }}
          </button>
        </template>
        <template v-else>
          <button class="rbtn rbtn-red rbtn-sm" @click="onResume">Tiếp tục</button>
          <button
            class="rbtn rbtn-sm"
            :class="confirmingAbort ? 'rbtn-red' : 'rbtn-ghost'"
            @click="onAbort"
          >
            {{ confirmingAbort ? '! Chắc hủy? Bấm lần nữa' : '✕ Hủy phiên' }}
          </button>
        </template>
        <button
          v-if="!t || t.phase === 'focus'"
          :class="game.longBreakDue.value ? 'rbtn rbtn-purple rbtn-sm' : 'rbtn rbtn-blue rbtn-sm'"
          @click="onBreak"
        >
          <template v-if="game.longBreakDue.value">🛋️ Nghỉ dài {{ game.mmss(breakMsOf(cfg, true)) }}</template>
          <template v-else>☕ Nghỉ {{ game.mmss(breakMsOf(cfg, false)) }}</template>
        </button>
      </div>
      <div class="f-mono" style="font-size: 10px; opacity: 0.65; margin-top: 7px">
        focus {{ game.mmss(focusMsOf(cfg)) }} → +{{ cfg.focusMinutes }} XP · +8 Sức mạnh · +1 sushi
      </div>
      <div
        class="f-mono"
        style="font-size: 10px; margin-top: 3px"
        :style="{ color: game.longBreakDue.value ? 'var(--purple)' : undefined, opacity: game.longBreakDue.value ? 1 : 0.65 }"
      >
        <template v-if="game.longBreakDue.value">ĐỦ {{ cfg.sessionsBeforeLongBreak }} PHIÊN — ĐẾN LƯỢT NGHỈ DÀI!</template>
        <template v-else>
          Chu kỳ: {{ cfg.sessionsBeforeLongBreak }} phiên tập → nghỉ dài {{ cfg.longBreakMinutes }} phút ·
          còn {{ untilLong }} phiên nữa
        </template>
      </div>
    </div>

    <!-- hôm nay + goal -->
    <div class="mcard tape" style="--rot: 0.8deg; margin-top: 18px">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 8px; flex-wrap: wrap">
        <span class="f-bungee" style="font-size: 12px">MỤC TIÊU NGÀY — 3 PHIÊN</span>
        <GoalDots :goal="GOAL_SESSIONS" :done="Math.min(state.pet.todaySessions, GOAL_SESSIONS)" />
      </div>
      <div class="f-mono" style="font-size: 12px; margin-top: 7px">
        Hôm nay: <b>{{ state.pet.todaySessions }}</b> phiên · <b>{{ state.pet.todayMinutes }}</b> phút ·
        <b>+{{ state.systems.stats.todayXp }}</b> XP
        <span v-if="state.pet.todaySessions >= GOAL_SESSIONS" class="chip" style="--rot: -2deg; background: var(--olive); color: var(--paper); margin-left: 6px; padding: 1px 6px; font-size: 10px">
          ĐỦ MỤC TIÊU ✓
        </span>
      </div>
      <div v-if="upNext" class="f-mono" style="font-size: 11px; margin-top: 6px; opacity: 0.8">
        Sắp tiến hoá: <b>{{ upNext.name }}</b> tại Lv.{{ upNext.atLevel }} (còn {{ upNext.atLevel - state.pet.level }} level)
      </div>
    </div>

    <!-- cho ăn -->
    <div class="mcard" style="--rot: -1deg; margin-top: 16px; display: flex; gap: 12px; align-items: center; flex-wrap: wrap">
      <button class="rbtn rbtn-olive" style="flex: 1; min-width: 180px" :disabled="state.pet.coins < 1" @click="onFeed">
        🍣 Cho ăn 1 sushi
      </button>
      <div class="f-mono" style="font-size: 11px; flex: 1; min-width: 150px">
        +5 XP · +15 thể trạng · {{ sick ? 'đang ốm — cho ăn để hồi phục nhanh' : 'bé thích lắm!' }}
      </div>
    </div>
  </div>
</template>
