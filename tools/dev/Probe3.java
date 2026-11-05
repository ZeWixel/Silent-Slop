import ss.core.*;
import ss.game.*;
import ss.level.*;

public class Probe3 {
  static void shot(String name) throws Exception {
    Fb fb = new Fb(176,208);
    Game g = cur;
    g.draw(fb);
    Preview.save(fb, "/tmp/shots/iso_"+name+".png");
    System.out.println(name+" tris="+Render3D.statsTris()+" bands="+Render3D.statsBands());
  }
  static Game cur;
  public static void main(String[] a) throws Exception {
    Game.initAssets();
    Game g = new Game(); g.loadLevel(Levels.PROLOGUE); g.state = Game.S_PLAY;
    g.player.x=-320; g.player.z=7280; g.camYaw=2048;
    g.fogVignette=0;
    cur=g;
    Input in=new Input(); g.update(in); g.player.flashlight=false;
    g.state = Game.S_PLAY;
    // count prop/floor/wall contributions
    int nfl=0,npr=0,nw=0;
    for(int i=0;i<g.level.nbands;i++){ Band b=g.level.bands[i]; if(b==null)continue;
      nfl+=b.nfl; npr+=b.npr; nw+=(b.mesh==null?0:b.mesh.nf);}
    System.out.println("floors="+nfl+" props="+npr+" wallfaces="+nw);
    g.draw(new Fb(176,208));
    System.out.println("tris="+Render3D.statsTris());
  }
}
