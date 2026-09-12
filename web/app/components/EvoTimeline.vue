<script setup lang="ts">
/**
 * EvoTimeline — timeline tiến hoá render TỪ CATALOG (trứng → 3 stage),
 * form hiện tại nổi lên viền đỏ, form chưa tới mờ đi (spec KB1 ③).
 */
import { computed } from 'vue'
import { EGG_STAGE_ID, type SpeciesLine } from '#shared/game/catalog'

const props = defineProps<{
  line: SpeciesLine
  currentStageId: string
}>()

const steps = computed(() => [
  { id: EGG_STAGE_ID, name: 'Trứng', atLevel: 0 as const, sprite: null, desc: 'Trứng nguyên tố' },
  ...props.line.stages.map((st) => ({ id: st.id, name: st.name, atLevel: st.atLevel as number, sprite: st.sprite, desc: st.desc })),
])
</script>

<template>
  <div style="display: flex; align-items: center; justify-content: space-between; gap: 4px; margin-top: 10px">
    <template v-for="(st, i) in steps" :key="st.id">
      <div
        :style="{
          textAlign: 'center',
          padding: '5px 3px 3px',
          border: currentStageId === st.id ? '3px solid var(--red)' : '3px solid transparent',
          background: currentStageId === st.id ? '#FBF0D2' : 'transparent',
          boxShadow: currentStageId === st.id ? '3px 3px 0 var(--red2)' : 'none',
          opacity: currentStageId === st.id ? 1 : 0.5,
          transform: currentStageId === st.id ? 'translateY(-3px)' : 'none',
        }"
      >
        <div v-if="i === 0" style="font-size: 30px; line-height: 1; padding: 7px 0" aria-label="Trứng">🥚</div>
        <PetStage
          v-else
          :src="st.sprite"
          :alt="st.name"
          :h="52 + i * 4"
          :nat-w="line.stages[i - 1]!.natW"
          :nat-h="line.stages[i - 1]!.natH"
          lite
          lock-label="?"
        />
        <div class="f-mono" style="font-size: 10.5px; font-weight: 700; margin-top: 3px; line-height: 1.25">
          {{ st.name }}
        </div>
        <div class="f-mono" style="font-size: 9.5px; opacity: 0.7">
          {{ i === 0 ? 'khởi đầu' : `Lv.${st.atLevel}` }}
        </div>
      </div>
      <svg
        v-if="i < steps.length - 1"
        width="15"
        height="14"
        viewBox="0 0 22 18"
        style="flex-shrink: 0"
        aria-hidden="true"
      >
        <path d="M2 9h14M12 3l6 6-6 6" fill="none" stroke="var(--ink)" stroke-width="3" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </template>
  </div>
</template>
