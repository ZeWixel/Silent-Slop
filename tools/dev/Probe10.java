import ss.core.*;
public class Probe10 { public static void main(String[] a) throws Exception {
  Game0();
}
static void Game0() throws Exception {
  ss.game.Game.initAssets();
  Fb fb=new Fb(176,208); fb.clear(0x0000);
  Render3D.begin(fb,-240,470,6270,0,341,84,88,114);
  Render3D.setFog(0,100000); Render3D.setLight(-260,820,-460,20);
  Render3D.setMode(Render3D.MODE_TEX);
  Render3D.drawFloor(-1320,5280,680,7280,-8,ss.gen.Tex.GRASS,16,0);
  System.out.println("clip="+Render3D.dbgClip+" drop="+Render3D.dbgDrop+" emit="+Render3D.dbgEmit);
  Render3D.flush();
  int n=0; for(int i=0;i<fb.px.length;i++) if((fb.px[i]&0xFFFF)!=0) n++;
  System.out.println("painted "+n);
}}
