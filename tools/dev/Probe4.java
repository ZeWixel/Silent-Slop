import ss.core.*;
import ss.game.*;
import ss.level.*;
public class Probe4 {
  public static void main(String[] a) throws Exception {
    Game.initAssets();
    Game g = new Game(); g.loadLevel(Levels.CAFE); g.state=Game.S_PLAY;
    Input in=new Input(); g.update(in);
    Fb fb=new Fb(176,208);
    g.player.flashlight=false; g.fogVignette=0;
    // 1: no props
    Game.models = new Model[Game.models.length];
    g.draw(fb); Preview.save(fb,"/tmp/shots/dbg_noprops.png");
    // count white-ish pixels
    System.out.println("white pixels: " + countWhite(fb));
    for (int i=0;i<g.level.nbands;i++){ Band b=g.level.bands[i]; if(b!=null) b.mesh=null; }
    g.draw(fb); Preview.save(fb,"/tmp/shots/dbg_nowalls.png");
    System.out.println("white pixels (no walls): " + countWhite(fb));
  }
  static int countWhite(Fb fb){int n=0;for(int i=0;i<fb.px.length;i++){int c=fb.px[i]&0xFFFF;
    int r=(c>>11)&0x1F,g2=(c>>5)&0x3F,b=c&0x1F; if(r>25&&g2>50&&b>25)n++;}return n;}
}
