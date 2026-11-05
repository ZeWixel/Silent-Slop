import ss.core.*; import ss.game.*; import ss.level.*;
import java.util.Random;
public class Smoke {
  public static void main(String[] a) throws Exception {
    Game.initAssets();
    Game g = new Game();
    Fb fb = new Fb(176,208);
    Random r = new Random(7);
    g.loadLevel(Levels.PROLOGUE);
    int[] keys = {0,1,2,3,4,5,6,7,8,9};
    long t0 = System.currentTimeMillis();
    int frames = 0;
    for (int i = 0; i < 4000; i++) {
      Input in = new Input();
      for (int k = 0; k <= Input.MAX; k++)
      {
        boolean d = r.nextInt(9) == 0;
        in.pressed[k] = d && !in.down[k];
        in.released[k] = !d && in.down[k];
        in.down[k] = d;
      }
      g.update(in);
      if ((i & 3) == 0) g.draw(fb);
      frames++;
      if (i == 1200) { g.goTo(Levels.CAFE); }
      if (i == 2000) { g.mapPanel.open(g); g.state = Game.S_MAP; }
      if (i == 2200) { g.state = Game.S_PAUSE; }
      if (i == 2400) { g.state = Game.S_INVENTORY; }
      if (i == 2600) { g.state = Game.S_DEV; }
      if (i == 2800) { g.state = Game.S_PLAY; }
      if (i == 3000) { g.goTo(Levels.PROLOGUE); }
      if (i == 3400) { g.state = Game.S_NOVEL; g.novel.start(0, g); }
      if (i == 3700) { g.state = Game.S_PLAY; }
    }
    long ms = System.currentTimeMillis() - t0;
    System.out.println("ok frames=" + frames + " in " + ms + "ms  level=" + g.level.name
        + " hp=" + g.player.hp + " enemies=" + countEnemies(g));
  }
  static int countEnemies(Game g){int n=0;for(int i=0;i<g.enemies.length;i++) if(g.enemies[i]!=null&&g.enemies[i].active) n++;return n;}
}
