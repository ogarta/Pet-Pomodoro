<script setup lang="ts">
import { computed } from 'vue'
import type { EggDef } from '#shared/game/catalog'

const props = defineProps<{
  egg: EggDef
  size?: number
}>()

const w = computed(() => props.size ?? 72)
const h = computed(() => Math.round(w.value * 1.22))

// chọn motif theo hệ: lửa / nước / lá
const motif = computed(() => {
  if (props.egg.spot === '#E8663C') return 'fire'
  if (props.egg.spot === '#2B54A3') return 'water'
  return 'leaf'
})
</script>

<template>
  <svg
    :width="w"
    :height="h"
    viewBox="0 0 50 61"
    role="img"
    aria-label="Trứng nguyên tố"
  >
    <path
      d="M25 3 C36 3 45 20 45 36 C45 50 36 58 25 58 C14 58 5 50 5 36 C5 20 14 3 25 3 Z"
      :fill="egg.base"
      stroke="#26201D"
      stroke-width="3"
    />
    <g v-if="motif === 'fire'">
      <path d="M25 16 C29 22 31 26 31 31 C31 36 28 39 25 39 C22 39 19 36 19 31 C19 26 21 22 25 16 Z" :fill="egg.spot" stroke="#26201D" stroke-width="2.5" />
      <path d="M25 24 C26.6 27 27.4 29 27.4 31.4 C27.4 33.8 26.2 35.4 25 35.4 C23.8 35.4 22.6 33.8 22.6 31.4 C22.6 29 23.4 27 25 24 Z" :fill="egg.motif" />
    </g>
    <g v-else-if="motif === 'water'">
      <path d="M12 34 C16 30 20 38 25 34 C30 30 34 38 38 34" fill="none" :stroke="egg.spot" stroke-width="3" stroke-linecap="round" />
      <path d="M14 43 C18 39 22 47 25 43 C28 39 32 47 36 43" fill="none" :stroke="egg.motif" stroke-width="3" stroke-linecap="round" />
      <circle cx="25" cy="20" r="5" :fill="egg.motif" stroke="#26201D" stroke-width="2.5" />
    </g>
    <g v-else>
      <path d="M25 14 C33 18 34 27 26 31 C24 25 23 19 25 14 Z" :fill="egg.spot" stroke="#26201D" stroke-width="2.5" />
      <path d="M25 20 C21 22 18 26 19 31 C23 29 25 25 25 20 Z" :fill="egg.motif" stroke="#26201D" stroke-width="2" />
    </g>
    <circle cx="14" cy="26" r="2.2" :fill="egg.spot" opacity="0.85" />
    <circle cx="36" cy="47" r="2.6" :fill="egg.spot" opacity="0.85" />
    <circle cx="33" cy="12" r="1.8" :fill="egg.spot" opacity="0.7" />
    <path d="M9 44 L14 46 L11 50" fill="none" stroke="#26201D" stroke-width="2" stroke-linecap="round" />
  </svg>
</template>
