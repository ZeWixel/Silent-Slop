package ss.map;

import ss.core.Core;
import ss.core.Fb;
import ss.game.Game;
import ss.game.Input;
import ss.level.Band;
import ss.level.Levels;
import ss.ui.Font6;

/**
 * The paper map, on the "#" key.
 *
 * The map is not a bitmap: it is drawn from the same band and collider data the
 * renderer uses, so it always matches the level exactly and costs no memory.
 * Zoom cycles 1x / 2x / 4x with 0, and multi-floor levels switch with "*".
 */
public final class MapPanel
{
    public static final int ZOOM_1 = 0;
    public static final int ZOOM_2 = 1;
    public static final int ZOOM_4 = 2;

    public int zoom;
    public int floor;
    public int levelId;

    public void setLevel(int id)
    {
        levelId = id;
        zoom = ZOOM_1;
        floor = 0;
    }

    public void open(Game g)
    {
        zoom = ZOOM_1;
        floor = 0;
    }

    public void update(Input in, Game g)
    {
        if (in.tapped(Input.POUND) || in.tapped(Input.SOFT_RIGHT))
        {
            g.state = Game.S_PLAY;
            return;
        }
        if (in.tapped(Input.NUM0))
        {
            zoom = (zoom + 1) % 3;
            ss.snd.Sfx.play(ss.snd.Sfx.MENU, 40);
        }
        if (in.tapped(Input.STAR) || in.tapped(Input.SOFT_LEFT))
        {
            // multi-floor buildings: the cafe has a cellar in the full game
            floor = (floor + 1) % 2;
            ss.snd.Sfx.play(ss.snd.Sfx.MENU, 40);
        }
    }

    public void draw(Fb fb, Game g)
    {
        int W = fb.w, H = fb.h;
        fb.rect(0, 0, W - 1, H - 1, 0x0861);
        fb.rect(2, 2, W - 3, H - 3, 0x18C3);

        Font6.draw(fb, levelId == Levels.PROLOGUE ? "RESIDENTIAL AREA" : "5 TO 2 - GROUND FLOOR",
                   4, 4, 0xC618, 0x0000);
        Font6.draw(fb, "x" + (zoom == 0 ? 1 : (zoom == 1 ? 2 : 4)), W - 16, 4, 0xC618, 0x0000);

        int scale = zoom == 0 ? 1 : (zoom == 1 ? 2 : 3);
        // fit scale so the level fills the panel
        int panelY = 14;
        int panelH = H - 30;
        int spanX = g.level.zoneX1 - g.level.zoneX0;
        int spanZ = g.level.zoneZ1 - g.level.zoneZ0;
        int sc = (W - 8) * 100 / (spanX == 0 ? 1 : spanX);
        int sc2 = panelH * 100 / (spanZ == 0 ? 1 : spanZ);
        if (sc2 < sc) sc = sc2;
        sc = sc * (100 + (zoom * 60)) / 100;
        if (sc < 1) sc = 1;

        int ox = 4 - (g.level.zoneX0 / 100 * sc) + (W - 8 - (spanX / 100 * sc)) / 2;
        int oy = panelY + (panelH - (spanZ / 100 * sc)) / 2 - (g.level.zoneZ0 / 100 * sc);

        // clip by drawing only inside the panel
        int clipY0 = panelY, clipY1 = H - 16;

        // walls: the band footprint
        for (int i = 0; i < g.level.nbands; i++)
        {
            Band b = g.level.bands[i];
            if (b == null || b.mesh == null) continue;
            int x0 = ox + (b.x0 / 100) * sc;
            int z0 = oy + (b.z0 / 100) * sc;
            int x1 = ox + (b.x1 / 100) * sc;
            int z1 = oy + (b.z1 / 100) * sc;
            if (x1 < 0 || x0 >= W) continue;
            if (z1 < clipY0 || z0 > clipY1) continue;
            fb.rect(x0, Math.max(z0, clipY0), x1, Math.min(z1, clipY1), 0x3186);
        }

        // colliders, as solid outlines
        for (int i = 0; i < Band.ncol; i++)
        {
            int o = i * 5;
            int x0 = ox + (Band.colData[o] / 100) * sc;
            int z0 = oy + (Band.colData[o + 1] / 100) * sc;
            int x1 = ox + (Band.colData[o + 2] / 100) * sc;
            int z1 = oy + (Band.colData[o + 3] / 100) * sc;
            if (z1 < clipY0 || z0 > clipY1) continue;
            int y0 = Math.max(z0, clipY0), y1 = Math.min(z1, clipY1);
            int ink = 0x8410;
            // Fb only has horizontal lines, so the verticals are drawn with rect
            fb.hline(x0, x1, y0, ink);
            fb.hline(x0, x1, y1, ink);
            fb.rect(x0, y0, x0 + 1, y1, ink);
            fb.rect(x1 - 1, y0, x1, y1, ink);
        }

        // doors and points of interest
        for (int i = 0; i < Band.ntrig; i++)
        {
            int o = i * 7;
            int kind = Band.trigData[o + 4];
            int x0 = ox + (Band.trigData[o] / 100) * sc;
            int z0 = oy + (Band.trigData[o + 1] / 100) * sc;
            if (z0 < clipY0 || z0 > clipY1) continue;
            if (kind == 2) fb.rect(x0 - 2, z0 - 1, x0 + 2, z0 + 1, 0x07E0);
        }

        // items
        for (int i = 0; i < g.level.nbands; i++)
        {
            Band b = g.level.bands[i];
            if (b == null) continue;
            for (int k = 0; k < b.ni; k++)
            {
                if (b.taken[k]) continue;
                int x = ox + (b.it[k * 5 + 1] / 100) * sc;
                int y = oy + (b.it[k * 5 + 3] / 100) * sc;
                if (y < clipY0 || y > clipY1) continue;
                fb.rect(x - 1, y - 1, x + 1, y + 1, 0xFFE0);
            }
        }

        // enemies
        for (int i = 0; i < g.enemies.length; i++)
        {
            if (g.enemies[i] == null || !g.enemies[i].active) continue;
            int x = ox + (g.enemies[i].x / 100) * sc;
            int y = oy + (g.enemies[i].z / 100) * sc;
            if (y < clipY0 || y > clipY1) continue;
            fb.rect(x - 1, y - 1, x + 1, y + 1, 0xF800);
        }

        // the player, as an arrow that turns with the camera
        int px = ox + (g.player.x / 100) * sc;
        int py = oy + (g.player.z / 100) * sc;
        if (py >= clipY0 && py <= clipY1)
        {
            fb.rect(px - 2, py - 2, px + 2, py + 2, 0xFFFF);
            // view cone: a short chevron pointing where the camera looks
            int ax = px + (Core.sin(g.camYaw) * 10 >> 12);
            int ay = py + (Core.cos(g.camYaw) * 10 >> 12);
            drawLine(fb, px, py, ax, ay, 0x07FF);
            drawLine(fb, ax, ay, ax + (Core.sin(g.camYaw + 400) * 5 >> 12),
                     ay + (Core.cos(g.camYaw + 400) * 5 >> 12), 0x07FF);
            drawLine(fb, ax, ay, ax + (Core.sin(g.camYaw - 400) * 5 >> 12),
                     ay + (Core.cos(g.camYaw - 400) * 5 >> 12), 0x07FF);
        }

        Font6.draw(fb, "# CLOSE   0 ZOOM   * FLOOR " + (floor + 1), 4, H - 12, 0x8410, 0x0000);
    }

    /** Bresenham, because Fb only offers horizontal spans. */
    static void drawLine(Fb fb, int x0, int y0, int x1, int y1, int c)
    {
        int dx = x1 - x0, dy = y1 - y0;
        int sx = dx < 0 ? -1 : 1, sy = dy < 0 ? -1 : 1;
        if (dx < 0) dx = -dx;
        if (dy < 0) dy = -dy;
        int err = dx - dy;
        for (int guard = 0; guard < 256; guard++)
        {
            fb.rect(x0, y0, x0, y0, c);
            if (x0 == x1 && y0 == y1) break;
            int e2 = err << 1;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 < dx) { err += dx; y0 += sy; }
        }
    }
}
