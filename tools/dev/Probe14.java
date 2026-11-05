import ss.core.*;
public class Probe14 { public static void main(String[] a){
  Fb fb=new Fb(176,208);
  int px=-240, pz=0, yaw=2048;
  int dist=520, ch=(int)((long)dist*512/1024), chz=(int)((long)dist*888/1024);
  int fx=Core.sin(yaw), fz=Core.cos(yaw);
  int camX=px-((fx*chz)>>12), camZ=pz-((fz*chz)>>12), camY=210+ch+120;
  System.out.println("cam "+camX+","+camY+","+camZ);
  Render3D.begin(fb,camX,camY,camZ,yaw,341,62,88,104-26);
  int[][] pts={{px,0,pz},{px,0,pz-560},{px,0,pz+560},{px-4000,0,pz},{px+4000,0,pz},{px,0,pz-4000},{px,0,pz+4000}};
  for(int i=0;i<pts.length;i++){ Render3D.project(pts[i][0],pts[i][1],pts[i][2]);
    System.out.println("pt "+pts[i][0]+","+pts[i][2]+" -> "+Render3D.tx+","+Render3D.ty+" z="+Render3D.tz); }
}}
