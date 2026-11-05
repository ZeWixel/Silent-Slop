package ss.level;

import ss.core.Render3D;
import ss.gen.Models;
import ss.gen.Tex;

/**
 * The two areas of the prototype.
 *
 * Coordinates are the original map units (1 unit = 40 cm) so the proportions
 * match Silent Hill 1: the street is 44 units wide (17.6 m), the cafe hall is
 * 30 x 30 units (12 x 12 m), and every prop sits where the decompilation says
 * it sits (Harry's car at -6.2, 160.5, the knife at 5.09, 274.12, ...).
 */
public final class Levels
{
    public static final int PROLOGUE = 0;
    public static final int CAFE = 1;

    private Levels() {}

    public static Level build(int which, iface.IProgress prog)
    {
        if (which == PROLOGUE) return prologue(prog);
        return cafe(prog);
    }

    // --------------------------------------------------------------- prologue
    /**
     * map0_s00.  The camera road band in the original is z 108.5..131.0 and
     * x -240..200; Harry starts next to the crashed car at map point 0.
     */
    public static Level prologue(iface.IProgress prog)
    {
        // Real-metric street: U is 40 cm, so u(25) == 10 m.  The road runs along
        // X, the two rows of buildings stand at z = -14u and z = +14u, and every
        // number below is roughly what the decomp says about the prologue block.
        Level lv = new Level("prologue", false, 9, 2200, 5200, 0x632C, 800, 320);
        lv.spawnX = Level.u(-6);
        lv.spawnZ = 0;
        lv.spawnAngle = 0;
        lv.zoneX0 = Level.u(-104);
        lv.zoneZ0 = Level.u(-16);
        lv.zoneX1 = Level.u(104);
        lv.zoneZ1 = Level.u(18);
        Band.resetTriggers();

        // ---- ground -------------------------------------------------------
        // One quad per strip, no overlap anywhere: road, then the two sidewalks,
        // then the lots that run up to the building fronts.
        lv.floor(Level.u(-120), Level.u(-11), Level.u(120), Level.u(11), 0, Tex.ROAD, 9, 0);
        lv.floor(Level.u(-120), Level.u(-14), Level.u(120), Level.u(-11), 0, Tex.CONCRETE, 3, 0);
        lv.floor(Level.u(-120), Level.u(11), Level.u(120), Level.u(14), 0, Tex.CONCRETE, 3, 0);
        lv.floor(Level.u(-120), Level.u(-70), Level.u(120), Level.u(-14), 0, Tex.GRASS, 24, 0);
        lv.floor(Level.u(-120), Level.u(14), Level.u(120), Level.u(70), 0, Tex.GRASS, 24, 0);
        lv.box(Level.u(-120), 0, Level.u(-12), Level.u(120), 9, Level.u(-11),
               Tex.CONCRETE, 0, 3);
        lv.box(Level.u(-120), 0, Level.u(11), Level.u(120), 9, Level.u(12),
               Tex.CONCRETE, 0, 3);

        // ---- south side (z = -14u and beyond) ------------------------------
        lv.building(Level.u(-104), Level.u(-58), Level.u(-46), Level.u(-14), 900,
                    Tex.BRICK, Tex.ROOF, new int[]{1, 0, 1, 1}, 6);
        lv.floor(Level.u(-46), Level.u(-58), Level.u(-38), Level.u(-14), 0, Tex.ALLEY, 5, 0);
        lv.box(Level.u(-46), 0, Level.u(-62), Level.u(-38), 700, Level.u(-56),
               Tex.ALLEY, Render3D.F_WALL, 4);
        lv.building(Level.u(-38), Level.u(-54), Level.u(12), Level.u(-14), 700,
                    Tex.SIDING, Tex.ROOF, new int[]{0, 1, 0, 1}, 6);
        lv.building(Level.u(18), Level.u(-60), Level.u(104), Level.u(-14), 1000,
                    Tex.BRICK_WARM, Tex.ROOF, new int[]{1, 1, 0, 1}, 6);

        // ---- north side: neighbour block, the cafe, then the east block ----
        lv.building(Level.u(-104), Level.u(14), Level.u(-24), Level.u(60), 800,
                    Tex.SIDING, Tex.ROOF, new int[]{0, 0, 1, 0}, 6);

        // "5 to 2" cafe: x -20..12, z 14..38, door on the -x half of the facade
        int cx0 = Level.u(-20), cx1 = Level.u(12);
        int cz0 = Level.u(14), cz1 = Level.u(38);
        int d0 = Level.u(-8), d1 = Level.u(-3);
        int t = Level.u(2);
        int h = 620;
        lv.box(cx0, 0, cz0, d0, h, cz0 + t, Tex.BRICK_WARM, Render3D.F_WALL, 5);
        lv.box(d1, 0, cz0, cx1, h, cz0 + t, Tex.BRICK_WARM, Render3D.F_WALL, 5);
        lv.box(d0, 400, cz0, d1, h, cz0 + t, Tex.BRICK_WARM, Render3D.F_WALL, 5);
        // the doorway itself: a dark panel a little behind the opening so the
        // hole reads as an interior instead of a view straight through the block
        lv.wallQuad(d0, 400, cz0 + t, d1, 400, cz0 + t,
                    d1, 0, cz0 + t, d0, 0, cz0 + t,
                    Tex.WINDOW_DARK, Render3D.F_DOUBLE, 4);
        lv.box(cx0, 0, cz1 - t, cx1, h, cz1, Tex.BRICK_WARM, Render3D.F_WALL, 5);
        lv.box(cx0, 0, cz0, cx0 + t, h, cz1, Tex.BRICK_WARM, Render3D.F_WALL, 5);
        lv.box(cx1 - t, 0, cz0, cx1, h, cz1, Tex.BRICK_WARM, Render3D.F_WALL, 5);
        lv.floor(cx0, cz0, cx1, cz1, h, Tex.ROOF, 9, Render3D.F_ROOF);
        lv.floor(cx0, Level.u(12), cx1, cz0, 0, Tex.CONCRETE, 4, 0);

        // backlit sign and the lit shop window, a hair in front of the facade
        lv.wallQuad(Level.u(-18), 330, cz0 - 3, Level.u(-9), 330, cz0 - 3,
                    Level.u(-9), 250, cz0 - 3, Level.u(-18), 250, cz0 - 3,
                    Tex.SIGN_CAFE, Render3D.F_EMIT | Render3D.F_DOUBLE, 3);
        lv.wallQuad(Level.u(-2), 280, cz0 - 3, Level.u(7), 280, cz0 - 3,
                    Level.u(7), 40, cz0 - 3, Level.u(-2), 40, cz0 - 3,
                    Tex.WINDOW_LIT, Render3D.F_EMIT | Render3D.F_DOUBLE, 3);
        lv.wallQuad(Level.u(-19), 280, cz0 - 3, Level.u(-11), 280, cz0 - 3,
                    Level.u(-11), 60, cz0 - 3, Level.u(-19), 60, cz0 - 3,
                    Tex.WINDOW_DARK, Render3D.F_DOUBLE, 3);

        lv.building(Level.u(18), Level.u(14), Level.u(104), Level.u(64), 900,
                    Tex.BRICK, Tex.ROOF, new int[]{0, 1, 0, 0}, 6);

        // ---- the street is closed at the east end by a fence ---------------
        lv.box(Level.u(112), 0, Level.u(-14), Level.u(116), 700, Level.u(14),
               Tex.FENCE, Render3D.F_ALPHA | Render3D.F_DOUBLE, 6);
        lv.box(Level.u(112), 700, Level.u(-14), Level.u(116), 730, Level.u(14),
               Tex.METAL, 0, 4);

        prog.step("street props");
        // ---- props ---------------------------------------------------------
        lv.prop(Models.CAR, Level.u(-14), 0, Level.u(4), 0);
        lv.prop(Models.CAR, Level.u(-26), 0, Level.u(4), 60);
        lv.prop(Models.LAMP, Level.u(-30), 0, Level.u(-13), 0);
        lv.prop(Models.LAMP, Level.u(6), 0, Level.u(-13), 0);
        lv.prop(Models.LAMP, Level.u(-30), 0, Level.u(13), 0);
        lv.prop(Models.LAMP, Level.u(46), 0, Level.u(-13), 0);
        lv.prop(Models.DUMPSTER, Level.u(-42), 0, Level.u(-16), 0);
        lv.prop(Models.TRASHCAN, Level.u(-44), 0, Level.u(-12), 0);
        lv.prop(Models.TRASHCAN, Level.u(20), 0, Level.u(12), 0);
        lv.prop(Models.BENCH, Level.u(30), 0, Level.u(-12), 0);
        lv.prop(Models.BENCH, Level.u(-60), 0, Level.u(12), 0);
        lv.prop(Models.ROAD_SIGN, Level.u(-34), 0, Level.u(-12), 0);
        lv.prop(Models.ROAD_SIGN, Level.u(34), 0, Level.u(12), 0);
        lv.prop(Models.WEEDS, Level.u(-16), 0, Level.u(-13), 0);
        lv.prop(Models.WEEDS, Level.u(58), 0, Level.u(13), 0);
        lv.prop(Models.WEEDS, Level.u(-70), 0, Level.u(-13), 0);
        lv.prop(Models.BLOOD, Level.u(-4), 0, Level.u(2), 0);

        // ---- items ---------------------------------------------------------
        lv.item(Tex.CAN, Level.u(-10), 20, Level.u(-13), 3);
        lv.item(Tex.CAN, Level.u(52), 20, Level.u(12), 3);

        prog.step("street triggers");
        lv.trigger(Level.u(10), Level.u(-8), Level.u(20), Level.u(8), 0, 1, 1);
        lv.trigger(Level.u(108), Level.u(-14), Level.u(118), Level.u(14), 0, 2, 2);
        lv.trigger(Level.u(-18), Level.u(10), Level.u(-10), Level.u(15), 2, 1, 0);
        lv.trigger(Level.u(-9), Level.u(13), Level.u(-2), Level.u(22), 0, 3, 4);
        lv.trigger(Level.u(-46), Level.u(-58), Level.u(-38), Level.u(-30), 0, 4, 5);
        lv.trigger(Level.u(-26), Level.u(-6), Level.u(-20), Level.u(6), 3, 1, 0);
        lv.trigger(Level.u(24), Level.u(-11), Level.u(40), Level.u(11), 4, 1, 0);
        prog.step("street done");
        return lv;
    }

    public static Level cafe(iface.IProgress prog)
    {
        Level lv = new Level("cafe", true, 8, 1400, 3000, 0x4208, 320, 40);
        lv.spawnX = Level.u(-4);
        lv.spawnZ = Level.u(152);
        lv.spawnAngle = 1024;
        lv.zoneX0 = Level.u(-14);
        lv.zoneZ0 = Level.u(141);
        lv.zoneX1 = Level.u(14);
        lv.zoneZ1 = Level.u(169);
        Band.resetTriggers();

        // Real size: 12 m x 12 m room, 3.2 m ceiling.  Everything that was
        // authored against the old 1200 cm room is re-cut to fit.
        int x0 = Level.u(-15), z0 = Level.u(140), x1 = Level.u(15), z1 = Level.u(170);
        int h = 320;
        int t = Level.u(2);

        lv.floor(x0, z0, x1, z1, 0, Tex.TILE, 5, 0);
        lv.floor(x0, z0, x1, z1, h, Tex.CEILING, 9, Render3D.F_ROOF);

        // shell: south wall split by the doorway, three other walls solid
        lv.box(x0, 0, z0, Level.u(-6), h, z0 + t, Tex.CAFE_WALL, Render3D.F_WALL, 5);
        lv.box(Level.u(-2), 0, z0, x1, h, z0 + t, Tex.CAFE_WALL, Render3D.F_WALL, 5);
        lv.box(Level.u(-6), 210, z0, Level.u(-2), h, z0 + t, Tex.CAFE_WALL,
               Render3D.F_WALL, 5);
        lv.wallQuad(Level.u(-6), 210, z0 + t, Level.u(-2), 210, z0 + t,
                    Level.u(-2), 0, z0 + t, Level.u(-6), 0, z0 + t,
                    Tex.WINDOW_DARK, Render3D.F_DOUBLE, 3);
        lv.box(x0, 0, z1 - t, x1, h, z1, Tex.CAFE_WALL, Render3D.F_WALL, 5);
        lv.box(x0, 0, z0 + t, x0 + t, h, z1 - t, Tex.CAFE_WALL, Render3D.F_WALL, 5);
        lv.box(x1 - t, 0, z0 + t, x1, h, z1 - t, Tex.CAFE_WALL, Render3D.F_WALL, 5);

        prog.step("cafe furniture");
        // counter along the west wall
        lv.prop(Models.COUNTER, Level.u(-12), 0, Level.u(158), 0);
        Band.addCollider(Level.u(-13), Level.u(155), Level.u(-11), Level.u(161), 110);

        // booths along the back wall
        for (int xx = -9; xx <= 3; xx += 6)
        {
            lv.prop(Models.CHAIR, Level.u(xx), 0, Level.u(166), 0);
            lv.prop(Models.TABLE, Level.u(xx), 0, Level.u(161), 0);
        }
        lv.prop(Models.TABLE, Level.u(-4), 0, Level.u(150), 0);
        lv.prop(Models.CHAIR, Level.u(-7), 0, Level.u(147), 300);
        lv.prop(Models.CHAIR, Level.u(-1), 0, Level.u(153), 1200);
        lv.prop(Models.PINBALL, Level.u(8), 0, Level.u(167), 1600);
        lv.prop(Models.TV, Level.u(11), 160, Level.u(160), 0);
        lv.prop(Models.JUKEBOX, Level.u(12), 0, Level.u(144), 2048);

        // the pickups, at the decomp coordinates
        lv.prop(Models.KNIFE, Level.u(9), 0, Level.u(158), 0);
        lv.prop(Models.FLASHLIGHT_P, Level.u(10), 0, Level.u(154), 0);
        lv.prop(Models.MAP, Level.u(11), 0, Level.u(152), 0);
        lv.prop(Models.RADIO_P, Level.u(8), 0, Level.u(154), 0);

        lv.item(Tex.KNIFE, Level.u(9), 78, Level.u(158), 10);
        lv.item(Tex.FLASHLIGHT, Level.u(10), 78, Level.u(154), 11);
        lv.item(Tex.PAPER, Level.u(11), 78, Level.u(152), 12);
        lv.item(Tex.RADIO, Level.u(2), 78, Level.u(154), 13);
        lv.item(Tex.CAN, x0 + 60, 118, Level.u(150), 14);

        prog.step("cafe triggers");
        lv.trigger(Level.u(-13), Level.u(155), Level.u(-10), Level.u(162), 0, 5, 15);
        lv.trigger(Level.u(6), Level.u(164), Level.u(11), Level.u(169), 0, 6, 3);
        lv.trigger(Level.u(10), Level.u(151), Level.u(14), Level.u(163), 0, 7, 17);
        lv.trigger(Level.u(-6), z0, Level.u(-2), z0 + Level.u(3), 2, 0, 0);
        lv.trigger(Level.u(-6), z0 + Level.u(3), Level.u(-2), z0 + Level.u(6), 3, 2, 0);
        lv.trigger(Level.u(8), Level.u(166), Level.u(13), Level.u(169), 3, 3, 0);
        lv.trigger(Level.u(8), Level.u(166), Level.u(13), Level.u(169), 4, 2, 0);
        prog.step("cafe done");
        return lv;
    }
}
