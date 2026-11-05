#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Silent-Slop asset baker  ->  res/ss_data.bin (+ a PNG preview sheet)

World unit in the engine  = 1 cm.
The original game's map coordinates are 1 unit = 40 cm
(a 24 x 28 unit cafe interior becomes a 9.6 x 11.2 m room, which matches the
PlayStation original), so every constant below that comes from
silent-hill-decomp is written as  Uu(units) = units * 40.
"""
import math
import os
import struct

U = 40.0                       # cm per original map unit


def Uu(v):
    return v * 40.0
HERE = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(HERE, '..', 'res')

# ---------------------------------------------------------------- palette
PAL = [
    (0, 0, 0), (14, 14, 18), (26, 26, 32), (40, 40, 48), (56, 56, 64),
    (74, 74, 80), (94, 92, 90), (118, 114, 108), (140, 136, 128), (168, 164, 154),
    (196, 192, 182), (228, 224, 212), (252, 250, 242),
    (46, 46, 50), (60, 60, 64), (78, 78, 82), (98, 98, 100), (112, 110, 104),
    (132, 128, 118), (152, 146, 132),
    (88, 46, 38), (110, 58, 46), (134, 72, 56), (158, 92, 72), (66, 34, 28),
    (78, 56, 34), (102, 74, 44), (128, 96, 60), (152, 118, 76),
    (34, 46, 28), (48, 66, 34), (66, 88, 46),
    (20, 28, 42), (34, 48, 68), (54, 76, 100), (84, 110, 138),
    (110, 80, 26), (162, 124, 40), (214, 182, 84), (255, 232, 150),
    (70, 16, 16), (108, 24, 22), (152, 38, 30), (192, 62, 48),
    (132, 92, 74), (168, 122, 96), (204, 158, 126),
    (56, 42, 32), (84, 64, 44), (114, 90, 64),
    (66, 70, 78), (90, 96, 106), (120, 128, 138), (152, 158, 168),
    (46, 50, 60), (30, 30, 36), (80, 86, 98), (104, 112, 126),
    (58, 50, 42), (136, 124, 96), (96, 28, 38), (44, 68, 58),
    (86, 38, 68), (200, 200, 210),
]
assert len(PAL) == 64, len(PAL)


def rgb565(c):
    r, g, b = c
    return ((r & 0xF8) << 8) | ((g & 0xFC) << 3) | ((b & 0xF8) >> 3)


class Rng:
    def __init__(self, s=12345):
        self.s = s & 0x7FFFFFFF

    def next(self):
        self.s = (self.s * 1103515245 + 12345) & 0x7FFFFFFF
        return self.s >> 8

    def r(self, a, b=None):
        if b is None:
            a, b = 0, a
        if b <= a:
            return a
        return a + self.next() % (b - a)


# ---------------------------------------------------------------- textures
class Tex:
    def __init__(self, w=16, h=16, fill=0):
        self.w = w
        self.h = h
        self.d = bytearray([fill]) * (w * h)

    def p(self, x, y, c):
        x %= self.w
        y %= self.h
        self.d[y * self.w + x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.p(x, y, c)

    def hl(self, y, x0, x1, c):
        for x in range(x0, x1 + 1):
            self.p(x, y, c)

    def speckle(self, r, x0, y0, x1, y1, cols, n):
        for _ in range(n):
            self.p(r.r(x0, x1 + 1), r.r(y0, y1 + 1), cols[r.r(len(cols))])


def build_textures():
    r = Rng(0xC0FFEE)
    T = []

    def add(name, t):
        T.append(t)
        return name

    # 0 asphalt
    t = Tex(32, 32, 14)
    t.speckle(r, 0, 0, 31, 31, [13, 15, 16, 4, 3], 300)
    for _ in range(4):
        t.hl(r.r(32), 0, 31, 13)
    add('asphalt', t)

    # 1 road with worn centre line
    t = Tex(32, 32, 14)
    t.speckle(r, 0, 0, 31, 31, [13, 15, 16, 4], 320)
    for yy in range(2, 32, 16):
        t.rect(15, yy, 17, yy + 8, 19)
    add('road', t)

    # 2 concrete sidewalk
    t = Tex(32, 32, 17)
    t.speckle(r, 0, 0, 31, 31, [16, 18, 8, 15, 19], 260)
    for yy in (10, 21):
        t.hl(yy, 0, 31, 15)
    for xx in (10, 21):
        for y in range(32):
            t.p(xx, y, 15)
    add('concrete', t)

    # 3 dirty brick
    t = Tex(32, 32, 21)
    for row in range(8):
        y = row * 4
        off = 0 if row % 2 == 0 else 4
        for bx in range(-1, 5):
            x0 = bx * 8 + off
            t.rect(x0, y, x0 + 6, y + 2, 21 + r.r(3))
            for yy in range(y, y + 3):
                t.p(x0 + 7, yy, 24)
    t.speckle(r, 0, 0, 31, 31, [24, 20, 29], 120)
    add('brick', t)

    # 4 warm cafe brick
    t = Tex(32, 32, 22)
    for row in range(8):
        y = row * 4
        off = 0 if row % 2 == 0 else 4
        for bx in range(-1, 5):
            x0 = bx * 8 + off
            t.rect(x0, y, x0 + 6, y + 2, 21 + r.r(3))
            for yy in range(y, y + 3):
                t.p(x0 + 7, yy, 20)
    add('brick_warm', t)

    # 5 stained concrete wall
    t = Tex(32, 32, 18)
    t.speckle(r, 0, 0, 31, 31, [16, 17, 19, 8, 7], 240)
    for _ in range(6):
        x = r.r(32)
        hh = r.r(10, 28)
        for y in range(hh):
            t.p(x, y, 8 if y > hh - 5 else 17)
            if r.r(4) == 0:
                t.p(x + 1, y, 16)
    add('wall', t)

    # 6 cafe interior: plaster above, dark wood wainscot below
    t = Tex(32, 32, 27)
    for y in range(32):
        for x in range(32):
            t.p(x, y, 27 if (x % 7) else 26)
    t.rect(0, 0, 31, 9, 19)
    t.rect(0, 9, 31, 10, 15)
    t.rect(0, 26, 31, 31, 25)
    add('cafe_wall', t)

    # 7 checker floor
    t = Tex(32, 32, 9)
    for ty in range(0, 32, 8):
        for tx in range(0, 32, 8):
            c = 10 if ((tx + ty) // 8) % 2 == 0 else 5
            t.rect(tx, ty, tx + 6, ty + 6, c)
            for k in range(8):
                t.p(tx + 7, ty + k, 6)
                t.p(tx + k, ty + 7, 6)
    add('tile', t)

    # 8 wooden floor boards
    t = Tex(32, 32, 25)
    for row in range(4):
        y = row * 8
        for x in range(32):
            for dy in range(7):
                t.p(x, y + dy, 25 + ((x // 5 + row + dy // 4) % 3))
        t.hl(y + 7, 0, 31, 24)
    add('wood_floor', t)

    # 9 ceiling
    t = Tex(32, 32, 18)
    for ty in range(0, 32, 16):
        for tx in range(0, 32, 16):
            t.rect(tx + 1, ty + 1, tx + 14, ty + 14, 17)
    t.speckle(r, 0, 0, 31, 31, [8, 7], 40)
    add('ceiling', t)

    # 10 lit window
    t = Tex(16, 16, 34)
    for y in range(16):
        for x in range(16):
            t.p(x, y, 35 if (y < 9) else 33)
    t.rect(0, 7, 15, 9, 37)
    add('window_lit', t)

    # 11 dark rainy window
    t = Tex(16, 16, 32)
    for _ in range(26):
        x = r.r(16)
        y0 = r.r(16)
        for y in range(y0, min(16, y0 + r.r(3, 12))):
            t.p(x, y, 33 if y % 2 else 34)
    t.hl(0, 0, 15, 2)
    t.hl(15, 0, 15, 2)
    add('window_dark', t)

    # 12 metal shutter
    t = Tex(16, 16, 52)
    for y in range(16):
        c = 52 if (y % 3) else 50
        for x in range(16):
            t.p(x, y, c)
    t.speckle(r, 0, 0, 15, 15, [48, 59], 40)
    add('shutter', t)

    # 13 wooden door
    t = Tex(16, 32, 26)
    for x in range(16):
        for y in range(32):
            t.p(x, y, 26 + ((x // 3 + y // 8) % 2))
    t.rect(1, 1, 14, 2, 25)
    t.rect(1, 29, 14, 30, 25)
    t.rect(11, 15, 13, 17, 38)
    add('door_wood', t)

    # 14 counter top / bar wood
    t = Tex(32, 32, 28)
    for y in range(32):
        for x in range(32):
            t.p(x, y, (28 if y < 5 else 26) - (y % 3 == 0 and 1 or 0))
    add('counter', t)

    # 15 pinball machine art
    t = Tex(16, 32, 42)
    t.rect(0, 0, 15, 5, 43)
    t.rect(1, 7, 14, 30, 33)
    t.rect(3, 9, 12, 20, 35)
    for i in range(5):
        t.p(4 + i * 2, 11 + i * 2, 39)
    t.rect(4, 24, 11, 25, 38)
    add('pinball', t)

    # 16 TV screen
    t = Tex(16, 16, 3)
    t.rect(1, 1, 14, 11, 2)
    for y in range(2, 11, 2):
        t.hl(y, 2, 13, 4)
    t.rect(5, 5, 10, 8, 33)
    add('tv', t)

    # 17 cafe sign "5 TO 2"
    t = Tex(32, 16, 37)
    t.rect(0, 0, 31, 15, 38)
    t.rect(1, 1, 30, 14, 39)
    # digit 5
    for (x, y) in [(6, 4), (7, 4), (8, 4), (5, 5), (5, 6), (6, 7), (7, 7), (8, 8),
                   (5, 9), (5, 10), (6, 11), (7, 11), (8, 10)]:
        t.p(x, y, 11)
    # T
    t.hl(4, 10, 13, 11)
    for y in range(5, 12):
        t.p(11, y, 11)
    # O
    for (x, y) in [(16, 5), (17, 5), (18, 5), (15, 6), (19, 6), (15, 7), (19, 7),
                   (15, 8), (19, 8), (15, 9), (19, 9), (16, 10), (17, 10), (18, 10)]:
        t.p(x, y, 11)
    # 2
    for (x, y) in [(23, 5), (24, 5), (25, 5), (26, 6), (25, 7), (24, 8), (23, 9),
                   (22, 10), (23, 11), (24, 11), (25, 11), (26, 11)]:
        t.p(x, y, 11)
    add('sign_cafe', t)

    # 18 lamp head
    t = Tex(8, 8, 39)
    t.rect(0, 0, 7, 7, 38)
    t.rect(1, 1, 6, 6, 39)
    add('lamp', t)

    # 19 chain link fence (index 0 = transparent)
    t = Tex(16, 16, 0)
    for i in range(-16, 16, 4):
        for k in range(16):
            t.p(i + k, k, 53)
            t.p(i - k, k, 52)
    add('fence', t)

    # 20 weeds (transparent)
    t = Tex(16, 16, 0)
    for _ in range(22):
        x = r.r(2, 14)
        hh = r.r(4, 12)
        c = 29 + r.r(2)
        for y in range(16 - hh, 16):
            t.p(x + (16 - y) // 4, y, c)
    add('weeds', t)

    # 21 cracked plaster
    t = Tex(32, 32, 8)
    t.speckle(r, 0, 0, 31, 31, [7, 8, 6, 4, 18], 280)
    for _ in range(7):
        x, y = r.r(32), r.r(32)
        for k in range(r.r(4, 12)):
            t.p(x + k, y + k // 2, 4)
    add('plaster', t)

    # 22 car paint
    t = Tex(32, 16, 34)
    for y in range(16):
        for x in range(32):
            t.p(x, y, 34 if y < 4 else (33 if y < 11 else 32))
    t.hl(4, 0, 31, 61)
    t.speckle(r, 0, 0, 31, 15, [47, 33], 40)
    add('car', t)

    # 23 car glass
    t = Tex(16, 16, 33)
    for y in range(16):
        t.p(0, y, 35)
        t.p(15, y, 2)
    add('carglass', t)

    # 24 rusty metal
    t = Tex(16, 16, 48)
    t.speckle(r, 0, 0, 15, 15, [47, 49, 50, 59], 180)
    for y in range(0, 16, 5):
        t.hl(y, 0, 15, 47)
    add('metal', t)

    # 25 can / health drink
    t = Tex(8, 8, 43)
    t.rect(0, 0, 7, 1, 10)
    t.rect(0, 6, 7, 7, 10)
    t.rect(2, 2, 5, 5, 12)
    add('can', t)

    # 26 paper map
    t = Tex(16, 16, 11)
    t.speckle(r, 0, 0, 15, 15, [10, 11, 9], 60)
    for y in range(2, 15, 3):
        t.hl(y, 2, 13, 7)
    for x in range(3, 14, 5):
        for y in range(2, 15):
            t.p(x, y, 7)
    add('paper', t)

    # 27 knife blade
    t = Tex(16, 8, 53)
    t.rect(0, 1, 10, 6, 53)
    t.hl(1, 0, 10, 63)
    t.rect(11, 2, 15, 5, 25)
    add('knife', t)

    # 28 flashlight
    t = Tex(8, 16, 50)
    t.rect(0, 0, 7, 4, 53)
    t.rect(0, 5, 7, 15, 49)
    add('flashlight', t)

    # 29 pocket radio
    t = Tex(8, 8, 4)
    t.rect(0, 0, 7, 7, 3)
    t.rect(0, 0, 7, 3, 49)
    t.rect(1, 4, 6, 6, 38)
    t.p(6, 1, 42)
    add('radio', t)

    # 30 monster flesh
    t = Tex(16, 16, 44)
    t.speckle(r, 0, 0, 15, 15, [43, 44, 47, 41, 59], 220)
    for _ in range(8):
        x, y = r.r(16), r.r(16)
        for k in range(r.r(2, 6)):
            t.p(x + k, y + (k % 2), 61)
    add('flesh', t)

    # 31 wing membrane
    t = Tex(16, 16, 47)
    for y in range(16):
        for x in range(16):
            t.p(x, y, 47 + ((x + y) % 2))
    for i in range(0, 16, 4):
        for k in range(16):
            t.p(i, k, 43)
    add('wing', t)

    # 32 harry jacket
    t = Tex(16, 16, 57)
    t.speckle(r, 0, 0, 15, 15, [57, 58, 26], 50)
    add('harry_top', t)

    # 33 harry trousers
    t = Tex(16, 16, 54)
    t.speckle(r, 0, 0, 15, 15, [54, 2, 4], 40)
    add('harry_legs', t)

    # 34 skin
    t = Tex(8, 8, 46)
    t.speckle(r, 0, 0, 7, 7, [45, 46, 44], 12)
    add('skin', t)

    # 35 hair
    t = Tex(8, 8, 59)
    t.speckle(r, 0, 0, 7, 7, [59, 49, 62], 16)
    add('hair', t)

    # 36 cybil uniform
    t = Tex(16, 16, 33)
    t.speckle(r, 0, 0, 15, 15, [32, 33, 34], 40)
    add('cybil_top', t)

    # 37 blood decal (transparent)
    t = Tex(16, 16, 0)
    for _ in range(70):
        x = 8 + r.r(-7, 7)
        y = 8 + r.r(-7, 7)
        if (x - 8) ** 2 + (y - 8) ** 2 < 30:
            t.p(x, y, 40 if r.r(3) else 41)
    add('blood', t)

    # 38 soft shadow blob (transparent)
    t = Tex(16, 16, 0)
    for y in range(16):
        for x in range(16):
            d = (x - 8) ** 2 + (y - 8) ** 2
            if d < 49:
                t.p(x, y, 1 if d < 25 else 2)
    add('shadow', t)

    # 39 vent
    t = Tex(8, 8, 5)
    for y in range(0, 8, 2):
        t.hl(y, 0, 7, 3)
    add('vent', t)

    # 40 road sign
    t = Tex(8, 8, 11)
    t.rect(0, 0, 7, 7, 11)
    t.rect(1, 2, 6, 3, 7)
    t.rect(1, 5, 6, 6, 7)
    add('sign_road', t)

    # 41 bench wood
    t = Tex(16, 16, 28)
    for x in range(16):
        for y in range(16):
            t.p(x, y, 27 + ((x // 2 + y // 6) % 2))
    add('bench', t)

    # 42 jukebox front
    t = Tex(16, 16, 39)
    t.rect(1, 1, 14, 6, 37)
    t.rect(2, 8, 13, 14, 35)
    t.rect(4, 9, 11, 12, 63)
    add('jukebox', t)

    # 43 diner booth vinyl
    t = Tex(16, 16, 59)
    t.speckle(r, 0, 0, 15, 15, [59, 41, 39], 40)
    add('booth', t)

    # 44 airport/plain concrete for the alley
    t = Tex(32, 32, 15)
    t.speckle(r, 0, 0, 31, 31, [14, 16, 13, 4], 200)
    add('alley', t)

    # 45 grass verge
    t = Tex(16, 16, 29)
    t.speckle(r, 0, 0, 15, 15, [29, 30, 31, 27], 140)
    add('grass', t)

    # 46 dirty white paint (for plaster of the big house)
    t = Tex(32, 32, 19)
    t.speckle(r, 0, 0, 31, 31, [18, 19, 8, 17], 160)
    for y in range(0, 32, 8):
        t.hl(y, 0, 31, 17)
    add('siding', t)

    # 47 sky / backdrop gradient
    t = Tex(16, 16, 32)
    for y in range(16):
        c = 32 + min(3, y // 5)
        for x in range(16):
            t.p(x, y, c)
    add('sky', t)

    # 48 door frame / trim
    t = Tex(8, 8, 25)
    t.rect(0, 0, 7, 0, 27)
    add('trim', t)

    # 49 roof tar
    t = Tex(16, 16, 13)
    t.speckle(r, 0, 0, 15, 15, [13, 12, 4, 15], 120)
    add('roof', t)

    # 50 air screamer beak / dark chitin
    t = Tex(8, 8, 62)
    t.speckle(r, 0, 0, 7, 7, [62, 2, 41], 14)
    add('chitin', t)

    return T


# ---------------------------------------------------------------- meshes
F_DOUBLE = 1
F_WALL = 2
F_NOFOG = 4
F_EMIT = 8
F_ROOF = 16
F_ALPHA = 32


class Mesh:
    """Vertex array + per-face texture/colour/flags. Normals are baked."""

    def __init__(self):
        self.v = []          # (x,y,z,u,v)
        self.f = []          # (a,b,c, texName, col, flags)
        self.vmap = {}

    def vert(self, x, y, z, u=0, v=0):
        key = (round(x, 1), round(y, 1), round(z, 1), u, v)
        i = self.vmap.get(key)
        if i is None:
            i = len(self.v)
            self.vmap[key] = i
            self.v.append(key)
        return i

    def tri(self, a, b, c, tex, col=0, flags=0):
        self.f.append((a, b, c, tex, col, flags))

    def quad(self, p0, p1, p2, p3, tex, col=0, flags=0, uvs=40.0):
        """Quad with texture coordinates derived from the plane it lies in."""
        ax = [p0[0], p0[1], p0[2]]
        bx = [p1[0], p1[1], p1[2]]
        cx = [p2[0], p2[1], p2[2]]
        ux = [bx[i] - ax[i] for i in range(3)]
        vx = [cx[i] - ax[i] for i in range(3)]
        lu = math.sqrt(ux[0] ** 2 + ux[1] ** 2 + ux[2] ** 2) or 1.0
        lv = math.sqrt(vx[0] ** 2 + vx[1] ** 2 + vx[2] ** 2) or 1.0
        uu = lu / uvs
        vv = lv / uvs
        a = self.vert(p0[0], p0[1], p0[2], 0, 0)
        b = self.vert(p1[0], p1[1], p1[2], uu, 0)
        c = self.vert(p2[0], p2[1], p2[2], uu, vv)
        d = self.vert(p3[0], p3[1], p3[2], 0, vv)
        self.tri(a, b, c, tex, col, flags)
        self.tri(a, c, d, tex, col, flags)

    def box(self, x0, y0, z0, x1, y1, z1, tex, col=0, flags=0, top=None,
            uvs=40.0, sides=(1, 1, 1, 1), topf=None, botf=None):
        if top is None:
            top = tex
        if topf is None:
            topf = flags
        if botf is None:
            botf = flags
        if sides[0]:   # -Z
            self.quad((x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0),
                      tex, col, flags, uvs)
        if sides[1]:   # +Z
            self.quad((x1, y1, z1), (x0, y1, z1), (x0, y0, z1), (x1, y0, z1),
                      tex, col, flags, uvs)
        if sides[2]:   # -X
            self.quad((x0, y1, z1), (x0, y1, z0), (x0, y0, z0), (x0, y0, z1),
                      tex, col, flags, uvs)
        if sides[3]:   # +X
            self.quad((x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0),
                      tex, col, flags, uvs)
        self.quad((x0, y1, z1), (x1, y1, z1), (x1, y1, z0), (x0, y1, z0),
                  top, col, topf, uvs)
        if y0 > 1:
            self.quad((x0, y0, z0), (x1, y0, z0), (x1, y0, z1), (x0, y0, z1),
                      tex, col, botf, uvs)

    def plane(self, x0, z0, x1, z1, y, tex, col=0, flags=0, uvs=64.0):
        self.quad((x0, y, z1), (x1, y, z1), (x1, y, z0), (x0, y, z0),
                  tex, col, flags, uvs)

    def verts_bytes(self):
        o = bytearray()
        for (x, y, z, u, v) in self.v:
            o += struct.pack('<hhhHH', int(round(x)), int(round(y)), int(round(z)),
                             int(round(u * 64)) & 0xFFFF, int(round(v * 64)) & 0xFFFF)
        return o


TEXID = {}


def faces_bytes(m):
    o = bytearray()
    n = len(m.v)
    wide = n > 255
    for (a, b, c, tex, col, flags) in m.f:
        if wide:
            o += struct.pack('<HHH', a, b, c)
        else:
            o += struct.pack('<BBB', a, b, c)
        o += struct.pack('<BBB', TEXID.get(tex, 255), col, flags)
    return o, wide


# ---------------------------------------------------------------- levels
BAND_W = 24.0 * U        # 960 cm : vertical thickness of every wall band


def key_of(x, z):
    return (x // BAND_W, z // BAND_W)


class Level:
    """
    A level is a dict of bands keyed by (ix, iz) on a 960 cm grid.
    Because every wall quad is cropped to the band that contains it, the
    renderer can drop a whole band of walls the moment it would hide the
    player - that is the cut-away feature, and it also culls half the world.
    """

    def __init__(self, name, interior=False, vheight=760.0, fog=(500, 3000),
                 fogcolor=(64, 68, 78), amb=5):
        self.name = name
        self.interior = interior
        self.vheight = vheight
        self.fog = fog
        self.fogcolor = fogcolor
        self.amb = amb
        self.bands = {}
        self.colliders = []
        self.verticals = []
        self.floors = []
        self.props = []
        self.items = []
        self.triggers = []
        self.spawn = (0, 0, 0, 0)

    def band(self, x, z):
        k = key_of(x, z)
        m = self.bands.get(k)
        if m is None:
            m = Mesh()
            self.bands[k] = m
        return m

    def floor_quad(self, x0, z0, x1, z1, y, tex, uvs=64.0, col=0, flags=0):
        self.floors.append((x0, z0, x1, z1, y, tex, col, flags, uvs))

    # -- buildings -----------------------------------------------------
    def building(self, x0, z0, x1, z1, height, tex='brick', top='roof',
                 uvs=48.0, windows=True, winpat=None):
        self.floor_quad(x0, z0, x1, z1, height, top, 96.0, 0, 0)
        i0 = int(math.floor(x0 / BAND_W))
        i1 = int(math.floor((x1 - 0.001) / BAND_W))
        k0 = int(math.floor(z0 / BAND_W))
        k1 = int(math.floor((z1 - 0.001) / BAND_W))
        for k in range(k0, k1 + 1):
            for i in range(i0, i1 + 1):
                bx0 = max(x0, i * BAND_W)
                bx1 = min(x1, (i + 1) * BAND_W)
                bz0 = max(z0, k * BAND_W)
                bz1 = min(z1, (k + 1) * BAND_W)
                if bx1 <= bx0 or bz1 <= bz0:
                    continue
                m = self.band((bx0 + bx1) * 0.5, (bz0 + bz1) * 0.5)
                sides = (i == i0, i == i1, k == k0, k == k1)
                m.box(bx0, 0, bz0, bx1, height, bz1, tex, 0, F_WALL,
                      top=None, uvs=uvs, sides=sides, topf=F_ROOF, botf=0)
                if windows:
                    self.windows_on(m, bx0, bz0, bx1, bz1, height, sides, winpat)
        self.colliders.append((x0, z0, x1, z1, height))

    def windows_on(self, m, x0, z0, x1, z1, height, sides, pat):
        # one or two windows per band face, held 2 cm off the wall to avoid z-fight
        xs = [x0 + (x1 - x0) * 0.3, x0 + (x1 - x0) * 0.7]
        ys = [140.0, 300.0] if height > 400 else [140.0]
        idx = 0
        for yy in ys:
            for xx in xs:
                lit = (pat is not None and idx < len(pat)) and pat[idx]
                idx += 1
                tex = 'window_lit' if lit else 'window_dark'
                w = 42.0
                h = 60.0
                if sides[2]:
                    m.quad((xx - w / 2, yy + h, z0 - 2), (xx + w / 2, yy + h, z0 - 2),
                           (xx + w / 2, yy, z0 - 2), (xx - w / 2, yy, z0 - 2),
                           tex, 0, F_EMIT if lit else F_DOUBLE, w)
                if sides[3]:
                    m.quad((xx - w / 2, yy + h, z1 + 2), (xx + w / 2, yy + h, z1 + 2),
                           (xx + w / 2, yy, z1 + 2), (xx - w / 2, yy, z1 + 2),
                           tex, 0, F_EMIT if lit else F_DOUBLE, w)
                if sides[0]:
                    m.quad((x0 - 2, yy + h, xx + w / 2), (x0 - 2, yy + h, xx - w / 2),
                           (x0 - 2, yy, xx - w / 2), (x0 - 2, yy, xx + w / 2),
                           tex, 0, F_EMIT if lit else F_DOUBLE, w)
                if sides[1]:
                    m.quad((x1 + 2, yy + h, xx - w / 2), (x1 + 2, yy + h, xx + w / 2),
                           (x1 + 2, yy, xx + w / 2), (x1 + 2, yy, xx - w / 2),
                           tex, 0, F_EMIT if lit else F_DOUBLE, w)

    def wall_quad(self, p0, p1, p2, p3, tex, flags=F_WALL | F_DOUBLE, col=0, uvs=48.0):
        """Vertical quad routed into the band that holds its centre."""
        cx = (p0[0] + p1[0] + p2[0] + p3[0]) * 0.25
        cz = (p0[2] + p1[2] + p2[2] + p3[2]) * 0.25
        self.band(cx, cz).quad(p0, p1, p2, p3, tex, col, flags, uvs)

    def wall_box(self, x0, z0, x1, z1, y0, y1, tex, flags=F_WALL, uvs=48.0, top=None):
        """A wall of thickness, split per band; used for interior walls."""
        i0 = int(math.floor(x0 / BAND_W))
        i1 = int(math.floor((x1 - 0.001) / BAND_W))
        k0 = int(math.floor(z0 / BAND_W))
        k1 = int(math.floor((z1 - 0.001) / BAND_W))
        for k in range(k0, k1 + 1):
            for i in range(i0, i1 + 1):
                bx0 = max(x0, i * BAND_W)
                bx1 = min(x1, (i + 1) * BAND_W)
                bz0 = max(z0, k * BAND_W)
                bz1 = min(z1, (k + 1) * BAND_W)
                if bx1 <= bx0 or bz1 <= bz0:
                    continue
                m = self.band((bx0 + bx1) * 0.5, (bz0 + bz1) * 0.5)
                m.box(bx0, y0, bz0, bx1, y1, bz1, tex, 0, flags,
                      top=None, uvs=uvs, topf=0, botf=0)
        self.colliders.append((x0, z0, x1, z1, y1))

    def collect_colliders(self):
        self.colliders.sort()


def Uu(v):
    return v * U



# ---------------------------------------------------------------- PROLOGUE
# map0_s00 "THR" - the street.
# Real coordinates from the decompilation (1 unit = 40 cm):
#   camera road band  z 108.5 .. 131.0,  x -240 .. 200
#   Harry's car map point 0 : (-6.2 , 160.5)
#   the cafe front sits on the north side around z = 176 .. 280
Uu = lambda v: v * 40.0


def build_prologue():
    lv = Level('prologue', False, vheight=800.0,
               fog=(2400, 9000), fogcolor=(62, 68, 78), amb=4)
    lv.spawn = (Uu(-6), Uu(168), 2048, Uu(6))
    lv.zone = (Uu(-200), Uu(70), Uu(200), Uu(290))

    # ---- streets (world scaled)
    lv.floor_quad(Uu(-200), Uu(96), Uu(200), Uu(156), 0, 'road', 60.0)
    lv.floor_quad(Uu(-200), Uu(80), Uu(200), Uu(96), 0, 'concrete', 56.0)
    lv.floor_quad(Uu(-200), Uu(156), Uu(200), Uu(172), 0, 'concrete', 56.0)
    lv.floor_quad(Uu(-200), Uu(60), Uu(200), Uu(80), 0, 'grass', 72.0)
    lv.floor_quad(Uu(-200), Uu(172), Uu(200), Uu(300), 0, 'grass', 72.0)
    # kerbs
    lv.wall_box(Uu(-200), Uu(94), Uu(200), Uu(96), 0, 12, 'concrete', 0, 32)
    lv.wall_box(Uu(-200), Uu(156), Uu(200), Uu(158), 0, 12, 'concrete', 0, 32)
    lv.colliders = [c for c in lv.colliders if c[4] > 20]

    # ---- south block (behind the camera on the first shot)
    lv.building(Uu(-198), Uu(30), Uu(-128), Uu(78), 400, 'brick', 'roof',
                winpat=[0, 0, 1, 0])
    lv.building(Uu(-122), Uu(24), Uu(-60), Uu(78), 320, 'siding', 'roof',
                winpat=[0, 1, 0, 0])
    # the alley between -60 and -30
    lv.floor_quad(Uu(-60), Uu(10), Uu(-30), Uu(78), 0, 'alley', 64.0)
    lv.wall_box(Uu(-60), Uu(6), Uu(-30), Uu(14), 0, 300, 'alley')
    lv.building(Uu(-28), Uu(28), Uu(30), Uu(78), 360, 'brick_warm', 'roof',
                winpat=[0, 0, 0, 0])
    lv.building(Uu(38), Uu(20), Uu(120), Uu(78), 420, 'brick', 'roof',
                winpat=[0, 0, 1, 0])
    lv.building(Uu(128), Uu(34), Uu(200), Uu(78), 300, 'siding', 'roof',
                winpat=[0, 0, 0, 0])

    # ---- north block
    lv.building(Uu(-198), Uu(172), Uu(-138), Uu(230), 380, 'siding', 'roof',
                winpat=[0, 0, 0, 1])
    lv.building(Uu(-130), Uu(176), Uu(-78), Uu(232), 440, 'brick', 'roof',
                winpat=[0, 1, 0, 0])

    # ---- the cafe "5 to 2" front, at -80..-24 x 176..236
    cx0, cx1 = Uu(-80), Uu(-24)
    cz0, cz1 = Uu(176), Uu(236)
    d0, d1 = Uu(-58), Uu(-44)              # doorway on the south facade
    t = 100.0                              # wall thickness (a house is 2.5 m deep)
    lv.wall_box(cx0, cz0, d0, cz0 + t, 0, 340, 'brick_warm')
    lv.wall_box(d1, cz0, cx1, cz0 + t, 0, 340, 'brick_warm')
    lv.wall_box(d0, cz0, d1, cz0 + t, 210, 340, 'brick_warm')
    lv.wall_box(cx0, cz1 - t, cx1, cz1, 0, 340, 'brick_warm')
    lv.wall_box(cx0, cz0, cx0 + t, cz1, 0, 340, 'brick_warm')
    lv.wall_box(cx1 - t, cz0, cx1, cz1, 0, 340, 'brick_warm')
    lv.floor_quad(cx0, cz0, cx1, cz1, 340, 'roof', 96.0, 0, F_ROOF)
    lv.floor_quad(cx0, Uu(172), cx1, cz0, 0, 'concrete', 48.0)

    # backlit "5 TO 2" sign over the door
    lv.wall_quad((Uu(-60), 200, cz0 - 4), (Uu(-42), 200, cz0 - 4),
                 (Uu(-42), 172, cz0 - 4), (Uu(-60), 172, cz0 - 4),
                 'sign_cafe', F_EMIT | F_DOUBLE, 0, 32.0)
    # lit shop window
    lv.wall_quad((Uu(-42), 150, cz0 - 4), (Uu(-26), 150, cz0 - 4),
                 (Uu(-26), 40, cz0 - 4), (Uu(-42), 40, cz0 - 4),
                 'window_lit', F_EMIT | F_DOUBLE, 0, 40.0)
    lv.wall_quad((Uu(-78), 150, cz0 - 4), (Uu(-62), 150, cz0 - 4),
                 (Uu(-62), 60, cz0 - 4), (Uu(-78), 60, cz0 - 4),
                 'window_dark', F_DOUBLE, 0, 40.0)

    # ---- east block: the alley Harry walks past
    lv.building(Uu(-20), Uu(176), Uu(36), Uu(238), 420, 'brick', 'roof',
                winpat=[0, 0, 0, 0])
    lv.building(Uu(44), Uu(176), Uu(120), Uu(240), 360, 'siding', 'roof',
                winpat=[0, 0, 0, 0])
    lv.floor_quad(Uu(36), Uu(160), Uu(44), Uu(240), 0, 'alley', 64.0)

    # ---- the street is closed to the north-east by the fence
    lv.wall_box(Uu(-200), Uu(286), Uu(200), Uu(290), 0, 200, 'fence', 0, 48)
    lv.wall_box(Uu(-200), Uu(286), Uu(200), Uu(290), 200, 212, 'metal', 0, 32)
    # and to the west
    lv.wall_box(Uu(-202), Uu(96), Uu(-198), Uu(172), 0, 200, 'fence', 0, 48)

    # ---- props
    def P(model, x, z, rot=0, y=0):
        lv.props.append((model, Uu(x), y, Uu(z), rot))

    P('car', -6, 168, 1500)
    P('car', -16, 168, 1520)
    P('lamp', -52, 150)
    P('lamp', 14, 150)
    P('lamp', -150, 152)
    P('lamp', 96, 152)
    P('dumpster', -40, 84)
    P('trashcan', -46, 86)
    P('trashcan', 30, 162)
    P('bench', -120, 90)
    P('bench', 70, 166)
    P('road_sign', -70, 90)
    P('road_sign', 40, 166)
    P('weeds', -190, 84)
    P('weeds', 188, 170)
    P('weeds', -62, 158)
    P('weeds', 36, 168)
    P('blood', -3, 163, 900)
    P('blood', 5, 173, 200)

    # ---- items: health drinks on the pavement / by the bench
    lv.items.append((TEXID['can'], Uu(-70), 30, Uu(88), 3))
    lv.items.append((TEXID['can'], Uu(68), 30, Uu(168), 3))

    # ---- triggers
    lv.triggers.append((Uu(-14), Uu(156), Uu(6), Uu(180), 'msg', 1, 1))     # the car
    lv.triggers.append((Uu(-200), Uu(282), Uu(200), Uu(292), 'msg', 2, 2))  # fence
    lv.triggers.append((Uu(-60), Uu(172), Uu(-42), Uu(186), 'door', 1, 0))  # cafe door
    lv.triggers.append((Uu(-62), Uu(186), Uu(-40), Uu(200), 'msg', 3, 4))   # cafe hint
    lv.triggers.append((Uu(-60), Uu(10), Uu(-30), Uu(40), 'msg', 4, 5))     # alley
    lv.triggers.append((Uu(-14), Uu(150), Uu(10), Uu(180), 'vn', 1, 0))     # intro
    lv.triggers.append((Uu(-70), Uu(100), Uu(40), Uu(140), 'enemy', 1, 0))  # first monster
    return lv


# ---------------------------------------------------------------- CAFE
def build_cafe():
    """
    map0_s01 - "5 to 2".
    The room is 10 x 12 m (25 x 30 original units), which is the size the
    PlayStation hall reads as; the counter runs along the west wall and the
    pinball machine sits in the north-east corner.
    """
    lv = Level('cafe', True, vheight=320.0,
               fog=(2200, 5200), fogcolor=(40, 34, 28), amb=6)
    lv.spawn = (Uu(-3), Uu(150), 1024, Uu(-3))
    lv.zone = (Uu(-90), Uu(120), Uu(20), Uu(250))

    x0, z0, x1, z1 = Uu(-25), Uu(140), Uu(5), Uu(170)
    h = 320.0
    t = 60.0

    lv.floor_quad(x0, z0, x1, z1, 0, 'tile', 56.0)
    lv.floor_quad(x0, z0, x1, z1, h, 'ceiling', 96.0, 0, F_ROOF)

    # shell
    lv.wall_box(x0, z0, x1, z0 + t, 0, h, 'cafe_wall')
    lv.wall_box(x0, z1 - t, x1, z1, 0, h, 'cafe_wall')
    lv.wall_box(x0, z0, x0 + t, z1, 0, h, 'cafe_wall')
    lv.wall_box(x1 - t, z0, x1, z1, 0, h, 'cafe_wall')

    # carve the doorway out of the south wall collider
    lv.colliders = [c for c in lv.colliders
                    if not (c[0] <= Uu(-14) and c[2] >= Uu(2) and c[1] <= z0 + t)]

    # counter along the west wall
    lv.band(x0 + 50, Uu(155)).box(x0 + 20, 0, Uu(145), x0 + 56, 110, Uu(165),
                                  'counter', 0, 0, uvs=48.0)
    lv.colliders.append((x0 + 20, Uu(145), x0 + 56, Uu(165), 110))
    # booths along the north wall
    for xx in (-20, -8, 2):
        if xx * 40 > x1 - 40 or xx * 40 < x0 + 80:
            continue
        lv.band(Uu(xx), z1 - 30).box(Uu(xx - 3), 0, z1 - 40, Uu(xx + 3), 46,
                                     z1 - 30, 'booth', 0, 0, uvs=40.0)
        lv.band(Uu(xx), z1 - 45).box(Uu(xx - 3), 0, z1 - 46, Uu(xx + 3), 78,
                                     z1 - 40, 'counter', 0, 0, uvs=40.0)
        lv.colliders.append((Uu(xx - 3), z1 - 46, Uu(xx + 3), z1 - 30, 80))

    # a table in the middle
    lv.band(Uu(-10), Uu(155)).box(Uu(-13), 70, Uu(152), Uu(-7), 76, Uu(158),
                                  'counter', 0, 0, uvs=40.0)
    lv.band(Uu(-10), Uu(155)).box(Uu(-11), 0, Uu(154), Uu(-9), 70, Uu(156),
                                  'metal', 0, 0, uvs=16.0)
    lv.colliders.append((Uu(-13), Uu(152), Uu(-7), Uu(158), 76))

    # pickups at the decomp coordinates, folded into the room
    def P(model, x, z, y=0, rot=0):
        lv.props.append((model, x, y, z, rot))

    P('knife', Uu(5.09), Uu(151.12))
    P('flashlight', Uu(5.18), Uu(147.28))
    P('map', Uu(5.24), Uu(147.21))
    P('radio', Uu(2.31), Uu(153.95))
    P('pinball', Uu(1), Uu(165), 0, 1600)
    P('tv', x0 + 90, Uu(158), 130)
    P('jukebox', Uu(-2), Uu(167), 0, 2048)
    P('chair', Uu(-16), Uu(150), 0, 300)
    P('chair', Uu(-4), Uu(158), 0, 1200)

    lv.items.append((TEXID['knife'], Uu(5.09), 78, Uu(151.12), 10))
    lv.items.append((TEXID['flashlight'], Uu(5.18), 78, Uu(147.28), 11))
    lv.items.append((TEXID['paper'], Uu(5.24), 78, Uu(147.21), 12))
    lv.items.append((TEXID['radio'], Uu(2.31), 78, Uu(153.95), 13))
    lv.items.append((TEXID['can'], x0 + 60, 118, Uu(150), 14))

    lv.triggers.append((x0 + 20, Uu(145), x0 + 70, Uu(165), 'msg', 5, 15))
    lv.triggers.append((Uu(-1), z1 - 50, Uu(4), z1 - 20, 'msg', 6, 16))
    lv.triggers.append((x0 + 70, Uu(154), x0 + 100, Uu(166), 'msg', 7, 17))
    lv.triggers.append((Uu(-14), z0, Uu(2), z0 + 40, 'door', 0, 0))
    lv.triggers.append((Uu(-14), z0 + 20, Uu(2), z0 + 70, 'vn', 2, 0))
    lv.triggers.append((x0 + 80, Uu(148), Uu(4), Uu(166), 'vn', 3, 0))
    lv.triggers.append((x0 + 80, Uu(148), Uu(4), Uu(166), 'enemy', 2, 0))
    return lv


# ---------------------------------------------------------------- models
def build_models():
    M = {}

    def box_at(m, cx, cy, cz, sx, sy, sz, tex, col=0, flags=0, uvs=32.0, top=None):
        m.box(cx - sx / 2, cy, cz - sz / 2, cx + sx / 2, cy + sy, cz + sz / 2,
              tex, col, flags, top, uvs)

    # --- abandoned car (the one Harry crashed) -------------------------
    m = Mesh()
    m.box(-90, 18, -200, 90, 78, 200, 'car', 0, 0, 'car', 40.0)
    m.box(-78, 78, -110, 78, 140, 70, 'car', 0, 0, 'car', 40.0)     # cabin
    m.quad((-78, 140, -110), (78, 140, -110), (78, 78, -110), (-78, 78, -110),
           'carglass', 0, F_DOUBLE, 40.0)
    m.quad((-78, 140, 70), (78, 140, 70), (78, 78, 70), (-78, 78, 70),
           'carglass', 0, F_DOUBLE, 40.0)
    for (wx, wz) in ((-84, -130), (84, -130), (-84, 130), (84, 130)):
        box_at(m, wx, 0, wz, 18, 40, 60, 'metal', 0, 0, 16.0)
    m.box(-80, 60, 200, 80, 96, 206, 'metal', 0, 0, 16.0)            # bumper
    M['car'] = m

    # --- street lamp ---------------------------------------------------
    m = Mesh()
    box_at(m, 0, 0, 0, 10, 320, 10, 'metal', 0, 0, 16.0)
    m.box(-12, 300, -40, 12, 316, 200, 'metal', 0, 0, 16.0)
    m.quad((-14, 296, 190), (14, 296, 190), (14, 282, 190), (-14, 282, 190),
           'lamp', 0, F_EMIT | F_DOUBLE, 16.0)
    M['lamp'] = m

    # --- dumpster ------------------------------------------------------
    m = Mesh()
    m.box(-90, 0, -50, 90, 110, 50, 'metal', 0, 0, 'metal', 32.0)
    m.box(-94, 110, -54, 94, 118, 54, 'metal', 0, 0, 'metal', 32.0)
    M['dumpster'] = m

    # --- trash can -----------------------------------------------------
    m = Mesh()
    m.box(-26, 0, -26, 26, 80, 26, 'metal', 0, 0, 'metal', 24.0)
    m.box(-30, 80, -30, 30, 86, 30, 'metal', 0, 0, 'metal', 24.0)
    M['trashcan'] = m

    # --- bench ---------------------------------------------------------
    m = Mesh()
    m.box(-80, 40, -22, 80, 48, 22, 'bench', 0, 0, 'bench', 24.0)
    m.box(-80, 48, 14, 80, 96, 22, 'bench', 0, 0, 'bench', 24.0)
    box_at(m, -66, 0, 0, 10, 40, 36, 'metal', 0, 0, 16.0)
    box_at(m, 66, 0, 0, 10, 40, 36, 'metal', 0, 0, 16.0)
    M['bench'] = m

    # --- road sign -----------------------------------------------------
    m = Mesh()
    box_at(m, 0, 0, 0, 8, 200, 8, 'metal', 0, 0, 16.0)
    m.quad((-26, 200, 0), (26, 200, 0), (26, 160, 0), (-26, 160, 0),
           'sign_road', 0, F_DOUBLE, 16.0)
    M['road_sign'] = m

    # --- weeds / grass tuft (crossed alpha quads) ----------------------
    m = Mesh()
    m.quad((-40, 50, 0), (40, 50, 0), (40, 0, 0), (-40, 0, 0), 'weeds', 0, F_DOUBLE | F_ALPHA, 32.0)
    m.quad((0, 50, -40), (0, 50, 40), (0, 0, 40), (0, 0, -40), 'weeds', 0, F_DOUBLE | F_ALPHA, 32.0)
    M['weeds'] = m

    # --- blood decal ---------------------------------------------------
    m = Mesh()
    m.quad((-50, 1, 50), (50, 1, 50), (50, 1, -50), (-50, 1, -50), 'blood', 0, F_DOUBLE | F_ALPHA, 100.0)
    M['blood'] = m

    # --- counter (cafe) ------------------------------------------------
    m = Mesh()
    m.box(-120, 0, -60, 120, 106, 60, 'counter', 0, 0, 'counter', 48.0)
    m.box(-128, 106, -68, 128, 112, 68, 'counter', 0, 0, 'counter', 48.0)
    M['counter'] = m

    # --- cafe table + chair --------------------------------------------
    m = Mesh()
    box_at(m, 0, 0, 0, 12, 70, 12, 'metal', 0, 0, 16.0)
    m.box(-40, 70, -40, 40, 76, 40, 'counter', 0, 0, 'counter', 40.0)
    M['table'] = m

    m = Mesh()
    m.box(-22, 40, -22, 22, 46, 22, 'bench', 0, 0, 'bench', 24.0)
    m.box(-22, 46, 16, 22, 90, 22, 'bench', 0, 0, 'bench', 24.0)
    for (dx, dz) in ((-18, -18), (18, -18), (-18, 16), (18, 16)):
        box_at(m, dx, 0, dz, 6, 40, 6, 'metal', 0, 0, 12.0)
    M['chair'] = m

    # --- pinball machine ----------------------------------------------
    m = Mesh()
    m.box(-46, 60, -70, 46, 110, 70, 'pinball', 0, 0, 'pinball', 40.0)
    m.box(-46, 0, -70, 46, 60, 70, 'metal', 0, 0, 24.0)
    m.quad((-46, 110, 70), (46, 110, 70), (46, 110, -70), (-46, 110, -70),
           'pinball', 0, 0, 40.0)
    M['pinball'] = m

    # --- CRT tv on a stand --------------------------------------------
    m = Mesh()
    m.box(-40, 0, -30, 40, 60, 30, 'counter', 0, 0, 24.0)
    m.box(-44, 60, -34, 44, 120, 34, 'metal', 0, 0, 24.0)
    m.quad((-34, 112, -36), (34, 112, -36), (34, 70, -36), (-34, 70, -36),
           'tv', 0, F_EMIT, 32.0)
    M['tv'] = m

    # --- jukebox -------------------------------------------------------
    m = Mesh()
    m.box(-40, 0, -26, 40, 140, 26, 'counter', 0, 0, 'counter', 40.0)
    m.quad((-34, 130, -28), (34, 130, -28), (34, 40, -28), (-34, 40, -28),
           'jukebox', 0, F_EMIT, 40.0)
    M['jukebox'] = m

    # --- pickups -------------------------------------------------------
    m = Mesh()
    m.quad((-14, 8, 3), (14, 8, 3), (14, 8, -3), (-14, 8, -3), 'knife', 0, F_DOUBLE, 16.0)
    M['knife'] = m

    m = Mesh()
    m.box(-5, 0, -5, 5, 26, 5, 'flashlight', 0, 0, 'flashlight', 16.0)
    M['flashlight'] = m

    m = Mesh()
    m.quad((-14, 2, 10), (14, 2, 10), (14, 2, -10), (-14, 2, -10), 'paper', 0, F_DOUBLE, 24.0)
    M['map'] = m

    m = Mesh()
    m.box(-8, 0, -4, 8, 10, 4, 'radio', 0, 0, 'radio', 16.0)
    M['radio'] = m

    # --- Harry Mason ---------------------------------------------------
    M['harry'] = humanoid('harry_top', 'harry_legs', 'skin', 'hair', 176)

    # --- Cybil Bennett -------------------------------------------------
    M['cybil'] = humanoid('cybil_top', 'cybil_legs' if 'cybil_legs' in TEXID else 'cybil_top',
                          'skin', 'hair', 170)

    # --- Air Screamer --------------------------------------------------
    m = Mesh()
    # body
    m.box(-14, 0, -70, 14, 26, 70, 'flesh', 0, 0, 'flesh', 24.0)
    # head + beak
    m.box(-12, 4, 70, 12, 28, 104, 'flesh', 0, 0, 'flesh', 24.0)
    m.quad((-6, 16, 104), (6, 16, 104), (6, 10, 128), (-6, 10, 128), 'chitin', 0, F_DOUBLE, 16.0)
    # wings: big double sided membranes with ribs
    for s in (-1, 1):
        x0 = s * 12
        x1 = s * 96
        m.quad((x0, 24, -40), (x1, 40, 10), (x1, 10, 60), (x0, 14, 20),
               'wing', 0, F_DOUBLE, 64.0)
        m.quad((x0, 14, 20), (x1, 10, 60), (x1, -10, 40), (x0, 2, -10),
               'wing', 0, F_DOUBLE, 64.0)
        # leg talons
        box_at(m, s * 10, -20, -40, 6, 22, 6, 'chitin', 0, 0, 12.0)
    # tail
    m.quad((-4, 10, -70), (4, 10, -70), (0, 0, -110), (0, 0, -110), 'flesh', 0, F_DOUBLE, 24.0)
    M['airscreamer'] = m

    # --- Shambler / grey child -----------------------------------------
    M['shambler'] = humanoid('flesh', 'flesh', 'flesh', 'chitin', 130, thin=True)

    return M


def humanoid(top, legs, skin, hair, height, thin=False):
    """Blocky PSX-style character, origin at the feet, facing +Z."""
    m = Mesh()
    s = 1.0 if not thin else 0.72
    hw = 15 * s     # half width of torso
    hh = height
    leg_h = hh * 0.44
    torso_h = hh * 0.34
    head_h = hh * 0.16
    arm_h = hh * 0.33

    # legs
    for sx in (-1, 1):
        m.box(sx * hw * 0.5 - 6 * s, 0, -7 * s, sx * hw * 0.5 + 6 * s, leg_h, 7 * s,
              legs, 0, 0, legs, 16.0)
    # torso
    m.box(-hw, leg_h, -8 * s, hw, leg_h + torso_h, 8 * s, top, 0, 0, top, 16.0)
    # shoulders / arms
    for sx in (-1, 1):
        x = sx * (hw + 5 * s)
        m.box(x - 5 * s, leg_h + 4, -6 * s, x + 5 * s, leg_h + arm_h, 6 * s,
              top, 0, 0, top, 16.0)
        # hand
        m.box(x - 4 * s, leg_h + arm_h - 10, -4 * s, x + 4 * s, leg_h + arm_h,
              4 * s, skin, 0, 0, skin, 8.0)
    # neck
    m.box(-4 * s, leg_h + torso_h, -4 * s, 4 * s, leg_h + torso_h + 5, 4 * s,
          skin, 0, 0, skin, 8.0)
    # head
    hy = leg_h + torso_h + 5
    m.box(-9 * s, hy, -9 * s, 9 * s, hy + head_h, 9 * s, skin, 0, 0, skin, 8.0)
    # hair cap
    m.box(-10 * s, hy + head_h * 0.6, -10 * s, 10 * s, hy + head_h + 3, 10 * s,
          hair, 0, 0, hair, 8.0)
    # face plate (dark, reads as a face at this resolution)
    m.quad((-6 * s, hy + head_h * 0.62, 9.5 * s), (6 * s, hy + head_h * 0.62, 9.5 * s),
           (6 * s, hy + head_h * 0.18, 9.5 * s), (-6 * s, hy + head_h * 0.18, 9.5 * s),
           hair, 0, 0, 8.0)
    return m


# ---------------------------------------------------------------- writer
def pack_level(lv):
    o = bytearray()
    o += struct.pack('<8sBB', lv.name.encode()[:8].ljust(8, b'\0'),
                     1 if lv.interior else 0, lv.amb)
    o += struct.pack('<HHH', int(lv.vheight), lv.fog[0], lv.fog[1])
    o += struct.pack('<H', rgb565(lv.fogcolor))
    o += struct.pack('<hhhh', int(lv.spawn[0]), int(lv.spawn[1]),
                     int(lv.zone[0]), int(lv.zone[1]))
    o += struct.pack('<hhhh', int(lv.zone[2]), int(lv.zone[3]), int(lv.spawn[2]),
                     int(lv.spawn[3]))

    # bands
    keys = sorted(lv.bands.keys())
    o += struct.pack('<H', len(keys))
    for k in keys:
        m = lv.bands[k]
        fb, wide = faces_bytes(m)
        o += struct.pack('<hhHH', int(k[0]), int(k[1]), len(m.v), len(m.f))
        o += m.verts_bytes()
        o += fb

    # floors
    o += struct.pack('<H', len(lv.floors))
    for (x0, z0, x1, z1, y, tex, col, flags, uvs) in lv.floors:
        o += struct.pack('<hhhhHBBBB', int(x0), int(z0), int(x1), int(z1), int(y),
                         TEXID[tex], col, flags, int(uvs))

    # colliders
    o += struct.pack('<H', len(lv.colliders))
    for (x0, z0, x1, z1, h) in lv.colliders:
        o += struct.pack('<hhhhH', int(x0), int(z0), int(x1), int(z1), int(h))

    # props
    o += struct.pack('<H', len(lv.props))
    for (name, x, y, z, rot) in lv.props:
        o += struct.pack('<BhhhH', MODELID[name], int(x), int(y), int(z), int(rot) & 0xFFFF)

    # items
    o += struct.pack('<B', len(lv.items))
    for (tex, x, y, z, msg) in lv.items:
        o += struct.pack('<BhhhB', tex, int(x), int(y), int(z), msg)

    # triggers
    o += struct.pack('<B', len(lv.triggers))
    for (x0, z0, x1, z1, kind, tid, msg) in lv.triggers:
        k = {'msg': 0, 'door': 2, 'vn': 3, 'enemy': 4}.get(kind, 0)
        o += struct.pack('<hhhhBBB', int(x0), int(z0), int(x1), int(z1), k,
                         tid & 0xFF, msg & 0xFF)
    return o


def main():
    global TEXID, MODELID
    tex = build_textures()
    # add a couple of late textures used by the models
    r = Rng(77)
    t = Tex(16, 16, 54)
    t.speckle(r, 0, 0, 15, 15, [54, 2, 4], 40)
    tex.append(t)
    for i, nm in enumerate(['cybil_legs']):
        pass
    names = ['asphalt', 'road', 'concrete', 'brick', 'brick_warm', 'wall', 'cafe_wall',
             'tile', 'wood_floor', 'ceiling', 'window_lit', 'window_dark', 'shutter',
             'door_wood', 'counter', 'pinball', 'tv', 'sign_cafe', 'lamp', 'fence',
             'weeds', 'plaster', 'car', 'carglass', 'metal', 'can', 'paper', 'knife',
             'flashlight', 'radio', 'flesh', 'wing', 'harry_top', 'harry_legs', 'skin',
             'hair', 'cybil_top', 'blood', 'shadow', 'vent', 'sign_road', 'bench',
             'jukebox', 'booth', 'alley', 'grass', 'siding', 'sky', 'trim', 'roof',
             'chitin', 'cybil_legs']
    TEXID = {n: i for i, n in enumerate(names)}
    assert len(tex) == len(names), (len(tex), len(names))

    models = build_models()
    mnames = list(models.keys())
    MODELID = {n: i for i, n in enumerate(mnames)}

    levels = [build_prologue(), build_cafe()]

    out = bytearray()
    out += b'SSLP'
    out += struct.pack('<HBBBBB', 1, len(PAL), len(tex), len(mnames), len(levels), 0)
    for c in PAL:
        out += struct.pack('<H', rgb565(c))
    for t in tex:
        out += struct.pack('<BB', t.w, t.h)
        out += bytes(t.d)
    for n in mnames:
        m = models[n]
        fb, wide = faces_bytes(m)
        out += struct.pack('<8sHH', n.encode()[:8].ljust(8, b'\0'), len(m.v), len(m.f))
        out += m.verts_bytes()
        out += fb
    for lv in levels:
        out += pack_level(lv)

    os.makedirs(RES, exist_ok=True)
    path = os.path.join(RES, 'ss_data.bin')
    with open(path, 'wb') as f:
        f.write(out)

    # stats
    obands = sum(len(l.bands) for l in levels)
    ofaces = sum(len(m.f) for l in levels for m in l.bands.values())
    print('levels: %s' % [(l.name, len(l.bands), sum(len(m.f) for m in l.bands.values()),
                           len(l.colliders), len(l.triggers)) for l in levels])
    print('models: %d  textures: %d  faces: %d  bytes: %d' %
          (len(mnames), len(tex), ofaces, len(out)))
    dump_preview(levels)


def dump_preview(levels):
    """Top-down plan of every level, for documentation and sanity checking."""
    try:
        from PIL import Image, ImageDraw
    except Exception:
        return
    for lv in levels:
        x0, z0, x1, z1 = lv.zone
        sc = 0.06
        W = int((x1 - x0) * sc) + 20
        H = int((z1 - z0) * sc) + 20
        im = Image.new('RGB', (W, H), (12, 12, 14))
        d = ImageDraw.Draw(im)
        for (fx0, fz0, fx1, fz1, y, tex, col, flags, uvs) in lv.floors:
            if y > 20:
                continue
            c = (34, 34, 40) if tex == 'road' else (26, 26, 30)
            if tex in ('tile', 'concrete', 'alley'):
                c = (30, 30, 36)
            d.rectangle([(fx0 - x0) * sc + 10, (fz0 - z0) * sc + 10,
                         (fx1 - x0) * sc + 10, (fz1 - z0) * sc + 10], fill=c)
        for (cx0, cz0, cx1, cz1, h) in lv.colliders:
            d.rectangle([(cx0 - x0) * sc + 10, (cz0 - z0) * sc + 10,
                         (cx1 - x0) * sc + 10, (cz1 - z0) * sc + 10],
                        outline=(120, 60, 50))
        for (kind, ID, m) in [(t[4], t[5], t[6]) for t in lv.triggers]:
            pass
        for tr in lv.triggers:
            tx0, tz0, tx1, tz1, kind, tid, msg = tr
            d.rectangle([(tx0 - x0) * sc + 10, (tz0 - z0) * sc + 10,
                         (tx1 - x0) * sc + 10, (tz1 - z0) * sc + 10],
                        outline=(60, 160, 220))
        for (texid, x, y, z, msg) in lv.items:
            d.ellipse([(x - x0) * sc + 6, (z - z0) * sc + 6,
                       (x - x0) * sc + 14, (z - z0) * sc + 14], fill=(220, 200, 90))
        for (name, x, y, z, rot) in lv.props:
            d.rectangle([(x - x0) * sc + 7, (z - z0) * sc + 7,
                         (x - x0) * sc + 13, (z - z0) * sc + 13], fill=(90, 200, 120))
        sx, sz = lv.spawn[0], lv.spawn[1]
        d.ellipse([(sx - x0) * sc + 4, (sz - z0) * sc + 4,
                   (sx - x0) * sc + 16, (sz - z0) * sc + 16], outline=(240, 60, 60), width=2)
        im.save(os.path.join(RES, 'plan_%s.png' % lv.name))
        print('plan written:', lv.name, W, H)


if __name__ == '__main__':
    main()
