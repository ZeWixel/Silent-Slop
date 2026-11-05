import ss.core.*; import ss.game.*; import ss.level.*;
public class Combat {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    g.player.x = Level.u(-6); g.player.z = 0;
    g.goTo(Levels.PROLOGUE); g.state = Game.S_PLAY; g.enemies[0].spawn(0, Level.u(-6), Level.u(-4));
    int alive = 0; for (int i=0;i<g.enemies.length;i++) if (g.enemies[i].active) alive++;
    System.out.println("spawned=" + alive);
    Enemy e = null; for (int i=0;i<g.enemies.length;i++) if (g.enemies[i].active) e = g.enemies[i];
    System.out.println("enemy at " + e.x + "," + e.z + " hp=" + e.hp);
    // stand next to it and swing
    g.player.x = e.x + 40; g.player.z = e.z; g.player.angle = Core.atan2(-40, 0);
    System.out.println("hitTest=" + e.hitTest(g.player) + " playerAttackTimer=" + g.player.attackTimer);
    int startHp = e.hp;
    for (int i=0;i<400 && e.active;i++) {
      Input in = new Input();
      in.pressed[Input.A] = true; in.down[Input.A] = true;
      g.update(in);
      if (i % 4 == 0) g.draw(fb);
      if (i < 6) System.out.println("  f" + i + " ehp=" + e.hp + " est=" + e.state + " act=" + e.active + " pAT=" + g.player.attackTimer);
    }
    System.out.println("enemy hp " + startHp + " -> " + e.hp + " active=" + e.active
        + " playerHP=" + g.player.hp);
    // player takes damage?  let the enemy attack a passive player
    g.player.hp = 100; g.godMode = false;
    for (int i=0;i<600;i++) { Input in = new Input(); g.update(in); }
    System.out.println("after idle: playerHP=" + g.player.hp + " state=" + g.state);
  }
}
