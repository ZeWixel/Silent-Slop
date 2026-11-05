import ss.core.*;
public class Probe7 { public static void main(String[] a) throws Exception {
  Fb fb=new Fb(176,208); fb.clear(0x0000);
  Render3D.begin(fb,-320,470,7280-450,0,341,84,88,114);
  Render3D.setFog(0,100000); Render3D.setLight(-260,820,-460,20);
  Render3D.setMode(Render3D.MODE_TEX);
  int x0=-8000,z0=-8000,x1=8000,z1=8000;
  Render3D.project(x0,0,z1); System.out.println("p0 "+Render3D.tx+","+Render3D.ty+" z="+Render3D.tz);
  Render3D.project(x1,0,z0); System.out.println("p2 "+Render3D.tx+","+Render3D.ty+" z="+Render3D.tz);
  Render3D.drawFloor(x0,z0,x1,z1,-8, ss.gen.Tex.GRASS, 400, 0);
  Render3D.flush();
  int n=0; for(int i=0;i<fb.px.length;i++) if((fb.px[i]&0xFFFF)!=0) n++;
  System.out.println("painted "+n);
  Preview.save(fb,"/tmp/shots/floor.png");
  // second: yaw with sin!=0
  Fb f2=new Fb(176,208); f2.clear(0x0000);
  Render3D.begin(f2,-320,470,6830,2048,341,84,88,114);
  Render3D.setFog(0,100000); Render3D.setLight(-260,820,-460,20);
  Render3D.setMode(Render3D.MODE_TEX);
  Render3D.drawFloor(x0,z0,x1,z1,-8, ss.gen.Tex.GRASS, 400, 0);
  Render3D.flush(); Preview.save(f2,"/tmp/shots/floor2.png");
}}
