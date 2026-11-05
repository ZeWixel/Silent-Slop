package ss.gen;

import ss.core.Model;

/**
 * The prop and character meshes.  Everything is low poly on purpose: the
 * silhouette has to read from an isometric camera at 176x208, and the PS1
 * originals were in the same ballpark (150-400 triangles per character).
 */
public final class Models
{
    public static final int CAR = 0;
    public static final int LAMP = 1;
    public static final int DUMPSTER = 2;
    public static final int TRASHCAN = 3;
    public static final int BENCH = 4;
    public static final int ROAD_SIGN = 5;
    public static final int WEEDS = 6;
    public static final int BLOOD = 7;
    public static final int COUNTER = 8;
    public static final int TABLE = 9;
    public static final int CHAIR = 10;
    public static final int PINBALL = 11;
    public static final int TV = 12;
    public static final int JUKEBOX = 13;
    public static final int KNIFE = 14;
    public static final int FLASHLIGHT_P = 15;
    public static final int MAP = 16;
    public static final int RADIO_P = 17;
    public static final int HARRY = 18;
    public static final int CYBIL = 19;
    public static final int AIRSCREAMER = 20;
    public static final int SHAMBLER = 21;
    public static final int COUNT = 22;

    private Models() {}

    private static void box(Gen.MB m, int cx, int cy, int cz, int sx, int sy, int sz,
                            int tex, int uvs)
    {
        m.box(cx - sx / 2, cy, cz - sz / 2, cx + sx / 2, cy + sy, cz + sz / 2,
              tex, 0, 0, uvs);
    }

    public static Model[] build()
    {
        Model[] a = new Model[COUNT];

        // --- cars ---------------------------------------------------------
        Gen.MB m = new Gen.MB();
        m.box(-90, 18, -200, 90, 78, 200, Tex.CAR, 0, 0, 10);
        m.box(-78, 78, -110, 78, 140, 70, Tex.CAR, 0, 0, 10);
        m.quad(-78, 140, -110, 78, 140, -110, 78, 78, -110, -78, 78, -110,
               Tex.CARGLASS, 0, Gen.F_DOUBLE, 10);
        m.quad(-78, 140, 70, 78, 140, 70, 78, 78, 70, -78, 78, 70,
               Tex.CARGLASS, 0, Gen.F_DOUBLE, 10);
        m.box(-96, 0, -130, -78, 40, -70, Tex.METAL, 0, 0, 4);
        m.box(78, 0, -130, 96, 40, -70, Tex.METAL, 0, 0, 4);
        m.box(-96, 0, 130, -78, 40, 190, Tex.METAL, 0, 0, 4);
        m.box(78, 0, 130, 96, 40, 190, Tex.METAL, 0, 0, 4);
        m.box(-80, 60, 200, 80, 96, 206, Tex.METAL, 0, 0, 4);
        a[CAR] = m.toModel("car");

        // --- street lamp ---------------------------------------------------
        m = new Gen.MB();
        box(m, 0, 0, 0, 10, 320, 10, Tex.METAL, 4);
        m.box(-12, 300, 160, 12, 316, 200, Tex.METAL, 0, 0, 4);
        m.quad(-14, 296, 190, 14, 296, 190, 14, 282, 190, -14, 282, 190,
               Tex.LAMP, 0, Gen.F_EMIT | Gen.F_DOUBLE, 4);
        a[LAMP] = m.toModel("lamp");

        // --- dumpster ------------------------------------------------------
        m = new Gen.MB();
        m.box(-90, 0, -50, 90, 110, 50, Tex.METAL, 0, 0, 8);
        m.box(-94, 110, -54, 94, 118, 54, Tex.METAL, 0, 0, 8);
        a[DUMPSTER] = m.toModel("dumpster");

        // --- trash can -----------------------------------------------------
        m = new Gen.MB();
        m.box(-26, 0, -26, 26, 80, 26, Tex.METAL, 0, 0, 6);
        m.box(-30, 80, -30, 30, 86, 30, Tex.METAL, 0, 0, 6);
        a[TRASHCAN] = m.toModel("trashcan");

        // --- bench ---------------------------------------------------------
        m = new Gen.MB();
        m.box(-80, 40, -22, 80, 48, 22, Tex.BENCH, 0, 0, 6);
        m.box(-80, 48, 14, 80, 96, 22, Tex.BENCH, 0, 0, 6);
        box(m, -66, 0, 0, 10, 40, 36, Tex.METAL, 4);
        box(m, 66, 0, 0, 10, 40, 36, Tex.METAL, 4);
        a[BENCH] = m.toModel("bench");

        // --- road sign -----------------------------------------------------
        m = new Gen.MB();
        box(m, 0, 0, 0, 8, 200, 8, Tex.METAL, 4);
        m.quad(-26, 200, 0, 26, 200, 0, 26, 160, 0, -26, 160, 0,
               Tex.SIGN_ROAD, 0, Gen.F_DOUBLE, 4);
        a[ROAD_SIGN] = m.toModel("sign");

        // --- weeds ---------------------------------------------------------
        m = new Gen.MB();
        m.quad(-40, 50, 0, 40, 50, 0, 40, 0, 0, -40, 0, 0,
               Tex.WEEDS, 0, Gen.F_DOUBLE | Gen.F_ALPHA, 8);
        m.quad(0, 50, -40, 0, 50, 40, 0, 0, 40, 0, 0, -40,
               Tex.WEEDS, 0, Gen.F_DOUBLE | Gen.F_ALPHA, 8);
        a[WEEDS] = m.toModel("weeds");

        // --- blood decal ---------------------------------------------------
        m = new Gen.MB();
        m.quad(-50, 1, 50, 50, 1, 50, 50, 1, -50, -50, 1, -50,
               Tex.BLOOD, 0, Gen.F_DOUBLE | Gen.F_ALPHA, 25);
        a[BLOOD] = m.toModel("blood");

        // --- cafe furniture -------------------------------------------------
        m = new Gen.MB();
        m.box(-120, 0, -60, 120, 106, 60, Tex.COUNTER, 0, 0, 12);
        m.box(-128, 106, -68, 128, 112, 68, Tex.COUNTER, 0, 0, 12);
        a[COUNTER] = m.toModel("counter");

        m = new Gen.MB();
        box(m, 0, 0, 0, 12, 70, 12, Tex.METAL, 4);
        m.box(-40, 70, -40, 40, 76, 40, Tex.COUNTER, 0, 0, 10);
        a[TABLE] = m.toModel("table");

        m = new Gen.MB();
        m.box(-22, 40, -22, 22, 46, 22, Tex.BENCH, 0, 0, 6);
        m.box(-22, 46, 16, 22, 90, 22, Tex.BENCH, 0, 0, 6);
        box(m, -18, 0, -18, 6, 40, 6, Tex.METAL, 3);
        box(m, 18, 0, -18, 6, 40, 6, Tex.METAL, 3);
        box(m, -18, 0, 16, 6, 40, 6, Tex.METAL, 3);
        box(m, 18, 0, 16, 6, 40, 6, Tex.METAL, 3);
        a[CHAIR] = m.toModel("chair");

        m = new Gen.MB();
        m.box(-46, 60, -70, 46, 110, 70, Tex.PINBALL, 0, 0, 10);
        m.box(-46, 0, -70, 46, 60, 70, Tex.METAL, 0, 0, 6);
        m.quad(-46, 110, 70, 46, 110, 70, 46, 110, -70, -46, 110, -70,
               Tex.PINBALL, 0, 0, 10);
        a[PINBALL] = m.toModel("pinball");

        m = new Gen.MB();
        m.box(-40, 0, -30, 40, 60, 30, Tex.COUNTER, 0, 0, 6);
        m.box(-44, 60, -34, 44, 120, 34, Tex.METAL, 0, 0, 6);
        m.quad(-34, 112, -36, 34, 112, -36, 34, 70, -36, -34, 70, -36,
               Tex.TV, 0, Gen.F_EMIT, 8);
        a[TV] = m.toModel("tv");

        m = new Gen.MB();
        m.box(-40, 0, -26, 40, 140, 26, Tex.COUNTER, 0, 0, 10);
        m.quad(-34, 130, -28, 34, 130, -28, 34, 40, -28, -34, 40, -28,
               Tex.JUKEBOX, 0, Gen.F_EMIT, 10);
        a[JUKEBOX] = m.toModel("jukebox");

        // --- pickups --------------------------------------------------------
        m = new Gen.MB();
        m.quad(-14, 8, 3, 14, 8, 3, 14, 8, -3, -14, 8, -3,
               Tex.KNIFE, 0, Gen.F_DOUBLE, 4);
        a[KNIFE] = m.toModel("knife");

        m = new Gen.MB();
        m.box(-5, 0, -5, 5, 26, 5, Tex.FLASHLIGHT, 0, 0, 4);
        a[FLASHLIGHT_P] = m.toModel("flsh");

        m = new Gen.MB();
        m.quad(-14, 2, 10, 14, 2, 10, 14, 2, -10, -14, 2, -10,
               Tex.PAPER, 0, Gen.F_DOUBLE, 6);
        a[MAP] = m.toModel("map");

        m = new Gen.MB();
        m.box(-8, 0, -4, 8, 10, 4, Tex.RADIO, 0, 0, 4);
        a[RADIO_P] = m.toModel("radio");

        // --- characters ------------------------------------------------------
        a[HARRY] = humanoid("harry", Tex.HARRY_TOP, Tex.HARRY_LEGS, 176, 25);
        a[CYBIL] = humanoid("cybil", Tex.CYBIL_TOP, Tex.CYBIL_LEGS, 170, 92);
        a[SHAMBLER] = humanoid("shambler", Tex.FLESH, Tex.FLESH, 132, 72);

        // --- air screamer ------------------------------------------------------
        m = new Gen.MB();
        m.box(-14, 0, -70, 14, 26, 70, Tex.FLESH, 0, 0, 6);
        m.box(-12, 4, 70, 12, 28, 104, Tex.FLESH, 0, 0, 6);
        m.quad(-6, 16, 104, 6, 16, 104, 6, 10, 128, -6, 10, 128,
               Tex.CHITIN, 0, Gen.F_DOUBLE, 4);
        for (int s = -1; s <= 1; s += 2)
        {
            m.quad(s * 12, 24, -40, s * 96, 40, 10, s * 96, 10, 60, s * 12, 14, 20,
                   Tex.WING, 0, Gen.F_DOUBLE, 64);
            m.quad(s * 12, 14, 20, s * 96, 10, 60, s * 96, -10, 40, s * 12, 2, -10,
                   Tex.WING, 0, Gen.F_DOUBLE, 64);
            box(m, s * 10, -20, -40, 6, 22, 6, Tex.CHITIN, 3);
        }
        a[AIRSCREAMER] = m.toModel("screamer");

        return a;
    }

    static Model humanoid(String name, int top, int legs, int height, int width)
    {
        Gen.MB m = new Gen.MB();
        int hw = width / 2;
        int legH = height * 44 / 100;
        int torsoH = height * 34 / 100;
        int headH = height * 16 / 100;
        int armH = height * 33 / 100;
        int d = width / 2;
        for (int s = -1; s <= 1; s += 2)
        {
            m.box(s * hw / 2 - 6, 0, -7, s * hw / 2 + 6, legH, 7, legs, 0, 0, 4);
        }
        m.box(-hw, legH, -d, hw, legH + torsoH, d, top, 0, 0, 4);
        for (int s = -1; s <= 1; s += 2)
        {
            int x = s * (hw + 5);
            m.box(x - 5, legH + 4, -d, x + 5, legH + armH, d, top, 0, 0, 4);
            m.box(x - 4, legH + armH - 10, -4, x + 4, legH + armH, 4, Tex.SKIN, 0, 0, 8);
        }
        m.box(-4, legH + torsoH, -4, 4, legH + torsoH + 5, 4, Tex.SKIN, 0, 0, 8);
        int hy = legH + torsoH + 5;
        m.box(-hw / 2 - 1, hy, -hw / 2 - 1, hw / 2 + 1, hy + headH, hw / 2 + 1,
              Tex.SKIN, 0, 0, 8);
        m.box(-hw / 2 - 2, hy + headH * 6 / 10, -hw / 2 - 2, hw / 2 + 2,
              hy + headH + 3, hw / 2 + 2, Tex.HAIR, 0, 0, 8);
        m.quad(-hw / 2, hy + headH * 62 / 100, hw / 2 + 1 + 1,
               hw / 2, hy + headH * 62 / 100, hw / 2 + 1 + 1,
               hw / 2, hy + headH * 18 / 100, hw / 2 + 1 + 1,
               -hw / 2, hy + headH * 18 / 100, hw / 2 + 1 + 1,
               Tex.HAIR, 0, 0, 8);
        return m.toModel(name);
    }
}
