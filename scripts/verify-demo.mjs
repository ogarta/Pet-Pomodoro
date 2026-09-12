// Click-test dùng chung cho 3 demo Pet Pomodoro
// Cách chạy: node verify-demo.mjs <đường-dẫn-file.html>
// Kiểm: 0 pageerror + các tương tác spec chung đổi state đúng

import { chromium } from 'playwright';
import path from 'node:path';
import { pathToFileURL } from 'node:url';

const args = process.argv.slice(2);
const file = args.find((a) => !a.startsWith('--'));
const petArgIdx = args.indexOf('--pet');
const PET_NAME = petArgIdx !== -1 && args[petArgIdx + 1] ? args[petArgIdx + 1] : 'Mochi';
if (!file) {
  console.error('Usage: node verify-demo.mjs <demo.html> [--pet "Tên Pet"]');
  process.exit(1);
}
const url = pathToFileURL(path.resolve(file)).href;

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1720, height: 1100 } });

const pageErrors = [];
page.on('pageerror', (e) => pageErrors.push(String(e)));
page.on('console', (m) => { if (m.type() === 'error') pageErrors.push(m.text()); });

await page.goto(url, { waitUntil: 'networkidle' });
await page.waitForTimeout(2500); // đợi Babel compile + render

const results = [];
const check = (name, ok, detail = '') => {
  results.push({ name, ok, detail });
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ' — ' + detail : ''}`);
};

// Helper: click phần tử có text trực tiếp (không tính text con cháu) — ưu tiên <button>, lấy phần tử sâu nhất
const clickByText = async (text, { exact = false } = {}) => {
  const clicked = await page.evaluate(({ t, exact }) => {
    const els = [...document.querySelectorAll('button, [role="button"], a, div, span')]
      .filter((el) => [...el.childNodes].some((n) => {
        if (n.nodeType !== 3) return false;
        return exact ? n.textContent.trim() === t : n.textContent.includes(t);
      }));
    if (!els.length) return null;
    const btn = els.find((el) => el.tagName === 'BUTTON');
    const target = btn || els[els.length - 1];
    target.click();
    return `${target.tagName}.${(target.className || '').toString().slice(0, 40)}`;
  }, { t: text, exact });
  return Boolean(clicked);
};
const bodyHas = async (text) => (await page.locator(`text=${text}`).count()) > 0;

// 1. Trang render, không lỗi
check('Không pageerror khi load', pageErrors.length === 0, pageErrors.slice(0, 3).join(' | '));

// 2. Nội dung spec xuất hiện
check('Có đồng hồ 24:31', await bodyHas('24:31'));
check(`Có tên pet ${PET_NAME}`, await bodyHas(PET_NAME));

// 3. Tạm dừng -> Tiếp tục
if (await clickByText('Tạm dừng')) {
  await page.waitForTimeout(400);
  check('Tạm dừng → hiện Tiếp tục', await bodyHas('Tiếp tục'));
} else {
  check('Tạm dừng → hiện Tiếp tục', false, 'không tìm thấy nút Tạm dừng (có thể đang ở màn khác)');
}

// 4. Sang tab Ăn -> cho ăn -> coin giảm / pet vui
await clickByText('Ăn', { exact: true }); // tab nav (nhãn ngắn "Ăn" của tab, khớp chính xác tránh trùng dòng mô tả)
await page.waitForTimeout(400);
const fedAny = await (async () => {
  for (const label of ['Cho ăn', 'cho ăn', 'Ăn 1 sushi']) {
    if (await clickByText(label)) {
      await page.waitForTimeout(500);
      for (const resp of ['thích lắm', 'còn 11', '× 11', '×11']) {
        if (await bodyHas(resp)) return label + ' → ' + resp;
      }
      return null;
    }
  }
  return null;
})();
check('Cho ăn → phản hồi vui', Boolean(fedAny), fedAny || 'không tìm/đủ phản hồi sau khi feed');

// 5. Chuyển tab về Trứng / Stats
if (await clickByText('Trứng')) {
  await page.waitForTimeout(400);
  check('Tab Trứng chuyển màn', await bodyHas('Chọn quả trứng'));
} else {
  check('Tab Trứng chuyển màn', false, 'không tìm thấy tab Trứng');
}
if (await clickByText('Stats')) {
  await page.waitForTimeout(400);
  check('Tab Stats chuyển màn', (await bodyHas('Streak')) || (await bodyHas('Đồng bộ')));
} else {
  check('Tab Stats chuyển màn', false, 'không tìm thấy tab Stats');
}

await browser.close();

const failed = results.filter((r) => !r.ok).length;
console.log(`\n${path.basename(file)}: ${results.length - failed}/${results.length} pass${failed ? ' — CẦN SỬA' : ' — OK'}`);
process.exit(failed ? 1 : 0);
