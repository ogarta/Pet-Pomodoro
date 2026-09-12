<script setup lang="ts">
/**
 * PetStage — sân khấu Memphis cho sprite (thẻ giấy viền mực + bóng cứng lệch),
 * kèm biến thể ỐM YẾU theo spec §2.4: nhợt màu (desaturate), dáng xìu,
 * shiver ±1px, vệt khói thay nguyên tố, 2 giọt mồ hôi, bong bóng "…", vũng tối.
 * sprite = null → silhouette form chưa mở.
 */
const props = withDefaults(
  defineProps<{
    src?: string | null
    alt?: string
    natW?: number
    natH?: number
    h?: number // chiều cao hiển thị (px)
    sick?: boolean
    bounce?: boolean
    rotate?: number
    lite?: boolean
    lockLabel?: string
  }>(),
  { src: null, alt: 'Pet', natW: undefined, natH: undefined, h: 120, sick: false, bounce: false, rotate: 0, lite: false, lockLabel: '???' },
)
</script>

<template>
  <div
    class="pet-stage"
    :class="{ 'pet-sick': sick }"
    :style="{ transform: rotate ? `rotate(${rotate}deg)` : undefined }"
  >
    <div
      :style="{
        background: 'var(--paper)',
        border: lite ? '2.5px solid var(--ink)' : '3px solid var(--ink)',
        boxShadow: lite ? '3px 3px 0 var(--ink)' : '6px 6px 0 var(--ink)',
        padding: lite ? '6px' : '10px',
        lineHeight: 0,
        display: 'inline-block',
      }"
    >
      <img
        v-if="src"
        class="pet-img"
        :class="{ 'pet-bounce': bounce && !sick }"
        :src="src"
        :alt="alt"
        :width="natW"
        :height="natH"
        :style="{ height: `${h}px` }"
        draggable="false"
      />
      <div
        v-else
        class="silhouette"
        :style="{ height: `${h}px`, width: `${Math.round(h * 0.86)}px`, fontSize: `${Math.round(h * 0.3)}px` }"
      >
        {{ lockLabel }}
      </div>
    </div>

    <!-- §2.4: hiệu ứng phụ khi ốm -->
    <template v-if="sick && src">
      <span class="sick-puddle" aria-hidden="true" />
      <span class="smoke-puff" style="right: 18%; bottom: 38%; animation-delay: 0s" aria-hidden="true" />
      <span class="smoke-puff" style="right: 26%; bottom: 48%; animation-delay: 0.5s" aria-hidden="true" />
      <span class="smoke-puff" style="right: 14%; bottom: 52%; animation-delay: 1s" aria-hidden="true" />
      <span class="sick-sweat" style="left: 14%; top: 26%" aria-hidden="true" />
      <span class="sick-sweat r" style="right: 10%; top: 22%" aria-hidden="true" />
      <span class="sick-bubble" style="left: 4%; top: 2%" aria-hidden="true">…</span>
    </template>
  </div>
</template>
