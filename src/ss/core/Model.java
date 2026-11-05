package ss.core;

/**
 * A renderable mesh instance definition.  Vertices are world-space centimetres
 * relative to the model origin; the renderer transforms them per draw call.
 */
public final class Model
{
    public String name;
    public int nv;
    public int nf;
    public int[] vx, vy, vz;
    public int[] tu, tv;
    public int[] fi;
    public int[] fnx, fny, fnz;
    public int[] fflags;
    public int[] ftex;
    public int[] fcol;
    public int minX, minY, minZ, maxX, maxY, maxZ;

    public Model(int nv, int nf)
    {
        this.nv = nv;
        this.nf = nf;
        vx = new int[nv]; vy = new int[nv]; vz = new int[nv];
        tu = new int[nv]; tv = new int[nv];
        fi = new int[nf * 3];
        fnx = new int[nf]; fny = new int[nf]; fnz = new int[nf];
        fflags = new int[nf];
        ftex = new int[nf];
        fcol = new int[nf];
    }

    public void computeNormals()
    {
        for (int f = 0; f < nf; f++)
        {
            int a = fi[f * 3], b = fi[f * 3 + 1], c = fi[f * 3 + 2];
            int ux = vx[b] - vx[a], uy = vy[b] - vy[a], uz = vz[b] - vz[a];
            int wx = vx[c] - vx[a], wy = vy[c] - vy[a], wz = vz[c] - vz[a];
            int nx = uy * wz - uz * wy;
            int ny = uz * wx - ux * wz;
            int nz = ux * wy - uy * wx;
            int len = Core.isqrt(nx * nx + ny * ny + nz * nz);
            if (len < 1) len = 1;
            fnx[f] = (nx * 1024) / len;
            fny[f] = (ny * 1024) / len;
            fnz[f] = (nz * 1024) / len;
        }
    }

    public void bounds()
    {
        int x0 = 99999, y0 = 99999, z0 = 99999;
        int x1 = -99999, y1 = -99999, z1 = -99999;
        for (int i = 0; i < nv; i++)
        {
            if (vx[i] < x0) x0 = vx[i];
            if (vy[i] < y0) y0 = vy[i];
            if (vz[i] < z0) z0 = vz[i];
            if (vx[i] > x1) x1 = vx[i];
            if (vy[i] > y1) y1 = vy[i];
            if (vz[i] > z1) z1 = vz[i];
        }
        minX = x0; minY = y0; minZ = z0; maxX = x1; maxY = y1; maxZ = z1;
    }
}
