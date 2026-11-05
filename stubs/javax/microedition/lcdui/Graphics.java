package javax.microedition.lcdui;

/**
 * Minimal compile-time stub of the MIDP 2.0 Graphics API.
 * Only the subset used by Silent-Slop is declared; the real implementation
 * is provided by the handset at runtime.
 */
public class Graphics
{
    public static final int HCENTER   = 1;
    public static final int VCENTER   = 2;
    public static final int LEFT      = 4;
    public static final int RIGHT     = 8;
    public static final int TOP       = 16;
    public static final int BOTTOM    = 32;
    public static final int BASELINE  = 64;
    public static final int SOLID     = 0;
    public static final int DOTTED    = 1;

    protected Graphics() {}

    public static Graphics getGraphics(Image img) { return null; }

    public void translate(int x, int y) {}
    public int getTranslateX() { return 0; }
    public int getTranslateY() { return 0; }
    public void setClip(int x, int y, int w, int h) {}
    public int getClipX() { return 0; }
    public int getClipY() { return 0; }
    public int getClipWidth() { return 0; }
    public int getClipHeight() { return 0; }
    public void clipRect(int x, int y, int w, int h) {}
    public void setColor(int rgb) {}
    public int getColor() { return 0; }
    public int getRedComponent() { return 0; }
    public int getGreenComponent() { return 0; }
    public int getBlueComponent() { return 0; }
    public void setGrayScale(int v) {}
    public int getGrayScale() { return 0; }
    public void setAlpha(int a) {}
    public int getAlpha() { return 255; }
    public void setStrokeStyle(int s) {}
    public int getStrokeStyle() { return 0; }
    public Font getFont() { return null; }
    public void setFont(Font f) {}
    public void fillRect(int x, int y, int w, int h) {}
    public void fillRoundRect(int x, int y, int w, int h, int rx, int ry) {}
    public void fillArc(int x, int y, int w, int h, int sa, int aa) {}
    public void drawRect(int x, int y, int w, int h) {}
    public void drawRoundRect(int x, int y, int w, int h, int rx, int ry) {}
    public void drawArc(int x, int y, int w, int h, int sa, int aa) {}
    public void drawLine(int x1, int y1, int x2, int y2) {}
    public void drawString(String s, int x, int y, int a) {}
    public void drawSubstring(String s, int o, int l, int x, int y, int a) {}
    public void drawChar(char c, int x, int y, int a) {}
    public void drawChars(char[] d, int o, int l, int x, int y, int a) {}
    public void drawImage(Image img, int x, int y, int a) {}
    public void drawRegion(Image src, int sx, int sy, int w, int h, int t, int x, int y, int a) {}
    public void copyArea(int x, int y, int w, int h, int dx, int dy, int a) {}
    public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {}
}
