import ss.snd.*;
/** Drains the WAV stream the way a device's player does: read to EOF. */
public class Audio {
  public static void main(String[] a) throws Exception {
    Sfx.load();
    java.io.InputStream in = Sfx.streamForTest();
    byte[] buf = new byte[4096];
    long total = 0; int n;
    long t0 = System.currentTimeMillis();
    while ((n = in.read(buf)) > 0) total += n;
    long ms = System.currentTimeMillis() - t0;
    System.out.println("stream bytes=" + total + " in " + ms + " ms (EOF reached)");
    long again = 0;
    while ((n = in.read(buf)) > 0) again += n;
    System.out.println("after EOF additional bytes=" + again);
    int declared = Sfx.headerSizeForTest();
    System.out.println("header data size=" + declared + " pcm bytes=" + (total - 44)
        + " match=" + (declared == total - 44));
  }
}
