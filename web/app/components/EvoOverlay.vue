<script setup lang="ts">
/**
 * EvoOverlay — khoảnh khắc tiến hoá khi VƯỢT NGƯỠNG THẬT (Lv.8/Lv.16):
 * lóe sáng (evoFlash) + morph rung (evoWiggle) + confetti, chốt bằng nút.
 */
import { computed } from 'vue'
import { lineById, stageById } from '#shared/game/catalog'
import { useGame } from '~/composables/useGame'

const game = useGame()

const info = computed(() => {
  const fx = game.evolvedFx.value
  if (!fx || !game.state.value) return null
  try {
    const line = lineById(game.state.value.pet.speciesId)
    const from = stageById(line, fx.from)
    const to = stageById(line, fx.to)
    return { from, to, fromSprite: from.sprite, toSprite: to.sprite }
  } catch {
    return null
  }
})
</script>

<template>
  <div v-if="info && game.evolvedFx.value" class="evo-overlay" role="dialog" aria-label="Tiến hoá">
    <span class="evo-flash-ring" />
    <ConfettiBurst :count="34" />

    <div class="f-bungee" style="color: var(--mustard); font-size: 19px; letter-spacing: 1.5px; z-index: 2">
      TIẾN HOÁ!
    </div>

    <div style="position: relative; z-index: 2; display: flex; align-items: center; gap: 18px">
      <div class="old-ghost" style="position: relative">
        <PetStage :src="info.fromSprite" :alt="info.from.name" :h="110" :nat-w="info.from.natW" :nat-h="info.from.natH" />
      </div>
      <svg width="34" height="30" viewBox="0 0 22 18" style="flex-shrink: 0">
        <path d="M2 9h14M12 3l6 6-6 6" fill="none" stroke="var(--mustard)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
      <div class="evo-wiggle">
        <PetStage :src="info.toSprite" :alt="info.to.name" :h="150" :nat-w="info.to.natW" :nat-h="info.to.natH" />
      </div>
    </div>

    <div style="text-align: center; z-index: 2">
      <div class="f-serif" style="color: var(--paper); font-size: 26px">
        {{ info.from.name }} tiến hoá thành <em>{{ info.to.name }}</em>!
      </div>
      <div class="f-mono" style="color: var(--cream2); font-size: 12px; margin-top: 5px">
        {{ info.to.desc }}
      </div>
    </div>

    <button class="rbtn rbtn-red" style="z-index: 2" @click="game.dismissEvolvedFx()">Tuyệt vời!</button>
  </div>
</template>
