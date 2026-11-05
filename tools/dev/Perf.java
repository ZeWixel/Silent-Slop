import ss.core.*; import ss.game.*; import ss.level.*;
public class Perf {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    g.player.x = Level.u(-6); g.player.z = 0;
    for (int i=0;i<20;i++) g.update(new Input());
    // warm up
    for (int i=0;i<200;i++) g.draw(fb);
    long t0 = System.nanoTime();
    int N = 600;
    for (int i=0;i<N;i++) { g.camYaw = (i*137) & 4095; g.draw(fb); }
    long dt = System.nanoTime()-t0;
    System.out.println("prologue ms/frame = " + (dt/1000000.0/N));

    g.loadLevel(Levels.CAFE); g.state = Game.S_PLAY;
    for (int i=0;i<20;i++) g.update(new Input());
    for (int i=0;i<200;i++) g.draw(fb);
    t0 = System.nanoTime();
    for (int i=0;i<N;i++) { g.camYaw = (i*137) & 4095; g.draw(fb); }
    dt = System.nanoTime()-t0;
    System.out.println("cafe ms/frame = " + (dt/1000000.0/N));
  }
}
