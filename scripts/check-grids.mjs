#!/usr/bin/env node
/* Kiểm tra grid sprite: đủ hàng, đủ cột, không ký tự lạ */
import { createRequire } from "node:module";
const require = createRequire(import.meta.url);
require("../design-demos/sprite-workshop-data.js");
const D = globalThis.SPRITE_DATA;

const bad = [];
for (const [key, dir] of Object.entries(D.dirs)) {
  for (const [name, grid] of Object.entries(dir.grids)) {
    if (grid.length !== 32) bad.push(`${key}.${name}: ${grid.length} hàng (cần 32)`);
    grid.forEach((row, i) => {
      if (row.length !== 32) bad.push(`${key}.${name} hàng ${i}: ${row.length} ký tự (cần 32)`);
      for (const ch of row) {
        if (ch !== "." && !D.palettes[key][ch]) bad.push(`${key}.${name} hàng ${i}: ký tự lạ '${ch}'`);
      }
    });
  }
}
if (bad.length) {
  console.log("LỖI GRID:");
  console.log(bad.join("\n"));
  process.exit(1);
}
console.log("OK — mọi grid đều 32×32, ký tự hợp lệ.");
