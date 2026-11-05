package javax.microedition.lcdui;

/** Compile-time stub of MIDP 2.0 GameCanvas. */
public abstract class GameCanvas extends Canvas
{
    public static final int UP_PRESSED    = 0x0002;
    public static final int DOWN_PRESSED  = 0x0040;
    public static final int LEFT_PRESSED  = 0x0004;
    public static final int RIGHT_PRESSED = 0x0020;
    public static final int FIRE_PRESSED  = 0x0100;
    public static final int GAME_A_PRESSED = 0x0200;
    public static final int GAME_B_PRESSED = 0x0400;
    public static final int GAME_C_PRESSED = 0x0800;
    public static final int GAME_D_PRESSED = 0x1000;

    protected GameCanvas(boolean suppressKeyEvents) {}

    public Graphics getGraphics() { return null; }
    public void flushGraphics() {}
    public void flushGraphics(int x, int y, int w, int h) {}
    protected void paint(Graphics g) {}
    public int getKeyStates() { return 0; }
}
