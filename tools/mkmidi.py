#!/usr/bin/env python3
"""Generates the four SMF tracks the game streams out of the JAR.

The PS1 originals are not redistributable and khinsider is unreachable from the
build sandbox, so the soundtrack is authored here as plain MIDI: slow minor-key
pads, a detuned bell motif and long silences - the Silent Hill register rather
than its actual notes.
"""
import struct, os

TPQ = 96  # ticks per quarter

def vlq(n):
    out = [n & 0x7F]
    n >>= 7
    while n:
        out.append((n & 0x7F) | 0x80)
        n >>= 7
    return bytes(reversed(out))

def track(events):
    """events: list of (abs_tick, bytes)"""
    out = bytearray()
    last = 0
    for tick, data in sorted(events, key=lambda e: e[0]):
        out += vlq(tick - last) + data
        last = tick
    out += vlq(0) + b'\xff\x2f\x00'
    return b'MTrk' + struct.pack('>I', len(out)) + bytes(out)

def smf(tracks):
    head = b'MThd' + struct.pack('>IHHH', 6, 1, len(tracks), TPQ)
    return head + b''.join(tracks)

def prog(ch, p):
    return bytes([0xC0 | ch, p])

def on(ch, note, vel):
    return bytes([0x90 | ch, note, vel])

def off(ch, note):
    return bytes([0x80 | ch, note, 0])

def cc(ch, n, v):
    return bytes([0xB0 | ch, n, v])

def pad_track(ch, patch, notes, bars, vel=52, length=4):
    ev = [(0, prog(ch, patch)), (0, cc(ch, 7, 92)), (0, cc(ch, 91, 110))]
    for b in range(bars):
        t = b * length * TPQ
        for i, n in enumerate(notes):
            ev.append((t + i * TPQ // 2, on(ch, n, vel)))
            ev.append((t + length * TPQ - 8, off(ch, n)))
    return ev

def bell_track(ch, patch, seq, bars, step, vel=44):
    ev = [(0, prog(ch, patch)), (0, cc(ch, 7, 78)), (0, cc(ch, 91, 127))]
    for b in range(bars):
        for i, n in enumerate(seq):
            if n is None:
                continue
            t = b * bars_step(bars) + i * step
            ev.append((t, on(ch, n, vel)))
            ev.append((t + step * 3, off(ch, n)))
    return ev

def bars_step(bars):
    return bars * 4 * TPQ

def make(path, tracks):
    with open(path, 'wb') as f:
        f.write(smf(tracks))

A2, C3, E3, G3 = 45, 48, 52, 55
D3, F3, A3, C4 = 50, 53, 57, 60
E3b, G3b, B3b = 51, 54, 58

os.makedirs('res/m', exist_ok=True)

# 1. theme: A minor pad, 16 bars, plus a sparse melody
t1 = pad_track(0, 89, [A2, E3, A3, C4], 16, vel=46)          # warm pad
t2 = pad_track(1, 91, [A2 - 12, A2, E3], 16, vel=38)          # low drone
t3 = [(0, prog(2, 11)), (0, cc(2, 7, 70))]
seq = [None, 76, None, 72, None, 74, None, 71]
for b in range(16):
    for i, n in enumerate(seq):
        if n is None: continue
        tk = b * 4 * TPQ + i * TPQ // 2
        t3.append((tk, on(2, n, 40)))
        t3.append((tk + TPQ, off(2, n)))
make('res/m/t_theme.mid', [track(t1), track(t2), track(t3)])

# 2. cafe: the jukebox - a lazy D minor loop with a walking bass
t1 = pad_track(0, 89, [D3, A3, C4, F3], 8, vel=50)
t2 = [(0, prog(1, 32)), (0, cc(1, 7, 96))]
bass = [D3 - 12, D3 - 12, F3 - 12, A3 - 12]
for b in range(8):
    for i, n in enumerate(bass):
        tk = b * 4 * TPQ + i * TPQ
        t2.append((tk, on(1, n, 60)))
        t2.append((tk + TPQ - 4, off(1, n)))
t3 = [(0, prog(2, 4)), (0, cc(2, 7, 84))]
chords = [[62, 65, 69], [65, 69, 72], [62, 65, 69], [60, 64, 67]]
for b in range(8):
    for i, ch in enumerate(chords):
        tk = b * 4 * TPQ + i * TPQ
        for n in ch:
            t3.append((tk, on(2, n, 34)))
        for n in ch:
            t3.append((tk + TPQ - 6, off(2, n)))
make('res/m/t_cafe.mid', [track(t1), track(t2), track(t3)])

# 3. chase: faster, dissonant, pulsing
t1 = [(0, prog(0, 87)), (0, cc(0, 7, 100))]
n = 1
for b in range(12):
    tk = b * 2 * TPQ
    t1.append((tk, on(0, A2 + (b % 2) * 1, 88)))
    t1.append((tk + TPQ - 2, off(0, A2 + (b % 2) * 1)))
t2 = [(0, prog(1, 30)), (0, cc(1, 7, 90))]
for b in range(24):
    tk = b * TPQ
    nn = [A3, C4, E3b, A3][b % 4]
    t2.append((tk, on(1, nn, 70)))
    t2.append((tk + TPQ // 2, off(1, nn)))
make('res/m/t_chase.mid', [track(t1), track(t2)])

# 4. siren: two detuned saw tones gliding, the air-raid feel
t1 = [(0, prog(0, 81)), (0, cc(0, 7, 110))]
t2 = [(0, prog(1, 81)), (0, cc(1, 7, 100))]
for b in range(8):
    tk = b * 8 * TPQ
    t1.append((tk, on(0, 57, 70)))
    t1.append((tk + 6 * TPQ, off(0, 57)))
    t2.append((tk + TPQ, on(1, 61, 62)))
    t2.append((tk + 7 * TPQ, off(1, 61)))
make('res/m/t_siren.mid', [track(t1), track(t2)])
print("wrote", os.listdir('res/m'))
