package ss.game;

import ss.core.Core;
import ss.core.Render3D;
import ss.gen.Models;
import ss.snd.Sfx;

/**
 * Air Screamer (and, reusing the same brain, the grey children shambler).
 *
 * Three states, no pathfinding: idle drift, chase, attack lunge.  They are
 * spawned per trigger and live in a fixed pool - no allocation per enemy.
 */
public final class Enemy
{
    public static final int KIND_SCREAMER = 0;
    public static final int KIND_SHAMBLER = 1;

    public static final int S_IDLE = 0;
    public static final int S_CHASE = 1;
    public static final int S_ATTACK = 2;
    public static final int S_DYING = 3;
    public static final int S_DEAD = 4;

    public boolean active;
    public int kind;
    public int x, z, y;
    public int angle;
    public int hp;
    public int state;
    public int timer;
    public int phase;
    public int anim;
    public int hitFlash;
    private int baseY;

    public void spawn(int kind, int x, int z)
    {
        this.kind = kind;
        this.x = x;
        this.z = z;
        this.y = kind == KIND_SCREAMER ? 130 : 0;
        baseY = y;
        angle = 2048;
        hp = kind == KIND_SCREAMER ? 36 : 20;
        state = S_IDLE;
        timer = 0;
        active = true;
        anim = 0;
        hitFlash = 0;
    }

    public void update(Game g)
    {
        if (!active) return;
        Player p = g.player;
        if (hitFlash > 0) hitFlash--;

        int dx = p.x - x;
        int dz = p.z - z;
        int dist = Core.dist2d(dx, dz);

        switch (state)
        {
            case S_IDLE:
                // drift around, wake up when the player comes close
                phase += 23;
                x += Core.sin(phase) >> 9;
                z += Core.cos(phase + 900) >> 9;
                y = baseY + (Core.sin(phase * 2) >> 6);
                if (dist < 520 && p.alive) state = S_CHASE;
                break;
            case S_CHASE:
                if (!p.alive) { state = S_IDLE; break; }
                int sp = kind == KIND_SCREAMER ? 24 : 13;
                if (dist > 1)
                {
                    x += (dx * sp) / dist;
                    z += (dz * sp) / dist;
                }
                angle = Core.atan2(dx, dz);
                phase += 40;
                if (kind == KIND_SCREAMER)
                {
                    // the screamer hovers and dives; keep it off the floor
                    baseY = 110 + (Core.sin(phase) >> 5);
                    y = baseY;
                }
                anim++;
                if (dist < 120) { state = S_ATTACK; timer = 26; }
                break;
            case S_ATTACK:
                angle = Core.atan2(dx, dz);
                if (timer > 0) timer--;
                if (timer == 14 && dist < 190)
                {
                    p.damage(kind == KIND_SCREAMER ? 18 : 10);
                    Sfx.play(Sfx.HURT, 80);
                }
                if (timer <= 0) state = S_CHASE;
                break;
            case S_DYING:
                timer--;
                anim++;
                y += kind == KIND_SCREAMER ? -4 : 0;
                if (timer <= 0)
                {
                    state = S_DEAD;
                    active = false;
                    Sfx.play(Sfx.ENEMY_DIE, 90);
                    g.onEnemyKilled(this);
                }
                break;
        }
    }

    public void hurt(int amount)
    {
        if (!active || state == S_DYING) return;
        hp -= amount;
        hitFlash = 8;
        if (hp <= 0) kill();
        else if (state == S_IDLE) state = S_CHASE;
    }

    public void kill()
    {
        state = S_DYING;
        timer = 22;
    }

    public void draw()
    {
        if (!active) return;
        Render3D.drawShadow(x, z, 30);
        int model = kind == KIND_SCREAMER ? Models.AIRSCREAMER : Models.SHAMBLER;
        int bob = (state == S_DYING) ? (22 - timer) : 0;
        // wing flap: rotate the body slightly, the wings are part of the mesh
        int roll = (Core.sin(phase) >> 8);
        Render3D.drawModel(Game.models[model], x, y - bob, z, angle);
    }

    /** Distance to the player's aim ray, used by the gun. */
    public int hitTest(Player p)
    {
        int dx = x - p.x;
        int dz = z - p.z;
        int d = Core.dist2d(dx, dz);
        if (d > 900) return 9999;
        int a = Core.atan2(dx, dz);
        int diff = ((a - p.angle) & 4095);
        if (diff > 2048) diff = 4096 - diff;
        if (diff > 130) return 9999;
        return d;
    }
}
