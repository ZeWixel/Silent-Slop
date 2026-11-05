import ss.core.*;
public class Probe15 {
  public static void main(String[] a) throws Exception {
    ss.game.Game.initAssets();
    Fb fb = new Fb(176,208);
    int px = -240, pz = 0, yaw = 2048;
    int dist = 520, ch = dist*512/1024, chz = dist*888/1024;
    int camX = px - ((Core.sin(yaw)*chz)>>12);
    int camZ = pz - ((Core.cos(yaw)*chz)>>12);
    int camY = 210 + ch + 120;
    // side view: same camera, same scene, but measure widths at a few depths
    Render3D.begin(fb, camX, camY, camZ, yaw, 341, 34, 88, 104+18);
    int[][] pts = {{px,0,pz},{px-480,0,pz},{px+480,0,pz},
                   {px,0,pz-480},{px,0,pz+480},
                   {px-2000,0,pz-2000},{px+2000,0,pz+2000}};
    for (int i=0;i<pts.length;i++){
      Render3D.project(pts[i][0],pts[i][1],pts[i][2]);
      System.out.println("(" + pts[i][0] + "," + pts[i][2] + ") -> "
          + Render3D.tx + "," + Render3D.ty + " z=" + Render3D.tz);
    }
    System.out.println("scale=" + 34 + " so 480cm ~ " + ((480*34)>>8) + " px");
  }
}
