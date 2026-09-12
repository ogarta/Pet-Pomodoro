import { chromium } from 'playwright';
import { pathToFileURL } from 'node:url';
const url = pathToFileURL('/Users/be_gt/Projects/persional/PetPomodoro/design-demos/kich-ban-1-hanh-trinh-tien-hoa.html').href;
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1720, height: 1100 } });
await page.goto(url, { waitUntil: 'networkidle' });
await page.waitForTimeout(3200);

// coords tài liệu (fullPage clip = toạ độ document)
const docBox = (sel, txt, nth = 0) => page.evaluate(([sel, txt, nth]) => {
  const els = [...document.querySelectorAll(sel)].filter((el) => el.textContent.includes(txt));
  const el = els[nth];
  if (!el) return null;
  const r = el.getBoundingClientRect();
  return { x: r.x + scrollX, y: r.y + scrollY, w: r.width, h: r.height };
}, [sel, txt, nth]);

const nb = await docBox('span.f-bungee', 'TÀN LỬA', 1); // phone ② focus
const clipH = { x: nb.x - 45, y: nb.y - 30, width: 350, height: 350 };

// 1) bỏ bê 5 ngày ở phone ④
await page.locator('button', { hasText: 'Mô phỏng bỏ bê 5 ngày' }).first().click();
await page.waitForTimeout(500);
// 2) phone ④ → tab Tập
await page.locator('button.ptab', { hasText: 'Tập' }).last().click();
await page.waitForTimeout(900);

const sb = await docBox('span.f-bungee', 'ỐM', 0); // "TÀN LỬA · ỐM"
const clipS = { x: sb.x - 45, y: sb.y - 30, width: 350, height: 350 };

await page.screenshot({ path: '/tmp/pet-healthy.png', clip: clipH, fullPage: true });
await page.screenshot({ path: '/tmp/pet-sick.png', clip: clipS, fullPage: true });

await page.evaluate(() => { document.documentElement.style.filter = 'grayscale(1) blur(2.5px)'; });
await page.waitForTimeout(400);
await page.screenshot({ path: '/tmp/pet-healthy-gray.png', clip: clipH, fullPage: true });
await page.screenshot({ path: '/tmp/pet-sick-gray.png', clip: clipS, fullPage: true });

await browser.close();
console.log('done', JSON.stringify({ clipH, clipS }));
