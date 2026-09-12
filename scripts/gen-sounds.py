#!/usr/bin/env python3
"""
gen-sounds.py — sinh 4 file WAV ngắn (16-bit mono 44.1kHz) cho Android res/raw.
Cùng giai điệu với chuông Web Audio của web (web/app/utils/alerts.ts):
  chime_focus.wav    C5→E5→G5   (hết phiên tập)
  chime_break.wav    G4→C5      (hết giờ nghỉ)
  pop_feed.wav       B5 blip    (cho ăn)
  fanfare_evolve.wav C5→E5→G5→C6 (tiến hoá)

Chạy: python3 scripts/gen-sounds.py
"""
import math
import struct
import wave
import pathlib

SR = 44_100

# (tần số Hz, bắt đầu s, kéo dài s, đỉnh biên độ) — khớp MELODIES bên web
MELODIES = {
    "chime_focus": [(523.25, 0.00, 0.28, 0.8), (659.25, 0.13, 0.28, 0.8), (783.99, 0.26, 0.42, 0.8)],
    "chime_break": [(392.00, 0.00, 0.26, 0.8), (523.25, 0.14, 0.40, 0.8)],
    "pop_feed": [(987.77, 0.00, 0.16, 0.6)],
    "fanfare_evolve": [(523.25, 0.00, 0.22, 0.8), (659.25, 0.12, 0.22, 0.8), (783.99, 0.24, 0.22, 0.8), (1046.50, 0.36, 0.55, 0.8)],
}


def render(notes) -> list[int]:
    total = max(at + dur for _, at, dur, _ in notes) + 0.08
    n = int(SR * total)
    buf = [0.0] * n
    for freq, at, dur, peak in notes:
        start = int(at * SR)
        length = int(dur * SR)
        attack = max(1, int(0.015 * SR))
        for i in range(length):
            t = i / SR
            # envelope: attack tuyến tính + suy giảm mũ (như web)
            env = (i / attack) if i < attack else math.exp(-4.5 * (i - attack) / (length - attack))
            buf[start + i] += peak * env * math.sin(2 * math.pi * freq * t)
    # fade-out nhẹ cuối file chống click
    fade = min(220, n)
    for i in range(fade):
        buf[n - 1 - i] *= i / fade
    return [max(-32767, min(32767, int(s * 32767))) for s in buf]


def main() -> None:
    out_dir = pathlib.Path(__file__).resolve().parent.parent / "android" / "app" / "src" / "main" / "res" / "raw"
    out_dir.mkdir(parents=True, exist_ok=True)
    for name, notes in MELODIES.items():
        path = out_dir / f"{name}.wav"
        with wave.open(str(path), "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(SR)
            w.writeframes(struct.pack(f"<{len(render(notes))}h", *render(notes)))
        print("wrote", path)


if __name__ == "__main__":
    main()
