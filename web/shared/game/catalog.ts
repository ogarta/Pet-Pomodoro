/**
 * PetPomodoro — Catalog sinh vật (spec §2, data-driven: thêm loài = thêm entry).
 * Mapping sprite từ `direction-approved.md`:
 *   🔥 Tàn Lửa/Bồng Bột/Diễn Long ← Petyre/Fyreox/Infernyx
 *   💧 Giọt/Suối Vọt/Triều Long   ← Shock-Fringe/Volt-Tide/Tide-Striker (ảnh mẫu dòng cá)
 *   🌿 Mầm/Búp Xanh/Cổ Thụ Linh   ← Sproutling/Florasaur/Ancient-Oak
 * Hệ Zap/Điện (Zap-Pup/Volt-Fox/Thundros) CHƯA mở theo M1 — thêm sau = thêm entry.
 */
import type { ElementId } from './types'
import type { Cond } from './conditions'

export interface StageDef {
  atLevel: number // 1 · 8 · 16
  id: string
  name: string
  desc: string
  sprite: string | null // đường dẫn /pets/*.png · null = silhouette
  natW: number
  natH: number
}

export interface BranchDef {
  id: string // 'hien' | 'chien-binh'
  name: string
  formName: string // "Diễm Long Hiền"
  meaning: string
  cond: Cond // registry COND (and[level≥16, …])
  sprite: string | null
}

export interface HybridDef {
  id: string
  name: string
  elements: [ElementId, ElementId]
  cond: Cond
  sprite: string | null
  desc: string
}

export interface EggDef {
  base: string
  spot: string
  motif: string
}

export interface SpeciesLine {
  id: ElementId
  elementLabel: string
  emoji: string
  name: string
  tagline: string
  egg: EggDef
  stages: [StageDef, StageDef, StageDef]
  branches: [BranchDef, BranchDef] // Stage 3: chọn 1 trong 2 + mặc định
  hybridsWith: ElementId[]
}

const cLevel = (v: number): Cond => ({ type: 'level_at_least', value: v })
const cStreak = (v: number): Cond => ({ type: 'streak_at_least', value: v })
const cPower = (v: number): Cond => ({ type: 'power_at_least', value: v })
const cAnd = (...of: Cond[]): Cond => ({ type: 'and', of })

/** Điều kiện nhánh Stage 3 theo spec §2.2: Hiền streak≥7 · Chiến Binh power≥80 (đều cần Lv≥16). */
const branchConds = (): [BranchDef['cond'], BranchDef['cond']] => [
  cAnd(cLevel(16), cStreak(7)),
  cAnd(cLevel(16), cPower(80)),
]

/** Form lai hiếm (spec §2.3): and[power≥85, streak≥10]. */
export const HYBRID_COND: Cond = cAnd(cPower(85), cStreak(10))

export const CATALOG: SpeciesLine[] = [
  {
    id: 'lua',
    elementLabel: 'Lửa',
    emoji: '🔥',
    name: 'Dòng Lửa',
    tagline: 'Từ hổ lửa con đến rồng lửa — nóng bỏng và lì lợm.',
    egg: { base: '#F7E7CE', spot: '#E8663C', motif: '#E3A72F' },
    stages: [
      { atLevel: 1, id: 'tan-lua', name: 'Tàn Lửa', desc: 'Hổ lửa con lúng túng, đuôi lửa nhỏ xíu.', sprite: '/pets/tanLua.png', natW: 112, natH: 114 },
      { atLevel: 8, id: 'bong-bot', name: 'Bồng Bột', desc: 'Thiếu niên lửa mọc cánh mầm, bờm cháy rực.', sprite: '/pets/bongBot.png', natW: 112, natH: 145 },
      { atLevel: 16, id: 'diem-long', name: 'Diễm Long', desc: 'Rồng lửa nhỏ — sừng, mào lửa, dáng lượn vòng.', sprite: '/pets/diemLong.png', natW: 171, natH: 188 },
    ],
    branches: [
      { id: 'hien', name: 'Hiền', formName: 'Diễm Long Hiền', meaning: 'Aura dịu — phần thưởng thói quen đều đặn.', cond: branchConds()[0], sprite: '/pets/diemLong-shiny.png' },
      { id: 'chien-binh', name: 'Chiến Binh', formName: 'Diễm Long Chiến Binh', meaning: 'Giáp uy phong — phần thưởng sức mạnh cao.', cond: branchConds()[1], sprite: '/pets/diemLong.png' },
    ],
    hybridsWith: ['thuy', 'thao'],
  },
  {
    id: 'thuy',
    elementLabel: 'Thủy',
    emoji: '💧',
    name: 'Dòng Thủy',
    tagline: 'Từ giọt nước ngố đến rồng sóng — dịu mà sâu.',
    egg: { base: '#DDEBF7', spot: '#2B54A3', motif: '#5FA8CC' },
    stages: [
      { atLevel: 1, id: 'giot', name: 'Giọt', desc: 'Gấu nước tròn trịa, đầu lúc nhúc bong bóng.', sprite: '/pets/giot.png', natW: 215, natH: 169 },
      { atLevel: 8, id: 'suoi-vot', name: 'Suối Vọt', desc: 'Cá heo nước ngọt, bơi nhanh như tên bắn.', sprite: '/pets/suoiVot.png', natW: 360, natH: 260 },
      { atLevel: 16, id: 'trieu-long', name: 'Triều Long', desc: 'Rồng sóng — vây gai, đuôi cuộn triều.', sprite: '/pets/trieuLong.png', natW: 400, natH: 502 },
    ],
    branches: [
      { id: 'hien', name: 'Hiền', formName: 'Triều Long Hiền', meaning: 'Aura dịu — phần thưởng thói quen đều đặn.', cond: branchConds()[0], sprite: '/pets/trieuLong.png' },
      { id: 'chien-binh', name: 'Chiến Binh', formName: 'Triều Long Chiến Binh', meaning: 'Giáp uy phong — phần thưởng sức mạnh cao.', cond: branchConds()[1], sprite: '/pets/trieuLong.png' },
    ],
    hybridsWith: ['lua', 'thao'],
  },
  {
    id: 'thao',
    elementLabel: 'Thảo',
    emoji: '🌿',
    name: 'Dòng Thảo',
    tagline: 'Từ hạt nảy mầm đến cổ thụ linh — chậm mà chắc.',
    egg: { base: '#E4F0D8', spot: '#5F6B2F', motif: '#93D06E' },
    stages: [
      { atLevel: 1, id: 'mam', name: 'Mầm', desc: 'Hạt nảy mầm ngủ gật, lá non vươn vẹo.', sprite: '/pets/mam.png', natW: 50, natH: 75 },
      { atLevel: 8, id: 'bup-xanh', name: 'Búp Xanh', desc: 'Hươu lá nhẹ nhàng, nụ hoa trên trán.', sprite: '/pets/bupXanh.png', natW: 103, natH: 105 },
      { atLevel: 16, id: 'co-thu-linh', name: 'Cổ Thụ Linh', desc: 'Linh thú cây cổ tích, tán lá nở rộng.', sprite: '/pets/coThuLinh.png', natW: 128, natH: 161 },
    ],
    branches: [
      { id: 'hien', name: 'Hiền', formName: 'Cổ Thụ Linh Hiền', meaning: 'Aura dịu — phần thưởng thói quen đều đặn.', cond: branchConds()[0], sprite: '/pets/coThuLinh.png' },
      { id: 'chien-binh', name: 'Chiến Binh', formName: 'Cổ Thụ Linh Chiến Binh', meaning: 'Giáp uy phong — phần thưởng sức mạnh cao.', cond: branchConds()[1], sprite: '/pets/coThuLinh.png' },
    ],
    hybridsWith: ['lua', 'thuy'],
  },
]

export const HYBRIDS: HybridDef[] = [
  { id: 'hoi-nuoc-boc', name: 'Hơi Nước Bốc', elements: ['lua', 'thuy'], cond: HYBRID_COND, sprite: null, desc: 'Sương nóng và hơi nước — Lửa × Thủy. Sprite chờ bộ art lai.' },
  { id: 'diem-hoa', name: 'Diễm Hoa', elements: ['thao', 'lua'], cond: HYBRID_COND, sprite: '/pets/infernoOak.png', desc: 'Hoa cháy rực rỡ — Thảo × Lửa.' },
  { id: 'sen-mua', name: 'Sen Mưa', elements: ['thuy', 'thao'], cond: HYBRID_COND, sprite: '/pets/aquaSprout.png', desc: 'Sen ngọc rưới mưa — Thủy × Thảo.' },
]

export const EGG_STAGE_ID = 'egg'

export function lineById(id: string): SpeciesLine {
  const line = CATALOG.find((l) => l.id === id)
  if (!line) throw new Error(`Không có line '${id}' trong catalog`)
  return line
}

export function stageById(line: SpeciesLine, stageId: string): StageDef {
  const st = line.stages.find((s) => s.id === stageId)
  if (!st) throw new Error(`Stage '${stageId}' không có trong line '${line.id}'`)
  return st
}

/** Stage ứng với level hiện tại (Lv.1 / Lv.8 / Lv.16). */
export function stageAtLevel(line: SpeciesLine, level: number): StageDef {
  let out = line.stages[0]
  for (const st of line.stages) if (level >= st.atLevel) out = st
  return out
}

/** Stage kế tiếp chưa đạt — để chip "Sắp tiến hoá". */
export function nextStage(line: SpeciesLine, level: number): StageDef | null {
  return line.stages.find((s) => s.atLevel > level) ?? null
}

export function hybridById(id: string): HybridDef | undefined {
  return HYBRIDS.find((h) => h.id === id)
}
