package ss.level;

import ss.core.Fb;
import ss.core.Model;
import ss.core.Render3D;
import ss.gen.Gen;

/**
 * One 960x960 cm cell of level geometry.
 *
 * A band owns the wall/floor geometry that falls inside its cell, its props and
 * its items.  The renderer walks the bands twice:
 * <ol>
 * <li>floors and props, far to near;</li>
 * <li>walls, skipping any band that would hide the player.</li>
 * </ol>
 * That is what makes both the cut-away and the cheap depth sort possible:
 * everything inside a band is nearly the same distance from the camera.
 */
public final class Band
{
    public final int kx, kz;
    public final int x0, z0, x1, z1;
    public Gen.MB m;
    public Model mesh;
    public boolean built;

    // floors: x0,z0,x1,z1,y,tex,uvs,flags
    public int nfl;
    public int[] fl = new int[8 * 8];

    // props: model,x,y,z,rot
    public int npr;
    public int[] pr = new int[8 * 5];

    // items: tex,x,y,z,msg
    public int ni;
    public int[] it = new int[4 * 5];
    public boolean[] taken = new boolean[4];

    public int cx, cz;

    Band(int kx, int kz)
    {
        this.kx = kx;
        this.kz = kz;
        x0 = kx * Level.BAND;
        z0 = kz * Level.BAND;
        x1 = x0 + Level.BAND;
        z1 = z0 + Level.BAND;
        m = new Gen.MB();
        cx = x0 + Level.BAND / 2;
        cz = z0 + Level.BAND / 2;
    }

    void build(Level lv) {}

    void addFloor(int ax0, int az0, int ax1, int az1, int y, int tex, int uvs, int flags)
    {
        if ((nfl + 1) * 8 > fl.length)
        {
            int[] n = new int[fl.length * 2];
            System.arraycopy(fl, 0, n, 0, fl.length);
            fl = n;
        }
        int o = nfl * 8;
        fl[o] = ax0; fl[o + 1] = az0; fl[o + 2] = ax1; fl[o + 3] = az1;
        fl[o + 4] = y; fl[o + 5] = tex; fl[o + 6] = uvs; fl[o + 7] = flags;
        nfl++;
    }

    void addProp(int model, int x, int y, int z, int rot)
    {
        if ((npr + 1) * 5 > pr.length)
        {
            int[] n = new int[pr.length * 2];
            System.arraycopy(pr, 0, n, 0, pr.length);
            pr = n;
        }
        int o = npr * 5;
        pr[o] = model; pr[o + 1] = x; pr[o + 2] = y; pr[o + 3] = z; pr[o + 4] = rot;
        npr++;
    }

    void addItem(int tex, int x, int y, int z, int msg)
    {
        if ((ni + 1) * 5 > it.length)
        {
            int[] n = new int[it.length * 2];
            System.arraycopy(it, 0, n, 0, it.length);
            it = n;
            boolean[] tk = new boolean[n.length / 5];
            System.arraycopy(taken, 0, tk, 0, taken.length);
            taken = tk;
        }
        int o = ni * 5;
        it[o] = tex; it[o + 1] = x; it[o + 2] = y; it[o + 3] = z; it[o + 4] = msg;
        ni++;
    }

    public boolean hasGeometry()
    {
        return m.nf > 0 || nfl > 0;
    }

    // ------------------------------------------------------------- collision
    // Colliders are static, shared by every level instance (they never change).
    public static int ncol;
    public static int[] colData = new int[4096 * 5];

    public static void dropLastCollider()
    {
        if (ncol > 0) ncol--;
    }

    public static void addCollider(int x0, int z0, int x1, int z1, int h)
    {
        if ((ncol + 1) * 5 > colData.length) return;
        int o = ncol * 5;
        colData[o] = x0; colData[o + 1] = z0; colData[o + 2] = x1;
        colData[o + 3] = z1; colData[o + 4] = h;
        ncol++;
    }

    // ------------------------------------------------------------- triggers
    public static int ntrig;
    public static int[] trigData = new int[256 * 7];   // x0,z0,x1,z1,kind,id,msg
    public static boolean[] trigDone = new boolean[256];

    static void addTrigger(int x0, int z0, int x1, int z1, int kind, int id, int msg)
    {
        if (ntrig >= trigDone.length) return;
        int o = ntrig * 7;
        trigData[o] = x0; trigData[o + 1] = z0; trigData[o + 2] = x1;
        trigData[o + 3] = z1; trigData[o + 4] = kind; trigData[o + 5] = id;
        trigData[o + 6] = msg;
        ntrig++;
    }

    public static void resetTriggers()
    {
        ntrig = 0;
        ncol = 0;
        for (int i = 0; i < trigDone.length; i++) trigDone[i] = false;
    }

    // ------------------------------------------------------------------ draw
    public void drawFloor(Fb fb)
    {
        for (int i = 0; i < nfl; i++)
        {
            int o = i * 8;
            Render3D.drawFloor(fl[o], fl[o + 1], fl[o + 2], fl[o + 3],
                               fl[o + 4], fl[o + 5], fl[o + 6], fl[o + 7]);
        }
    }

    public void drawProps(Level lv)
    {
        for (int i = 0; i < npr; i++)
        {
            int o = i * 5;
            Render3D.drawModel(lv.models[pr[o]], pr[o + 1], pr[o + 2], pr[o + 3], pr[o + 4]);
        }
    }

    public void drawWalls()
    {
        if (mesh != null) Render3D.drawBand(mesh, 0, 0, 0);
    }
}
