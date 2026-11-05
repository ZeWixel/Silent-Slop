import ss.core.*; import ss.game.*; import ss.level.*;
public class Modes2 {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.CAFE); g.state = Game.S_PLAY;
    g.player.x = Level.u(-4); g.player.z = Level.u(152);
    g.update(new Input());
    System.out.println("texcount=" + TexStore.count() + " mode=" + g.objMode
        + " MODE_FLAT=" + Render3D.MODE_FLAT);
    for (int md = 0; md < 2; md++) {
      g.objMode = md;
      g.draw(fb);
      System.out.println("mode=" + md + " tris=" + Render3D.statsTris());
    }
  }
}
