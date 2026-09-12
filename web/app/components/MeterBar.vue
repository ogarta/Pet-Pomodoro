<script setup lang="ts">
/**
 * MeterBar — thanh chỉ số với vạch cap theo level (Sức mạnh 62/64 · vạch cap 64).
 * Dùng chung cho XP (cap = need) và power (cap = min(100, 40+6L)).
 */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    label: string
    value: number
    max: number
    cap?: number | null
    color?: string
    note?: string
  }>(),
  { cap: null, color: '#E3A72F', note: '' },
)

const pct = computed(() => Math.max(0, Math.min(100, (props.value / Math.max(1, props.max)) * 100)))
const capPct = computed(() =>
  props.cap != null ? Math.max(0, Math.min(100, (props.cap / Math.max(1, props.max)) * 100)) : null,
)
</script>

<template>
  <div>
    <div style="display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 4px">
      <span class="f-mono" style="font-size: 12px; font-weight: 700; letter-spacing: 0.4px">{{ label }}</span>
      <span class="f-mono" style="font-size: 12px; font-weight: 700">{{ value }}/{{ max }}</span>
    </div>
    <div class="track">
      <div class="fill" :style="{ width: `${pct}%`, background: color }" />
      <template v-if="capPct !== null">
        <span class="cap-tick" :style="{ left: `${capPct}%` }" />
        <span class="cap-label" :style="{ left: `${capPct}%` }">TỐI ĐA</span>
      </template>
    </div>
    <div v-if="note" class="f-mono" style="font-size: 10.5px; opacity: 0.7; margin-top: 3px">{{ note }}</div>
  </div>
</template>
