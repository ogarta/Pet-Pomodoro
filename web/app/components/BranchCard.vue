<script setup lang="ts">
/**
 * BranchCard — thẻ điều kiện nhánh Stage 3, ĐỌC TỪ REGISTRY COND
 * (Hiền: and[Lv≥16, streak≥7] · Chiến Binh: and[Lv≥16, power≥80] — spec §2.2).
 */
import { computed } from 'vue'
import type { BranchDef } from '#shared/game/catalog'
import { condView, evalCond } from '#shared/game/conditions'
import type { GameState } from '#shared/game/types'

const props = defineProps<{
  branch: BranchDef
  state: GameState
  rotate?: number
}>()

const view = computed(() => condView(props.state))
const achieved = computed(() => evalCond(view.value, props.branch.cond))
const currentBranch = computed(() => props.state.systems.evolution.branchId === props.branch.id)
</script>

<template>
  <div
    class="mcard"
    :style="{
      '--rot': `${rotate ?? 0}deg`,
      flex: 1,
      background: achieved ? 'var(--mustard)' : 'var(--paper)',
      boxShadow: currentBranch ? '5px 5px 0 var(--red2)' : undefined,
      borderColor: currentBranch ? 'var(--red)' : undefined,
      padding: '9px',
    }"
  >
    <div style="display: flex; justify-content: space-between; align-items: center; gap: 6px">
      <span class="f-bungee" style="font-size: 12.5px">{{ branch.name }}</span>
      <span v-if="currentBranch" class="chip" style="--rot: 3deg; background: var(--red); color: var(--paper); padding: 1px 6px; font-size: 10px">ĐANG GIỮ</span>
      <span v-else-if="achieved" class="chip" style="--rot: -3deg; background: var(--olive); color: var(--paper); padding: 1px 6px; font-size: 10px">ĐẠT</span>
    </div>
    <div class="f-serif" style="font-size: 15px; margin: 2px 0 3px">{{ branch.formName }}</div>
    <div class="f-mono" style="font-size: 10.5px; opacity: 0.8; margin-bottom: 7px">{{ branch.meaning }}</div>
    <CondProgress :cond="branch.cond" :state="state" />
  </div>
</template>
