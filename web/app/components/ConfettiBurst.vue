<script setup lang="ts">
/** ConfettiBurst — SVG confetti DNA demo KB1 (màu Memphis, rơi + xoay). */
import { computed } from 'vue'

const props = withDefaults(defineProps<{ count?: number }>(), { count: 26 })

const COLORS = ['#BC3A28', '#E3A72F', '#2B54A3', '#6B4C9A', '#5F6B2F', '#E89BB0']

const pieces = computed(() =>
  Array.from({ length: props.count }, (_, i) => ({
    id: i,
    x: Math.round((i * 97) % 100), // pseudo-random ổn định (không hydration mismatch)
    delay: Math.round(((i * 53) % 40)) / 10,
    dur: 1.4 + (((i * 31) % 10) / 10),
    color: COLORS[i % COLORS.length],
    w: 5 + ((i * 13) % 6),
    h: 8 + ((i * 7) % 8),
    round: i % 3 === 0,
  })),
)
</script>

<template>
  <div aria-hidden="true" style="position: absolute; inset: 0; overflow: hidden; pointer-events: none">
    <span
      v-for="p in pieces"
      :key="p.id"
      :style="{
        position: 'absolute',
        left: `${p.x}%`,
        top: '-4%',
        width: `${p.w}px`,
        height: `${p.h}px`,
        background: p.color,
        border: '2px solid var(--ink)',
        borderRadius: p.round ? '50%' : '1px',
        animation: `confettiFall ${p.dur}s linear ${p.delay}s infinite`,
      }"
    />
  </div>
</template>
