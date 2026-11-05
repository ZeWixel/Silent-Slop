import ss.core.*;
import ss.game.*;
import ss.level.Levels;
public class Probe2 {
  public static void main(String[] a){
    Game.initAssets(); Game g=new Game(); g.loadLevel(Levels.PROLOGUE); g.state=Game.S_PLAY;
    Input in=new Input(); g.update(in); g.draw(new Fb(176,208));
    System.out.println("player "+g.player.x+","+g.player.z+" camYaw "+g.camYaw+" tris "+Render3D.statsTris()+" bands "+Render3D.statsBands());
    System.out.println("fogEnd "+g.fogEnd+" camdist "+g.camDist+" fov "+g.fov);
    // check a known wall band
    ss.level.Band b=g.level.bands[0];
    System.out.println("band0 "+b.x0+","+b.z0+" mesh "+(b.mesh==null?-1:b.mesh.nf)+" far "+(b.mesh==null?0:b.mesh.nv));
  }
}
