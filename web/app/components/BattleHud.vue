<script setup lang="ts">
/**
 * BattleHUD Gen-1 (spec §2.5): tên + Lv + thanh TT kiểu Pokémon —
 * xanh lá ≥50% · vàng 20–49% · đỏ <20%; ốm (critical) → đỏ + nhấp nháy.
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    name: string
    level: number
    value: number
    max?: number
    critical?: boolean
  }>(),
  { max: 100, critical: false },
)

const SEGS = 10
const pct = computed(() => (props.value / props.max) * 100)
const filled = computed(() => Math.max(0, Math.min(SEGS, Math.round((pct.value / 100) * SEGS))))
const hpColor = computed(() => {
  if (props.critical) return '#D84028'
  if (pct.value >= 50) return '#4CA83D'
  if (pct.value >= 20) return '#E8B830'
  return '#D84028'
})
</script>

<template>
  <div
    :style="{
      background: 'var(--paper)',
      border: '3px solid var(--ink)',
      boxShadow: '4px 4px 0 var(--ink)',
      padding: '4px 9px 5px',
      minWidth: '172px',
      display: 'inline-block',
    }"
  >
    <div style="display: flex; justify-content: space-between; align-items: baseline; gap: 10px">
      <span class="f-mono" style="font-size: 13.5px; font-weight: 700; letter-spacing: 1px">{{ name }}</span>
      <span class="f-mono" style="font-size: 11px; font-weight: 700">Lv.{{ level }}</span>
    </div>
    <div style="display: flex; align-items: center; gap: 6px; margin-top: 4px">
      <span class="f-mono" title="TT = Thể trạng" style="font-size: 11px; font-weight: 700">TT</span>
      <div
        style="display: flex; gap: 2px; flex: 1; border: 2px solid var(--ink); padding: 2px; background: var(--cream2)"
      >
        <div
          v-for="i in SEGS"
          :key="i"
          :class="{ 'hud-blink': critical && i <= filled }"
          :style="{
            flex: 1,
            height: '8px',
            background: i <= filled ? hpColor : 'rgba(38,32,29,.12)',
          }"
        />
      </div>
    </div>
    <div class="f-mono" style="font-size: 10.5px; font-weight: 700; text-align: right; margin-top: 3px">
      {{ value }}/{{ max }}
    </div>
  </div>
</template>
