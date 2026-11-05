import ss.core.*; import ss.game.*; import ss.level.*;
public class Death {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    g.player.damage(999);
    Input in = new Input(); g.update(in); g.draw(fb);
    System.out.println("state=" + g.state + " hp=" + g.player.hp + " (S_DEAD=" + Game.S_DEAD + ")");
    Preview.save(fb, "/tmp/shots/dead.png");
    Input in2 = new Input(); in2.pressed[Input.A] = true;
    g.update(in2);
    System.out.println("after 5: state=" + g.state + " hp=" + g.player.hp);
  }
}
