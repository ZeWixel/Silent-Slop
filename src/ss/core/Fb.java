package ss.core;

/**
 * Software framebuffer, RGB565.
 *
 * RGB565 is chosen on purpose: on S60/S40 handsets the frame can be handed to
 * `DirectGraphics.drawPixels(short[], .., TYPE_USHORT_565_RGB, ..)` without any
 * conversion, and on MIDP-2-only devices it converts to an Image with a single
 * shift/mask pass.
 */
public final class Fb
{
    public int w;
    public int h;
    public short[] px;

    public Fb(int w, int h)
    {
        this.w = w;
        this.h = h;
        px = new short[w * h];
    }

    public void clear(int c)
    {
        short v = (short) c;
        short[] p = px;
        for (int i = p.length - 1; i >= 0; i--) p[i] = v;
    }

    /** Vertical gradient, used for the fog-drowned sky / backdrop. */
    public void vgrad(int c0, int c1)
    {
        short[] p = px;
        int ww = w;
        for (int y = 0; y < h; y++)
        {
            int c = Core.mix565(c0, c1, (y * 256) / (h <= 1 ? 1 : h - 1));
            short v = (short) c;
            int o = y * ww;
            for (int x = 0; x < ww; x++) p[o + x] = v;
        }
    }

    public void hline(int x0, int x1, int y, int c)
    {
        if (y < 0 || y >= h) return;
        if (x0 < 0) x0 = 0;
        if (x1 >= w) x1 = w - 1;
        if (x1 < x0) return;
        short v = (short) c;
        short[] p = px;
        int o = y * w;
        for (int x = x0; x <= x1; x++) p[o + x] = v;
    }

    public void rect(int x0, int y0, int x1, int y1, int c)
    {
        for (int y = y0; y <= y1; y++) hline(x0, x1, y, c);
    }

    /** Copy with a nearest-neighbour integer upscale (keeps fill rate low). */
    public void upscaleTo(Fb dst)
    {
        int fx = dst.w / w;
        int fy = dst.h / h;
        if (fx < 1) fx = 1;
        if (fy < 1) fy = 1;
        short[] s = px;
        short[] d = dst.px;
        int dw = dst.w;
        int dy = 0;
        for (int y = 0; y < h; y++)
        {
            int so = y * w;
            for (int ry = 0; ry < fy; ry++)
            {
                int dofs = (dy + ry) * dw;
                if (dy + ry >= dst.h) break;
                for (int x = 0; x < w; x++)
                {
                    short v = s[so + x];
                    int doff = dofs + x * fx;
                    for (int rx = 0; rx < fx; rx++)
                    {
                        int idx = doff + rx;
                        if (idx < dofs + dw) d[idx] = v;
                    }
                }
            }
            dy += fy;
        }
    }

    /** Fade the whole buffer towards black, f = 0 (none) .. 256 (black). */
    public void fadeTo(int color, int f)
    {
        if (f <= 0) return;
        short[] p = px;
        if (f >= 256)
        {
            clear(color);
            return;
        }
        for (int i = p.length - 1; i >= 0; i--)
            p[i] = (short) Core.mix565(p[i], color, f);
    }
}
