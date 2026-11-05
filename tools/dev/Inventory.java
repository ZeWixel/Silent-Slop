import ss.core.*; import ss.game.*; import ss.level.*;
public class Inventory {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_INVENTORY;
    g.player.hp = 40; g.player.healthDrinks = 2;
    Input in = new Input();
    in.pressed[Input.STAR] = true;
    g.update(in);
    System.out.println("after * : hp=" + g.player.hp + " drinks=" + g.player.healthDrinks
        + " state=" + g.state);
    g.draw(fb); Preview.save(fb, "/tmp/shots/inv.png");
    Input in2 = new Input();
    in2.pressed[Input.SOFT_LEFT] = true;
    g.update(in2);
    System.out.println("after soft-left: state=" + g.state);
  }
}
