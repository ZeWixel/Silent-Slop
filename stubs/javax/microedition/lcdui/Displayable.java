package javax.microedition.lcdui;

/** Compile-time stub of MIDP 2.0 Displayable. */
public class Displayable
{
    protected Displayable() {}

    public void addCommand(Command c) {}
    public void removeCommand(Command c) {}
    public void setCommandListener(CommandListener l) {}
    public Display getDisplay() { return null; }
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public String getTitle() { return null; }
    public void setTitle(String s) {}
    public void setTicker(Ticker t) {}
    public void setFullScreenMode(boolean on) {}
    protected void sizeChanged(int w, int h) {}
    protected void showNotify() {}
    protected void hideNotify() {}
}
