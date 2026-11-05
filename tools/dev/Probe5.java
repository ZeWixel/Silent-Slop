import ss.core.*;
public class Probe5 {
  public static void main(String[] a){
    Fb fb=new Fb(176,208); fb.clear(0x1234);
    // camera at the player's default spot in the cafe
    int px=-120, pz=5840;
    int camX=px, camZ=pz-450, camY=470;
    Render3D.begin(fb, camX, camY, camZ, 2048, 341, 300, 88, 114);
    Render3D.setFog(0, 100000);
    Render3D.setLight(-260,820,-460,8);
    Render3D.setMode(Render3D.MODE_TEX);
    // draw one 1200x1200 floor at y=0 in front of the camera
    int[] pts={-1000,6000,-200,6600,200,6600,1000,6000};
    int[][] quads={{-1000,6000,200,6600},{-1000,6000,1000,6000}};
    int x0=-1000,z0=6000,x1=1000,z1=7200;
    Render3D.project(x0,0,z1); System.out.println("p0 "+Render3D.tx+","+Render3D.ty+" z"+Render3D.tz);
    Render3D.project(x1,0,z1); System.out.println("p1 "+Render3D.tx+","+Render3D.ty+" z"+Render3D.tz);
    Render3D.project(x1,0,z0); System.out.println("p2 "+Render3D.tx+","+Render3D.ty+" z"+Render3D.tz);
    Render3D.project(x0,0,z0); System.out.println("p3 "+Render3D.tx+","+Render3D.ty+" z"+Render3D.tz);
    Render3D.drawFloor(x0,z0,x1,z1,0,1,56,0);
    Render3D.flush();
    int n=0;
    for(int i=0;i<fb.px.length;i++) if((fb.px[i]&0xFFFF)!=0x1234) n++;
    System.out.println("painted "+n);
  }
}
