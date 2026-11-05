import ss.core.*; import ss.game.*; import ss.level.*;
public class Zoom {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    g.player.x = Level.u(-6); g.player.z = 0;
    int[] zooms = {34, 46, 60};
    for (int z = 0; z < zooms.length; z++) {
      g.fov = zooms[z];
      g.camYaw = 2048;
      g.draw(fb);
      Preview.save(fb, "/tmp/shots/zoom" + zooms[z] + ".png");
    }
    System.out.println("ok");
  }
}
