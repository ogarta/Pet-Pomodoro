<script setup lang="ts">
/**
 * PetPomodoro M1 — KB1 "Hành Trình Tiến Hoá".
 * 1 page, tab bar dưới (Trứng chỉ khi chưa nhận / Tập / Tiến hoá / Stats / Cài đặt),
 * DevPanel chỉ khi ?dev=1, EvoOverlay khi vượt ngưỡng tiến hoá thật.
 */
import { computed, onMounted, ref, watch, watchEffect } from 'vue'
import { useRoute } from '#imports'
import { useGame } from '~/composables/useGame'
import { unlockAudio } from '~/utils/alerts'
import type { TabDef } from '~/components/AppTabBar.vue'

const route = useRoute()
const game = useGame()

const tab = ref<'egg' | 'tap' | 'evo' | 'stats' | 'set'>('tap')

const showDev = computed(() => route.query.dev === '1')

const tabs = computed<TabDef[]>(() => {
  const t: TabDef[] = []
  if (!game.hasPet.value) t.push({ id: 'egg', label: 'Trứng' })
  else
    t.push(
      { id: 'tap', label: 'Tập' },
      { id: 'evo', label: 'Tiến hoá' },
      { id: 'stats', label: 'Stats' },
      { id: 'set', label: 'Cài đặt' },
    )
  return t
})

watch(
  () => game.hasPet.value,
  (has) => {
    tab.value = has ? 'tap' : 'egg'
  },
  { immediate: true },
)

/** Tiêu đề tab đếm ngược theo timer — nhìn tab là biết còn bao lâu, khỏi phải quay lại trang. */
const BASE_TITLE = 'PetPomodoro — Pomodoro nuôi pet'
watchEffect(() => {
  const flash = game.finishedFlash.value
  const t = game.timer.value
  const rem = game.remainingMs.value
  let title = BASE_TITLE
  if (flash) title = '🔔 Hết giờ rồi! — PetPomodoro'
  else if (t?.running && rem != null) {
    title = `${game.mmss(rem)} · ${t.phase === 'focus' ? 'Tập trung' : 'Nghỉ'} — PetPomodoro`
  }
  if (import.meta.client) document.title = title
})

/** Chạm đầu tiên → mở khóa AudioContext (autoplay policy) để chuông cuối giờ kêu được. */
onMounted(() => {
  document.addEventListener('pointerdown', unlockAudio, { once: true, capture: true })
})
</script>

<template>
  <div>
    <!-- splash cho tới khi load xong docs -->
    <div v-if="!game.ready.value" class="app-shell" style="text-align: center; padding-top: 40vh">
      <div class="f-bungee" style="font-size: 18px">PetPomodoro</div>
      <div class="f-mono" style="font-size: 12px; margin-top: 6px; opacity: 0.7">đang mở nhật ký pet…</div>
    </div>

    <div v-else class="app-shell">
      <header style="margin-bottom: 10px">
        <div class="f-bungee" style="font-size: 15px; letter-spacing: 1px">
          PET<span style="color: var(--red)">POMODORO</span>
        </div>
      </header>

      <EggScreen v-if="tab === 'egg' || !game.hasPet.value" />
      <FocusScreen v-else-if="tab === 'tap'" />
      <EvolveScreen v-else-if="tab === 'evo'" />
      <StatsScreen v-else-if="tab === 'stats'" />
      <SettingsScreen v-else />

      <AppTabBar v-model="tab" :tabs="tabs" />
    </div>

    <!-- toast -->
    <div class="toast-layer" aria-live="polite">
      <div v-for="t in game.toasts.value" :key="t.id" class="toast">{{ t.text }}</div>
    </div>

    <!-- khoảnh khắc tiến hoá (flash + morph + confetti) -->
    <EvoOverlay />

    <!-- DEV QA — chỉ khi ?dev=1 -->
    <DevPanel :visible="showDev" />
  </div>
</template>
