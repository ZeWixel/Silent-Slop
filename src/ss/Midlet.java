package ss;

import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

/**
 * Entry point.  One canvas, one thread (the MIDlet's own), no timers.
 */
public final class Midlet extends MIDlet
{
    private MainCanvas canvas;

    public Midlet() {}

    protected void startApp() throws MIDletStateChangeException
    {
        if (canvas == null)
        {
            canvas = new MainCanvas();
            canvas.init(this);
        }
        Display.getDisplay(this).setCurrent(canvas);
        canvas.resume0();
    }

    protected void pauseApp()
    {
        if (canvas != null) canvas.pause0();
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException
    {
        if (canvas != null) canvas.destroy0();
    }
}
