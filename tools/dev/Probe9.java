import ss.core.*; import ss.game.*; import ss.level.*;
public class Probe9 { public static void main(String[] a) throws Exception {
  Game.initAssets(); Game g=new Game(); g.loadLevel(Levels.PROLOGUE); g.state=Game.S_PLAY;
  g.update(new Input());
  // recompute the camera the same way Scene does
  int dist=g.camDist, pitch=341;
  int ch=(int)((long)dist*512/1024), chz=(int)((long)dist*888/1024);
  int fx=Core.sin(g.camYaw), fz=Core.cos(g.camYaw);
  int camX=g.player.x-((fx*chz)>>12), camZ=g.player.z-((fz*chz)>>12), camY=g.camHeight+ch;
  System.out.println("cam "+camX+","+camY+","+camZ+" yaw="+g.camYaw);
  int px=g.player.x, pz=g.player.z;
  for(int i=0;i<g.level.nbands;i++){
    Band b=g.level.bands[i]; if(b==null||i%20!=0) continue;
    System.out.println("band "+i+" x="+b.x0+".."+b.x1+" z="+b.z0+".."+b.z1+" faces="+(b.mesh==null?-1:b.mesh.nf));
  }
  Fb fb=new Fb(176,208);
  Render3D.begin(fb,camX,camY,camZ,g.camYaw,pitch,g.fov,88,114);
  Render3D.project(px,0,pz);
  System.out.println("player on screen "+Render3D.tx+","+Render3D.ty+" z="+Render3D.tz);
  Render3D.setFog(0,100000);
  Render3D.drawFloor(px-1000,pz-1000,px+1000,pz+1000,-8,ss.gen.Tex.GRASS,16,0);
  Render3D.flush();
  int n=0; for(int i=0;i<fb.px.length;i++) if((fb.px[i]&0xFFFF)!=0) n++;
  System.out.println("painted "+n);
}}
