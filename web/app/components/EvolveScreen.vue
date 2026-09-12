<script setup lang="ts">
/**
 * Màn TIẾN HOÁ (KB1 ③): timeline line hiện tại render từ CATALOG, panel nhánh
 * Stage 3 (Hiền vs Chiến Binh) + form lai — điều kiện đều ĐỌC TỪ REGISTRY COND.
 * Khoảnh khắc tiến hoá thật (vượt mốc level trong CATALOG) hiện qua EvoOverlay ở page gốc.
 */
import { computed } from 'vue'
import { HYBRIDS, lineById, nextStage, stageById } from '#shared/game/catalog'
import { powerCap, statusBand, STATUS_LABEL, xpNeed } from '#shared/game/formulas'
import { useGame } from '~/composables/useGame'

const game = useGame()
const state = computed(() => game.state.value as NonNullable<typeof game.state.value>)
const line = computed(() => lineById(state.value.pet.speciesId))
const stage = computed(() => stageById(line.value, state.value.pet.stageId))
const upNext = computed(() => nextStage(line.value, state.value.pet.level))
const cap = computed(() => powerCap(state.value.pet.level))
const need = computed(() => xpNeed(state.value.pet.level))
const condLabel = computed(() => STATUS_LABEL[statusBand(state.value.pet.condition)])
const branchHeld = computed(() =>
  state.value.systems.evolution.branchId
    ? line.value.branches.find((b) => b.id === state.value!.systems.evolution.branchId) ?? null
    : null,
)

// Bằng chứng data-driven: UI tiến hoá đọc thẳng từ CATALOG + registry COND
const relevantHybrids = computed(() =>
  HYBRIDS.filter((h) => h.elements.includes(state.value.pet.speciesId)),
)
</script>

<template>
  <div>
    <span class="chip" style="--rot: -1.3deg; background: var(--purple); color: var(--paper)">
      NHẬT KÝ TIẾN HOÁ · {{ line.name.toUpperCase() }}
    </span>

    <!-- form hiện tại -->
    <div class="mcard" style="--rot: 0.6deg; margin-top: 12px; text-align: center">
      <div style="display: flex; justify-content: center">
        <PetStage
          :src="stage.sprite"
          :alt="stage.name"
          :nat-w="stage.natW"
          :nat-h="stage.natH"
          :h="150"
          :sick="state.pet.condition < 30"
          bounce
          :rotate="-0.8"
        />
      </div>
      <div class="f-serif" style="font-size: 24px; margin-top: 8px">
        {{ branchHeld ? branchHeld.formName : stage.name }}
        <span v-if="branchHeld" class="chip" style="--rot: 2deg; background: var(--pink); padding: 1px 7px; font-size: 10px; vertical-align: middle">
          NHÁNH {{ branchHeld.name.toUpperCase() }}
        </span>
      </div>
      <div class="f-mono" style="font-size: 12.5px; margin-top: 6px">
        {{ stage.desc }} · mở ở Lv.{{ stage.atLevel }} · hiện tại Lv.{{ state.pet.level }}
      </div>
      <div v-if="upNext" class="chip" style="--rot: -1.5deg; background: var(--mustard); margin-top: 9px">
        SẮP TIẾN HOÁ: {{ upNext.name }} TẠI LV.{{ upNext.atLevel }} (CÒN {{ upNext.atLevel - state.pet.level }})
      </div>
      <div v-else class="chip" style="--rot: -1.5deg; background: var(--olive); color: var(--paper); margin-top: 9px">
        FORM CUỐI CỦA LINE — SĂN NHÁNH & FORM LAI Ở DƯỚI
      </div>
    </div>

    <!-- chỉ số hiện tại (chuyển từ màn Tập) -->
    <div class="mcard" style="--rot: -0.6deg; margin-top: 14px">
      <MeterBar :label="`XP · Lv.${state.pet.level}`" :value="state.pet.xp" :max="need" color="var(--purple)" />
      <div style="height: 14px" />
      <MeterBar label="Sức mạnh" :value="state.systems.power.value" :max="100" :cap="cap" color="var(--red)" />
      <div style="height: 14px" />
      <MeterBar :label="`Thể trạng · ${condLabel}`" :value="state.pet.condition" :max="100" color="var(--olive)" />
    </div>

    <!-- timeline tiến hoá -->
    <div class="mcard" style="--rot: -0.5deg; margin-top: 16px">
      <span class="f-bungee" style="font-size: 12.5px">LỘ TRÌNH CỦA BÉ</span>
      <EvoTimeline :line="line" :current-stage-id="state.pet.stageId" />
      <div class="f-mono" style="font-size: 10.5px; margin-top: 9px; opacity: 0.75">
        {{ line.stages.map((st) => `Lv.${st.atLevel} ${st.name}`).join(' → ') }}
      </div>
    </div>

    <!-- nhánh Stage 3 -->
    <div class="mcard" style="--rot: 0.9deg; margin-top: 16px; background: var(--purple); color: var(--paper)">
      <div class="f-bungee" style="font-size: 13px">NHÁNH STAGE 3 — CHỌN 1 TRONG 2</div>
      <div style="display: flex; gap: 10px; margin-top: 12px; align-items: stretch; flex-wrap: wrap">
        <BranchCard
          v-for="(b, i) in line.branches"
          :key="b.id"
          :branch="b"
          :state="state"
          :rotate="i === 0 ? -1.2 : 1.1"
        />
      </div>
      <div class="f-mono" style="font-size: 11px; margin-top: 10px; opacity: 0.9">
        Đạt Lv.{{ line.stages[2].atLevel }} kèm điều kiện của nhánh để đổi form — chưa đủ thì bé ở form thường {{ line.stages[2].name }}.
      </div>
    </div>

    <!-- form lai hiếm -->
    <div style="margin-top: 18px">
      <span class="chip" style="--rot: 1.2deg; background: var(--red); color: var(--paper)">FORM LAI HIẾM</span>
      <div style="display: flex; flex-direction: column; gap: 12px; margin-top: 10px">
        <HybridCard
          v-for="(h, i) in relevantHybrids"
          :key="h.id"
          :hybrid="h"
          :state="state"
          :rotate="[-0.8, 0.7, -0.5][i % 3]"
        />
      </div>
    </div>
  </div>
</template>
