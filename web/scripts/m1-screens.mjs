/**
 * M1 screenshots — chạy: node web/scripts/m1-screens.mjs (dev server phải đang chạy :3000).
 * Chụp: onboarding / focus (sau dev-complete-session) / khoảnh khắc tiến hoá + tab tiến hoá /
 * stats (390×844) + desktop 1280×800. In lỗi console nếu có; exit 1 khi có pageerror.
 */
import { chromium } from 'playwright'
import { mkdirSync } from 'node:fs'

const BASE = process.env.BASE_URL ?? 'http://localhost:3000'
const OUT = new URL('../docs/m1-screens/', import.meta.url).pathname
mkdirSync(OUT, { recursive: true })

const consoleErrors = []
const pageErrors = []

function wire(page, label) {
  page.on('console', (msg) => {
    if (msg.type() === 'error') consoleErrors.push(`[${label}] ${msg.text()}`)
  })
  page.on('pageerror', (err) => pageErrors.push(`[${label}] ${err.message}`))
  page.on('requestfailed', (req) => {
    consoleErrors.push(`[${label}] requestfailed ${req.url()} :: ${req.failure()?.errorText}`)
  })
}

async function pickEggAndGoHome(page) {
  await page.getByText('Chọn quả trứng', { exact: true }).waitFor({ timeout: 20000 })
  await page.getByText('Trứng Lửa', { exact: false }).first().click()
  await page.getByRole('button', { name: /Nhận Tàn Lửa về nhà/ }).click()
  await page.getByRole('button', { name: /Bắt đầu 25:00/ }).waitFor({ timeout: 15000 })
}

const browser = await chromium.launch()

/* ---------- Mobile 390×844 ---------- */
const ctxA = await browser.newContext({
  viewport: { width: 390, height: 844 },
  deviceScaleFactor: 2,
  hasTouch: true,
  isMobile: true,
})
const page = await ctxA.newPage()
wire(page, 'mobile')

// 1) onboarding
await page.goto(BASE + '/', { waitUntil: 'networkidle' })
await page.getByText('Chọn quả trứng', { exact: true }).waitFor({ timeout: 20000 })
await page.waitForTimeout(600)
await page.screenshot({ path: `${OUT}01-onboarding.png`, fullPage: false })

// 2) hatch → Tập, dev hoàn thành phiên ngay → timer chạy
await pickEggAndGoHome(page)
await page.goto(BASE + '/?dev=1', { waitUntil: 'networkidle' })
await page.getByRole('button', { name: /Bắt đầu 25:00/ }).waitFor({ timeout: 20000 })
await page.getByRole('button', { name: /hoàn thành phiên ngay/ }).click()
await page.getByText(/Hoàn thành 25 phút/).first().waitFor({ timeout: 8000 })
await page.getByRole('button', { name: /Bắt đầu 25:00/ }).click()
await page.waitForTimeout(1300) // timer chạy xuống 24:5x, toast còn hiện
await page.screenshot({ path: `${OUT}02-focus.png`, fullPage: false })

// 3) khoảnh khắc tiến hoá (Lv.8 thật qua dev XP) + tab tiến hoá
const dialog = page.getByRole('dialog', { name: 'Tiến hoá' })
for (let i = 0; i < 10 && !(await dialog.isVisible().catch(() => false)); i++) {
  await page.getByRole('button', { name: /\+1000 XP/ }).click()
  await page.waitForTimeout(250)
}
await dialog.waitFor({ timeout: 8000 })
await page.waitForTimeout(1200)
await page.screenshot({ path: `${OUT}03-evolution-moment.png`, fullPage: false })
await page.getByRole('button', { name: 'Tuyệt vời!' }).click()
await page.getByRole('button', { name: 'Tiến hoá' }).first().click()
await page.getByText('NHẬT KÝ TIẾN HOÁ', { exact: false }).waitFor({ timeout: 8000 })
await page
  .waitForFunction(() => (document.querySelector('.toast-layer')?.children.length ?? 0) === 0, { timeout: 9000 })
  .catch(() => {})
await page.waitForTimeout(400)
await page.screenshot({ path: `${OUT}04-evolution.png`, fullPage: false })

// 4) stats
await page.getByRole('button', { name: 'Stats' }).first().click()
await page.getByText('STREAK — CHUỖI NGÀY CÓ PHIÊN', { exact: false }).waitFor({ timeout: 8000 })
await page.waitForTimeout(500)
await page.screenshot({ path: `${OUT}05-stats.png`, fullPage: false })
await ctxA.close()

/* ---------- Desktop 1280×800 ---------- */
const ctxB = await browser.newContext({ viewport: { width: 1280, height: 800 } })
const pageB = await ctxB.newPage()
wire(pageB, 'desktop')
await pageB.goto(BASE + '/', { waitUntil: 'networkidle' })
await pickEggAndGoHome(pageB)
await pageB.waitForTimeout(600)
await pageB.screenshot({ path: `${OUT}06-desktop-home.png`, fullPage: false })
await pageB.getByRole('button', { name: 'Tiến hoá' }).first().click()
await pageB.getByText('NHẬT KÝ TIẾN HOÁ', { exact: false }).waitFor({ timeout: 8000 })
await pageB.waitForTimeout(500)
await pageB.screenshot({ path: `${OUT}07-desktop-evolution.png`, fullPage: false })
await ctxB.close()

await browser.close()

console.log('Saved screenshots to', OUT)
if (pageErrors.length || consoleErrors.length) {
  console.log(`CONSOLE ERRORS (${consoleErrors.length}):`)
  for (const e of consoleErrors) console.log('  ' + e)
  console.log(`PAGE ERRORS (${pageErrors.length}):`)
  for (const e of pageErrors) console.log('  ' + e)
  process.exit(1)
}
console.log('ZERO console errors, ZERO page errors ✔')
