import ss.core.*; import ss.game.*; import ss.level.*;
public class Light {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.CAFE); g.state = Game.S_PLAY;
    g.player.x = Level.u(-4); g.player.z = Level.u(152);
    g.player.hasFlashlight = true;
    g.update(new Input());
    g.player.flashlight = true;
    for (int i=0;i<3;i++) g.update(new Input());
    g.player.flashlight = true;
    g.draw(fb); Preview.save(fb, "/tmp/shots/light_on.png");
    g.player.flashlight = false;
    g.draw(fb); Preview.save(fb, "/tmp/shots/light_off.png");
    System.out.println("ok");
  }
}
