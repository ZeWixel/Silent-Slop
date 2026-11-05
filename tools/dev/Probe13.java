import ss.core.*; import ss.game.*; import ss.level.*;
public class Probe13 { public static void main(String[] a) throws Exception {
  Game.initAssets(); Game g=new Game(); g.loadLevel(Levels.PROLOGUE); g.state=Game.S_PLAY;
  g.player.x=Level.u(-6); g.player.z=0; g.update(new Input());
  System.out.println("player "+g.player.x+","+g.player.z+" camYaw="+g.camYaw);
  for(int i=0;i<g.level.nbands && i<12;i++){
    Band b=g.level.bands[i];
    System.out.println("band "+i+" k=("+b.kx+","+b.kz+") nf="+(b.mesh==null?-1:b.mesh.nf));
  }
  System.out.println("total bands "+g.level.nbands);
  int near=0;
  for(int i=0;i<g.level.nbands;i++){ Band b=g.level.bands[i];
    int dx=0,dz=0; if(g.player.x<b.x0)dx=b.x0-g.player.x; else if(g.player.x>b.x1)dx=g.player.x-b.x1;
    if(g.player.z<b.z0)dz=b.z0-g.player.z; else if(g.player.z>b.z1)dz=g.player.z-b.z1;
    if(dx+dz<=g.fogEnd && b.mesh!=null) near++; }
  System.out.println("bands in range with mesh: "+near);
  Fb fb=new Fb(176,208); g.draw(fb);
  System.out.println("tris="+Render3D.statsTris()+" bands="+Render3D.statsBands());
}}
