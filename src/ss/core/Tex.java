package ss.core;

/**
 * Palettized texture with baked lighting levels.
 *
 * Storage: one byte of palette index per texel plus a palette bank of
 * {@link #LEVELS} graduations (dark -> bright). Shading therefore costs a
 * single table lookup per pixel, which is the only way a 104 MHz ARM9 can
 * afford textured, per-face-lit geometry at 176x208.
 *
 * A texel is read as: rgb = pal[(level << 6) + idx[texel]]
 */
public final class Tex
{
    public static final int LEVELS = 16;
    public static final int MAXCOL = 64;

    public int w;
    public int h;
    public int maskW;
    public int maskH;
    public byte[] idx;
    public short[] pal; // LEVELS * MAXCOL

    public Tex(int w, int h)
    {
        this.w = w;
        this.h = h;
        maskW = w - 1;
        maskH = h - 1;
        idx = new byte[w * h];
        pal = new short[LEVELS * MAXCOL];
    }

    /** Multiplies a 565 color, keeping the hue - used to bake the light levels. */
    public static int shade(int c, int f)
    {
        int r = ((c >> 11) & 0x1F) * f >> 8;
        int g = ((c >> 5) & 0x3F) * f >> 8;
        int b = (c & 0x1F) * f >> 8;
        return (r << 11) | (g << 5) | b;
    }

    /**
     * Rebuilds the palette bank from the base colors.
     * level 0 = pitch black, level 15 = full brightness.
     */
    public void bakePalette(short[] base, int count)
    {
        for (int l = 0; l < LEVELS; l++)
        {
            // Non linear ramp: keeps the dark end (night, fog) readable.
            int f = (l * 256) / (LEVELS - 1);
            f = (f * f) / 256;
            int o = l << 6;
            for (int i = 0; i < count; i++) pal[o + i] = (short) shade(base[i], f);
        }
    }
}
