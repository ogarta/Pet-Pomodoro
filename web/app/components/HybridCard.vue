<script setup lang="ts">
/**
 * HybridCard — form lai hiếm (spec §2.3): điều kiện KÉP power≥85 ∧ streak≥10,
 * chưa mở → silhouette + tag "HIẾM"; đọc điều kiện từ registry.
 */
import { computed } from 'vue'
import type { GameState } from '#shared/game/types'
import { lineById, type HybridDef } from '#shared/game/catalog'
import { condView, evalCond } from '#shared/game/conditions'

const props = defineProps<{
  hybrid: HybridDef
  state: GameState
  rotate?: number
}>()

const elLabel = (id: string): string => {
  try {
    return lineById(id).elementLabel
  } catch {
    return id
  }
}

const view = computed(() => condView(props.state))
const achieved = computed(() => evalCond(view.value, props.hybrid.cond))
const unlocked = computed(() => props.state.systems.evolution.unlocked.includes(props.hybrid.id))
</script>

<template>
  <div
    class="mcard"
    :style="{ '--rot': `${rotate ?? 0}deg`, padding: '9px', position: 'relative' }"
  >
    <span
      class="chip"
      style="--rot: -4deg; position: absolute; top: -11px; right: 8px; background: var(--purple); color: var(--paper); padding: 1px 7px; font-size: 10px"
    >
      HIẾM
    </span>
    <div style="display: flex; gap: 10px; align-items: center">
      <PetStage
        :src="unlocked ? hybrid.sprite : null"
        :alt="hybrid.name"
        :h="64"
        lite
        lock-label="???"
      />
      <div style="flex: 1; min-width: 0">
        <div class="f-serif" style="font-size: 16.5px">{{ hybrid.name }}</div>
        <div class="f-mono" style="font-size: 10.5px; opacity: 0.75; margin: 2px 0 6px">
          {{ elLabel(hybrid.elements[0]) }} × {{ elLabel(hybrid.elements[1]) }} · {{ hybrid.desc }}
        </div>
        <CondProgress :cond="hybrid.cond" :state="state" />
        <div v-if="unlocked" class="f-bungee" style="font-size: 11px; color: var(--olive); margin-top: 5px">
          ĐÃ SƯU TẦM
        </div>
      </div>
    </div>
  </div>
</template>
