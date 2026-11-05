import ss.core.*;
import ss.gen.Gen;
public class Probe12 { public static void main(String[] a) throws Exception {
  ss.game.Game.initAssets();
  Fb fb=new Fb(176,208); fb.clear(0xF81F);
  // camera at origin, looking along +z (yaw 0, pitch 0)
  Render3D.begin(fb,0,0,0,0,0,64,88,104);
  Render3D.setFog(0,100000); Render3D.setLight(0,1024,0,12);
  Render3D.setMode(Render3D.MODE_TEX);
  Gen.MB m=new Gen.MB();
  m.box(-100,-100,400,100,100,600, ss.gen.Tex.BRICK,0,0,10);
  Model mod=m.toModel("probe");
  Render3D.drawBand(mod,0,0,0); Render3D.flush();
  int n=0; for(int i=0;i<fb.px.length;i++) if((fb.px[i]&0xFFFF)!=0xF81F) n++;
  System.out.println("painted="+n+" (expect ~ a large square)");
}}
