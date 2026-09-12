<script setup lang="ts">
/**
 * Màn CHỌN TRỨNG (onboarding khi chưa có pet — KB1 ①):
 * 3 trứng nguyên tố → chọn → "Nhận <Stage1> về nhà".
 */
import { computed, ref } from 'vue'
import { CATALOG, lineById } from '#shared/game/catalog'
import type { ElementId } from '#shared/game/types'
import { useGame } from '~/composables/useGame'

const game = useGame()
const picked = ref<ElementId | null>(null)

const confirmText = computed(() =>
  picked.value ? `Nhận ${lineById(picked.value).stages[0].name} về nhà` : '',
)

function confirmHatch(): void {
  if (!picked.value) return
  game.hatch(picked.value)
}
</script>

<template>
  <div>
    <h1 class="screen-title f-bungee">Chọn quả trứng</h1>
    <p class="screen-sub f-mono">
      Một quả trứng nguyên tố sẽ nở thành bạn đồng hành pomodoro của bạn. Chọn line rồi xác nhận —
      chọn rồi là nuôi thật đấy!
    </p>

    <div style="display: flex; flex-direction: column; gap: 14px">
      <button
        v-for="(line, i) in CATALOG"
        :key="line.id"
        class="eggcard"
        :class="{ picked: picked === line.id }"
        :style="{ '--rot': `${[-1.2, 0.9, -0.8][i % 3]}deg`, textAlign: 'left' }"
        @click="picked = line.id"
      >
        <span v-if="picked === line.id" class="pickbadge">ĐANG CHỌN</span>
        <EggSprite :egg="line.egg" :size="58" />
        <span style="display: block; flex: 1">
          <span class="f-serif" style="font-size: 20px; display: block; line-height: 1.1">
            Trứng {{ line.elementLabel }} {{ line.emoji }}
          </span>
          <span class="f-mono" style="font-size: 11.5px; display: block; margin-top: 3px; opacity: 0.8">
            {{ line.tagline }}
          </span>
          <span class="f-mono" style="font-size: 11px; display: block; margin-top: 4px; font-weight: 700">
            {{ line.stages.map((s) => `${s.name} (Lv.${s.atLevel})`).join(' → ') }}
          </span>
        </span>
      </button>
    </div>

    <div style="margin-top: 18px; text-align: center">
      <button class="rbtn rbtn-red" style="width: 100%; font-size: 16px" :disabled="!picked" @click="confirmHatch">
        {{ confirmText || 'Chọn một quả trứng trước' }}
      </button>
      <p class="f-mono" style="font-size: 10.5px; opacity: 0.65; margin-top: 8px">
        Bé được lưu ngay trên máy này — nhớ xuất bản lưu ở Cài đặt để giữ bé an toàn nhé.
      </p>
    </div>
  </div>
</template>

<style scoped>
.eggcard {
  display: flex;
  gap: 14px;
  align-items: center;
  border: 3px solid var(--ink);
  box-shadow: 4px 4px 0 var(--ink);
  padding: 10px 14px;
  cursor: pointer;
  transform: rotate(var(--rot, 0deg));
  position: relative;
  transition: transform 0.15s, box-shadow 0.15s, background-color 0.15s;
  background: var(--paper);
  color: var(--ink);
  font-family: 'Space Mono', monospace;
}
.eggcard:hover {
  transform: rotate(0deg) translate(-2px, -2px);
  box-shadow: 7px 7px 0 var(--ink);
}
.eggcard.picked {
  --rot: 0deg;
  border-color: var(--red);
  box-shadow: 6px 6px 0 var(--red2);
  background: #fbf0d2;
}
.pickbadge {
  position: absolute;
  top: -10px;
  right: 10px;
  background: var(--red);
  color: var(--paper);
  font-size: 12px;
  font-weight: 700;
  border: 2.5px solid var(--ink);
  padding: 2px 7px;
  transform: rotate(3deg);
}
</style>
