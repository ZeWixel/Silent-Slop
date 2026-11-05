import ss.core.*; import ss.game.*; import ss.level.*;
public class Menus {
  static Input down() { Input i = new Input(); i.keyDown(Input.DOWN); return i; }
  static Input a() { Input i = new Input(); i.keyDown(Input.A); return i; }
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    for (int i=0;i<5;i++) g.update(new Input());

    // open the pause menu the way the player does: right soft key
    Input open = new Input(); open.keyDown(Input.SOFT_RIGHT);
    g.update(open);
    if (g.state != Game.S_PAUSE) System.out.println("FAIL: soft right did not pause");

    // walk to the config rows and toggle them, checking the label follows
    for (int k = 0; k < 3; k++) g.update(down());
    System.out.println("row3 " + g.menu.items[3] + g.menu.values[3] + " (sel=" + g.menu.sel + ")");
    g.update(a());
    System.out.println("  after A: " + g.menu.items[3] + g.menu.values[3] + " objMode=" + g.objMode);
    g.update(down());
    System.out.println("row4 " + g.menu.items[4] + g.menu.values[4]);
    g.update(a());
    System.out.println("  after A: " + g.menu.items[4] + g.menu.values[4] + " tank=" + g.tankControls);
    for (int k = 0; k < 2; k++) g.update(down());
    System.out.println("row6 " + g.menu.items[6] + g.menu.values[6]);
    g.update(a());
    System.out.println("  after A: " + g.menu.items[6] + g.menu.values[6]);

    g.draw(fb);
    Preview.save(fb, "/tmp/shots/menu_pause.png");
    g.state = Game.S_DEV; g.devMenu.reset(Texts.DEV); g.draw(fb);
    Preview.save(fb, "/tmp/shots/menu_dev.png");
    System.out.println("pause=" + Texts.PAUSE.length + " dev=" + Texts.DEV.length);
  }
}
