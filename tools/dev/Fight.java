import ss.core.*; import ss.game.*; import ss.level.*;
public class Fight {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.CAFE); g.state = Game.S_PLAY;
    g.player.hasKnife = true;
    g.enemies[0].spawn(Enemy.KIND_SCREAMER, g.player.x + 40, g.player.z);
    Enemy e = g.enemies[0];
    for (int f = 0; f < 120; f++) {
      g.player.x = e.x + 40; g.player.z = e.z;
      g.player.angle = Core.atan2(-40, 0);
      Input k = new Input(); k.pressed[Input.A] = true; k.down[Input.A] = true;
      int before = e.hp;
      g.update(k);
      System.out.println("f" + f + " ehp=" + e.hp + " (dmg " + (before-e.hp) + ")"
          + " est=" + e.state + " hp=" + g.player.hp + " atk=" + g.player.attackTimer
          + " inv=" + g.player.invuln);
      if (!e.active) { System.out.println("dead at f" + f + " hp=" + g.player.hp); break; }
    }
    GlobalPrint(e, g);
  }
  static void GlobalPrint(Enemy e, Game g) {
    System.out.println("end: ehp=" + e.hp + " active=" + e.active + " playerHP=" + g.player.hp);
  }
}
