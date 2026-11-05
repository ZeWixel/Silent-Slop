package ss.core;

/**
 * A low-poly mesh, plain arrays (no per-frame allocation, no object graph).
 *
 * Vertex coordinates are in world "centimetres" * 1, i.e. the same unit the
 * decompilation uses for map coordinates (1.0 unit == 1 cm).
 */
public final class Mesh
{
    public int nv;
    public int nf;
    public int[] vx, vy, vz;
    public int[] fi;            // 3 indices per face
    public int[] fnx, fny, fnz; // unit normals * 1024, model space
    public int[] fflags;
    public int[] ftex;          // texture id or -1
    public int[] fcol;          // palette index, 0..PALN-1
    public int[] tu, tv;        // texture coords per vertex, fixed point *64

    public int cx, cy, cz;      // bounding box centre

    public Mesh(int nv, int nf)
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

    /** Recomputes face normals from the winding (CCW when seen from outside). */
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
}
