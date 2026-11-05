package javax.microedition.lcdui;

/** Compile-time stub of MIDP 2.0 Display. */
public class Display
{
    protected Display() {}

    public static Display getDisplay(javax.microedition.midlet.MIDlet m) { return null; }
    public void setCurrent(Displayable d) {}
    public Displayable getCurrent() { return null; }
    public void callSerially(Runnable r) {}
    public boolean isColor() { return true; }
    public int numColors() { return 65536; }
    public int numAlphaLevels() { return 256; }
    public void flashBacklight(int ms) {}
    public void vibrate(int ms) {}
}
