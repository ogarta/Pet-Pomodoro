#!/usr/bin/env node
/* Export từng sprite → design-demos/sprites/<name>.png (nền trong, scale nguyên)
   + gallery-full.png. Chạy: node scripts/shoot-sprites.mjs */
import { chromium } from "playwright";
import { pathToFileURL } from "node:url";
import { mkdirSync } from "node:fs";

const root = new URL("..", import.meta.url).pathname;
mkdirSync(root + "design-demos/sprites", { recursive: true });

const browser = await chromium.launch();
const page = await browser.newPage({ viewport: { width: 1720, height: 1100 }, deviceScaleFactor: 1 });

const errors = [];
page.on("pageerror", e => errors.push(String(e)));
page.on("console", m => { if (m.type() === "error") errors.push(m.text()); });

await page.goto(pathToFileURL(root + "design-demos/sprite-gallery.html").href, { waitUntil: "networkidle" });
await page.waitForTimeout(600);

const sprites = await page.evaluate(async () => {
  const D = globalThis.SPRITE_DATA;
  // dựng SVG chân растер cho từng sprite, trả về data cần thiết
  const out = [];
  for (const [name, def] of Object.entries(D.grids)) {
    out.push({ name, size: def.size, grid: def.grid, paletteKey: def.palette });
  }
  return out;
});

// dùng chính trang gallery: tìm mọi .cell .sprite svg theo thứ tự xuất hiện không đáng tin
// → thay vào đó render standalone page export (nhúng thẳng data, không dùng src tương đối)
const dataJs = (await import("node:fs")).readFileSync(root + "design-demos/sprite-data.js", "utf8");
const html = `<!DOCTYPE html><html><head><meta charset="utf-8"></head>
<body style="margin:0;background:transparent;">
<div id="host"></div>
<script>
${dataJs}
</script>
<script>
  const D = globalThis.SPRITE_DATA;
  const host = document.getElementById("host");
  const scale = 8;
  for (const [name, def] of Object.entries(D.grids)) {
    const pal = D.palettes[def.palette];
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("width", def.size * scale);
    svg.setAttribute("height", def.size * scale);
    svg.setAttribute("viewBox", "0 0 " + def.size + " " + def.size);
    svg.setAttribute("shape-rendering", "crispEdges");
    svg.id = "sp-" + name;
    for (let y = 0; y < def.grid.length; y++) {
      const row = def.grid[y];
      for (let x = 0; x < row.length; x++) {
        const ch = row[x];
        if (ch !== "." && pal[ch]) {
          const r = document.createElementNS("http://www.w3.org/2000/svg", "rect");
          r.setAttribute("x", x); r.setAttribute("y", y);
          r.setAttribute("width", 1); r.setAttribute("height", 1);
          r.setAttribute("fill", pal[ch].hex);
          svg.appendChild(r);
        }
      }
    }
    host.appendChild(svg);
  }
<\/script></body></html>`;

const exportPage = await browser.newPage({ viewport: { width: 1600, height: 900 } });
await exportPage.setContent(html, { waitUntil: "load" });
await exportPage.waitForTimeout(300);

for (const { name, size } of sprites) {
  const el = exportPage.locator("#sp-" + name);
  await el.screenshot({ path: root + `design-demos/sprites/${name}.png` });
  console.log(`✓ ${name}.png (${size * 8}px)`);
}

// full gallery
await page.screenshot({ path: root + "design-demos/sprite-gallery.png", fullPage: true });
console.log("✓ sprite-gallery.png (full page)");

if (errors.length) {
  console.log("\nCONSOLE/PAGE ERRORS:");
  errors.forEach(e => console.log(" -", e));
  process.exit(1);
}
console.log("\n0 page error ✓");
await browser.close();
