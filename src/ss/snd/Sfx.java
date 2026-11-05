package ss.snd;

import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/**
 * Sound effects: a tiny software mixer that plays 8 kHz unsigned PCM through a
 * single JSR-135 player.  One player, one stream, mixed by hand - the only way
 * to get more than one sound at a time on a Sony Ericsson handset.
 *
 * The samples are synthesised at start-up by {@link SfxGen} so the JAR stays
 * small and no PCM resource has to be shipped.
 */
public final class Sfx
{
    public static final int RADIO = 0;      // pocket radio static
    public static final int PICKUP = 1;     // item pickup
    public static final int SHOT = 2;       // handgun
    public static final int HIT = 3;        // melee hit
    public static final int ENEMY_DIE = 4;  // enemy death
    public static final int SIREN = 5;      // air raid siren
    public static final int STEP = 6;       // footstep
    public static final int DOOR = 7;       // door creak
    public static final int MENU = 8;       // ui blip
    public static final int HURT = 9;       // Harry takes damage
    public static final int COUNT = 10;

    public static final int RATE = 8000;

    static byte[][] data = new byte[COUNT][];
    static int[] vol = new int[8];
    static int[] pos = new int[8];
    static int[] id = new int[8];
    static boolean[] active = new boolean[8];

    static Player player;
    static boolean enabled = true;
    static boolean audioAvailable = true;
    static byte[] mixBuf = new byte[1600];
    static Thread thread;
    static volatile boolean running;

    private Sfx() {}

    public static void load()
    {
        data[RADIO] = SfxGen.radioStatic();
        data[PICKUP] = SfxGen.pickup();
        data[SHOT] = SfxGen.shot();
        data[HIT] = SfxGen.hit();
        data[ENEMY_DIE] = SfxGen.enemyDeath();
        data[SIREN] = SfxGen.siren();
        data[STEP] = SfxGen.step();
        data[DOOR] = SfxGen.door();
        data[MENU] = SfxGen.menu();
        data[HURT] = SfxGen.hurt();
    }

    public static void setEnabled(boolean on)
    {
        enabled = on;
        if (!on)
        {
            for (int i = 0; i < active.length; i++) active[i] = false;
        }
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    /** Plays a sound on the first free channel. */
    public static void play(int sound)
    {
        play(sound, 100);
    }

    public static void play(int sound, int volume)
    {
        if (!enabled || !audioAvailable || sound < 0 || sound >= COUNT) return;
        if (data[sound] == null) return;
        for (int i = 0; i < active.length; i++)
        {
            if (!active[i])
            {
                active[i] = true;
                id[i] = sound;
                pos[i] = 0;
                vol[i] = volume;
                start();
                return;
            }
        }
        // all channels busy: steal the oldest
        int oldest = 0, best = -1;
        for (int i = 0; i < active.length; i++)
        {
            if (pos[i] > best) { best = pos[i]; oldest = i; }
        }
        id[oldest] = sound;
        pos[oldest] = 0;
        vol[oldest] = volume;
    }

    /** Continuous looped sound, used for the radio static. */
    static boolean radioOn;

    public static void radioStatic(boolean on, int volume)
    {
        if (on == radioOn) { if (on) vol[7] = volume; return; }
        radioOn = on;
        if (on)
        {
            active[7] = true;
            id[7] = RADIO;
            pos[7] = 0;
            vol[7] = volume;
            start();
        }
        else
        {
            active[7] = false;
        }
    }

    static void start()
    {
        if (!audioAvailable) return;
        if (player != null) return;
        try
        {
            javax.microedition.media.Player p =
                Manager.createPlayer(new PcmStream(), "audio/x-wav");
            p.realize();
            p.prefetch();
            p.start();
            player = p;
        }
        catch (Throwable e)
        {
            audioAvailable = false;
            player = null;
        }
    }

    public static void stopAll()
    {
        for (int i = 0; i < active.length; i++) active[i] = false;
        radioOn = false;
    }

    public static void release()
    {
        stopAll();
        if (player != null)
        {
            try
            {
                player.stop();
                player.deallocate();
                player.close();
            }
            catch (Throwable e) {}
            player = null;
        }
    }

    /** Fills the next 20 ms of mixed audio. */
    public static int mix(byte[] out, int len)
    {
        int n = len < mixBuf.length ? len : mixBuf.length;
        boolean any = false;
        for (int i = 0; i < active.length; i++)
        {
            if (active[i]) { any = true; break; }
        }
        if (!any)
        {
            for (int i = 0; i < n; i++) out[i] = (byte) 0x80;
            return n;
        }
        int[] acc = accBuf;
        for (int i = 0; i < n; i++) acc[i] = 0;
        for (int ch = 0; ch < active.length; ch++)
        {
            if (!active[ch]) continue;
            byte[] d = data[id[ch]];
            if (d == null) { active[ch] = false; continue; }
            int p = pos[ch];
            int v = vol[ch];
            boolean loop = (ch == 7 && radioOn);
            for (int i = 0; i < n; i++)
            {
                if (p >= d.length)
                {
                    if (loop) p = 0;
                    else { active[ch] = false; break; }
                }
                acc[i] += ((d[p++] & 0xFF) - 128) * v / 100;
            }
            pos[ch] = p;
        }
        for (int i = 0; i < n; i++)
        {
            int s = acc[i];
            if (s > 127) s = 127;
            if (s < -128) s = -128;
            out[i] = (byte) (s + 128);
        }
        return n;
    }

    static final int[] accBuf = new int[1600];

    /** The endless WAV stream handed to JSR-135. */
    static final class PcmStream extends java.io.InputStream
    {
        int state = 0;
        int offset = 0;
        byte[] buf = new byte[1600];

        public int read() throws IOException
        {
            if (buf == null) return -1;
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n <= 0 ? -1 : (one[0] & 0xFF);
        }

        public int read(byte[] b, int off, int len) throws IOException
        {
            if (state == 0)
            {
                byte[] h = header();
                int n = Math.min(len, h.length - offset);
                System.arraycopy(h, offset, b, off, n);
                offset += n;
                if (offset >= h.length) { state = 1; offset = 0; }
                return n;
            }
            int n = mix(b, len);
            if (n <= 0) return -1;
            return n;
        }

        public int available()
        {
            return 1600;
        }

        public void close() {}
    }

    /** 44 byte canonical mono 8 kHz 8 bit PCM WAV header = 0x7FFFFFFF samples. */
    static byte[] header()
    {
        int size = 0x7FFFFF00;
        byte[] h = new byte[44];
        h[0] = 'R'; h[1] = 'I'; h[2] = 'F'; h[3] = 'F';
        le32(h, 4, size);
        h[8] = 'W'; h[9] = 'A'; h[10] = 'V'; h[11] = 'E';
        h[12] = 'f'; h[13] = 'm'; h[14] = 't'; h[15] = ' ';
        le32(h, 16, 16);
        h[20] = 1; h[21] = 0;          // PCM
        h[22] = 1; h[23] = 0;          // mono
        le32(h, 24, RATE);
        le32(h, 28, RATE);             // byte rate
        h[32] = 1; h[33] = 0;          // block align
        h[34] = 8; h[35] = 0;          // bits
        h[36] = 'd'; h[37] = 'a'; h[38] = 't'; h[39] = 'a';
        le32(h, 40, size);
        return h;
    }

    static void le32(byte[] b, int o, int v)
    {
        b[o] = (byte) v;
        b[o + 1] = (byte) (v >> 8);
        b[o + 2] = (byte) (v >> 16);
        b[o + 3] = (byte) (v >> 24);
    }
}
