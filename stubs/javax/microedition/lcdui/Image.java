package javax.microedition.lcdui;

import java.io.IOException;
import java.io.InputStream;

/** Compile-time stub of MIDP 2.0 Image. */
public class Image
{
    protected Image() {}

    public static Image createImage(int w, int h) { return null; }
    public static Image createImage(String name) throws IOException { return null; }
    public static Image createImage(byte[] data, int off, int len) { return null; }
    public static Image createImage(Image src, int x, int y, int w, int h, int t) { return null; }
    public static Image createImage(InputStream is) throws IOException { return null; }
    public static Image createRGBImage(int[] rgb, int w, int h, boolean alpha) { return null; }

    public Graphics getGraphics() { return null; }
    public int getWidth() { return 0; }
    public int getHeight() { return 0; }
    public void getRGB(int[] rgb, int off, int scan, int x, int y, int w, int h) {}
    public boolean isMutable() { return true; }
}
