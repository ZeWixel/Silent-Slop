package ss.core;

/**
 * Core math and constants for Silent-Slop.
 *
 * All rendering math is integer / fixed point: CLDC 1.0 has no FPU on the
 * target hardware and `Math.sin` is not even part of the profile.
 *
 * Angle unit: 4096 units == 360 degrees (same convention as Mascot Capsule).
 */
public final class Core
{
    /** Angle units per full circle. */
    public static final int ANG = 4096;

    /** Screen/world fixed-point scale used by the renderer (1 unit == 64). */
    public static final int WS = 64;
    public static final int WS_BITS = 6;

    /** Fixed point unit for texture coordinates (1 texel == 64). */
    public static final int TS = 64;

    private static int[] SIN = new int[ANG];
    private static int[] ATAN = null;
    private static Rng rng = new Rng(0x5eed1234);

    static
    {
        // Quarter of the circle, mirrored for the rest: 1025 entries (+1 guard).
        for (int i = 0; i <= ANG / 4; i++)
        {
            double a = (i * 6.283185307179586) / ANG;
            SIN[i] = (int) (Math.round(sinApprox(a) * 4096.0));
        }
    }

    /** Uses the tiny Taylor series - only runs once, at class init, on the host JVM. */
    private static double sinApprox(double a)
    {
        // sin via its Taylor series is accurate enough, but Math is available in
        // CLDC only for abs/min/max; keeping the init independent of it.
        double x = a;
        if (x > 3.14159265358979) x -= 6.28318530717959;
        double x2 = x * x;
        double t = x;
        double s = x;
        for (int n = 1; n < 9; n++)
        {
            t *= -x2 / ((2 * n) * (2 * n + 1));
            s += t;
        }
        return s;
    }

    private Core() {}

    /** Sine, angle in 4096 units, result in 4096 units. */
    public static int sin(int a)
    {
        a &= ANG - 1;
        if (a < ANG / 4) return SIN[a];
        if (a < ANG / 2) return SIN[ANG / 2 - a];
        if (a < ANG * 3 / 4) return -SIN[a - ANG / 2];
        return -SIN[ANG - a];
    }

    /** Cosine, angle in 4096 units, result in 4096 units. */
    public static int cos(int a)
    {
        return sin(a + ANG / 4);
    }

    public static int abs(int v)
    {
        return v < 0 ? -v : v;
    }

    public static int clamp(int v, int lo, int hi)
    {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** Integer square root (no Math.sqrt in CLDC 1.0). */
    public static int isqrt(int v)
    {
        if (v <= 0) return 0;
        int r = v;
        int b = (v >> 1) + 1;
        while (b < r)
        {
            r = b;
            b = (v / b + b) >> 1;
        }
        return r;
    }

    /** Distance in the XZ plane, world units. */
    public static int dist2d(int dx, int dz)
    {
        return isqrt(dx * dx + dz * dz);
    }

    public static int sign(int v)
    {
        return v > 0 ? 1 : (v < 0 ? -1 : 0);
    }

    /** atan2 in 4096-unit angle, uses a small table cooked on first use. */
    public static int atan2(int y, int x)
    {
        if (ATAN == null) buildAtan();
        if (x == 0 && y == 0) return 0;
        int ax = abs(x);
        int ay = abs(y);
        int idx;
        int v;
        if (ax >= ay)
        {
            idx = (ay * 256) / ax;
            v = ATAN[idx];
        }
        else
        {
            idx = (ax * 256) / ay;
            v = 1024 - ATAN[idx];
        }
        if (x < 0) v = 2048 - v;
        if (y < 0) v = -v;
        return v & (ANG - 1);
    }

    private static void buildAtan()
    {
        // atan(t) for t in [0,1] -> angle in 1024 units per 90 degrees.
        ATAN = new int[257];
        for (int i = 0; i <= 256; i++)
        {
            double t = i / 256.0;
            double x = t;
            boolean inv = false;
            if (x > 0.4142135623730951)
            {
                inv = true;
                x = 1.0 / x;
            }
            double x2 = x * x;
            double term = x;
            double sum = x;
            for (int n = 1; n < 26; n++)
            {
                term *= -x2;
                sum += term / (2 * n + 1);
            }
            if (inv) sum = 1.5707963267948966 - sum;
            ATAN[i] = (int) Math.round(sum * (1024.0 / 1.5707963267948966));
        }
    }

    public static Rng rng()
    {
        return rng;
    }

    /** Blend two RGB565 values. f in 0..256. */
    public static int mix565(int a, int b, int f)
    {
        int ar = (a >> 11) & 0x1F;
        int ag = (a >> 5) & 0x3F;
        int ab = a & 0x1F;
        int br = (b >> 11) & 0x1F;
        int bg = (b >> 5) & 0x3F;
        int bb = b & 0x1F;
        int r = ar + (((br - ar) * f) >> 8);
        int g = ag + (((bg - ag) * f) >> 8);
        int bl = ab + (((bb - ab) * f) >> 8);
        return (r << 11) | (g << 5) | bl;
    }

    public static int rgb565(int r, int g, int b)
    {
        return ((r & 0xF8) << 8) | ((g & 0xFC) << 3) | ((b & 0xF8) >> 3);
    }

    public static int r565(int c) { return (c >> 8) & 0xF8; }
    public static int g565(int c) { return (c >> 3) & 0xFC; }
    public static int b565(int c) { return (c << 3) & 0xF8; }
}
