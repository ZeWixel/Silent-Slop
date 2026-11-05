package ss.game;

import ss.core.Core;
import ss.core.Fb;
import ss.core.Render3D;
import ss.gen.Models;
import ss.gen.Tex;
import ss.level.Band;
import ss.level.Level;

/**
 * Draws the world for one frame.
 *
 * Order (chosen so the painter sort works without a depth buffer):
 * <ol>
 * <li>sky gradient,</li>
 * <li>floors of the near bands,</li>
 * <li>props and characters,</li>
 * <li>wall bands from far to near, flushing whenever the face buffer fills.</li>
 * </ol>
 *
 * Two things are pure optimisation and both are visible in the dev overlay:
 * the fog radius (bands outside it never get transformed) and the cut-away
 * (near bands that would hide Harry have their walls skipped).
 */
public final class Scene
{
    /** Bands closer than this to Harry are candidates for the cut-away. */
    static final int CUT_RADIUS = 1300;

    private Scene() {}

    public static void render(Game g, Fb fb)
    {
        Level lv = g.level;
        Player p = g.player;

        // ---- camera ------------------------------------------------------
        int dist = g.camDist;
        int pitch = 341;                                    // 30 degrees
        int ch = (int) ((long) dist * 512 / 1024);          // sin 30
        int chz = (int) ((long) dist * 888 / 1024);         // cos 30
        int fx = Core.sin(g.camYaw);
        int fz = Core.cos(g.camYaw);
        int camX = p.x - ((fx * chz) >> 12);
        int camZ = p.z - ((fz * chz) >> 12);
        // Outside, lift the eye so roofs and wall tops read; inside, stay at
        // head height and hide the ceiling instead.
        int camY = g.camHeight + ch + (lv.interior ? 0 : 120);

        // keep the camera out of walls: pull it in toward Harry
        for (int i = 0; i < 6 && g.isBlocked(camX, camZ, 34); i++)
        {
            camX = (camX * 3 + p.x) / 4;
            camZ = (camZ * 3 + p.z) / 4;
            camY += 6;                       // rise a little, look over the wall
            if (camY > 900) break;
        }

        if (lv.interior)
        {
            // Interiors: a dark wash instead of pure black - the walls of the
            // room are cut away, so what shows through must still read as night.
            fb.vgrad(Core.mix565(g.fogColor, 0x0000, 130), Core.mix565(g.fogColor, 0x0000, 210));
        }
        else
        {
            fb.vgrad(g.skyTop, g.skyBottom);
        }
        // Harry sits a little below the middle so there is more street in front
        // of him than behind: the classic isometric framing.
        Render3D.begin(fb, camX, camY, camZ, g.camYaw, pitch, g.fov,
                       fb.w / 2, fb.h / 2 + 18);
        Render3D.hideRoofs = lv.interior;
        Render3D.setMode(g.objMode);
        Render3D.setLight(g.lightX, g.lightY, g.lightZ, lv.ambient + 2);
        Render3D.setFog(lv.fogStart, g.fogEnd);
        Render3D.setFogColor(g.fogColor);
        Render3D.setPlayerScreen(p.x, 90, p.z);
        Render3D.setFlashlight(p.flashlight, Core.sin(p.angle) << 6, -20,
                               Core.cos(p.angle) << 6, g.flashRange);

        int n = lv.nbands;

        // ---- 0. endless backdrop, drawn and flushed first so that the real
        //         floors always win the painter sort regardless of depth ties ----
        if (!lv.interior)
        {
            // Tiled, not one giant quad: fog is per face, so a single 480 m
            // quad would be fogged by its average depth and grey out the screen.
            int T = 4000;
            int gx = (p.x / T) * T;
            int gz = (p.z / T) * T;
            for (int tz = -3; tz <= 3; tz++)
            {
                for (int tx = -3; tx <= 3; tx++)
                {
                    int x0 = gx + tx * T, z0 = gz + tz * T;
                    Render3D.drawFloor(x0, z0, x0 + T, z0 + T, -8,
                                       ss.gen.Tex.GRASS, 90, 0);
                }
            }
            Render3D.flush();
        }

        // ---- 1. floors of the visible bands -------------------------------
        for (int i = 0; i < n; i++)
        {
            Band b = lv.bands[i];
            if (b == null) continue;
            if (distToBand(b, p.x, p.z) > g.fogEnd) continue;
            b.drawFloor(fb);
        }

        // ---- 2. walls, far to near ----------------------------------------
        // A simple ordering by band distance keeps the painter sort honest for
        // the big wall quads; the per-face sort inside a flush does the rest.
        for (int pass = 0; pass < 2; pass++)
        {
            for (int i = 0; i < n; i++)
            {
                Band b = lv.bands[i];
                if (b == null || b.mesh == null) continue;
                int d = distToBand(b, p.x, p.z);
                if (d > g.fogEnd) continue;
                boolean near = d < CUT_RADIUS;
                if ((pass == 0) == near) continue;      // far pass first
                Render3D.drawBand(b.mesh, 0, 0, 0);
                if (Render3D.needsFlush()) Render3D.flush();
            }
        }

        // ---- 3. props, items and characters (on top of the walls) --------------------------------------
        for (int i = 0; i < n; i++)
        {
            Band b = lv.bands[i];
            if (b == null) continue;
            if (distToBand(b, p.x, p.z) > g.fogEnd) continue;
            b.drawProps(lv);
            drawItems(g, b);
        }
        for (int i = 0; i < g.enemies.length; i++)
        {
            Enemy e = g.enemies[i];
            if (e != null && e.active) e.draw();
        }
        if (g.showCybil)
        {
            Render3D.drawShadow(g.cybilX, g.cybilZ, 26);
            Render3D.drawModel(Game.models[Models.CYBIL], g.cybilX, 0, g.cybilZ,
                               g.cybilAngle);
        }
        p.draw();

        // ---- 4. flashlight pool + fog vignette, then one last flush -------
        Render3D.flush();
        if (p.flashlight)
        {
            Render3D.overlayFlashlight(p.x + ((Core.sin(p.angle) * 90) >> 12),
                                       p.z + ((Core.cos(p.angle) * 90) >> 12),
                                       g.flashWide, (g.flashWide * 5) / 6);
        }
        Render3D.overlayFogVignette(g.fogVignette);
    }

    static void drawItems(Game g, Band b)
    {
        for (int i = 0; i < b.ni; i++)
        {
            if (b.taken[i]) continue;
            int o = i * 5;
            int tex = b.it[o];
            int x = b.it[o + 1], y = b.it[o + 2], z = b.it[o + 3];
            int a = (g.frames * 24) & 4095;      // slow spin, reads at any angle
            Render3D.drawModel(Game.models[itemModel(tex)], x, y, z, a);
        }
    }

    static int itemModel(int tex)
    {
        if (tex == Tex.KNIFE) return Models.KNIFE;
        if (tex == Tex.PAPER) return Models.MAP;
        if (tex == Tex.RADIO) return Models.RADIO_P;
        if (tex == Tex.FLASHLIGHT) return Models.FLASHLIGHT_P;
        return Models.TRASHCAN;
    }

    /** Manhattan-ish distance from a band to a point, in cm. */
    static int distToBand(Band b, int x, int z)
    {
        int dx = 0, dz = 0;
        if (x < b.x0) dx = b.x0 - x;
        else if (x > b.x1) dx = x - b.x1;
        if (z < b.z0) dz = b.z0 - z;
        else if (z > b.z1) dz = z - b.z1;
        return dx + dz;
    }

    /**
     * The cut-away rule: a band of walls is hidden only when it sits between
     * the camera and Harry AND is close enough to him to be in the way.  The
     * far side of the room, the ceiling and everything behind Harry stay.
     */
    static boolean isBetweenCameraAndPlayer(Game g, Band b, int camX, int camZ, Player p)
    {
        int cx = (b.x0 + b.x1) / 2;
        int cz = (b.z0 + b.z1) / 2;
        int dx = cx - p.x;
        int dz = cz - p.z;
        // close to Harry?
        if (dx * dx + dz * dz > CUT_RADIUS * CUT_RADIUS) return false;
        // on the camera side?
        int toCamX = camX - p.x;
        int toCamZ = camZ - p.z;
        return (toCamX * dx + toCamZ * dz) > 0;
    }
}
