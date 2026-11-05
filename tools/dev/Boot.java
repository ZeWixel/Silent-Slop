import ss.core.*; import ss.game.*;
/**
 * Boots the MIDlet the way a phone does: constructor -> startApp() -> running.
 * Covers both a handset that reports its screen immediately and one that has
 * not measured the display yet (width/height still 0).
 */
public class Boot {
  public static void main(String[] a) throws Exception {
    javax.microedition.lcdui.Canvas.setScreenSizeForTest(0, 0);
    javax.microedition.midlet.MIDlet m = new ss.Midlet();
    System.out.println("midlet constructed");
    java.lang.reflect.Method start = ss.Midlet.class.getDeclaredMethod("startApp", (Class[]) null);
    start.setAccessible(true);
    start.invoke(m, (Object[]) null);
    System.out.println("startApp ok with an unmeasured screen");
    Thread.sleep(600);
    java.lang.reflect.Field cf = ss.Midlet.class.getDeclaredField("canvas");
    cf.setAccessible(true);
    Object canvas = cf.get(m);
    java.lang.reflect.Field f = ss.MainCanvas.class.getDeclaredField("game");
    f.setAccessible(true);
    Object game = f.get(canvas);
    System.out.println("canvas after frames: " + (canvas == null ? "NULL" : "live")
        + ", game: " + (game == null ? "NULL" : "live"));

    // now a real screen size and a restart, like a rotating device
    javax.microedition.lcdui.Canvas.setScreenSizeForTest(240, 320);
    java.lang.reflect.Method pause = ss.Midlet.class.getDeclaredMethod("pauseApp", (Class[]) null);
    pause.setAccessible(true);
    pause.invoke(m, (Object[]) null);
    start.invoke(m, (Object[]) null);
    Thread.sleep(400);
    System.out.println("restart on 240x320 ok");
  }
}
