<script setup lang="ts">
/**
 * DevPanel — QA CHỈ CHO DEV, hiện khi URL có ?dev=1 (ẩn hoàn toàn khi thường).
 * Nút "hoàn thành phiên ngay" / "qua ngày mới" (+ XP thử mốc tiến hoá, xoá data).
 */
import { useGame } from '~/composables/useGame'

const game = useGame()

defineProps<{ visible?: boolean }>()
</script>

<template>
  <aside v-if="visible" class="dev-panel" aria-label="Bảng QA dev">
    <span class="dev-tag">DEV QA</span>
    <span style="opacity: 0.8">Chỉ dành để kiểm thử (?dev=1)</span>
    <button @click="game.dev.completeSession()">▶ hoàn thành phiên ngay (+25 phút)</button>
    <button @click="game.dev.nextDay()">⏭ qua ngày mới (decay · streak)</button>
    <button @click="game.dev.addXp(1000)">✦ +1000 XP (thử mốc Lv.8/Lv.16)</button>
    <button style="margin-bottom: 2px" @click="game.dev.reset()">✕ xoá dữ liệu (reload onboarding)</button>
  </aside>
</template>
