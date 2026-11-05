import ss.core.*;
import ss.game.*;
import ss.level.Levels;

public class Probe
{
    public static void main(String[] a)
    {
        Game.initAssets();
        Game g = new Game();
        g.loadLevel(Levels.PROLOGUE);
        g.state = Game.S_PLAY;
        g.fogVignette = 0;
        g.player.flashlight = false;
        int[] in = null;
        Input inp = new Input();
        g.update(inp);
        g.camYaw = 2048;
        g.draw(new Fb(176,208));
        System.out.println("tris " + Render3D.statsTris() + " bands " + Render3D.statsBands());
        // draw one floor quad and see
        Render3D.begin(new Fb(176,208), 0, 600, 0, 2048, 341, 190, 88, 100);
        Render3D.setFog(0, 100000);
        Render3D.setLight(-260,820,-460,4);
        Render3D.project(0,0,0);
        System.out.println("origin -> " + Render3D.tx + "," + Render3D.ty + " z " + Render3D.tz);
        Render3D.project(0,0,600);
        System.out.println("north  -> " + Render3D.tx + "," + Render3D.ty + " z " + Render3D.tz);
        Render3D.project(0,0,-600);
        System.out.println("south  -> " + Render3D.tx + "," + Render3D.ty + " z " + Render3D.tz);
        Render3D.project(600,0,0);
        System.out.println("east   -> " + Render3D.tx + "," + Render3D.ty + " z " + Render3D.tz);
    }
}
