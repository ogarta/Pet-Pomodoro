import { chromium } from 'playwright';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const file = process.argv[2];
const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1720, height: 1100 } });
await page.goto(pathToFileURL(path.resolve(file)).href, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500);

// click tab có text đúng "Ăn" (tab nav ngắn)
await page.evaluate(() => {
  const els = [...document.querySelectorAll('button, [role="button"], a, div, span')]
    .filter((el) => [...el.childNodes].some((n) => n.nodeType === 3 && n.textContent.trim() === 'Ăn'));
  if (els.length) els[els.length - 1].click();
});
await page.waitForTimeout(600);

const found = await page.evaluate(() => {
  const els = [...document.querySelectorAll('button, [role="button"], div, span')]
    .filter((el) => [...el.childNodes].some((n) => n.nodeType === 3 && /ăn/i.test(n.textContent)));
  return els.map((el) => `${el.tagName} | ${el.textContent.trim().slice(0, 70)}`);
});
console.log(found.join('\n') || '(không có phần tử nào chứa "ăn")');
await browser.close();
