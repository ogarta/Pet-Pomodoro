<script setup lang="ts">
/**
 * Màn STATS: streak flame cells, hôm nay (phút/phiên/XP), history list,
 * cây tiến hoá mini (3 line từ catalog — đã mở màu, chưa mở silhouette),
 * chip sắp tiến hoá, card đồng bộ đám mây (placeholder "sắp có").
 */
import { computed } from 'vue'
import { CATALOG, lineById, nextStage } from '#shared/game/catalog'
import { GOAL_SESSIONS } from '#shared/game/formulas'
import { useGame } from '~/composables/useGame'

const game = useGame()
const state = computed(() => game.state.value as NonNullable<typeof game.state.value>)
const line = computed(() => lineById(state.value.pet.speciesId))

const DAYS = 14
const cells = computed(() =>
  game.dayKeysBack(DAYS).map((date) => {
    if (date === state.value.meta.today) {
      const s = state.value.pet.todaySessions
      return { date, sessions: s, live: true, goal: s >= GOAL_SESSIONS }
    }
    const rec = state.value.systems.stats.history.find((h) => h.date === date)
    return { date, sessions: rec?.sessions ?? 0, live: false, goal: rec?.goal ?? false }
  }),
)

const historyRows = computed(() =>
  [...state.value.systems.stats.history].reverse().slice(0, 7).map((h) => ({
    ...h,
    label: `${h.date.slice(8, 10)}/${h.date.slice(5, 7)}`,
  })),
)

const unlocked = computed(() => new Set(state.value.systems.evolution.unlocked))

function cellColor(c: { sessions: number; goal: boolean }): string {
  if (c.goal) return 'var(--red)'
  if (c.sessions > 0) return 'var(--mustard)'
  return 'var(--cream2)'
}

const upNext = computed(() => nextStage(line.value, state.value.pet.level))

function shortDate(key: string): string {
  return `${key.slice(8, 10)}/${key.slice(5, 7)}`
}
const todayShort = computed(() => shortDate(state.value.meta.today))
</script>

<template>
  <div>
    <span class="chip" style="--rot: -1.2deg; background: var(--blue); color: var(--paper)">THỐNG KÊ · {{ line.name.toUpperCase() }}</span>

    <!-- streak flame cells -->
    <div class="mcard" style="--rot: 0.7deg; margin-top: 12px">
      <div style="display: flex; justify-content: space-between; align-items: baseline; gap: 8px; flex-wrap: wrap">
        <span class="f-bungee" style="font-size: 12.5px">STREAK — CHUỖI NGÀY CÓ PHIÊN</span>
        <span class="chip" style="--rot: -2deg; background: var(--red); color: var(--paper); padding: 1px 7px; font-size: 11px">
          {{ state.pet.streak }} ngày 🔥
        </span>
      </div>
      <div style="display: flex; gap: 4px; margin-top: 10px; flex-wrap: wrap">
        <div
          v-for="c in cells"
          :key="c.date"
          :style="{
            flex: '1 0 26px',
            maxWidth: '34px',
            textAlign: 'center',
          }"
          :title="`${c.date}: ${c.sessions} phiên`"
        >
          <div
            :style="{
              border: '2.5px solid var(--ink)',
              background: cellColor(c),
              borderRadius: '4px',
              padding: '7px 0',
              fontSize: '15px',
              animation: c.sessions > 0 ? 'flameFlick 1.2s ease-in-out infinite' : 'none',
              opacity: c.sessions > 0 ? 1 : 0.6,
            }"
          >
            {{ c.sessions > 0 ? '🔥' : '·' }}
          </div>
          <div class="f-mono" style="font-size: 8.5px; opacity: 0.7; margin-top: 2px">
            {{ c.live ? 'nay' : c.date.slice(8, 10) }}
          </div>
        </div>
      </div>
    </div>

    <!-- hôm nay -->
    <div class="mcard tape" style="--rot: -0.8deg; margin-top: 16px">
      <span class="f-bungee" style="font-size: 12.5px">HÔM NAY ({{ todayShort }})</span>
      <div style="display: flex; gap: 10px; margin-top: 9px; flex-wrap: wrap">
        <div class="mcard" style="--rot: -1deg; flex: 1; min-width: 96px; text-align: center; padding: 8px">
          <div class="f-mono" style="font-size: 20px; font-weight: 700">{{ state.pet.todayMinutes }}</div>
          <div class="f-mono" style="font-size: 10px; opacity: 0.7">phút focus</div>
        </div>
        <div class="mcard" style="--rot: 1.2deg; flex: 1; min-width: 96px; text-align: center; padding: 8px">
          <div class="f-mono" style="font-size: 20px; font-weight: 700">{{ state.pet.todaySessions }}/{{ GOAL_SESSIONS }}</div>
          <div class="f-mono" style="font-size: 10px; opacity: 0.7">phiên</div>
        </div>
        <div class="mcard" style="--rot: -0.6deg; flex: 1; min-width: 96px; text-align: center; padding: 8px">
          <div class="f-mono" style="font-size: 20px; font-weight: 700">+{{ state.systems.stats.todayXp }}</div>
          <div class="f-mono" style="font-size: 10px; opacity: 0.7">XP</div>
        </div>
      </div>
      <div v-if="upNext" class="chip" style="--rot: 1.4deg; background: var(--mustard); margin-top: 10px">
        SẮP TIẾN HOÁ: LV.{{ upNext.atLevel }} — {{ upNext.name.toUpperCase() }}
      </div>
    </div>

    <!-- history -->
    <div class="mcard" style="--rot: 0.5deg; margin-top: 16px">
      <span class="f-bungee" style="font-size: 12.5px">LỊCH SỬ GẦN ĐÂY</span>
      <div v-if="historyRows.length === 0" class="f-mono" style="font-size: 11.5px; opacity: 0.7; margin-top: 7px">
        Chưa có ngày nào khép lại — hoàn thành phiên đầu tiên đi!
      </div>
      <table v-else class="f-mono" style="width: 100%; border-collapse: collapse; margin-top: 7px; font-size: 11.5px">
        <tbody>
          <tr v-for="h in historyRows" :key="h.date" style="border-bottom: 2px dashed rgba(38,32,29,.18)">
            <td style="padding: 5px 4px; font-weight: 700">{{ h.label }}</td>
            <td style="padding: 5px 4px">{{ h.sessions }} phiên</td>
            <td style="padding: 5px 4px">{{ h.minutes }} phút</td>
            <td style="padding: 5px 4px">+{{ h.xp }} XP</td>
            <td style="padding: 5px 4px">{{ h.goal ? '🔥 đủ' : '—' }}</td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- cây tiến hoá mini -->
    <div class="mcard" style="--rot: -0.6deg; margin-top: 16px">
      <span class="f-bungee" style="font-size: 12.5px">CÂY TIẾN HOÁ MINI</span>
      <div
        v-for="ln in CATALOG"
        :key="ln.id"
        style="display: flex; align-items: center; gap: 8px; margin-top: 11px; flex-wrap: nowrap"
      >
        <span class="f-mono" style="font-size: 11px; font-weight: 700; width: 34px">{{ ln.emoji }} {{ ln.elementLabel }}</span>
        <div style="display: flex; align-items: center; gap: 5px; flex: 1; min-width: 0">
          <span
            class="silhouette"
            v-if="ln.id !== state.pet.speciesId"
            style="width: 30px; height: 30px; border-radius: 8px; font-size: 11px"
            title="Chưa mở"
          >?</span>
          <span v-else style="font-size: 22px" title="Trứng của bạn">🥚</span>
          <template v-for="(st, i) in ln.stages" :key="st.id">
            <svg width="12" height="11" viewBox="0 0 22 18" style="flex-shrink: 0" aria-hidden="true">
              <path d="M2 9h14M12 3l6 6-6 6" fill="none" stroke="var(--ink)" stroke-width="3.4" stroke-linecap="round" />
            </svg>
            <PetStage
              v-if="ln.id === state.pet.speciesId && unlocked.has(st.id)"
              :src="st.sprite"
              :alt="st.name"
              :nat-w="st.natW"
              :nat-h="st.natH"
              :h="i === 0 ? 30 : 34 + i * 4"
              lite
            />
            <span
              v-else
              class="silhouette"
              style="width: 32px; height: 34px; border-radius: 7px; font-size: 11px"
              :title="ln.id === state.pet.speciesId ? st.name : 'Line khác — sắp mở'"
            >?</span>
          </template>
        </div>
      </div>
      <div class="f-mono" style="font-size: 10px; opacity: 0.6; margin-top: 9px">
        Bé đã mở {{ unlocked.size }} form · các line khác và form lai sẽ mở sau.
      </div>
    </div>

    <!-- sync placeholder -->
    <div class="mcard" style="--rot: 1deg; margin-top: 16px; background: var(--cream2)">
      <div style="display: flex; justify-content: space-between; align-items: center; gap: 10px; flex-wrap: wrap">
        <div>
          <div class="f-bungee" style="font-size: 12.5px">ĐỒNG BỘ ĐÁM MÂY</div>
          <div class="f-mono" style="font-size: 11.5px; margin-top: 3px; opacity: 0.8">
            Sắp có — dữ liệu bé hiện đang lưu an toàn trên máy này.
          </div>
        </div>
        <button class="rbtn rbtn-sm" disabled title="Sắp có">Đăng nhập Google</button>
      </div>
    </div>
  </div>
</template>
