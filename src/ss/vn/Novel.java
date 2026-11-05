package ss.vn;

import ss.core.Core;
import ss.core.Fb;
import ss.game.Game;
import ss.game.Input;
import ss.ui.Font6;

/**
 * Cut-scenes, as a visual novel in the spirit of "Play Novel: Silent Hill" on
 * the Game Boy Advance: a painted still, a dialogue box, a typewriter reveal and
 * a character portrait.
 *
 * All the script data lives in {@link Scenes}.  The engine is deliberately
 * dumb: one still per beat, up to four lines of text, a speaker name and an
 * optional one-frame effect (shake, flash, siren).
 */
public final class Novel
{
    /** Frame counter for the non-deterministic-free background animation. */
    private int animTicks;
    public static final int INTRO = 1;
    public static final int CYBIL = 2;
    public static final int SCREAMER = 3;

    // effects
    public static final int FX_NONE = 0;
    public static final int FX_SHAKE = 1;
    public static final int FX_FLASH = 2;
    public static final int FX_SIREN = 3;

    public int id;
    public int beat;
    public int typeTimer;
    public boolean finished;
    public int shake;
    public int flash;
    public int fade;

    private String[] lines;
    private String speaker;
    private int portrait;
    private int effect;

    public void start(int id, Game g)
    {
        this.id = id;
        beat = 0;
        finished = false;
        typeTimer = 0;
        shake = 0;
        flash = 0;
        fade = 0;
        load(g);
    }

    void load(Game g)
    {
        Scenes.Beat b = Scenes.get(id, beat);
        if (b == null)
        {
            finished = true;
            return;
        }
        lines = b.lines;
        speaker = b.speaker;
        portrait = b.portrait;
        effect = b.effect;
        typeTimer = 0;
        if (effect == FX_SIREN) ss.snd.Sfx.play(ss.snd.Sfx.SIREN, 60);
        if (effect == FX_FLASH) flash = 24;
        if (effect == FX_SHAKE) shake = 18;
    }

    public void update(Input in)
    {
        int total = 0;
        if (lines != null)
        {
            for (int i = 0; i < lines.length; i++) total += lines[i].length();
        }
        boolean typing = typeTimer < total;
        if (typing) typeTimer += 2;

        if (flash > 0) flash--;
        if (shake > 0) shake--;
        if (fade < 256) fade += 24;

        boolean skipAll = in.tapped(Input.SOFT_RIGHT);
        if (skipAll)
        {
            finished = true;
            return;
        }
        if (in.tapped(Input.A) || in.tapped(Input.SOFT_LEFT))
        {
            if (typing)
            {
                typeTimer = total;      // first press: reveal the whole line
            }
            else
            {
                beat++;
                Scenes.Beat b = Scenes.get(id, beat);
                if (b == null)
                {
                    finished = true;
                }
                else
                {
                    load(null);
                }
            }
        }
    }

    // ------------------------------------------------------------------- draw
    public void draw(Fb fb)
    {
        animTicks++;
        int sx = 0, sy = 0;
        if (shake > 0)
        {
            sx = Core.sin(shake * 900) >> 7;
            sy = Core.cos(shake * 1300) >> 7;
        }

        drawStill(fb, sx, sy);

        if (flash > 0)
        {
            fb.fadeTo(0xFFFF, Core.clamp(flash * 6, 0, 120));
        }

        // ---- dialogue box
        int h = 62;
        int y0 = fb.h - h;
        fb.rect(0, y0, fb.w - 1, fb.h - 1, 0x0000);
        fb.rect(0, y0 + 1, fb.w - 1, y0 + 14, 0x0841);
        fb.hline(0, fb.w - 1, y0, 0x39E7);
        if (speaker != null)
        {
            Font6.draw(fb, speaker, 4, y0 + 4, 0xC618, 0x0000);
        }
        int n = 0;
        int i = 0;
        int yy = y0 + 18;
        int total = typeTimer;
        if (lines != null)
        {
            for (int l = 0; l < lines.length && yy < fb.h - 10; l++)
            {
                String s = lines[l];
                if (total <= 0) break;
                int len = s.length();
                if (len > total) len = total;
                // wrap to the box: a page of prose must never run off the edge
                int used = Font6.drawWrapped(fb, s.substring(0, len), 4, yy,
                                             fb.w - 8, 0xE71C, 0x0000, 3);
                total -= s.length();
                yy += used * 10;
                n++;
            }
        }
        if ((typeTimer / 8) % 2 == 0)
        {
            Font6.draw(fb, "5", fb.w - 8, fb.h - 9, 0x8410, 0x0000);
        }
    }

    /**
     * The "CG" of each beat, drawn procedurally: silhouettes, fog bands and a
     * couple of shapes.  Cheap enough to repaint every frame (no images, no
     * allocation), and it reads at 128x128 as well as at 240x320.
     */
    void drawStill(Fb fb, int sx, int sy)
    {
        int W = fb.w, H = fb.h;
        int kind = portrait;         // reuse the portrait id as a scene id
        int horizon = H * 55 / 100;

        // sky / fog gradient
        for (int y = 0; y < H; y++)
        {
            int c;
            if (y < horizon)
            {
                c = Core.mix565(0x2104, 0x4A55, (y * 256) / (horizon == 0 ? 1 : horizon));
            }
            else
            {
                c = Core.mix565(0x4A55, 0x18C3, ((y - horizon) * 256) / (H - horizon + 1));
            }
            fb.hline(0, W - 1, y, c);
        }

        switch (kind)
        {
            case 1: // crashed car on a wet road, seen from the side
                roadScene(fb, sx, sy, horizon);
                break;
            case 2: // Cheryl walking into the fog
                figureScene(fb, sx, sy, horizon, 0);
                break;
            case 3: // the alley, darkness closing in
                alleyScene(fb, sx, sy, horizon);
                break;
            case 4: // cafe interior: counter, warm light
                cafeScene(fb, sx, sy, horizon);
                break;
            case 5: // Cybil with her flashlight
                figureScene(fb, sx, sy, horizon, 1);
                break;
            case 6: // the radio on the counter
                radioScene(fb, sx, sy, horizon);
                break;
            case 7: // the air screamer breaking through the window
                screamScene(fb, sx, sy, horizon);
                break;
            default:
                roadScene(fb, sx, sy, horizon);
                break;
        }

        // fog bands
        for (int i = 0; i < 3; i++)
        {
            int yy = horizon - 10 + i * 7 + (Core.sin(animTicks * 6 + i * 300) >> 9);
            int a = 40 - i * 10;
            for (int y = yy - 4; y < yy + 4; y++) fb.hline(0, W - 1, y + sy, Core.mix565(0x4A55, 0x630C, a));
        }

        if (fade < 256)
        {
            fb.fadeTo(0x0000, 256 - fade);
        }
    }

    void roadScene(Fb fb, int sx, int sy, int hz)
    {
        int W = fb.w;
        fb.rect(0, hz + sy, W - 1, fb.h - 1, 0x2124);
        // road
        for (int y = hz; y < fb.h; y++)
        {
            int k = (y - hz);
            fb.hline(0, W - 1, y + sy, Core.mix565(0x2124, 0x10A2, k));
        }
        // buildings
        rect3(fb, 4 + sx, hz - 40 + sy, 40, 40, 0x18E3);
        rect3(fb, 44 + sx, hz - 30 + sy, 30, 30, 0x10A2);
        rect3(fb, W - 50 + sx, hz - 46 + sy, 46, 46, 0x18E3);
        // the car, a dark wedge
        int cx = W / 2 - 26 + sx;
        int cy = hz + 6 + sy;
        fb.rect(cx, cy, cx + 52, cy + 18, 0x0861);
        fb.rect(cx + 10, cy - 10, cx + 40, cy, 0x0861);
        fb.rect(cx + 14, cy - 7, cx + 36, cy - 2, 0x2104);
        fb.rect(cx - 4, cy + 14, cx + 56, cy + 20, 0x0000);
        // headlights in the fog
        fb.rect(cx - 10, cy + 6, cx - 2, cy + 10, 0xCE79);
    }

    void figureScene(Fb fb, int sx, int sy, int hz, int cop)
    {
        int W = fb.w;
        fb.rect(0, hz + sy, W - 1, fb.h - 1, 0x18C3);
        int fx = W / 2 + sx;
        int fy = hz + sy;
        int body = cop != 0 ? 0x18E3 : 0x2104;
        // silhouette of a person walking away
        fb.rect(fx - 5, fy - 34, fx + 5, fy - 6, body);
        fb.rect(fx - 4, fy - 44, fx + 4, fy - 34, 0x2C43);
        fb.rect(fx - 4, fy - 6, fx - 1, fy + 10, body);
        fb.rect(fx + 1, fy - 6, fx + 4, fy + 10, body);
        // long shadow toward the camera
        for (int i = 0; i < 26; i++)
        {
            int y = fy + 10 + i;
            int w = 5 + i / 2;
            fb.hline(fx - w, fx + w, y + sy, Core.mix565(0x18C3, 0x0861, (i * 8)));
        }
    }

    void alleyScene(Fb fb, int sx, int sy, int hz)
    {
        int W = fb.w;
        fb.rect(0, 0, W - 1, fb.h - 1, 0x0861);
        // perspective walls
        for (int i = 0; i < 24; i++)
        {
            int k = 24 - i;
            fb.rect(i * 3 + sx, hz - k * 2 + sy, W - 1 - i * 3 + sx, fb.h - 1 - i * 2, 0x10A2 + (i / 8));
        }
        fb.rect(W / 2 - 8, hz + sy, W / 2 + 8, fb.h - 1, 0x0421);
    }

    void cafeScene(Fb fb, int sx, int sy, int hz)
    {
        int W = fb.w;
        fb.rect(0, hz + sy, W - 1, fb.h - 1, 0x2945);
        // warm interior
        fb.rect(0, 0, W - 1, fb.h - 1, 0x18A2);
        fb.rect(0, hz + sy, W - 1, fb.h - 1, 0x2104);
        // counter
        fb.rect(W / 6 + sx, hz - 6 + sy, W - W / 6 + sx, hz + 14 + sy, 0x39A6);
        fb.rect(W / 6 + sx, hz - 10 + sy, W - W / 6 + sx, hz - 6 + sy, 0x4E69);
        // hanging lamp
        fb.hline(W / 2 + sx, W / 2 + sx, 0, 0x2104);
        fb.rect(W / 2 - 8 + sx, 8 + sy, W / 2 + 8 + sx, 14 + sy, 0x6B4D);
        for (int y = 14; y < hz + 10; y++)
        {
            int w = 8 + (y - 14) / 2;
            int a = 60 - (y - 14);
            if (a > 0) fb.hline(W / 2 - w + sx, W / 2 + w + sx, y + sy, Core.mix565(0x2104, 0xEF5D, a));
        }
    }

    void radioScene(Fb fb, int sx, int sy, int hz)
    {
        cafeScene(fb, sx, sy, hz);
        int cx = fb.w / 2 + sx;
        int cy = hz + sy;
        fb.rect(cx - 14, cy - 10, cx + 14, cy + 10, 0x3186);
        fb.rect(cx - 10, cy - 7, cx + 10, cy - 1, 0x630C);
        fb.rect(cx - 8, cy + 2, cx + 8, cy + 7, 0x5186);
        // static sparkles
        for (int i = 0; i < 12; i++)
        {
            int x = cx - 9 + ((i * 37) % 18);
            int y = cy - 6 + ((i * 13) % 5);
            fb.hline(x, x + 1, y, 0xFFFF);
        }
    }

    void screamScene(Fb fb, int sx, int sy, int hz)
    {
        int W = fb.w;
        cafeScene(fb, sx, sy, hz);
        int cx = W / 2 + sx;
        int cy = hz - 10 + sy;
        // shattering glass
        for (int i = 0; i < 22; i++)
        {
            int x = cx - 40 + ((i * 53) % 80);
            int y = cy - 30 + ((i * 29) % 40);
            fb.hline(x, x + 3, y, 0xCE79);
            fb.hline(x, x, y + 1, 0x9CF3);
        }
        // the screamer silhouette
        fb.rect(cx - 18, cy - 26, cx + 18, cy + 4, 0x0841);
        fb.rect(cx - 8, cy - 38, cx + 8, cy - 26, 0x1082);
        fb.hline(cx - 26, cx - 18, cy - 20, 0x0841);
        fb.hline(cx + 18, cx + 26, cy - 20, 0x0841);
    }

    static void rect3(Fb fb, int x, int y, int w, int h, int c)
    {
        fb.rect(x, y, x + w, y + h, c);
    }
}
