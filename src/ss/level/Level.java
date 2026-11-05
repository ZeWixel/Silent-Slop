package ss.level;

import ss.core.Core;
import ss.core.Model;
import ss.core.Render3D;
import ss.gen.Gen;
import ss.gen.Tex;

/**
 * A playable area, described entirely by code.
 *
 * Numbers in brackets are the original map units from silent-hill-decomp,
 * converted with {@link #U}.  The geometry is emitted into 960 cm "bands"
 * (see {@link Band}), because the renderer culls, sorts and cuts away bands,
 * not individual faces.
 */
public final class Level
{
    /** Centimetres per original Silent Hill map unit. */
    public static final int U = 40;

    public static final int BAND = 480;

    public final String name;
    public final boolean interior;
    public final int ambient;
    public final int fogStart, fogEnd, fogColor;
    public final int ceiling;

    // spawn
    public int spawnX, spawnZ, spawnAngle;
    // playable rectangle
    public int zoneX0, zoneZ0, zoneX1, zoneZ1;

    public Band[] bands;
    public int nbands;

    public final Model[] models;

    public Level(String name, boolean interior, int ambient, int fogStart, int fogEnd,
                 int fogColor, int ceiling, int maxBands)
    {
        this.name = name;
        this.interior = interior;
        this.ambient = ambient;
        this.fogStart = fogStart;
        this.fogEnd = fogEnd;
        this.fogColor = fogColor;
        this.ceiling = ceiling;
        bands = new Band[maxBands];
        models = ss.gen.Models.build();
    }

    public static int u(double v)
    {
        return (int) (v * U);
    }

    // ------------------------------------------------------------------ build
    private Band band(int x, int z)
    {
        if (x < 0) x -= BAND - 1;
        if (z < 0) z -= BAND - 1;
        int kx = x / BAND;
        int kz = z / BAND;
        for (int i = 0; i < nbands; i++)
        {
            if (bands[i].kx == kx && bands[i].kz == kz) return bands[i];
        }
        if (nbands >= bands.length)
        {
            // grow instead of silently dumping the geometry into band 0
            Band[] nb = new Band[bands.length * 2];
            System.arraycopy(bands, 0, nb, 0, nbands);
            bands = nb;
        }
        Band b = new Band(kx, kz);
        b.build(this);
        bands[nbands++] = b;
        return b;
    }

    private Gen.MB bandMesh(int x, int z)
    {
        return band(x, z).m;
    }

    /**
     * Adds a box; it is split per band so the cut-away can open exactly the
     * part of the wall that hides the player.
     */
    void box(int x0, int y0, int z0, int x1, int y1, int z1, int tex, int flags, int uvs)
    {
        int i0 = x0 / BAND;
        int i1 = (x1 - 1) / BAND;
        int k0 = z0 / BAND;
        int k1 = (z1 - 1) / BAND;
        for (int k = k0; k <= k1; k++)
        {
            for (int i = i0; i <= i1; i++)
            {
                int bx0 = Math.max(x0, i * BAND);
                int bx1 = Math.min(x1, (i + 1) * BAND);
                int bz0 = Math.max(z0, k * BAND);
                int bz1 = Math.min(z1, (k + 1) * BAND);
                if (bx1 <= bx0 || bz1 <= bz0) continue;
                Gen.MB m = bandMesh((bx0 + bx1) >> 1, (bz0 + bz1) >> 1);
                m.box(bx0, y0, bz0, bx1, y1, bz1, tex, 0, flags, uvs);
            }
        }
        Band.addCollider(x0, z0, x1, z1, y1);
    }

    void wallQuad(int x0, int y0, int z0, int x1, int y1, int z1,
                  int x2, int y2, int z2, int x3, int y3, int z3,
                  int tex, int flags, int uvs)
    {
        int cx = (x0 + x1 + x2 + x3) >> 2;
        int cz = (z0 + z1 + z2 + z3) >> 2;
        bandMesh(cx, cz).quad(x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3,
                              tex, 0, flags, uvs);
    }

    void floor(int x0, int z0, int x1, int z1, int y, int tex, int uvs, int flags)
    {
        // split the floor the same way the walls are split, for culling
        int i0 = x0 / BAND, i1 = (x1 - 1) / BAND;
        int k0 = z0 / BAND, k1 = (z1 - 1) / BAND;
        for (int k = k0; k <= k1; k++)
        {
            for (int i = i0; i <= i1; i++)
            {
                int bx0 = Math.max(x0, i * BAND);
                int bx1 = Math.min(x1, (i + 1) * BAND);
                int bz0 = Math.max(z0, k * BAND);
                int bz1 = Math.min(z1, (k + 1) * BAND);
                if (bx1 <= bx0 || bz1 <= bz0) continue;
                Band b = band((bx0 + bx1) >> 1, (bz0 + bz1) >> 1);
                b.addFloor(bx0, bz0, bx1, bz1, y, tex, uvs, flags);
            }
        }
    }

    void prop(int model, int x, int y, int z, int rot)
    {
        Band b = band(x, z);
        b.addProp(model, x, y, z, rot);
    }

    void item(int tex, int x, int y, int z, int msg)
    {
        Band b = band(x, z);
        b.addItem(tex, x, y, z, msg);
    }

    void trigger(int x0, int z0, int x1, int z1, int kind, int id, int msg)
    {
        Band.addTrigger(x0, z0, x1, z1, kind, id, msg);
    }

    /** A house block: walls on the perimeter, a roof slab, windows on request. */
    void building(int x0, int z0, int x1, int z1, int h, int wallTex, int roofTex,
                  int[] winPat, int uvs)
    {
        // walls, as four thin boxes so the cut-away can eat them per band
        int t = 60;
        box(x0, 0, z0, x1, h, z0 + t, wallTex, Render3D.F_WALL, uvs);
        box(x0, 0, z1 - t, x1, h, z1, wallTex, Render3D.F_WALL, uvs);
        box(x0, 0, z0 + t, x0 + t, h, z1 - t, wallTex, Render3D.F_WALL, uvs);
        box(x1 - t, 0, z0 + t, x1, h, z1 - t, wallTex, Render3D.F_WALL, uvs);
        floor(x0, z0, x1, z1, h, roofTex, 20, Render3D.F_ROOF);

        // windows on the two long facades
        int y = 150;
        int idx = 0;
        for (int x = x0 + 120; x < x1 - 120; x += 180)
        {
            int lit = 0;
            if (winPat != null && idx < winPat.length) lit = winPat[idx];
            idx++;
            int tex = lit != 0 ? Tex.WINDOW_LIT : Tex.WINDOW_DARK;
            int f = lit != 0 ? Render3D.F_EMIT : Render3D.F_DOUBLE;
            wallQuad(x - 42, y + 60, z0 - 3, x + 42, y + 60, z0 - 3,
                     x + 42, y, z0 - 3, x - 42, y, z0 - 3, tex, f, 42);
            wallQuad(x + 42, y + 60, z1 + 3, x - 42, y + 60, z1 + 3,
                     x - 42, y, z1 + 3, x + 42, y, z1 + 3, tex, f, 42);
        }
    }

    /** Bakes every band's vertex array into the final render mesh. */
    public void finish()
    {
        for (int i = 0; i < nbands; i++)
        {
            Band b = bands[i];
            if (b.m.nf > 0) b.mesh = b.m.toModel(name + i);
        }
    }

    /** Number of finished bands (for the profiler overlay). */
    public int bandCount()
    {
        return nbands;
    }

    // ------------------------------------------------------------------ queries
    public Band bandAtIndex(int i)
    {
        return i < nbands ? bands[i] : null;
    }
}
