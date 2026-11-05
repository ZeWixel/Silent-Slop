package ss.game;

import ss.core.Core;
import ss.core.Render3D;
import ss.gen.Models;
import ss.snd.Sfx;

/**
 * Harry Mason.
 *
 * Movement is 8-directional relative to the camera (the requested default);
 * the tank control option is handled in {@link Input} by feeding different
 * values here, so the walking code itself stays the same.
 */
public final class Player
{
    public int x, z;
    public int angle;           // facing, 4096 units
    public int speed;           // cm per frame, current
    public int hp = 100;
    public int maxHp = 100;

    public boolean running;
    public boolean flashlight;
    public boolean aiming;
    public boolean hasGun;
    public boolean hasKnife;
    public boolean hasRadio;
    public boolean hasMap;
    public int ammo = 12;

    // animation
    public int animPhase;
    public int attackTimer;
    public int hurtTimer;
    public int invuln;
    public boolean alive = true;
    public boolean godMode;

    // camera relative movement, filled by the game loop
    public int moveX, moveZ;

    public String currentItem = "none";
    public int healthDrinks;

    public int walkPhase;

    public void hasFlashlightOn()
    {
        hasFlashlight = true;
        flashlight = true;
    }

    public boolean hasFlashlight;

    public void reset(int sx, int sz, int ang)
    {
        x = sx;
        z = sz;
        angle = ang;
        hp = 100;
        speed = 0;
        running = false;
        aiming = false;
        alive = true;
        attackTimer = 0;
        hurtTimer = 0;
        invuln = 0;
        healthDrinks = 1;
    }

    public int model()
    {
        return Models.HARRY;
    }

    /** Steps the player.  dt is in "frames at 12 fps" units, 1 = nominal. */
    public void update(Game g, int dx, int dz, boolean run, boolean aim)
    {
        if (!alive) return;

        // limit dt so a long pause does not teleport Harry through a wall
        int frames = g.dtFrames;
        if (frames > 3) frames = 3;

        aiming = aim && (hasGun || hasKnife);
        if (aiming)
        {
            // Aiming locks the walk: Harry can only turn, like the original.
            dx = 0;
            dz = 0;
            running = false;
        }

        running = run && !aiming;
        int base = running ? 26 : 12;         // cm per frame
        int target = 0;

        if (dx != 0 || dz != 0)
        {
            int len = Core.isqrt(dx * dx + dz * dz);
            if (len == 0) len = 1;
            int mx = (dx * base) / len;
            int mz = (dz * base) / len;
            int steps = frames;
            for (int i = 0; i < steps; i++)
            {
                int[] p = tmp;
                p[0] = x + mx;
                p[2] = z + mz;
                g.resolve(p, 16);
                x = p[0];
                z = p[2];
            }
            // face the walking direction
            int want = Core.atan2(mx, mz);
            angle = turnTo(angle, want, running ? 620 : 420);
            target = running ? 26 : 12;
        }

        speed = target;
        walkPhase += target;
        if (target > 0 && (walkPhase & 512) < target && g.frames % 6 == 0)
        {
            Sfx.play(Sfx.STEP, running ? 45 : 30);
        }

        if (attackTimer > 0) attackTimer--;
        if (hurtTimer > 0) hurtTimer--;
        if (invuln > 0) invuln--;
    }

    /** Rotates towards an angle by at most `rate` units, shortest way round. */
    static int turnTo(int from, int to, int rate)
    {
        int d = (to - from) & 4095;
        if (d > 2048) d -= 4096;
        if (d > rate) d = rate;
        if (d < -rate) d = -rate;
        return (from + d) & 4095;
    }

    static final int[] tmp = new int[3];

    public void damage(int amount)
    {
        if (godMode || invuln > 0 || !alive) return;
        hp -= amount;
        hurtTimer = 18;
        if (hp <= 0)
        {
            hp = 0;
            alive = false;
        }
    }

    public void heal(int amount)
    {
        hp += amount;
        if (hp > maxHp) hp = maxHp;
    }

    /** Draws Harry plus his contact shadow.  Called by the scene renderer. */
    public void draw()
    {
        Render3D.drawShadow(x, z, 26);
        int bob = 0;
        if (speed > 0) bob = ((walkPhase >> 5) & 1) == 0 ? 0 : -2;
        Render3D.drawModel(Game.models[Models.HARRY], x, bob, z, angle);
    }
}
