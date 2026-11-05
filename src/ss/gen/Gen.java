package ss.gen;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ss.core.Core;
import ss.core.Model;
import ss.core.Tex;

/**
 * Procedural art.
 *
 * Every texture and mesh in the game is generated at start-up by this class
 * instead of shipping bitmaps: a 3D engine needs the textures in RAM anyway,
 * and generating them costs no heap in the JAR and almost none after loading.
 * The functions mirror tools/assetgen.py one to one; that script is used to
 * preview the same data as a PNG on a PC.
 */
public final class Gen
{
    public static int[] rnd = new int[1];

    private Gen() {}

    public static void seed(int s)
    {
        rnd[0] = s == 0 ? 0x1234567 : s;
    }

    public static int next()
    {
        rnd[0] = (rnd[0] * 1103515245 + 12345) & 0x7FFFFFFF;
        return rnd[0] >> 8;
    }

    public static int r(int n)
    {
        if (n <= 0) return 0;
        return next() % n;
    }

    public static int r(int a, int b)
    {
        if (b <= a) return a;
        return a + next() % (b - a);
    }

    // ------------------------------------------------------------------ textures
    private static short[] PAL;


    /**
     * The global 64 colour PSX palette (4 bits of hue, mud and blood).  Every
     * texture indexes into this, which is what lets light and fog live in a
     * single 16 KB table instead of per texture ramps.
     */
    public static short[] palette()
    {
        int[][] p = {
            {0, 0, 0}, {14, 14, 18}, {26, 26, 32}, {40, 40, 48}, {56, 56, 64},
            {74, 74, 80}, {94, 92, 90}, {118, 114, 108}, {140, 136, 128}, {168, 164, 154},
            {196, 192, 182}, {228, 224, 212}, {252, 250, 242},
            {46, 46, 50}, {60, 60, 64}, {78, 78, 82}, {98, 98, 100}, {112, 110, 104},
            {132, 128, 118}, {152, 146, 132},
            {88, 46, 38}, {110, 58, 46}, {134, 72, 56}, {158, 92, 72}, {66, 34, 28},
            {78, 56, 34}, {102, 74, 44}, {128, 96, 60}, {152, 118, 76},
            {34, 46, 28}, {48, 66, 34}, {66, 88, 46},
            {20, 28, 42}, {34, 48, 68}, {54, 76, 100}, {84, 110, 138},
            {110, 80, 26}, {162, 124, 40}, {214, 182, 84}, {255, 232, 150},
            {70, 16, 16}, {108, 24, 22}, {152, 38, 30}, {192, 62, 48},
            {132, 92, 74}, {168, 122, 96}, {204, 158, 126},
            {56, 42, 32}, {84, 64, 44}, {114, 90, 64},
            {66, 70, 78}, {90, 96, 106}, {120, 128, 138}, {152, 158, 168},
            {46, 50, 60}, {30, 30, 36}, {80, 86, 98}, {104, 112, 126},
            {58, 50, 42}, {136, 124, 96}, {96, 28, 38}, {44, 68, 58},
            {86, 38, 68}, {200, 200, 210}
        };
        short[] out = new short[p.length];
        for (int i = 0; i < p.length; i++)
        {
            int r = p[i][0] & 0xF8;
            int g = p[i][1] & 0xFC;
            int b = p[i][2] & 0xF8;
            out[i] = (short) ((r << 8) | (g << 3) | (b >> 3));
        }
        return out;
    }

    public static void setPalette(short[] p)
    {
        PAL = p;
    }

    private static void px(Tex t, int x, int y, int c)
    {
        if (c < 0) c = 0;
        if (c > 63) c = 63;
        t.idx[((y & t.maskH) * t.w) + (x & t.maskW)] = (byte) c;
    }

    private static void fill(Tex t, int c)
    {
        for (int i = 0; i < t.idx.length; i++) t.idx[i] = (byte) c;
    }

    private static void rect(Tex t, int x0, int y0, int x1, int y1, int c)
    {
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) px(t, x, y, c);
    }

    private static void speckle(Tex t, int x0, int y0, int x1, int y1, int[] cols, int n)
    {
        for (int i = 0; i < n; i++)
            px(t, r(x0, x1 + 1), r(y0, y1 + 1), cols[r(cols.length)]);
    }

    /**
     * Builds every texture.  The sizes are power of two so the rasterizer can
     * mask instead of modulo.
     */
    public static Tex[] textures()
    {
        java.util.Vector v = new java.util.Vector();
        seed(0xC0FFEE);
        Tex t;

        // 0 asphalt
        t = new Tex(32, 32); fill(t, 14);
        speckle(t, 0, 0, 31, 31, new int[]{13, 15, 16, 4, 3}, 300);
        for (int i = 0; i < 4; i++) { int y = r(32); for (int x = 0; x < 32; x++) px(t, x, y, 13); }
        v.addElement(t);

        // 1 road with worn centre line
        t = new Tex(32, 32); fill(t, 14);
        speckle(t, 0, 0, 31, 31, new int[]{13, 15, 16, 4}, 320);
        for (int yy = 2; yy < 32; yy += 16) rect(t, 15, yy, 17, yy + 8, 19);
        v.addElement(t);

        // 2 concrete
        t = new Tex(32, 32); fill(t, 17);
        speckle(t, 0, 0, 31, 31, new int[]{16, 18, 8, 15, 19}, 260);
        for (int x = 0; x < 32; x++) { px(t, x, 10, 15); px(t, x, 21, 15); }
        for (int y = 0; y < 32; y++) { px(t, 10, y, 15); px(t, 21, y, 15); }
        v.addElement(t);

        // 3 brick
        t = new Tex(32, 32); fill(t, 21);
        for (int row = 0; row < 8; row++)
        {
            int y = row * 4;
            int off = (row % 2 == 0) ? 0 : 4;
            for (int bx = -1; bx < 5; bx++)
            {
                int x0 = bx * 8 + off;
                rect(t, x0, y, x0 + 6, y + 2, 21 + r(3));
                for (int yy = y; yy < y + 3; yy++) px(t, x0 + 7, yy, 24);
            }
        }
        speckle(t, 0, 0, 31, 31, new int[]{24, 20, 29}, 120);
        v.addElement(t);

        // 4 warm brick
        t = new Tex(32, 32); fill(t, 22);
        for (int row = 0; row < 8; row++)
        {
            int y = row * 4;
            int off = (row % 2 == 0) ? 0 : 4;
            for (int bx = -1; bx < 5; bx++)
            {
                int x0 = bx * 8 + off;
                rect(t, x0, y, x0 + 6, y + 2, 21 + r(3));
                for (int yy = y; yy < y + 3; yy++) px(t, x0 + 7, yy, 20);
            }
        }
        v.addElement(t);

        // 5 stained wall
        t = new Tex(32, 32); fill(t, 18);
        speckle(t, 0, 0, 31, 31, new int[]{16, 17, 19, 8, 7}, 240);
        for (int i = 0; i < 6; i++)
        {
            int x = r(32);
            int hh = r(10, 28);
            for (int y = 0; y < hh; y++)
            {
                px(t, x, y, y > hh - 5 ? 8 : 17);
                if (r(4) == 0) px(t, x + 1, y, 16);
            }
        }
        v.addElement(t);

        // 6 cafe interior wall
        t = new Tex(32, 32); fill(t, 27);
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 32; x++) px(t, x, y, (x % 7) == 0 ? 26 : 27);
        rect(t, 0, 0, 31, 9, 19);
        rect(t, 0, 9, 31, 10, 15);
        rect(t, 0, 26, 31, 31, 25);
        v.addElement(t);

        // 7 checker floor
        t = new Tex(32, 32); fill(t, 9);
        for (int ty = 0; ty < 32; ty += 8)
            for (int tx = 0; tx < 32; tx += 8)
            {
                int c = (((tx + ty) / 8) % 2 == 0) ? 9 : 6;
                rect(t, tx, ty, tx + 6, ty + 6, c);
                for (int k = 0; k < 8; k++) { px(t, tx + 7, ty + k, 6); px(t, tx + k, ty + 7, 6); }
            }
        v.addElement(t);

        // 8 wooden floor
        t = new Tex(32, 32); fill(t, 25);
        for (int row = 0; row < 4; row++)
        {
            int y = row * 8;
            for (int x = 0; x < 32; x++)
                for (int dy = 0; dy < 7; dy++)
                    px(t, x, y + dy, 25 + ((x / 5 + row + dy / 4) % 3));
            for (int x = 0; x < 32; x++) px(t, x, y + 7, 24);
        }
        v.addElement(t);

        // 9 ceiling
        t = new Tex(32, 32); fill(t, 18);
        for (int ty = 0; ty < 32; ty += 16)
            for (int tx = 0; tx < 32; tx += 16) rect(t, tx + 1, ty + 1, tx + 14, ty + 14, 17);
        speckle(t, 0, 0, 31, 31, new int[]{8, 7}, 40);
        v.addElement(t);

        // 10 lit window
        t = new Tex(16, 16); fill(t, 34);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(t, x, y, y < 9 ? 35 : 33);
        rect(t, 0, 7, 15, 9, 37);
        v.addElement(t);

        // 11 dark rainy window
        t = new Tex(16, 16); fill(t, 32);
        for (int i = 0; i < 26; i++)
        {
            int x = r(16), y0 = r(16), len = r(3, 12);
            for (int y = y0; y < y0 + len && y < 16; y++) px(t, x, y, (y % 2) == 0 ? 34 : 33);
        }
        for (int x = 0; x < 16; x++) { px(t, x, 0, 2); px(t, x, 15, 2); }
        v.addElement(t);

        // 12 metal shutter
        t = new Tex(16, 16); fill(t, 52);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(t, x, y, (y % 3) == 0 ? 50 : 52);
        speckle(t, 0, 0, 15, 15, new int[]{48, 59}, 40);
        v.addElement(t);

        // 13 wooden door
        t = new Tex(16, 32);
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 32; y++) px(t, x, y, 26 + ((x / 3 + y / 8) % 2));
        rect(t, 1, 1, 14, 2, 25);
        rect(t, 1, 29, 14, 30, 25);
        rect(t, 11, 15, 13, 17, 38);
        v.addElement(t);

        // 14 counter wood
        t = new Tex(32, 32);
        for (int y = 0; y < 32; y++)
            for (int x = 0; x < 32; x++) px(t, x, y, (y < 5 ? 28 : 26) - ((y % 3) == 0 ? 1 : 0));
        v.addElement(t);

        // 15 pinball art
        t = new Tex(16, 32); fill(t, 42);
        rect(t, 0, 0, 15, 5, 43);
        rect(t, 1, 7, 14, 30, 33);
        rect(t, 3, 9, 12, 20, 35);
        for (int i = 0; i < 5; i++) px(t, 4 + i * 2, 11 + i * 2, 39);
        rect(t, 4, 24, 11, 25, 38);
        v.addElement(t);

        // 16 TV screen
        t = new Tex(16, 16); fill(t, 3);
        rect(t, 1, 1, 14, 11, 2);
        for (int y = 2; y < 11; y += 2) for (int x = 2; x < 14; x++) px(t, x, y, 4);
        rect(t, 5, 5, 10, 8, 33);
        v.addElement(t);

        // 17 cafe sign
        t = new Tex(32, 16); fill(t, 37);
        rect(t, 0, 0, 31, 15, 38);
        rect(t, 1, 1, 30, 14, 39);
        int[] five = {6, 4, 7, 4, 8, 4, 5, 5, 5, 6, 6, 7, 7, 7, 8, 8, 5, 9, 5, 10, 6, 11, 7, 11, 8, 10};
        for (int i = 0; i < five.length; i += 2) px(t, five[i], five[i + 1], 11);
        for (int x = 10; x < 14; x++) px(t, x, 4, 11);
        for (int y = 5; y < 12; y++) px(t, 11, y, 11);
        int[] o = {16, 5, 17, 5, 18, 5, 15, 6, 19, 6, 15, 7, 19, 7, 15, 8, 19, 8, 15, 9, 19, 9, 16, 10, 17, 10, 18, 10};
        for (int i = 0; i < o.length; i += 2) px(t, o[i], o[i + 1], 11);
        int[] two = {23, 5, 24, 5, 25, 5, 26, 6, 25, 7, 24, 8, 23, 9, 22, 10, 23, 11, 24, 11, 25, 11, 26, 11};
        for (int i = 0; i < two.length; i += 2) px(t, two[i], two[i + 1], 11);
        v.addElement(t);

        // 18 lamp
        t = new Tex(8, 8); fill(t, 39);
        rect(t, 0, 0, 7, 7, 38);
        rect(t, 1, 1, 6, 6, 39);
        v.addElement(t);

        // 19 fence (0 = transparent)
        t = new Tex(16, 16); fill(t, 0);
        for (int i = -16; i < 16; i += 4)
            for (int k = 0; k < 16; k++)
            {
                px(t, i + k, k, 53);
                px(t, i - k, k, 52);
            }
        v.addElement(t);

        // 20 weeds
        t = new Tex(16, 16); fill(t, 0);
        for (int i = 0; i < 22; i++)
        {
            int x = r(2, 14), hh = r(4, 12), c = 29 + r(2);
            for (int y = 16 - hh; y < 16; y++) px(t, x + (16 - y) / 4, y, c);
        }
        v.addElement(t);

        // 21 plaster
        t = new Tex(32, 32); fill(t, 8);
        speckle(t, 0, 0, 31, 31, new int[]{7, 8, 6, 4, 18}, 280);
        for (int i = 0; i < 7; i++)
        {
            int x = r(32), y = r(32), n = r(4, 12);
            for (int k = 0; k < n; k++) px(t, x + k, y + k / 2, 4);
        }
        v.addElement(t);

        // 22 car paint
        t = new Tex(32, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 32; x++) px(t, x, y, y < 4 ? 34 : (y < 11 ? 33 : 32));
        for (int x = 0; x < 32; x++) px(t, x, 4, 61);
        speckle(t, 0, 0, 31, 15, new int[]{47, 33}, 40);
        v.addElement(t);

        // 23 car glass
        t = new Tex(16, 16); fill(t, 33);
        for (int y = 0; y < 16; y++) { px(t, 0, y, 35); px(t, 15, y, 2); }
        v.addElement(t);

        // 24 rusty metal
        t = new Tex(16, 16); fill(t, 48);
        speckle(t, 0, 0, 15, 15, new int[]{47, 49, 50, 59}, 180);
        for (int y = 0; y < 16; y += 5) for (int x = 0; x < 16; x++) px(t, x, y, 47);
        v.addElement(t);

        // 25 can
        t = new Tex(8, 8); fill(t, 43);
        rect(t, 0, 0, 7, 1, 10);
        rect(t, 0, 6, 7, 7, 10);
        rect(t, 2, 2, 5, 5, 12);
        v.addElement(t);

        // 26 paper map
        t = new Tex(16, 16); fill(t, 11);
        speckle(t, 0, 0, 15, 15, new int[]{10, 11, 9}, 60);
        for (int y = 2; y < 15; y += 3) for (int x = 2; x < 14; x++) px(t, x, y, 7);
        for (int x = 3; x < 14; x += 5) for (int y = 2; y < 15; y++) px(t, x, y, 7);
        v.addElement(t);

        // 27 knife
        t = new Tex(16, 8); fill(t, 53);
        rect(t, 0, 1, 10, 6, 53);
        for (int x = 0; x < 11; x++) px(t, x, 1, 63);
        rect(t, 11, 2, 15, 5, 25);
        v.addElement(t);

        // 28 flashlight
        t = new Tex(8, 16); fill(t, 50);
        rect(t, 0, 0, 7, 4, 53);
        rect(t, 0, 5, 7, 15, 49);
        v.addElement(t);

        // 29 radio
        t = new Tex(8, 8); fill(t, 4);
        rect(t, 0, 0, 7, 7, 3);
        rect(t, 0, 0, 7, 3, 49);
        rect(t, 1, 4, 6, 6, 38);
        px(t, 6, 1, 42);
        v.addElement(t);

        // 30 flesh
        t = new Tex(16, 16); fill(t, 44);
        speckle(t, 0, 0, 15, 15, new int[]{43, 44, 47, 41, 59}, 220);
        for (int i = 0; i < 8; i++)
        {
            int x = r(16), y = r(16), n = r(2, 6);
            for (int k = 0; k < n; k++) px(t, x + k, y + (k % 2), 61);
        }
        v.addElement(t);

        // 31 wing
        t = new Tex(16, 16);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++) px(t, x, y, 47 + ((x + y) % 2));
        for (int i = 0; i < 16; i += 4)
            for (int k = 0; k < 16; k++) px(t, i, k, 43);
        v.addElement(t);

        // 32 harry jacket
        t = new Tex(16, 16); fill(t, 57);
        speckle(t, 0, 0, 15, 15, new int[]{57, 58, 26}, 50);
        v.addElement(t);

        // 33 harry trousers
        t = new Tex(16, 16); fill(t, 54);
        speckle(t, 0, 0, 15, 15, new int[]{54, 2, 4}, 40);
        v.addElement(t);

        // 34 skin
        t = new Tex(8, 8); fill(t, 46);
        speckle(t, 0, 0, 7, 7, new int[]{45, 46, 44}, 12);
        v.addElement(t);

        // 35 hair
        t = new Tex(8, 8); fill(t, 59);
        speckle(t, 0, 0, 7, 7, new int[]{59, 49, 62}, 16);
        v.addElement(t);

        // 36 cybil uniform
        t = new Tex(16, 16); fill(t, 33);
        speckle(t, 0, 0, 15, 15, new int[]{32, 33, 34}, 40);
        v.addElement(t);

        // 37 blood
        t = new Tex(16, 16); fill(t, 0);
        for (int i = 0; i < 70; i++)
        {
            int x = 8 + r(-7, 7), y = 8 + r(-7, 7);
            if ((x - 8) * (x - 8) + (y - 8) * (y - 8) < 30) px(t, x, y, r(3) == 0 ? 41 : 40);
        }
        v.addElement(t);

        // 38 shadow blob
        t = new Tex(16, 16); fill(t, 0);
        for (int y = 0; y < 16; y++)
            for (int x = 0; x < 16; x++)
            {
                int d = (x - 8) * (x - 8) + (y - 8) * (y - 8);
                if (d < 49) px(t, x, y, d < 25 ? 1 : 2);
            }
        v.addElement(t);

        // 39 vent
        t = new Tex(8, 8); fill(t, 5);
        for (int y = 0; y < 8; y += 2) for (int x = 0; x < 8; x++) px(t, x, y, 3);
        v.addElement(t);

        // 40 road sign
        t = new Tex(8, 8); fill(t, 11);
        rect(t, 0, 0, 7, 7, 11);
        rect(t, 1, 2, 6, 3, 7);
        rect(t, 1, 5, 6, 6, 7);
        v.addElement(t);

        // 41 bench
        t = new Tex(16, 16);
        for (int x = 0; x < 16; x++)
            for (int y = 0; y < 16; y++) px(t, x, y, 27 + ((x / 2 + y / 6) % 2));
        v.addElement(t);

        // 42 jukebox
        t = new Tex(16, 16); fill(t, 39);
        rect(t, 1, 1, 14, 6, 37);
        rect(t, 2, 8, 13, 14, 35);
        rect(t, 4, 9, 11, 12, 63);
        v.addElement(t);

        // 43 booth vinyl
        t = new Tex(16, 16); fill(t, 59);
        speckle(t, 0, 0, 15, 15, new int[]{59, 41, 39}, 40);
        v.addElement(t);

        // 44 alley concrete
        t = new Tex(32, 32); fill(t, 15);
        speckle(t, 0, 0, 31, 31, new int[]{14, 16, 13, 4}, 200);
        v.addElement(t);

        // 45 grass
        t = new Tex(16, 16); fill(t, 29);
        speckle(t, 0, 0, 15, 15, new int[]{29, 30, 31, 27}, 140);
        v.addElement(t);

        // 46 siding
        t = new Tex(32, 32); fill(t, 19);
        speckle(t, 0, 0, 31, 31, new int[]{18, 19, 8, 17}, 160);
        for (int y = 0; y < 32; y += 8) for (int x = 0; x < 32; x++) px(t, x, y, 17);
        v.addElement(t);

        // 47 sky
        t = new Tex(16, 16);
        for (int y = 0; y < 16; y++)
        {
            int c = 32 + (y / 5 > 3 ? 3 : y / 5);
            for (int x = 0; x < 16; x++) px(t, x, y, c);
        }
        v.addElement(t);

        // 48 trim
        t = new Tex(8, 8); fill(t, 25);
        for (int x = 0; x < 8; x++) px(t, x, 0, 27);
        v.addElement(t);

        // 49 roof
        t = new Tex(16, 16); fill(t, 17);
        speckle(t, 0, 0, 15, 15, new int[]{17, 16, 19, 15}, 120);
        v.addElement(t);

        // 50 chitin
        t = new Tex(8, 8); fill(t, 62);
        speckle(t, 0, 0, 7, 7, new int[]{62, 2, 41}, 14);
        v.addElement(t);

        // 51 cybil trousers (used by the second humanoid)
        t = new Tex(16, 16); fill(t, 54);
        speckle(t, 0, 0, 15, 15, new int[]{54, 2, 4}, 40);
        v.addElement(t);

        Tex[] out = new Tex[v.size()];
        for (int i = 0; i < out.length; i++) out[i] = (Tex) v.elementAt(i);
        return out;
    }

    // ------------------------------------------------------------------ meshes
    public static final int F_DOUBLE = 1;
    public static final int F_WALL = 2;
    public static final int F_NOFOG = 4;
    public static final int F_EMIT = 8;
    public static final int F_ROOF = 16;
    public static final int F_ALPHA = 32;

    /** Growable mesh builder, no per-call allocation. */
    public static final class MB
    {
        public int nv, nf;
        public int[] vx = new int[64], vy = new int[64], vz = new int[64];
        public int[] tu = new int[64], tv = new int[64];
        public int[] fi = new int[192];
        public int[] ftex = new int[64], fcol = new int[64], fflags = new int[64];

        void growV()
        {
            if (nv < vx.length) return;
            int n = vx.length * 2;
            vx = grow(vx, n); vy = grow(vy, n); vz = grow(vz, n);
            tu = grow(tu, n); tv = grow(tv, n);
        }

        void growF()
        {
            if (nf * 3 + 3 <= fi.length) return;
            int n = fi.length * 2;
            fi = grow(fi, n);
            ftex = grow(ftex, n / 3);
            fcol = grow(fcol, n / 3);
            fflags = grow(fflags, n / 3);
        }

        static int[] grow(int[] a, int n)
        {
            int[] b = new int[n];
            System.arraycopy(a, 0, b, 0, a.length);
            return b;
        }

        public int vert(int x, int y, int z, int u, int v)
        {
            growV();
            vx[nv] = x; vy[nv] = y; vz[nv] = z; tu[nv] = u; tv[nv] = v;
            return nv++;
        }

        public void tri(int a, int b, int c, int tex, int col, int flags)
        {
            growF();
            fi[nf * 3] = a; fi[nf * 3 + 1] = b; fi[nf * 3 + 2] = c;
            ftex[nf] = tex; fcol[nf] = col; fflags[nf] = flags;
            nf++;
        }

        /** Quad with planar UVs derived from its edges (uvs = cm per texel). */
        public void quad(int x0, int y0, int z0, int x1, int y1, int z1,
                         int x2, int y2, int z2, int x3, int y3, int z3,
                         int tex, int col, int flags, int uvs)
        {
            int ux = x1 - x0, uy = y1 - y0, uz = z1 - z0;
            int wx = x2 - x0, wy = y2 - y0, wz = z2 - z0;
            int lu = (int) Core.isqrt(ux * ux + uy * uy + uz * uz);
            int lw = (int) Core.isqrt(wx * wx + wy * wy + wz * wz);
            int uu = lu * 64 / uvs;
            int vv = lw * 64 / uvs;
            int a = vert(x0, y0, z0, 0, 0);
            int b = vert(x1, y1, z1, uu, 0);
            int c = vert(x2, y2, z2, uu, vv);
            int d = vert(x3, y3, z3, 0, vv);
            tri(a, b, c, tex, col, flags);
            tri(a, c, d, tex, col, flags);
        }

        public void box(int x0, int y0, int z0, int x1, int y1, int z1,
                        int tex, int col, int flags, int uvs)
        {
            quad(x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, tex, col, flags, uvs);
            quad(x1, y1, z1, x0, y1, z1, x0, y0, z1, x1, y0, z1, tex, col, flags, uvs);
            quad(x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, tex, col, flags, uvs);
            quad(x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, tex, col, flags, uvs);
            quad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, tex, col, flags, uvs);
            if (y0 > 0)
                quad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, tex, col, flags, uvs);
        }

        public Model toModel(String name)
        {
            Model m = new Model(nv, nf);
            m.name = name;
            System.arraycopy(vx, 0, m.vx, 0, nv);
            System.arraycopy(vy, 0, m.vy, 0, nv);
            System.arraycopy(vz, 0, m.vz, 0, nv);
            System.arraycopy(tu, 0, m.tu, 0, nv);
            System.arraycopy(tv, 0, m.tv, 0, nv);
            System.arraycopy(fi, 0, m.fi, 0, nf * 3);
            System.arraycopy(ftex, 0, m.ftex, 0, nf);
            System.arraycopy(fcol, 0, m.fcol, 0, nf);
            System.arraycopy(fflags, 0, m.fflags, 0, nf);
            m.computeNormals();
            m.bounds();
            return m;
        }
    }
}
