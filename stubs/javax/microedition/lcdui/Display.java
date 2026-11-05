package javax.microedition.lcdui;

/**
 * Display.
 *
 * Declared exactly as MIDP 2.0 declares it (this is what the device provides),
 * but backed by a tiny desktop implementation: the desktop harnesses and the
 * JAR verification boot the MIDlet through the real startApp() path, which needs
 * a Display that actually holds a current Displayable.
 */
public class Display
{
    private static Display instance;
    private Displayable current;

    protected Display() {}

    public static Display getDisplay(javax.microedition.midlet.MIDlet m)
    {
        if (instance == null) instance = new Display();
        return instance;
    }

    public void setCurrent(Displayable d) { current = d; }
    public Displayable getCurrent() { return current; }
    public void callSerially(Runnable r) { r.run(); }
    public boolean isColor() { return true; }
    public int numColors() { return 65536; }
    public int numAlphaLevels() { return 256; }
    public void flashBacklight(int ms) {}
    public void vibrate(int ms) {}
}
