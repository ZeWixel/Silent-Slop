import ss.core.*; import ss.game.*; import ss.level.*;
public class MapShot { public static void main(String[] a) throws Exception {
  Game.initAssets(); Game g=new Game(); g.loadLevel(Levels.PROLOGUE); g.state=Game.S_PLAY;
  g.update(new Input()); Fb fb=new Fb(176,208);
  g.mapPanel.open(g); g.mapPanel.draw(fb,g);
  Preview.save(fb,"/tmp/shots/map_pro.png");
  g.loadLevel(Levels.CAFE); g.state=Game.S_PLAY; g.update(new Input());
  g.mapPanel.open(g); g.mapPanel.draw(fb,g);
  Preview.save(fb,"/tmp/shots/map_cafe.png");
  System.out.println("ok");
}}
