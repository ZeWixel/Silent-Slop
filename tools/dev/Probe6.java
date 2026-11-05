import ss.core.*; import ss.game.*; import ss.level.*;
public class Probe6 { public static void main(String[] a) throws Exception {
  Game.initAssets(); Game g=new Game(); g.loadLevel(Levels.PROLOGUE); g.state=Game.S_PLAY;
  g.update(new Input()); Fb fb=new Fb(176,208); g.player.flashlight=false; g.fogVignette=0;
  Render3D.setFog(100000,200000);
  g.draw(fb); Preview.save(fb,"/tmp/shots/nofog.png");
  System.out.println("skyTop="+Integer.toHexString(g.skyTop)+" skyBot="+Integer.toHexString(g.skyBottom));
  int[] c={fb.px[20*176+20]&0xFFFF, fb.px[60*176+20]&0xFFFF, fb.px[100*176+20]&0xFFFF};
  for(int i=0;i<3;i++) System.out.println("px "+i+"="+Integer.toHexString(c[i]));
}}
