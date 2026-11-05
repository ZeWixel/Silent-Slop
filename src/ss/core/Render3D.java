package ss.core;

/**
 * Silent-Slop renderer.
 *
 * <pre>
 * world -> camera (free yaw, pitch locked at 30 deg, isometric framing)
 *       -> near clip -> screen -> frustum/cut-away culling
 *       -> back-to-front paint -> affine textured or flat-shaded scanlines
 * </pre>
 *
 * Design notes (all of them are about the S40/S60 budget):
 * <ul>
 * <li>Textures are palettized and every texel is fetched as
 *     {@code out[(light*FG + fog)*PALN + index]}; lighting and fog therefore
 *     cost one table lookup, not a multiply.</li>
 * <li>Fog is global: it is baked into that same table, and it also shortens the
 *     draw distance. The heavy draw distance that made the PS1 version stream
 *     data is replaced with fog + far fade, which is what actually saves heap
 *     here (fewer bands resident, no per-frame colour math).</li>
 * <li>Affine texture mapping, on purpose: no perspective divide in the inner
 *     loop, and the resulting warp is exactly the PS1 look.</li>
 * <li>Everything is integer; the only floating point in the engine is the
 *     one-off table cook in {@link Core}.</li>
 * </ul>
 */
public final class Render3D
{
    // ------------------------------------------------------------------ mode
    /** Textured + directional light, i.e. Affine Texture Mapping + Directional Lighting. */
    public static final int MODE_TEX = 0;
    /** Flat Shading + Directional Lighting (untextured, one colour per face). */
    public static final int MODE_FLAT = 1;

    // face flags (shared with the level data)
    public static final int F_DOUBLE = 1;
    public static final int F_WALL   = 2;
    public static final int F_NOFOG  = 4;
    public static final int F_EMIT   = 8;
    public static final int F_ROOF   = 16;
    public static final int F_ALPHA  = 32;

    public static final int MAXF = 1400;
    public static final int MAXV = 256;
    public static final int ZNEAR = 70;
    public static final int LV = 16;   // light levels
    public static final int FG = 8;    // fog steps
    public static final int PALN = 64; // palette size

    // ------------------------------------------------------------------ state
    static Fb fb;
    static int camX, camY, camZ;
    static int cosY, sinY, cosP, sinP;
    static int scale, scrCx, scrCy;
    static int mode = MODE_TEX;

    static int ldirX, ldirY, ldirZ;
    static int ambient;
    static int flashOn;
    static int beamX, beamY, beamZ;
    static int beamLen;
    static int playerZ, pxScreen, pyScreen;

    static int fogStart, fogEnd;
    public static int FOGCOLOR = 0x5290;

    // ------------------------------------------------------------------ palette
    public static final short[] PAL = new short[PALN];
    public static final short[] out = new short[LV * FG * PALN];

    public static void setFogColor(int c565)
    {
        if (c565 == FOGCOLOR) return;
        FOGCOLOR = c565;
        rebake();
    }

    public static void bakePalette(short[] base)
    {
        for (int i = 0; i < PALN; i++) PAL[i] = i < base.length ? base[i] : 0;
        rebake();
    }

    static void rebake()
    {
        for (int l = 0; l < LV; l++)
        {
            // Non linear light ramp: keeps the night readable.
            int f = (l * 256) / (LV - 1);
            f = (f * f) / 256;
            for (int g = 0; g < FG; g++)
            {
                int fog = (g * 256) / (FG - 1);
                int o = (l * FG + g) * PALN;
                for (int c = 0; c < PALN; c++)
                {
                    int s = Tex.shade(PAL[c], f);
                    if (fog > 0)
                    {
                        // Two step blend: the second pass toward the fog colour
                        // kills the saturation, so distant walls read as haze
                        // rather than as "grey paint".
                        int m = (fog * fog) >> 8;      // haze only really bites far away
                        s = Core.mix565(s, FOGCOLOR, m);
                        if (fog > 200) s = Core.mix565(s, FOGCOLOR, (fog - 200) >> 1);
                    }
                    out[o + c] = (short) s;
                }
            }
        }
    }

    // ------------------------------------------------------------------ scratch
    static final int[] cvx = new int[MAXV];
    static final int[] cvy = new int[MAXV];
    static final int[] cvz = new int[MAXV];
    static final int[] sx  = new int[MAXV];
    static final int[] sy  = new int[MAXV];
    static final boolean[] vis = new boolean[MAXV];
    static final boolean[] cutV = new boolean[MAXV];

    static int nf;
    static final int[][] fsx = new int[3][MAXF];
    static final int[][] fsy = new int[3][MAXF];
    static final int[][] fuv = new int[6][MAXF];
    static final int[] ftex = new int[MAXF];
    static final int[] fpal = new int[MAXF];
    static final int[] fcol = new int[MAXF];
    static final int[] fz = new int[MAXF];
    static final int[] fkey = new int[MAXF];
    static final int[] falpha = new int[MAXF];

    static int drawnTris;
    static int drawnBands;

    // near-plane clipping scratch
    static final int[] clx = new int[8];
    static final int[] cly = new int[8];
    static final int[] clz = new int[8];
    static final int[] clu = new int[8];
    static final int[] clv = new int[8];
    static final int[] ppx = new int[8];
    static final int[] ppy = new int[8];
    static int pn;

    private Render3D() {}

    // ------------------------------------------------------------------ camera
    public static void begin(Fb target, int ex, int ey, int ez, int yaw, int pitch,
                             int sc, int cx, int cy)
    {
        fb = target;
        camX = ex; camY = ey; camZ = ez;
        cosY = Core.cos(yaw); sinY = Core.sin(yaw);
        cosP = Core.cos(pitch); sinP = Core.sin(pitch);
        scale = sc;
        scrCx = cx; scrCy = cy;
        nf = 0;
        drawnTris = 0;
        drawnBands = 0;
    }

    public static void setMode(int m)
    {
        mode = m;
    }

    public static int getMode()
    {
        return mode;
    }

    public static void setLight(int dx, int dy, int dz, int amb)
    {
        // normalise the (already unit-ish) direction once
        ldirX = dx; ldirY = dy; ldirZ = dz;
        ambient = amb;
    }

    static int skyTopC, skyBottomC;

    public static void setSkyOnly(int top, int bottom)
    {
        skyTopC = top;
        skyBottomC = bottom;
    }

    public static void setFog(int start, int end)
    {
        fogStart = start;
        fogEnd = end < start + 64 ? start + 64 : end;
    }

    public static void setFlashlight(boolean on, int dx, int dy, int dz, int range)
    {
        flashOn = on ? 1 : 0;
        beamLen = range;
        int l = Core.dist2d(dx, dz);
        if (l < 1) l = 1;
        beamX = (dx << 6) / l;
        beamZ = (dz << 6) / l;
        beamY = (dy << 6) / (l + 200);   // slight downward tilt
    }

    /** Screen position of the player: the cut-away and the flashlight overlay use it. */
    public static void setPlayerScreen(int wx, int wy, int wz)
    {
        int dx = wx - camX;
        int dy = wy - camY;
        int dz = wz - camZ;
        int rx = (dx * cosY - dz * sinY) >> 12;
        int fz = (dx * sinY + dz * cosY) >> 12;
        int uy = (dy * cosP + fz * sinP) >> 12;
        int vz = (fz * cosP - dy * sinP) >> 12;
        playerZ = vz;
        pxScreen = scrCx + ((rx * scale) >> 8);
        pyScreen = scrCy - ((uy * scale) >> 8);
    }

    /** Projects a world point; result in {@link #tx},{@link #ty},{@link #tz}. */
    public static int tx, ty, tz;

    public static void project(int wx, int wy, int wz)
    {
        int dx = wx - camX;
        int dy = wy - camY;
        int dz = wz - camZ;
        int rx = (dx * cosY - dz * sinY) >> 12;
        int fz = (dx * sinY + dz * cosY) >> 12;
        int uy = (dy * cosP + fz * sinP) >> 12;
        int vz = (fz * cosP - dy * sinP) >> 12;
        if (vz < ZNEAR) vz = ZNEAR;
        tx = scrCx + ((rx * scale) >> 8);
        ty = scrCy - ((uy * scale) >> 8);
        tz = vz;
    }

    /** Camera space depth of a world point (used for prop culling). */
    public static int depth(int wx, int wy, int wz)
    {
        int dy = wy - camY;
        int dz = wz - camZ;
        int fz = ((wx - camX) * sinY + dz * cosY) >> 12;
        return ((fz * cosP - dy * sinP) >> 12);
    }

    // ------------------------------------------------------------------ shading
    static int shade(int nx, int ny, int nz, int flags, int cz)
    {
        if ((flags & F_EMIT) != 0) return LV - 1;
        int lv = ambient;
        int ndl = (nx * ldirX + ny * ldirY + nz * ldirZ) >> 10;
        if (ndl > 0) lv += (ndl * (LV - 1 - ambient)) >> 6;
        if (flashOn != 0 && playerZ < beamLen)
        {
            int nd = (nx * beamX + ny * beamY + nz * beamZ) >> 10;
            if (nd > 4)
            {
                int k = ((LV - 1) * (beamLen - playerZ)) / beamLen;
                k = (k * nd) >> 6;
                lv += k;
            }
        }
        if (lv > LV - 1) lv = LV - 1;
        if (lv < 0) lv = 0;
        return lv;
    }

    static int fogOf(int cz, int flags)
    {
        if ((flags & F_NOFOG) != 0) return 0;
        int f = ((cz - fogStart) * FG) / (fogEnd - fogStart);
        if (f < 0) return 0;
        if (f > FG - 1) return FG - 1;
        return f;
    }

    // ------------------------------------------------------------------ meshes
    static void xformModel(Model m, int px, int py, int pz, int rotY)
    {
        int c = Core.cos(rotY);
        int s = Core.sin(rotY);
        int nv = m.nv > MAXV ? MAXV : m.nv;
        for (int i = 0; i < nv; i++)
        {
            int x = m.vx[i];
            int z = m.vz[i];
            int wx = px + ((x * c - z * s) >> 12);
            int wz = pz + ((x * s + z * c) >> 12);
            int wy = py + m.vy[i];
            int dx = wx - camX;
            int dy = wy - camY;
            int dz = wz - camZ;
            int rx = (dx * cosY - dz * sinY) >> 12;
            int fz = (dx * sinY + dz * cosY) >> 12;
            int uy = (dy * cosP + fz * sinP) >> 12;
            int vz = (fz * cosP - dy * sinP) >> 12;
            cvx[i] = rx; cvy[i] = uy; cvz[i] = vz;
            if (vz < ZNEAR) vis[i] = false;
            else
            {
                vis[i] = true;
                sx[i] = scrCx + ((rx * scale) >> 8);
                sy[i] = scrCy - ((uy * scale) >> 8);
            }
        }
    }

    /**
     * Draws a prop / character mesh.
     * In {@link #MODE_FLAT} the faces are emitted untextured (PS1 flat shading),
     * in {@link #MODE_TEX} they use the affine mapper.
     */
    public static void drawModel(Model m, int px, int py, int pz, int rotY)
    {
        if (m == null) return;
        xformModel(m, px, py, pz, rotY);
        int c = Core.cos(rotY);
        int s = Core.sin(rotY);
        int nv = m.nv > MAXV ? MAXV : m.nv;
        int flat = (mode == MODE_FLAT) ? 1 : 0;
        for (int f = 0; f < m.nf && nf < MAXF - 3; f++)
        {
            int a = m.fi[f * 3], b = m.fi[f * 3 + 1], cc = m.fi[f * 3 + 2];
            if (a >= nv || b >= nv || cc >= nv) continue;
            int flags = m.fflags[f];
            if (!vis[a] && !vis[b] && !vis[cc]) continue;
            int area = (cvx[b] - cvx[a]) * (cvy[cc] - cvy[a]) -
                       (cvx[cc] - cvx[a]) * (cvy[b] - cvy[a]);
            if (area <= 0 && (flags & F_DOUBLE) == 0) continue;   // band meshes are wound CW
            int nx = m.fnx[f], ny = m.fny[f], nz = m.fnz[f];
            // rotate the normal into world space
            int wnx = (nx * c - nz * s) >> 12;
            int wnz = (nx * s + nz * c) >> 12;
            if (area < 0) { wnx = -wnx; ny = -ny; wnz = -wnz; }
            int cz = (cvz[a] + cvz[b] + cvz[cc]) / 3;
            int lv = shade(wnx, ny, wnz, flags, cz);
            int fg = fogOf(cz, flags);
            int tex = m.ftex[f];
            if (flat != 0 || tex < 0 || tex >= TexStore.count())
            {
                emitFlatClipped(cvx[a], cvy[a], cvz[a],
                                cvx[b], cvy[b], cvz[b],
                                cvx[cc], cvy[cc], cvz[cc],
                                out[(lv * FG + fg) * PALN + (m.fcol[f] & 0x3F)], cz);
            }
            else
            {
                emitClipped(cvx[a], cvy[a], cvz[a], m.tu[a], m.tv[a],
                            cvx[b], cvy[b], cvz[b], m.tu[b], m.tv[b],
                            cvx[cc], cvy[cc], cvz[cc], m.tu[cc], m.tv[cc],
                            tex, (lv * FG + fg) * PALN, cz,
                            (flags & F_ALPHA) != 0 ? 1 : 0);
            }
        }
    }

    /** Flat triangle with the same near clipping. */
    static void emitFlatClipped(int ax, int ay, int az, int bx, int by, int bz,
                                int cx, int cy, int cz, int col, int depth)
    {
        pn = 0;
        clipEdge(ax, ay, az, 0, 0, bx, by, bz, 0, 0);
        clipEdge(bx, by, bz, 0, 0, cx, cy, cz, 0, 0);
        clipEdge(cx, cy, cz, 0, 0, ax, ay, az, 0, 0);
        if (pn < 3) return;
        for (int i = 0; i < pn; i++)
        {
            projCam(clx[i], cly[i], clz[i], pj);
            ppx[i] = pj[0];
            ppy[i] = pj[1];
        }
        for (int i = 1; i + 1 < pn; i++)
            emitFlat(ppx[0], ppy[0], ppx[i], ppy[i], ppx[i + 1], ppy[i + 1], col, depth);
    }

    /** Cheap contact shadow: an ellipse of the darkest palette entry. */
    public static void drawShadow(int wx, int wz, int r)
    {
        project(wx, 2, wz);
        int cx = tx, cy = ty;
        int rr = (r * scale) >> 8;
        if (rr < 2) rr = 2;
        int ry = (rr * 5) >> 3;         // squashed: the camera looks down 30 deg
        short sv = out[(0 * FG + fogOf(tz, 0)) * PALN];
        short[] p = fb.px;
        int W = fb.w, H = fb.h;
        for (int y = -ry; y <= ry; y++)
        {
            int yy = cy + y;
            if (yy < 0 || yy >= H) continue;
            int k = (y * 64) / (ry == 0 ? 1 : ry);
            int half = rr - ((k * k * rr) >> 12);
            if (half < 0) half = 0;
            int l = cx - half, rgt = cx + half;
            if (l < 0) l = 0;
            if (rgt >= W) rgt = W - 1;
            int o = yy * W;
            for (int x = l; x <= rgt; x++) p[o + x] = sv;
        }
    }

    // ------------------------------------------------------------------ bands
    /**
     * Draws a wall band.  Bands are always rendered textured (the levels have
     * exactly one render mode, Affine Texture Mapping), and they are the only
     * geometry that can be cut away.
     */
    public static void drawBand(Model m, int px, int py, int pz)
    {
        drawnBands++;
        int nv = m.nv > MAXV ? MAXV : m.nv;
        for (int i = 0; i < nv; i++)
        {
            int wx = px + m.vx[i];
            int wy = py + m.vy[i];
            int wz = pz + m.vz[i];
            int dx = wx - camX;
            int dy = wy - camY;
            int dz = wz - camZ;
            int rx = (dx * cosY - dz * sinY) >> 12;
            int fz = (dx * sinY + dz * cosY) >> 12;
            int uy = (dy * cosP + fz * sinP) >> 12;
            int vz = (fz * cosP - dy * sinP) >> 12;
            cvx[i] = rx; cvy[i] = uy; cvz[i] = vz;
            vis[i] = vz > ZNEAR;
            if (vis[i])
            {
                sx[i] = scrCx + ((rx * scale) >> 8);
                sy[i] = scrCy - ((uy * scale) >> 8);
                cutV[i] = vz < playerZ - 16;
            }
            else
            {
                cutV[i] = false;
            }
        }
        for (int f = 0; f < m.nf && nf < MAXF - 3; f++)
        {
            int a = m.fi[f * 3], b = m.fi[f * 3 + 1], c = m.fi[f * 3 + 2];
            if (a >= nv || b >= nv || c >= nv) continue;
            int flags = m.fflags[f];
            if ((flags & F_ROOF) != 0) continue;         // never seen from below
            int tex = m.ftex[f];
            if (tex < 0 || tex >= TexStore.count()) continue;
            if (!vis[a] && !vis[b] && !vis[c]) continue;
            // back-face test on camera space (independent of clipping)
            int area = (cvx[b] - cvx[a]) * (cvy[c] - cvy[a]) -
                       (cvx[c] - cvx[a]) * (cvy[b] - cvy[a]);
            // camera space is Y-up, so a front facing triangle is CCW (area > 0)
            if (area <= 0 && (flags & F_DOUBLE) == 0) continue;
            if (vis[a] && vis[b] && vis[c])
            {
                int scrArea = (sx[b] - sx[a]) * (sy[c] - sy[a]) -
                              (sx[c] - sx[a]) * (sy[b] - sy[a]);
                if (scrArea == 0) continue;
                // Cut-away: skip any wall face that (a) sits between the camera
                // and the player and (b) covers his silhouette on screen.  The
                // bbox test is what makes it work for a whole wall segment, not
                // just for faces that fit inside the silhouette.
                if (cutV[a] && cutV[b] && cutV[c] && overPlayer(a, b, c)) continue;
            }
            int nx = m.fnx[f], ny = m.fny[f], nz = m.fnz[f];
            if (area < 0) { nx = -nx; ny = -ny; nz = -nz; }
            int cz = (cvz[a] + cvz[b] + cvz[c]) / 3;
            int lv = shade(nx, ny, nz, flags, cz);
            int fg = fogOf(cz, flags);
            emitClipped(cvx[a], cvy[a], cvz[a], m.tu[a], m.tv[a],
                        cvx[b], cvy[b], cvz[b], m.tu[b], m.tv[b],
                        cvx[c], cvy[c], cvz[c], m.tu[c], m.tv[c],
                        tex, (lv * FG + fg) * PALN, cz, 0);
        }
    }

    /**
     * Projects and emits a camera-space triangle, clipping it against the near
     * plane first.  Without this a wall that the camera stands inside would
     * smear across the whole screen.
     */
    static void emitClipped(int ax, int ay, int az, int au, int av,
                            int bx, int by, int bz, int bu, int bv,
                            int cx, int cy, int cz, int cu, int cv,
                            int tex, int palOff, int depth, int alpha)
    {
        // gather the polygon on the near side of z = ZNEAR
        pn = 0;
        clipEdge(ax, ay, az, au, av, bx, by, bz, bu, bv);
        clipEdge(bx, by, bz, bu, bv, cx, cy, cz, cu, cv);
        clipEdge(cx, cy, cz, cu, cv, ax, ay, az, au, av);
        dbgClip++;
        if (pn < 3) { dbgDrop++; return; }
        int W = fb.w, H = fb.h;
        for (int i = 0; i < pn; i++)
        {
            projCam(clx[i], cly[i], clz[i], pj);
            ppx[i] = pj[0];
            ppy[i] = pj[1];
        }
        for (int i = 1; i + 1 < pn; i++)
        {
            int q0x = ppx[0], q0y = ppy[0];
            int q1x = ppx[i], q1y = ppy[i];
            int q2x = ppx[i + 1], q2y = ppy[i + 1];
            // Reject on the screen-space bounding box only.  A big floor quad
            // in an isometric view routinely has every corner off screen while
            // still covering it, so a per-corner test would punch holes in the
            // ground.
            int bxl = q0x, bxr = q0x, byt = q0y, byb = q0y;
            if (q1x < bxl) bxl = q1x;
            if (q1x > bxr) bxr = q1x;
            if (q2x < bxl) bxl = q2x;
            if (q2x > bxr) bxr = q2x;
            if (q1y < byt) byt = q1y;
            if (q1y > byb) byb = q1y;
            if (q2y < byt) byt = q2y;
            if (q2y > byb) byb = q2y;
            if (bxr < 0 || bxl > W || byb < 0 || byt > H) { dbgDrop++; continue; }
            dbgEmit++;
            emitTex(q0x, q0y, q1x, q1y, q2x, q2y,
                    clu[0], clv[0], clu[i], clv[i], clu[i + 1], clv[i + 1],
                    tex, palOff, depth, alpha);
        }
    }

    /** Projects a camera-space point, clamping to a sane screen box. */
    static void projCam(int x, int y, int z, int[] out)
    {
        int sxv = scrCx + ((x * scale) >> 8);
        int syv = scrCy - ((y * scale) >> 8);
        int lim = 4096;
        if (sxv > lim) sxv = lim;
        if (sxv < -lim) sxv = -lim;
        if (syv > lim) syv = lim;
        if (syv < -lim) syv = -lim;
        out[0] = sxv;
        out[1] = syv;
    }

    static final int[] pj = new int[2];

    static void clipEdge(int ax, int ay, int az, int au, int av,
                         int bx, int by, int bz, int bu, int bv)
    {
        boolean inA = az >= ZNEAR;
        boolean inB = bz >= ZNEAR;
        if (inA && pn < 8)
        {
            clx[pn] = ax; cly[pn] = ay; clz[pn] = az;
            clu[pn] = au; clv[pn] = av; pn++;
        }
        if (inA != inB && pn < 8)
        {
            int dz = bz - az;
            if (dz == 0) return;
            int t = ((ZNEAR - az) << 12) / dz;      // 0..4096 along A->B
            clx[pn] = ax + (((bx - ax) * t) >> 12);
            cly[pn] = ay + (((by - ay) * t) >> 12);
            clz[pn] = ZNEAR;
            clu[pn] = au + (((bu - au) * t) >> 12);
            clv[pn] = av + (((bv - av) * t) >> 12);
            pn++;
        }
    }

    /** Draws a level floor quad (always textured), with near clipping. */
    public static void drawFloor(int x0, int z0, int x1, int z1, int y, int tex,
                                 int uvs, int flags)
    {
        if (tex < 0 || tex >= TexStore.count()) return;
        // A ceiling seen from above hides the room it belongs to, and a roof
        // above the camera shows us its underside: skip both.  Exterior roofs
        // below the camera are the thing the isometric camera is there for.
        if ((flags & F_ROOF) != 0 && (hideRoofs || y > camY - 20)) return;

        int[] cx = fl4x;
        int[] cy = fl4y;
        int[] cz = fl4z;
        camSpace(x0, y, z1, cx, cy, cz, 0);
        camSpace(x1, y, z1, cx, cy, cz, 1);
        camSpace(x1, y, z0, cx, cy, cz, 2);
        camSpace(x0, y, z0, cx, cy, cz, 3);

        // Average depth drives the fog (the nearest corner alone would leave a
        // big quad unfogged, the farthest one would fog the whole screen).
        int depth = (cz[0] + cz[1] + cz[2] + cz[3]) >> 2;
        if (depth < ZNEAR) depth = ZNEAR;
        int lv = shade(0, 1024, 0, flags, depth);
        int fg = fogOf(depth, flags);
        int palOff = (lv * FG + fg) * PALN;
        int uu = ((x1 - x0) << 6) / uvs;
        int vv = ((z1 - z0) << 6) / uvs;

        emitClipped(cx[0], cy[0], cz[0], 0, 0,
                    cx[1], cy[1], cz[1], uu, 0,
                    cx[2], cy[2], cz[2], uu, vv, tex, palOff, depth, 0);
        emitClipped(cx[0], cy[0], cz[0], 0, 0,
                    cx[2], cy[2], cz[2], uu, vv,
                    cx[3], cy[3], cz[3], 0, vv, tex, palOff, depth, 0);
    }

    static final int[] fl4x = new int[8];
    static final int[] fl4y = new int[8];
    static final int[] fl4z = new int[8];

    static void camSpace(int wx, int wy, int wz, int[] ox, int[] oy, int[] oz, int i)
    {
        int dx = wx - camX;
        int dy = wy - camY;
        int dz = wz - camZ;
        int rx = (dx * cosY - dz * sinY) >> 12;
        int fz = (dx * sinY + dz * cosY) >> 12;
        ox[i] = rx;
        oy[i] = (dy * cosP + fz * sinP) >> 12;
        oz[i] = (fz * cosP - dy * sinP) >> 12;
    }

    static final int[] flat4x = new int[8];
    static final int[] flat4y = new int[8];
    static final int[] cam4z = new int[8];

    // ------------------------------------------------------------------ emit
    static void emitFlat(int x0, int y0, int x1, int y1, int x2, int y2, int col, int z)
    {
        if (nf >= MAXF) return;
        int i = nf++;
        fsx[0][i] = x0; fsy[0][i] = y0;
        fsx[1][i] = x1; fsy[1][i] = y1;
        fsx[2][i] = x2; fsy[2][i] = y2;
        ftex[i] = -1;
        fcol[i] = col;
        falpha[i] = 0;
        fz[i] = z;
        fkey[i] = z;
    }

    static void emitTex(int x0, int y0, int x1, int y1, int x2, int y2,
                        int u0, int v0, int u1, int v1, int u2, int v2,
                        int tex, int palOff, int z, int alpha)
    {
        if (nf >= MAXF) return;
        int i = nf++;
        fsx[0][i] = x0; fsy[0][i] = y0;
        fsx[1][i] = x1; fsy[1][i] = y1;
        fsx[2][i] = x2; fsy[2][i] = y2;
        fuv[0][i] = u0; fuv[1][i] = v0;
        fuv[2][i] = u1; fuv[3][i] = v1;
        fuv[4][i] = u2; fuv[5][i] = v2;
        ftex[i] = tex;
        fpal[i] = palOff;
        falpha[i] = alpha;
        fz[i] = z;
        fkey[i] = z;
    }

    // ------------------------------------------------------------------ flush
    /** Sorts back to front and rasterizes. */
    public static void flush()
    {
        int n = nf;
        sortFaces(n);
        for (int k = 0; k < n; k++)
        {
            int i = order[k];
            drawnTris++;
            if (ftex[i] < 0)
                triFlat(fsx[0][i], fsy[0][i], fsx[1][i], fsy[1][i],
                        fsx[2][i], fsy[2][i], fcol[i], 0);
            else triTex(i);
        }
        nf = 0;
    }

    static final int SORT_BUCKETS = 64;
    static final int[] bucketCount = new int[SORT_BUCKETS + 2];
    static final int[] order = new int[MAXF + 1];

    /**
     * Counting sort on 64 depth buckets, far to near.  No allocation, no
     * comparisons - a 900 triangle frame sorts in a few thousand ops.
     */
    static void sortFaces(int n)
    {
        if (n < 2) return;
        int zmin = fz[0], zmax = fz[0];
        for (int i = 1; i < n; i++)
        {
            int z = fz[i];
            if (z < zmin) zmin = z;
            if (z > zmax) zmax = z;
        }
        int span = zmax - zmin;
        if (span < SORT_BUCKETS) span = SORT_BUCKETS;
        for (int i = 0; i <= SORT_BUCKETS + 1; i++) bucketCount[i] = 0;
        for (int i = 0; i < n; i++)
        {
            int b = ((zmax - fz[i]) * SORT_BUCKETS) / span;
            if (b < 0) b = 0;
            if (b > SORT_BUCKETS) b = SORT_BUCKETS;
            fkey[i] = b;
            bucketCount[b + 1]++;
        }
        for (int i = 1; i <= SORT_BUCKETS + 1; i++) bucketCount[i] += bucketCount[i - 1];
        for (int i = 0; i < n; i++) order[bucketCount[fkey[i]]++] = i;
    }

    // ------------------------------------------------------------------ raster
    /** Filled, flat triangle with optional 2x2 ordered dither alpha. */
    public static void triFlat(int x0, int y0, int x1, int y1, int x2, int y2,
                               int col, int alpha16)
    {
        if (y0 > y1) { int t = x0; x0 = x1; x1 = t; t = y0; y0 = y1; y1 = t; }
        if (y1 > y2) { int t = x1; x1 = x2; x2 = t; t = y1; y1 = y2; y2 = t; }
        if (y0 > y1) { int t = x0; x0 = x1; x1 = t; t = y0; y0 = y1; y1 = t; }
        if (y2 == y0) return;
        short v = (short) col;
        short[] p = fb.px;
        int W = fb.w;
        int yStart = y0 < 0 ? 0 : y0;
        int yEnd = y2 >= fb.h ? fb.h - 1 : y2;
        int dy1 = y1 - y0;
        int dy2 = y2 - y0;
        int dy3 = y2 - y1;
        for (int y = yStart; y <= yEnd; y++)
        {
            int xa, xb;
            if (y < y1)
            {
                xa = x0 + (int) ((long) (x1 - x0) * (y - y0) / dy1);
                xb = x0 + (int) ((long) (x2 - x0) * (y - y0) / dy2);
            }
            else
            {
                if (dy3 == 0) continue;
                xa = x1 + (int) ((long) (x2 - x1) * (y - y1) / dy3);
                xb = x0 + (int) ((long) (x2 - x0) * (y - y0) / dy2);
            }
            if (xa > xb) { int t = xa; xa = xb; xb = t; }
            if (xb < 0 || xa >= W) continue;
            if (xa < 0) xa = 0;
            if (xb >= W) xb = W - 1;
            int o = y * W + xa;
            int cnt = xb - xa + 1;
            if (alpha16 >= 16 || alpha16 <= 0)
            {
                for (int i = 0; i < cnt; i++) p[o + i] = v;
            }
            else
            {
                int row = (y & 3) << 2;
                for (int i = 0; i < cnt; i++)
                {
                    if (BAYER[row | ((xa + i) & 3)] < alpha16) p[o + i] = v;
                }
            }
        }
    }

    static final byte[] BAYER = { 0, 8, 2, 10, 12, 4, 14, 6, 3, 11, 1, 9, 15, 7, 13, 5 };

    /** Affine textured triangle - the PS1 look, no perspective divide. */
    public static void triTex(int idx)
    {
        int x0 = fsx[0][idx], y0 = fsy[0][idx];
        int x1 = fsx[1][idx], y1 = fsy[1][idx];
        int x2 = fsx[2][idx], y2 = fsy[2][idx];
        int u0 = fuv[0][idx], v0 = fuv[1][idx];
        int u1 = fuv[2][idx], v1 = fuv[3][idx];
        int u2 = fuv[4][idx], v2 = fuv[5][idx];

        if (y0 > y1) { int t;
            t = x0; x0 = x1; x1 = t; t = y0; y0 = y1; y1 = t;
            t = u0; u0 = u1; u1 = t; t = v0; v0 = v1; v1 = t; }
        if (y1 > y2) { int t;
            t = x1; x1 = x2; x2 = t; t = y1; y1 = y2; y2 = t;
            t = u1; u1 = u2; u2 = t; t = v1; v1 = v2; v2 = t; }
        if (y0 > y1) { int t;
            t = x0; x0 = x1; x1 = t; t = y0; y0 = y1; y1 = t;
            t = u0; u0 = u1; u1 = t; t = v0; v0 = v1; v1 = t; }

        Tex tex = TexStore.get(ftex[idx]);
        if (tex == null) return;

        short[] p = fb.px;
        int W = fb.w, H = fb.h;
        byte[] ti = tex.idx;
        short[] pal = out;
        int palOff = fpal[idx];
        int mw = tex.maskW, mh = tex.maskH;
        int tw = tex.w;
        int alpha = falpha[idx] != 0 ? 12 : 16;
        int dy1 = y1 - y0;
        int dy2 = y2 - y0;
        int dy3 = y2 - y1;
        int yStart = y0 < 0 ? 0 : y0;
        int yEnd = y2 >= H ? H - 1 : y2;
        for (int y = yStart; y <= yEnd; y++)
        {
            int xa, xb, ua, va, ub, vb;
            if (y < y1)
            {
                xa = x0 + (int) ((long) (x1 - x0) * (y - y0) / dy1);
                ua = u0 + (int) ((long) (u1 - u0) * (y - y0) / dy1);
                va = v0 + (int) ((long) (v1 - v0) * (y - y0) / dy1);
                xb = x0 + (int) ((long) (x2 - x0) * (y - y0) / dy2);
                ub = u0 + (int) ((long) (u2 - u0) * (y - y0) / dy2);
                vb = v0 + (int) ((long) (v2 - v0) * (y - y0) / dy2);
            }
            else
            {
                if (dy3 == 0) continue;
                int d = y - y1;
                xa = x1 + (int) ((long) (x2 - x1) * d / dy3);
                ua = u1 + (int) ((long) (u2 - u1) * d / dy3);
                va = v1 + (int) ((long) (v2 - v1) * d / dy3);
                xb = x0 + (int) ((long) (x2 - x0) * (y - y0) / dy2);
                ub = u0 + (int) ((long) (u2 - u0) * (y - y0) / dy2);
                vb = v0 + (int) ((long) (v2 - v0) * (y - y0) / dy2);
            }
            if (xa == xb) continue;
            if (xa > xb)                     // keep the span left-to-right
            {
                int t;
                t = xa; xa = xb; xb = t;
                t = ua; ua = ub; ub = t;
                t = va; va = vb; vb = t;
            }
            int dx = xb - xa;
            int du = (ub - ua) / dx;
            int dv = (vb - va) / dx;
            if (xa < 0)
            {
                ua -= du * xa;
                va -= dv * xa;
                xa = 0;
            }
            if (xb >= W) xb = W - 1;
            if (xb < xa) continue;
            int off = y * W + xa;
            int u = ua, v = va;
            int cnt = xb - xa + 1;
            if (alpha >= 16)
            {
                for (int i = 0; i < cnt; i++)
                {
                    int c = ti[(((v >> 6) & mh) * tw) + ((u >> 6) & mw)];
                    if (c != 0) p[off + i] = pal[palOff + c];
                    u += du;
                    v += dv;
                }
            }
            else
            {
                for (int i = 0; i < cnt; i++)
                {
                    int c = ti[(((v >> 6) & mh) * tw) + ((u >> 6) & mw)];
                    if (c != 0) p[off + i] = pal[palOff + c];
                    u += du;
                    v += dv;
                }
            }
        }
    }

    // ------------------------------------------------------------------ overlays
    /**
     * Flashlight pool, drawn as a dithered radial falloff straight into the
     * framebuffer.  The alpha per texel is a 16x16 lookup, so this is a few
     * thousand blended pixels and no per pixel division.
     */
    static final byte[] POOL = new byte[16 * 16];

    static
    {
        for (int y = 0; y < 16; y++)
        {
            for (int x = 0; x < 16; x++)
            {
                int dx = x - 8, dy = y - 8;
                int d2 = dx * dx + dy * dy;
                int a = 0;
                if (d2 < 62)
                {
                    a = 15 - (d2 * 15) / 62;
                }
                POOL[y * 16 + x] = (byte) a;
            }
        }
    }

    public static void overlayFlashlight(int wx, int wz, int wide, int highV)
    {
        if (flashOn == 0 || fb == null) return;
        project(wx, 40, wz);
        int cx = tx, cy = ty;
        int half = wide >> 1;
        int hh = highV >> 1;
        short[] p = fb.px;
        int W = fb.w, H = fb.h;
        int warm = PAL[11];
        for (int y = -hh; y < hh; y++)
        {
            int yy = cy + y;
            if (yy < 0 || yy >= H) continue;
            int ry = ((y + hh) * 16) / (hh < 1 ? 1 : hh);
            int row = (ry >> 1) << 4;
            int o = yy * W;
            for (int x = -half; x < half; x++)
            {
                int xx = cx + x;
                if (xx < 0 || xx >= W) continue;
                int a = POOL[row | (((x + half) * 16) / (half < 1 ? 1 : half) >> 1)];
                if (a <= 0) continue;
                if (BAYER[((yy & 3) << 2) | (xx & 3)] >= a) continue;
                int c = p[o + xx];
                p[o + xx] = (short) Core.mix565(c, warm, 26);
            }
        }
    }

    /**
     * Fog vignette: the far world is drawn with fog baked into the palette, and
     * this dithered border sells the transition at the edge of the screen.
     * Drawn as a handful of rectangles, not per pixel.
     */
    public static void overlayFogVignette(int strength)
    {
        if (strength <= 0) return;
        short[] p = fb.px;
        int W = fb.w, H = fb.h;
        int rings = 4;
        for (int r = 0; r < rings; r++)
        {
            int a = (strength * (rings - r) * 16) / (rings * rings);
            if (a <= 0) continue;
            int inset = r * 2;
            int x0 = inset, y0 = inset, x1 = W - 1 - inset, y1 = H - 1 - inset;
            int band = 2;
            for (int y = y0; y < y0 + band && y < H; y++) blendRow(p, W, y, x0, x1, a);
            for (int y = y1 - band; y <= y1 && y < H; y++)
                if (y > y0 + band) blendRow(p, W, y, x0, x1, a);
            for (int y = y0 + band; y < y1 - band; y++)
            {
                if (y < 0 || y >= H) continue;
                blendSpan(p, y * W, x0, x0 + band, a);
                blendSpan(p, y * W, x1 - band, x1, a);
            }
        }
    }

    static void blendRow(short[] p, int W, int y, int x0, int x1, int a)
    {
        int o = y * W;
        for (int x = x0; x <= x1; x++)
        {
            if (x < 0 || x >= W) continue;
            if (BAYER[((y & 3) << 2) | (x & 3)] >= a) continue;
            p[o + x] = (short) Core.mix565(p[o + x], FOGCOLOR, 20 + a * 3);
        }
    }

    static void blendSpan(short[] p, int o, int x0, int x1, int a)
    {
        int y = o / fb.w;
        for (int x = x0; x <= x1; x++)
        {
            if (x < 0 || x >= fb.w) continue;
            if (BAYER[((y & 3) << 2) | (x & 3)] >= a) continue;
            p[o + x] = (short) Core.mix565(p[o + x], FOGCOLOR, 20 + a * 3);
        }
    }

    /** True when the screen-space bbox of a face covers Harry's silhouette. */
    public static int dbgClip, dbgDrop, dbgEmit;
    /** Interior levels: never draw ceilings, the camera looks down through them. */
    public static boolean hideRoofs;

    static boolean overPlayer(int a, int b, int c)
    {
        int x0 = sx[a], x1 = sx[a], y0 = sy[a], y1 = sy[a];
        if (sx[b] < x0) x0 = sx[b];
        if (sx[b] > x1) x1 = sx[b];
        if (sy[b] < y0) y0 = sy[b];
        if (sy[b] > y1) y1 = sy[b];
        if (sx[c] < x0) x0 = sx[c];
        if (sx[c] > x1) x1 = sx[c];
        if (sy[c] < y0) y0 = sy[c];
        if (sy[c] > y1) y1 = sy[c];
        // Very large faces are a whole wall segment: cutting them would punch a
        // hole straight through the building, so leave them and let Harry be
        // painted on top of the walls instead.
        if ((x1 - x0) * (y1 - y0) > 26000) return false;
        if (x1 < pxScreen - 34) return false;
        if (x0 > pxScreen + 34) return false;
        if (y1 < pyScreen - 100) return false;
        if (y0 > pyScreen + 26) return false;
        return true;
    }

    public static boolean needsFlush()
    {
        return nf > MAXF - 320;
    }

    public static int statsTris()
    {
        return drawnTris;
    }

    public static int statsBands()
    {
        return drawnBands;
    }
}
