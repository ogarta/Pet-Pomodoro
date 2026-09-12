<script setup lang="ts">
/** Tab bar dưới: Trứng (chỉ khi chưa nhận) / Tập / Tiến hoá / Stats. */
export interface TabDef {
  id: string
  label: string
}

defineProps<{
  tabs: TabDef[]
  modelValue: string
}>()

const emit = defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const ROTS = [-1.4, 0.8, -0.7, 1.3]
</script>

<template>
  <nav class="tabbar" aria-label="Điều hướng chính">
    <button
      v-for="(t, i) in tabs"
      :key="t.id"
      class="ptab"
      :class="{ on: modelValue === t.id }"
      :style="{ '--tr': `${ROTS[i % ROTS.length]}deg` }"
      @click="emit('update:modelValue', t.id)"
    >
      {{ t.label }}
    </button>
  </nav>
</template>
