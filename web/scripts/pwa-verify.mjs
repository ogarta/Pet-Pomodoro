/**
 * pwa-verify — kiểm PWA trên BUILD PRODUCTION (chạy `npm run build` trước):
 *   node .output/server/index.mjs &   # preview :3000
 *   node scripts/pwa-verify.mjs
 * Check: link manifest · /manifest.webmanifest 200 · icons 200 · sw.js 200
 * · service worker đăng ký & active. Exit 0 = pass.
 */
import { chromium } from 'playwright'

const BASE = process.env.BASE_URL ?? 'http://localhost:3000'
const results = []
const ok = (name, cond) => {
  results.push([cond ? 'PASS' : 'FAIL', name])
  if (!cond) process.exitCode = 1
}

const browser = await chromium.launch()
const page = await browser.newPage()
page.on('console', (m) => { if (m.type() === 'error') console.log('[console.error]', m.text()) })

await page.goto(BASE + '/', { waitUntil: 'load', timeout: 30000 })
await page.waitForTimeout(2500) // nhường SW register + precache

ok('link rel=manifest trong <head>', (await page.locator('link[rel="manifest"]').count()) > 0)
ok('theme-color meta', (await page.locator('meta[name="theme-color"]').count()) > 0)
ok('apple-touch-icon', (await page.locator('link[rel="apple-touch-icon"]').count()) > 0)

const manifestRes = await page.request.get(BASE + '/manifest.webmanifest')
ok('/manifest.webmanifest 200', manifestRes.ok())
if (manifestRes.ok()) {
  const manifest = await manifestRes.json()
  ok('manifest icons 192+512+maskable', (manifest.icons?.length ?? 0) >= 3)
  ok('manifest display standalone', manifest.display === 'standalone')
}

for (const asset of ['/icon-192.png', '/icon-512.png', '/maskable-512.png', '/favicon-32.png']) {
  ok(`${asset} 200`, (await page.request.get(BASE + asset)).ok())
}

const swRes = await page.request.get(BASE + '/sw.js')
ok('/sw.js 200', swRes.ok())

const swActive = await page.evaluate(async () => {
  const withTimeout = (ms) => new Promise((_, rej) => setTimeout(() => rej(new Error('timeout')), ms))
  try {
    const reg = await Promise.race([navigator.serviceWorker.ready, withTimeout(8000)])
    return !!reg.active
  } catch {
    return false
  }
})
ok('service worker đăng ký & active (production)', swActive)

await browser.close()
for (const [tag, name] of results) console.log(`${tag}  ${name}`)
if (process.exitCode) console.log('\nCÓ FAIL — xem trên'); else console.log('\nALL PASS')
