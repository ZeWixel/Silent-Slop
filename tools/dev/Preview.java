import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import ss.core.Fb;
import ss.game.Game;
import ss.game.Input;
import ss.level.Levels;

/** Desktop harness: renders real engine frames to PNG. Not part of the JAR. */
public class Preview
{
    public static void main(String[] a) throws Exception
    {
        Game.initAssets();
        Fb fb = new Fb(176, 208);
        Game g = new Game();
        g.loadLevel(Levels.PROLOGUE);
        g.state = Game.S_PLAY;
        g.player.hasFlashlightOn();
        g.player.hasKnife = true;
        g.player.hasGun = true;
        g.player.hasRadio = true;
        g.player.hasMap = true;

        // advance a few frames so the camera settles; dismiss any opening text
        Input in = new Input();
        for (int i = 0; i < 3; i++) { g.update(in); }
        if (g.state == Game.S_MESSAGE) g.state = Game.S_PLAY;
        // move Harry forward a bit (towards the cafe)
        g.player.x = LevelU(-6); g.player.z = 0; g.camYaw = 2048;
        for (int i = 0; i < 4; i++) g.update(in);
        g.player.aiming = false;
        if (g.state == Game.S_MESSAGE) g.state = Game.S_PLAY;

        g.draw(fb);
        System.out.println("tris="+ss.core.Render3D.statsTris()+" bands="+ss.core.Render3D.statsBands()
            +" nbands="+g.level.nbands+" px="+g.player.x+","+g.player.z
            +" camdist="+g.camDist+" fov="+g.fov);
        int[] yaws = { 0, 512, 1024, 1536, 2048, 2560, 3072, 3584 };
        for (int i = 0; i < yaws.length; i++)
        {
            g.camYaw = yaws[i];
            g.state = Game.S_PLAY;
            g.draw(fb);
            save(fb, "/tmp/shots/street_" + i + ".png");
        }
        // cafe
        g.loadLevel(Levels.CAFE);
        g.state = Game.S_PLAY;
        g.player.hasFlashlightOn();
        for (int i = 0; i < 4; i++) g.update(in);
        if (g.state == Game.S_MESSAGE) g.state = Game.S_PLAY;
        for (int i = 0; i < 4; i++)
        {
            g.camYaw = i * 1024;
            g.draw(fb);
            save(fb, "/tmp/shots/cafe_" + i + ".png");
        }
        // title
        g.state = Game.S_TITLE;
        g.draw(fb);
        save(fb, "/tmp/shots/title.png");
        // novel
        g.state = Game.S_NOVEL;
        g.novel.start(1, g);
        for (int i = 0; i < 30; i++) g.novel.update(in);
        g.novel.typeTimer = 400;
        g.draw(fb);
        save(fb, "/tmp/shots/novel.png");
        System.out.println("ok");
    }

    static int LevelU(double v) { return (int) (v * 40); }

    static void save(Fb fb, String path) throws Exception
    {
        new File(path).getParentFile().mkdirs();
        BufferedImage im = new BufferedImage(fb.w, fb.h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < fb.h; y++)
            for (int x = 0; x < fb.w; x++)
            {
                int c = fb.px[y * fb.w + x] & 0xFFFF;
                int r = (c >> 11) & 0x1F, gg = (c >> 5) & 0x3F, b = c & 0x1F;
                r = (r << 3) | (r >> 2);
                gg = (gg << 2) | (gg >> 4);
                b = (b << 3) | (b >> 2);
                im.setRGB(x, y, (r << 16) | (gg << 8) | b);
            }
        ImageIO.write(im, "png", new File(path));
    }
}
