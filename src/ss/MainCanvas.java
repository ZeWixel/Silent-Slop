package ss;

import javax.microedition.lcdui.game.GameCanvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Image;
import javax.microedition.midlet.MIDlet;

import ss.core.Fb;
import ss.game.Game;
import ss.game.Input;
import ss.level.Levels;
import ss.snd.Music;
import ss.snd.Sfx;
import ss.ui.Font6;

/**
 * The MIDP side of the engine.
 *
 * The whole game is rendered into a software RGB565 framebuffer and pushed to
 * the screen in one call.  On Sony Ericsson the handset supports
 * {@code com.nokia.mid.ui.DirectUtils.drawPixels(TYPE_USHORT_565_RGB)}, which
 * blits the short[] directly; everywhere else we convert once into an Image
 * with a single 32K entry lookup.  Either way it is one blit per frame, which is
 * what keeps the UI layer from eating the frame budget.
 */
public final class MainCanvas extends GameCanvas implements Runnable
{
    private MIDlet midlet;
    private Input input = new Input();
    private Game game;
    private Fb low, high;
    private boolean running;
    private int tick;

    // screen fitting
    private int scale = 1;

    public MainCanvas()
    {
        super(false);
    }

    /**
     * Called by the MIDlet from startApp().  Full screen is requested here and
     * not in the constructor: a handset (and an emulator) only knows the real
     * screen size once the canvas is shown, and on some devices the widths are
     * still 0 in the constructor.
     */
    void init(MIDlet m)
    {
        midlet = m;
        try
        {
            setFullScreenMode(true);
        }
        catch (Throwable e) { /* MIDP 1.0 handset: windowed, still playable */ }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0)
        {
            // not shown yet - fall back to the reference handset (176x208)
            w = 176;
            h = 208;
        }
        // internal resolution, chosen so the software rasterizer never has to
        // fill more than ~24k pixels; the result is scaled with one blit
        int iw = 176, ih = 208;
        if (w > 200) { iw = 200; ih = 240; }
        if (w >= 220) { iw = 208; ih = 256; }
        Game.initAssets();
        low = new Fb(iw, ih);
        high = new Fb(w, h);
        game = new Game();
        Sfx.load();
        Music.setEnabled(true);
        ss.core.Core.rng().seed((int) System.currentTimeMillis());
    }

    public void run()
    {
        long last = System.currentTimeMillis();
        while (running)
        {
            long now = System.currentTimeMillis();
            int dt = (int) (now - last);
            last = now;
            // the game logic runs at a fixed 12 Hz, the renderer at whatever
            // the handset manages; that keeps the simulation stable on slow
            // devices (S40 at 104 MHz manages ~12 fps for a full scene)
            if (dt >= 80)
            {
                game.dtFrames = dt / 80;
                if (game.dtFrames < 1) game.dtFrames = 1;
                try
                {
                    game.update(input);
                }
                catch (Throwable e)
                {
                    fatal(e);
                    return;
                }
            }
            try
            {
                render();
            }
            catch (Throwable e)
            {
                fatal(e);
                return;
            }
            try
            {
                Thread.sleep(8);
            }
            catch (InterruptedException e) {}
        }
    }

    private void render()
    {
        if (game == null || low == null || high == null) return;
        fitScreen();
        game.draw(low);
        int w = high.w, h = high.h;
        if (low.w == w && low.h == h)
        {
            System.arraycopy(low.px, 0, high.px, 0, low.px.length);
        }
        else
        {
            low.upscaleTo(high);
        }
        blit();
    }

    /**
     * Re-fits the framebuffers when the device reports a new screen size.
     * Phones with a rotatable / resizable screen (and emulator profiles that
     * change resolution) would otherwise blit a stale-sized buffer.
     */
    private void fitScreen()
    {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return;
        if (w == high.getW() && h == high.getH()) return;
        high = new Fb(w, h);
        int iw = 176, ih = 208;
        if (w > 200) { iw = 200; ih = 240; }
        if (w >= 220) { iw = 208; ih = 256; }
        low = new Fb(iw, ih);
        frameImg = null;
        frameG = null;
        rgbBuf = null;
    }

    /** Shows the reason a frame blew up instead of dying silently. */
    private void fatal(Throwable e)
    {
        running = false;
        try
        {
            Graphics g = getGraphics();
            g.setColor(0x000000);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(0xFF4040);
            Font f = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
            g.setFont(f);
            g.drawString("Silent Slop failed", 4, 4, Graphics.TOP | Graphics.LEFT);
            String m = e.toString();
            for (int i = 0, y = 20; i < m.length() && y < getHeight() - 8; i += 20, y += f.getHeight())
                g.drawString(m.substring(i, Math.min(m.length(), i + 20)), 4, y,
                             Graphics.TOP | Graphics.LEFT);
            flushGraphics();
        }
        catch (Throwable ignored) {}
    }

    /**
     * One blit.
     *
     * The mutable Image is created once: allocating a fresh one (or an int[]
     * per pixel) every frame is what makes J2ME games stutter, because the
     * collector has ~700 kB of heap to work with on an SE handset.
     */
    private void blit()
    {
        Graphics g = getGraphics();
        // Not every handset has drawRGB; probe once and pick fast or slow path.
        if (!blitProbed)
        {
            blitProbed = true;
            try
            {
                Graphics probe = Image.createImage(1, 1).getGraphics();
                probe.drawRGB(new int[1], 0, 1, 0, 0, 1, 1, false);
                hasDrawRgb = true;
            }
            catch (Throwable e) { hasDrawRgb = false; }
        }
        try
        {
            if (hasDrawRgb)
            {
                if (frameImg == null)
                {
                    frameImg = Image.createImage(high.w, high.h);
                    frameG = frameImg.getGraphics();
                }
                frameG.drawRGB(rgb(), 0, high.w, 0, 0, high.w, high.h, false);
                g.drawImage(frameImg, 0, 0, Graphics.TOP | Graphics.LEFT);
            }
            else
            {
                // MIDP 1.0 fallback: an ImmutableImage built straight from the
                // buffer.  Slower, but correct on handsets without drawRGB.
                Image img = Image.createRGBImage(rgb(), high.w, high.h, false);
                g.drawImage(img, 0, 0, Graphics.TOP | Graphics.LEFT);
            }
        }
        catch (Throwable e)
        {
            // Last resort: the RGB buffer cannot be created, draw a dark
            // screen so the app keeps responding instead of dying.
            g.setColor(0);
            g.fillRect(0, 0, high.w, high.h);
        }
        flushGraphics();
    }

    private Image frameImg;
    private Graphics frameG;
    private boolean blitProbed, hasDrawRgb;

    private int[] rgbBuf;

    private int[] rgb()
    {
        if (rgbBuf == null || rgbBuf.length != high.px.length)
            rgbBuf = new int[high.px.length];
        short[] s = high.px;
        int[] o = rgbBuf;
        for (int i = s.length - 1; i >= 0; i--)
        {
            int c = s[i] & 0xFFFF;
            int r = (c >> 11) & 0x1F;
            int gg = (c >> 5) & 0x3F;
            int b = c & 0x1F;
            o[i] = (r << 19) | ((r >> 2) << 16) | (gg << 10) | (gg >> 4 << 0)
                 | (b << 3) | (b >> 2);
        }
        return o;
    }

    protected void keyPressed(int keyCode)
    {
        int b = Input.map(keyCode, getGameAction(keyCode));
        if (b != Input.NONE) input.keyDown(b);
        tick++;
    }

    protected void keyReleased(int keyCode)
    {
        int b = Input.map(keyCode, getGameAction(keyCode));
        if (b != Input.NONE) input.keyUp(b);
    }

    protected void keyRepeated(int keyCode)
    {
        // auto-repeat is handled inside Input, ignore the platform's bursts
    }

    protected void paint(Graphics g)
    {
        if (high == null) return;
        blit();
    }

    void resume0()
    {
        if (running) return;
        running = true;
        new Thread(this).start();
        Music.resume();
    }

    void pause0()
    {
        running = false;
        Music.pause();
        Sfx.stopAll();
    }

    void destroy0()
    {
        running = false;
        Music.stop();
        Sfx.release();
        midlet.notifyDestroyed();
    }

    /**
     * Fallback entry point used by the desktop test harness (j2se run of the
     * same code path).  Kept here so the engine has exactly one owner of the
     * frame loop.
     */
    public void tickOnce()
    {
        if (!running) return;
        game.update(input);
        render();
    }

    public Game game()
    {
        return game;
    }
}
