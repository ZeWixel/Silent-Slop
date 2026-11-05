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

    // 5) pick up the knife, a health drink and the flashlight in the cafe
    pickAt(Level.u(9), Level.u(158));
    pickAt(Level.u(-11), Level.u(150));
    pickAt(Level.u(10), Level.u(154));
    System.out.println("inventory: knife=" + g.player.hasKnife + " drinks=" + g.player.healthDrinks
        + " flashlight=" + g.player.hasFlashlight + " radio=" + g.player.hasRadio);
    // 6) the screamer appears once the knife and the map are in hand
    for (int i = 0; i < 200; i++) {
      if (g.state == Game.S_MESSAGE) g.state = Game.S_PLAY;
      g.update(new Input());
    }
    int alive = 0;
    for (int i = 0; i < g.enemies.length; i++) if (g.enemies[i].active) alive++;
    System.out.println("enemies after intro trigger: " + alive + " state=" + g.state
        + " msg=" + g.messageText);
    // 7) kill it with the knife (Harry is healed so the test is about combat)
    g.state = Game.S_PLAY;
    g.player.hp = 100; g.player.alive = true;
    for (int i = 0; i < g.enemies.length; i++) {
      Enemy e = g.enemies[i];
      if (!e.active) continue;
      g.player.x = e.x + 40; g.player.z = e.z;
      g.player.angle = Core.atan2(-40, 0);
      int hits = 0;
      for (int f = 0; f < 500 && e.active; f++) {
        if (g.state == Game.S_MESSAGE) { Input dis = new Input(); dis.pressed[Input.A] = true; g.update(dis); continue; }
        if (g.state != Game.S_PLAY) break;
        Input k = new Input(); k.pressed[Input.A] = true; k.down[Input.A] = true;
        int before = e.hp; g.update(k);
        if (e.hp < before) hits++;
        g.player.x = e.x + 40; g.player.z = e.z;
      }
      System.out.println("killed enemy in " + hits + " hits, hp=" + e.hp
          + " active=" + e.active + " playerHP=" + g.player.hp);
    }
    System.out.println("final: level=" + g.level.name + " state=" + g.state);
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
  static void pickAt(int x, int z) {
    g.player.x = x; g.player.z = z;
    Input in = new Input(); in.pressed[Input.A] = true;
    g.update(in);
    g.state = Game.S_PLAY;
  }

  static void step(int n) { for (int i = 0; i < n; i++) g.update(new Input()); }
}
