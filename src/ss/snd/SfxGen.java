package ss.snd;

import ss.core.Core;
import ss.core.Rng;

/**
 * Synthesises the ten sound effects at start-up (8 kHz, 8 bit unsigned).
 * Total ~120 KB of RAM, generated in a few milliseconds, nothing shipped.
 */
public final class SfxGen
{
    private static Rng rnd = new Rng(0x51ce);

    private SfxGen() {}

    static byte[] buf(int ms)
    {
        int n = (Sfx.RATE * ms) / 1000;
        return new byte[n];
    }

    static void envPut(byte[] b, int i, int v, int env)
    {
        int s = (v * env) >> 10;
        if (s > 127) s = 127;
        if (s < -128) s = -128;
        b[i] = (byte) (s + 128);
    }

    /** Pocket radio static: filtered white noise with a slow AM wobble. */
    public static byte[] radioStatic()
    {
        byte[] b = buf(1200);
        int lp = 0;
        int hp = 0;
        int last = 0;
        for (int i = 0; i < b.length; i++)
        {
            int nz = rnd.pm(120);
            lp = (lp * 3 + nz) >> 2;          // low pass
            hp = nz - lp;                     // high pass -> "hiss"
            int wob = 700 + ((ss_sin(i >> 6) * 120) >> 10);
            int crackle = (rnd.range(900) == 0) ? rnd.pm(90) : 0;
            int v = (hp * wob) >> 10;
            v += crackle;
            if (v > 127) v = 127;
            if (v < -128) v = -128;
            b[i] = (byte) (v + 128);
            last = v;
        }
        return b;
    }

    static int ss_sin(int a)
    {
        return Core.sin(a * 8);
    }

    /** Item pickup: short two note pluck. */
    public static byte[] pickup()
    {
        byte[] b = buf(260);
        for (int i = 0; i < b.length; i++)
        {
            int t = i;
            int f = t < 90 ? 880 : 1320;
            int ph = (int) ((long) t * f / Sfx.RATE * 4096 / 8);
            int v = (ss_sin(ph) * 70) >> 10;
            int env = 1024 - (t * 1024) / b.length;
            envPut(b, i, v, env);
        }
        return b;
    }

    /** Handgun: noise burst plus a low thump. */
    public static byte[] shot()
    {
        byte[] b = buf(230);
        int lp = 0;
        for (int i = 0; i < b.length; i++)
        {
            int nz = rnd.pm(127);
            lp = (lp + nz) >> 1;
            int thump = (ss_sin((i * 90) & 4095) * 60) >> 10;
            int env = 1024 - (i * 1024) / b.length;
            int v = (lp * 90 + thump * 2) >> 7;
            env = (env * env) >> 10;
            envPut(b, i, v, env);
        }
        return b;
    }

    /** Melee hit: a dull wooden crack. */
    public static byte[] hit()
    {
        byte[] b = buf(130);
        int lp = 0;
        for (int i = 0; i < b.length; i++)
        {
            int nz = rnd.pm(110);
            lp = (lp * 5 + nz) >> 3;
            int thump = (ss_sin((i * 60) & 4095) * 70) >> 10;
            int env = 1024 - (i * 1024) / b.length;
            int v = (lp + thump) >> 2;
            envPut(b, i, v, (env * env) >> 10);
        }
        return b;
    }

    /** Enemy death: descending growl with a wet tail. */
    public static byte[] enemyDeath()
    {
        byte[] b = buf(620);
        int f = 320;
        int ph = 0;
        for (int i = 0; i < b.length; i++)
        {
            if ((i & 63) == 0 && f > 60) f -= 4;
            ph += (f * 4096) / Sfx.RATE;
            int nz = rnd.pm(40);
            int v = (ss_sin(ph & 4095) * 60 + nz) >> 6;
            int env = 1024 - (i * 1024) / b.length;
            envPut(b, i, v, (env * 3) >> 2);
        }
        return b;
    }

    /** Air raid siren: the Silent Hill alarm, two tones sweeping. */
    public static byte[] siren()
    {
        byte[] b = buf(2400);
        int ph = 0;
        int ph2 = 0;
        for (int i = 0; i < b.length; i++)
        {
            int sweep = (i % 1200);
            int f = 300 + (sweep < 600 ? sweep / 3 : (1200 - sweep) / 3);
            ph += (f * 4096) / Sfx.RATE;
            ph2 += ((f * 2) * 4096) / Sfx.RATE;
            int v = (ss_sin(ph & 4095) * 55 + ss_sin(ph2 & 4095) * 14) >> 10;
            int env = 1024;
            if (i < 400) env = i * 1024 / 400;
            if (i > b.length - 400) env = (b.length - i) * 1024 / 400;
            envPut(b, i, v, env);
        }
        return b;
    }

    /** Footstep on wet asphalt. */
    public static byte[] step()
    {
        byte[] b = buf(90);
        int lp = 0;
        for (int i = 0; i < b.length; i++)
        {
            int nz = rnd.pm(90);
            lp = (lp * 3 + nz) >> 2;
            int env = 1024 - (i * 1024) / b.length;
            envPut(b, i, lp, (env * env) >> 10);
        }
        return b;
    }

    /** Door creak. */
    public static byte[] door()
    {
        byte[] b = buf(700);
        int ph = 0;
        for (int i = 0; i < b.length; i++)
        {
            int f = 90 + ((i * 40) / b.length);
            ph += (f * 4096) / Sfx.RATE;
            int v = (ss_sin(ph & 4095) * 30 + rnd.pm(18)) >> 6;
            int env = 1024 - (i * 1024) / b.length;
            envPut(b, i, v, (env * 3) >> 2);
        }
        return b;
    }

    /** UI blip. */
    public static byte[] menu()
    {
        byte[] b = buf(60);
        for (int i = 0; i < b.length; i++)
        {
            int ph = (i * 1760 * 4096) / Sfx.RATE;
            int env = 1024 - (i * 1024) / b.length;
            envPut(b, i, ss_sin(ph & 4095) * 60 >> 10, env);
        }
        return b;
    }

    /** Harry hurt. */
    public static byte[] hurt()
    {
        byte[] b = buf(300);
        int f = 220;
        int ph = 0;
        for (int i = 0; i < b.length; i++)
        {
            if ((i & 127) == 0 && f > 90) f -= 10;
            ph += (f * 4096) / Sfx.RATE;
            int v = (ss_sin(ph & 4095) * 50 + rnd.pm(20)) >> 6;
            int env = 1024 - (i * 1024) / b.length;
            envPut(b, i, v, (env * 3) >> 2);
        }
        return b;
    }
}
