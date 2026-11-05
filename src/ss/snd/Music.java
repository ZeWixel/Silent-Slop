package ss.snd;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.microedition.media.Manager;
import javax.microedition.media.Player;

/**
 * MIDI music.
 *
 * J2ME players are limited (S40 usually allows one MIDI player plus a few
 * tone sequences), so the engine keeps a single music channel and cross-fades
 * by stopping the old track - that is also what the "Sony Ericsson" sound
 * architecture expects.  Tracks are plain SMF type 0/1 streams from the JAR.
 */
public final class Music
{
    public static final int NONE = -1;
    public static final int THEME = 0;
    public static final int CAFE = 1;
    public static final int CHASE = 2;
    public static final int SIREN = 3;

    private static final String[] FILES = {
        "/m/t_theme.mid",
        "/m/t_cafe.mid",
        "/m/t_chase.mid",
        "/m/t_siren.mid"
    };

    private static Player player;
    private static int current = NONE;
    private static int pending = NONE;
    private static int pendingDelay;
    private static boolean enabled = true;

    private Music() {}

    public static void setEnabled(boolean on)
    {
        enabled = on;
        if (!on) stop();
    }

    public static boolean isEnabled()
    {
        return enabled;
    }

    public static int currentTrack()
    {
        return current;
    }

    /** Starts a track (no-op when it is already playing). */
    public static void play(int track)
    {
        if (!enabled) { current = track; return; }
        if (track == current && player != null) return;
        stop();
        current = track;
        if (track < 0) return;
        try
        {
            // The tracks are plain MIDI so the device's own synth plays them:
            // an SE phone has ~700 kB of heap for the whole MIDlet, so a
            // sampled soundtrack is not an option.
            java.io.InputStream in = Music.class.getResourceAsStream(FILES[track]);
            if (in == null) { current = NONE; return; }
            Player p = Manager.createPlayer(in, "audio/midi");
            p.realize();
            p.prefetch();
            p.setLoopCount(-1);
            p.start();
            player = p;
        }
        catch (Throwable e)
        {
            // No MIDI support or out of players: the game must not care.
            player = null;
        }
    }

    /** Fades are expensive on these devices; we just duck and restart later. */
    public static void queue(int track, int frames)
    {
        pending = track;
        pendingDelay = frames;
    }

    public static void update()
    {
        if (pending != NONE)
        {
            if (--pendingDelay <= 0)
            {
                int t = pending;
                pending = NONE;
                play(t);
            }
        }
    }

    public static void stop()
    {
        pending = NONE;
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
        current = NONE;
    }

    public static void pause()
    {
        if (player == null) return;
        try
        {
            player.stop();
        }
        catch (Throwable e) {}
    }

    public static void resume()
    {
        if (player == null || !enabled) return;
        try
        {
            player.start();
        }
        catch (Throwable e) {}
    }
}
