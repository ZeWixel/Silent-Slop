import ss.core.*; import ss.game.*; import ss.level.*;
public class Modes {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.CAFE); g.state = Game.S_PLAY;
    g.player.x = Level.u(-4); g.player.z = Level.u(152);
    g.update(new Input());
    g.objMode = Render3D.MODE_TEX;  g.draw(fb); Preview.save(fb, "/tmp/shots/mode_tex.png");
    g.objMode = Render3D.MODE_FLAT; g.draw(fb); Preview.save(fb, "/tmp/shots/mode_flat.png");
    System.out.println("ok");
  }
}
