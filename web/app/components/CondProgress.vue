<script setup lang="ts">
/** CondProgress — 1 điều kiện registry: text + tiến độ (cur/target). */
import { computed } from 'vue'
import { condProgress, condView, describeCond, type Cond } from '#shared/game/conditions'
import type { GameState } from '#shared/game/types'

const props = defineProps<{
  cond: Cond
  state: GameState
  light?: boolean
}>()

const view = computed(() => condView(props.state))
const progress = computed(() => condProgress(view.value, props.cond))
const pct = computed(() =>
  Math.max(0, Math.min(100, (progress.value.cur / Math.max(1, progress.value.target)) * 100)),
)
const done = computed(() => progress.value.cur >= progress.value.target)
</script>

<template>
  <div>
    <div
      class="f-mono"
      :style="{
        fontSize: '11.5px',
        fontWeight: 700,
        color: light ? 'var(--paper)' : 'var(--ink)',
      }"
    >
      {{ describeCond(cond) }}
      <span :style="{ color: done ? '#B8E88A' : light ? 'var(--mustard)' : 'var(--red)' }">
        ({{ Math.min(progress.cur, progress.target) }}/{{ progress.target }})
      </span>
    </div>
    <div
      :style="{
        border: '2px solid',
        borderColor: light ? 'var(--paper)' : 'var(--ink)',
        background: 'rgba(38,32,29,.18)',
        height: '9px',
        marginTop: '4px',
        position: 'relative',
      }"
    >
      <div
        :style="{
          height: '100%',
          width: `${pct}%`,
          background: done ? '#B8E88A' : 'var(--mustard)',
          transition: 'width .3s ease',
        }"
      />
    </div>
  </div>
</template>
