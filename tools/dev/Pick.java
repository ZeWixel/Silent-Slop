import ss.core.*; import ss.game.*; import ss.level.*;
public class Pick {
  static Game g;
  public static void main(String[] x) throws Exception {
    Game.initAssets(); g = new Game();
    g.loadLevel(Levels.CAFE); g.state = Game.S_PLAY;
    g.player.x = Level.u(9); g.player.z = Level.u(158);
    Input in = new Input(); in.keyDown(Input.A); g.update(in);
    System.out.println("1 knife=" + g.player.hasKnife + " flash=" + g.player.hasFlashlight + " state=" + g.state);
    g.state = Game.S_PLAY;
    g.player.x = Level.u(10); g.player.z = Level.u(154);
    in = new Input(); in.keyDown(Input.A); g.update(in);
    System.out.println("2 knife=" + g.player.hasKnife + " flash=" + g.player.hasFlashlight
      + " lit=" + g.player.flashlight + " taken=" + g.level.bands[4].taken[0]
      + " msg=" + g.messageText + " state=" + g.state);
    g.state = Game.S_PLAY;
    in = new Input(); in.keyDown(Input.A); g.update(in);
    System.out.println("3 flash=" + g.player.hasFlashlight + " radio=" + g.player.hasRadio
      + " msg=" + g.messageText);
  }
}
