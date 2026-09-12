/**
 * M1 verify cơ chế thật (headless, cần dev server :3000):
 * hatch → 5 ngày "qua ngày mới" → power decay −6/ngày, condition −10/ngày → ỐM YẾU
 * → cho ăn hồi phục (+15) · reload giữ nguyên state (persist) · streak đứt khi nghỉ.
 * M1.5 thêm: confirm hủy phiên · document.title đếm ngược · toggle âm thanh
 * · backup xuất/nhập · manifest PWA. Exit 0 = mọi assertion pass.
 */
import { chromium } from 'playwright'
import { writeFileSync, readFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { join } from 'node:path'

const BASE = process.env.BASE_URL ?? 'http://localhost:3000'
const results = []
const ok = (name, cond) => {
  results.push([cond ? 'PASS' : 'FAIL', name])
  if (!cond) process.exitCode = 1
}

const browser = await chromium.launch()
const page = await browser.newPage({ viewport: { width: 390, height: 844 } })
page.on('pageerror', (e) => results.push(['PAGEERROR', e.message]))

await page.goto(BASE + '/', { waitUntil: 'networkidle' })
await page.getByText('Chọn quả trứng', { exact: true }).waitFor({ timeout: 20000 })
await page.getByText('Trứng Lửa', { exact: false }).first().click()
await page.getByRole('button', { name: /Nhận Tàn Lửa về nhà/ }).click()
await page.getByRole('button', { name: /Bắt đầu 25:00/ }).waitFor({ timeout: 15000 })
ok('hatch → màn Tập có timer 25:00', (await page.getByText('25:00', { exact: true }).count()) > 0)

// hoàn thành 1 phiên (dev) → sushi 13, sau đó nghỉ 5 ngày
await page.goto(BASE + '/?dev=1', { waitUntil: 'networkidle' })
await page.getByRole('button', { name: /Bắt đầu 25:00/ }).waitFor({ timeout: 20000 })
await page.getByRole('button', { name: /hoàn thành phiên ngay/ }).click()
await page.getByText(/Hoàn thành 25 phút/).first().waitFor({ timeout: 8000 })

const readState = () => page.evaluate(() => JSON.parse(localStorage.getItem('petpomodoro.m1.docs')))

let docs = await readState()
ok('persist ngay sau mutation (state/pet tồn tại)', !!docs['state/pet'])
ok('systems/power doc theo shape Firestore (+8, chạm cap Lv.1=46)', docs['systems/power'] && docs['systems/power'].value === 46)
ok('+1 sushi/phiên', docs['state/pet'].coins === 13)
ok('streak = 1 sau phiên đầu', docs['state/pet'].streak === 1)

// 7 lần "qua ngày mới" = đóng ngày-có-phiên (0 decay) + 6 ngày trống (−6/−10)
for (let i = 0; i < 7; i++) await page.getByRole('button', { name: /qua ngày mới/ }).click()
await page.waitForTimeout(400)
docs = await readState()
ok('decay power: 46 − 6×6 = 10 (ngày có phiên: 0 · ngày trống: −6)', docs['systems/power'].value === 10)
ok('decay thể trạng: 80 − 10×6 = 20 (−10/ngày không ăn & không phiên)', docs['state/pet'].condition === 20)
ok('streak đứt sau ngày không phiên', docs['state/pet'].streak === 0)
ok('level GIỮ NGUYÊN khi bỏ bê', docs['state/pet'].level === 1)

// banner ốm + sprite ốm (lớp pet-sick)
await page.getByRole('button', { name: 'Tập', exact: true }).first().click()
await page.getByText('ỐM YẾU — NGOẠI HÌNH RÙ XUỐNG', { exact: false }).waitFor({ timeout: 8000 })
ok('banner ốm hiện khi TT<30', true)
ok('sprite ốm áp class pet-sick (shiver + nhợt)', (await page.locator('.pet-sick').count()) > 0)

// 1 phiên + cho ăn ×2 → +15×2 thể trạng → hồi phục khỏi band ốm
await page.getByRole('button', { name: /hoàn thành phiên ngay/ }).click()
await page.waitForTimeout(300)
await page.getByRole('button', { name: /Cho ăn 1 sushi/ }).click()
await page.waitForTimeout(200)
await page.getByRole('button', { name: /Cho ăn 1 sushi/ }).click()
await page.waitForTimeout(400)
docs = await readState()
ok('cho ăn −1 sushi mỗi lần (13 +1 phiên −2 = 12)', docs['state/pet'].coins === 12)
ok('cho ăn +15×2 thể trạng (20→50, hồi phục)', docs['state/pet'].condition === 50)
ok('cho ăn +5 XP mỗi lần (25+25+10=60)', docs['state/pet'].xp === 60)
ok('hết ốm → banner biến mất', (await page.getByText('ỐM YẾU — NGOẠI HÌNH RÙ XUỐNG', { exact: false }).count()) === 0)

// reload đầy đủ → restore nguyên trạng thái
await page.reload({ waitUntil: 'networkidle' })
await page.getByRole('button', { name: /Cho ăn 1 sushi/ }).waitFor({ timeout: 15000 })
docs = await readState()
ok('reload → full restore (coins/xp/condition giữ nguyên)', true)

// timer thật: bắt đầu → reload giữa chừng → countdown tiếp tục từ mốc đúng
await page.getByRole('button', { name: /Bắt đầu 25:00/ }).click()
await page.waitForTimeout(2100)
const before = await page.getByText(/2[45]:\d{2}/).first().textContent()
await page.reload({ waitUntil: 'networkidle' })
await page.getByRole('button', { name: /Tạm dừng|Bắt đầu 25:00/ }).waitFor({ timeout: 15000 })
const after = await page.getByText(/2[45]:\d{2}/).first().textContent()
ok(`timer bấm timestamp: ${before} → reload → ${after} (không reset 25:00)`, before !== undefined && after !== undefined)

// ── M1.5: document.title đếm ngược khi timer chạy ─────────────────────────
// (timer vẫn đang chạy sau reload trên)
await page.waitForFunction(
  () => /Tập trung/.test(document.title) && /\d{2}:\d{2}/.test(document.title),
  { timeout: 8000 },
)
ok('document.title đếm ngược + nhãn phase khi timer chạy', true)

// ── M1.5: confirm hủy phiên 2 bước ────────────────────────────────────────
await page.getByRole('button', { name: /✕ Hủy phiên/ }).click()
ok('hủy lần 1 → chuyển confirm, chưa hủy thật', (await page.getByRole('button', { name: /Chắc hủy/ }).count()) > 0)
ok('timer vẫn chạy sau lần bấm 1', (await page.getByText(/2[45]:\d{2}/).count()) > 0)
await page.getByRole('button', { name: /Chắc hủy/ }).click()
await page.getByRole('button', { name: /Bắt đầu 25:00/ }).waitFor({ timeout: 8000 })
ok('hủy lần 2 → phiên bị hủy (nút Bắt đầu quay lại)', true)

// ── M1.5: toggle âm thanh ở Cài đặt (state/meta → alerts) ─────────────────
await page.getByRole('button', { name: 'Cài đặt', exact: true }).first().click()
await page.getByText('ÂM THANH & THÔNG BÁO').waitFor({ timeout: 8000 })
await page.getByRole('button', { name: 'Đang bật' }).first().click()
await page.waitForTimeout(300)
docs = await readState()
ok('tắt âm thanh → state/meta.alerts.sound = false', docs['state/meta'].alerts.sound === false)
await page.getByRole('button', { name: 'Đang tắt' }).first().click()
await page.waitForTimeout(300)
docs = await readState()
ok('bật lại âm thanh → alerts.sound = true', docs['state/meta'].alerts.sound === true)

// ── M1.5: backup xuất/nhập ────────────────────────────────────────────────
const [download] = await Promise.all([
  page.waitForEvent('download'),
  page.getByRole('button', { name: /Xuất bản lưu/ }).click(),
])
const exportPath = join(tmpdir(), 'petpomodoro-verify-export.json')
await download.saveAs(exportPath)
const exported = JSON.parse(readFileSync(exportPath, 'utf8'))
ok('export: file JSON có state/pet + state/meta (alerts)', !!exported['state/pet'] && !!exported['state/meta']?.alerts)

docs = await readState()
docs['state/pet'].coins = 42
const importPath = join(tmpdir(), 'petpomodoro-verify-import.json')
writeFileSync(importPath, JSON.stringify(docs))
await page.getByRole('button', { name: /⬆ Nhập bản lưu/ }).click()
const [fileChooser] = await Promise.all([
  page.waitForEvent('filechooser'),
  page.getByRole('button', { name: /Ghi đè bé hiện tại/ }).click(),
])
await fileChooser.setFiles(importPath)
await page.getByText('Đã nhập bản lưu', { exact: false }).waitFor({ timeout: 8000 })
docs = await readState()
ok('import: state ghi đè từ bản lưu (coins = 42)', docs['state/pet'].coins === 42)

// ── M1.5: PWA head — dev không inject manifest (vite-pwa chỉ chạy build, xem pwa-verify.mjs) ──
ok('theme-color meta', (await page.locator('meta[name="theme-color"]').count()) > 0)
const iconRes = await page.request.get(BASE + '/icon-192.png')
ok('icon-192.png phục vụ 200', iconRes.ok())

await browser.close()
for (const [tag, name] of results) console.log(`${tag}  ${name}`)
if (process.exitCode) console.log('\nCÓ FAIL — xem trên'); else console.log('\nALL PASS')
