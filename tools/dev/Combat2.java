import ss.core.*; import ss.game.*; import ss.level.*;
public class Combat2 {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    Enemy e = g.enemies[0];
    e.spawn(0, Level.u(-6), Level.u(-4));
    g.player.x = e.x + 40; g.player.z = e.z; g.player.angle = Core.atan2(-40, 0);
    int hits = 0, lastHp = e.hp;
    for (int i = 0; i < 400 && e.active; i++) {
      Input in = new Input();
      in.pressed[Input.A] = true; in.down[Input.A] = true;
      int before = e.hp;
      g.update(in);
      if (e.hp < before) { hits++; System.out.println("hit " + hits + " at f" + i + " hp=" + e.hp + " state=" + e.state); }
      if (!e.active) System.out.println("inactive at f" + i + " hp=" + e.hp + " state=" + e.state);
      if (e.hp != lastHp) lastHp = e.hp;
      g.draw(fb);
    }
    System.out.println("hits=" + hits + " hp=" + e.hp + " active=" + e.active
        + " pDist=" + Core.dist2d(e.x-g.player.x, e.z-g.player.z));
  }
}
