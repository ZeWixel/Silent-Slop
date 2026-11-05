import ss.core.*; import ss.game.*; import ss.level.*;
public class Probe8 { public static void main(String[] a) throws Exception {
  Game.initAssets(); Game g=new Game(); g.loadLevel(Levels.PROLOGUE); g.state=Game.S_PLAY;
  g.update(new Input()); Fb fb=new Fb(176,208);
  g.player.flashlight=false; g.fogVignette=0;
  g.draw(fb); Preview.save(fb,"/tmp/shots/n1_fog.png");
  Render3D.setFog(1000000,2000000);
  g.draw(fb); Preview.save(fb,"/tmp/shots/n2_nofog.png");
  g.fogVignette=0;
  System.out.println("ambient="+g.level.ambient+" fogStart="+g.level.fogStart);
}}
