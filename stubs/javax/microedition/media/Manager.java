package javax.microedition.media;

import java.io.InputStream;

/** Compile-time stub of JSR-135 Manager. */
public class Manager
{
    protected Manager() {}

    public static Player createPlayer(String locator) throws java.io.IOException, MediaException { return null; }
    public static Player createPlayer(InputStream stream, String type) throws java.io.IOException, MediaException { return null; }
    public static void playTone(int note, int duration, int volume) throws MediaException {}
}
