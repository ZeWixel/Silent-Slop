import ss.core.*; import ss.game.*; import ss.level.*;
/** Walks Harry through the prologue objectives and reports what fires. */
public class Play {
  static Game g; static Fb fb = new Fb(176,208);
  public static void main(String[] a) throws Exception {
    Game.initAssets(); g = new Game();
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    g.player.x = g.level.spawnX; g.player.z = g.level.spawnZ;
    step(1);
    System.out.println("start state=" + g.state + " msg=" + g.messageText);
    // 1) walk east toward the car / first message
    walkTo(Level.u(15), 0);
    System.out.println("after east: state=" + g.state + " msg=" + g.messageText);
    g.state = Game.S_PLAY;
    // 2) pick up the can on the south sidewalk
    walkTo(Level.u(-10), Level.u(-12.5));
    g.player.angle = Core.atan2(0, -1);
    Input in = new Input(); in.pressed[Input.A] = true; g.update(in);
    System.out.println("cans=" + g.player.healthDrinks + " msg=" + g.messageText);
    g.state = Game.S_PLAY;
    // 3) the street visual novel
    walkTo(Level.u(-30), 0);
    System.out.println("after VN trigger: state=" + g.state + " (S_NOVEL=" + Game.S_NOVEL + ")");
    // play the novel through: press 5 until it is over
    for (int i = 0; i < 400 && g.state == Game.S_NOVEL; i++) {
      Input n = new Input();
      if (i % 6 == 0) n.pressed[Input.A] = true;
      g.update(n);
    }
    System.out.println("novel done: state=" + g.state + " hasMap=" + g.player.hasMap
        + " msg=" + g.messageText);
    g.state = Game.S_PLAY;
    // 4) walk to the cafe door and press 5
    walkTo(Level.u(-12), Level.u(14));
    System.out.println("at door: x=" + g.player.x + " z=" + g.player.z
        + " hasMap=" + g.player.hasMap);
    Input d = new Input(); d.pressed[Input.A] = true; g.update(d);
    step(20);
    System.out.println("after door: level=" + g.level.name + " state=" + g.state);
    // let the fade finish, then walk in and look around
    for (int i = 0; i < 60; i++) g.update(new Input());
    System.out.println("settled: state=" + g.state + " px=" + g.player.x + " pz=" + g.player.z
        + " msg=" + g.messageText);
    g.state = Game.S_PLAY;
    g.camYaw = 0; g.draw(fb); Preview.save(fb, "/tmp/shots/play_cafe.png");
    g.camYaw = 2048; g.draw(fb); Preview.save(fb, "/tmp/shots/play_cafe2.png");
  }
  static void walkTo(int x, int z) {
    for (int i = 0; i < 900; i++) {
      int dx = x - g.player.x, dz = z - g.player.z;
      if (Core.dist2d(dx, dz) < 40) break;
      Input in = new Input();
      int ang = Core.atan2(dx, dz);
      g.player.angle = ang;
      // simple: nudge straight toward the target, letting colliders slide
      g.player.x += Core.clamp(dx, -24, 24);
      g.player.z += Core.clamp(dz, -24, 24);
      g.update(in);
      if (g.state != Game.S_PLAY) return;
    }
    step(2);
  }
  static void step(int n) { for (int i = 0; i < n; i++) g.update(new Input()); }
}
