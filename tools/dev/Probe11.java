import ss.core.*;
public class Probe11 { public static void main(String[] a) throws Exception {
  ss.game.Game.initAssets();
  Fb fb=new Fb(176,208); fb.clear(0xF81F);
  Render3D.begin(fb,-240,470,6270,0,341,84,88,114);
  Render3D.setFog(0,100000); Render3D.setLight(-260,820,-460,20);
  Render3D.setMode(Render3D.MODE_TEX);
  Render3D.project(-1320,-8,7280); System.out.println("A "+Render3D.tx+","+Render3D.ty);
  Render3D.project(680,-8,7280);   System.out.println("B "+Render3D.tx+","+Render3D.ty);
  Render3D.project(680,-8,5280);   System.out.println("C "+Render3D.tx+","+Render3D.ty);
  Render3D.project(-1320,-8,5280); System.out.println("D "+Render3D.tx+","+Render3D.ty);
  Render3D.drawFloor(-1320,5280,680,7280,-8,ss.gen.Tex.GRASS,16,0);
  Render3D.flush();
  System.out.println("emitted="+Render3D.dbgEmit);int n=0;for(int i=0;i<fb.px.length;i++) if((fb.px[i]&0xFFFF)!=0xF81F) n++;System.out.println("painted "+n);
}}
