<script setup lang="ts">
/** GoalDots — mục tiêu ngày 3 phiên (spec §3): dot đủ = đỏ, thiếu = rỗng. */
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{ goal?: number; done?: number }>(),
  { goal: 3, done: 0 },
)
const dots = computed(() => Array.from({ length: props.goal }, (_, i) => i < props.done))
</script>

<template>
  <span style="display: inline-flex; align-items: center; gap: 5px" aria-label="Mục tiêu ngày">
    <span
      v-for="(on, i) in dots"
      :key="i"
      :style="{
        width: '13px',
        height: '13px',
        borderRadius: '50%',
        border: '2.5px solid var(--ink)',
        background: on ? 'var(--red)' : 'var(--paper)',
        boxShadow: on ? '1.5px 1.5px 0 var(--red2)' : 'none',
        display: 'inline-block',
      }"
    />
  </span>
</template>
