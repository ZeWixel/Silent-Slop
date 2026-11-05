import ss.core.*; import ss.game.*; import ss.level.*;
public class NovelShot {
  public static void main(String[] a) throws Exception {
    Game.initAssets(); Game g = new Game(); Fb fb = new Fb(176,208);
    g.loadLevel(Levels.PROLOGUE);
    for (int id = 0; id < 3; id++) {
      g.state = Game.S_NOVEL;
      g.novel.start(id, g);
      for (int f = 0; f < 60; f++) { g.novel.update(new Input()); g.novel.draw(fb); }
      Preview.save(fb, "/tmp/shots/novel" + id + ".png");
      // advance to the next page/panel
      for (int f = 0; f < 30; f++) {
        Input in = new Input();
        if (f == 0 || f == 12) in.pressed[Input.A] = true;
        g.novel.update(in); g.novel.draw(fb);
      }
      Preview.save(fb, "/tmp/shots/novel" + id + "b.png");
    }
  }
}
