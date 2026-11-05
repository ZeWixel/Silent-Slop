package ss.game;

import ss.core.Core;
import ss.core.Fb;
import ss.core.Model;
import ss.core.Render3D;
import ss.core.TexStore;
import ss.gen.Gen;
import ss.gen.Models;
import ss.gen.Tex;
import ss.level.Band;
import ss.level.Level;
import ss.level.Levels;
import ss.snd.Music;
import ss.snd.Sfx;
import ss.ui.Font6;
import ss.vn.Novel;

/**
 * The game: levels, Harry, enemies, items, the radio, the flashlight, the HUD
 * and the flow between the street and the cafe.
 *
 * Everything is polled from {@link #update(Input)} once per frame; the renderer
 * runs from {@link #draw(Fb)}.  No threads, no allocation in the loop.
 */
public final class Game
{
    public static Model[] models;

    // ---- screens
    public static final int S_PLAY = 0;
    public static final int S_NOVEL = 1;
    public static final int S_MAP = 2;
    public static final int S_PAUSE = 3;
    public static final int S_INVENTORY = 4;
    public static final int S_MESSAGE = 5;
    public static final int S_DEAD = 6;
    public static final int S_LOADING = 7;
    public static final int S_TITLE = 8;
    public static final int S_DEV = 9;

    public int state = S_TITLE;
    public final Player player = new Player();
    public final Enemy[] enemies = new Enemy[6];
    public Level level;

    // camera
    public int camYaw;
    public int camDist = 520;
    public int camHeight = 210;
    /** Isometric zoom: 256 == one pixel per centimetre.  72 -> ~2.8 px per 10 cm. */
    public int fov = 34;
    public int frames;
    public int dtFrames = 1;

    // lighting / fog
    public int lightX = -260, lightY = 820, lightZ = -460;
    public int fogEnd = 6000;
    public int fogColor = 0x5290;
    public int skyTop = 0x294A, skyBottom = 0x5290;
    public int fogVignette = 6;
    public int flashRange = 2000;
    public int flashWide = 150;

    // render options
    public int objMode = Render3D.MODE_TEX;   // TEX or FLAT for models
    public boolean tankControls;
    public boolean showMap;

    // flow flags
    public int levelId = Levels.PROLOGUE;
    public int pendingLevel = -1;
    public int transition = 0;      // >0: fade in/out counter
    public boolean transitionOut;
    public boolean introDone;
    public boolean cafeCutsceneDone;
    public boolean screamIntroDone;
    public boolean cybilActive;
    public boolean showCybil;
    public int cybilX, cybilZ, cybilAngle;
    public int lastDir;
    public int messageId;
    public String messageText;
    public int messageTimer;
    public boolean waitingForInput;

    // dev mode
    public boolean devMode;
    public boolean godMode;
    public boolean showFps;
    public int fps;
    private int fpsAccum;
    private int fpsTimer;

    // novel
    public final Novel novel = new Novel();

    // map
    public final ss.map.MapPanel mapPanel = new ss.map.MapPanel();

    // pause menu
    public final ss.ui.Menu menu = new ss.ui.Menu();
    public final ss.ui.Menu devMenu = new ss.ui.Menu();

    // text
    public static String[] messages = Texts.STREET;

    private final int[] tmpPos = new int[3];

    public Game()
    {
        for (int i = 0; i < enemies.length; i++) enemies[i] = new Enemy();
    }

    /**
     * One-off initialisation: palette, generated textures and meshes.  Called by
     * the MIDlet before the first frame; the harness calls it directly.
     */
    public static void initAssets()
    {
        if (models != null) return;
        short[] pal = Gen.palette();
        Gen.setPalette(pal);
        Render3D.bakePalette(pal);
        TexStore.reset();
        ss.core.Tex[] t = Gen.textures();
        for (int i = 0; i < t.length; i++) TexStore.add(t[i]);
        if (models == null) models = Models.build();
    }

    // ------------------------------------------------------------------ setup
    public void start(int lvl)
    {
        loadLevel(lvl);
        state = S_PLAY;
    }

    public void loadLevel(int id)
    {
        levelId = id;
        Band.resetTriggers();
        final Level[] holder = new Level[1];
        level = Levels.build(id, new iface.IProgress()
        {
            public void step(String what) {}
            public boolean cancelled() { return false; }
        });
        level.finish();
        fogEnd = level.fogEnd;
        fogColor = level.fogColor;
        skyTop = Core.mix565(level.fogColor, 0x0000, 110);
        skyBottom = level.fogColor;
        player.reset(level.spawnX, level.spawnZ, level.spawnAngle);
        for (int i = 0; i < enemies.length; i++) enemies[i].active = false;
        camYaw = level.spawnAngle + 2048;
        showCybil = false;
        if (id == Levels.PROLOGUE)
        {
            messages = Texts.STREET;
            Music.play(Music.THEME);
        }
        else
        {
            messages = Texts.CAFE;
            Music.play(Music.CAFE);
        }
        mapPanel.setLevel(id);
    }

    // ---------------------------------------------------------------- helpers
    public void resolve(int[] pos, int rad)
    {
        for (int i = 0; i < Band.ncol; i++)
        {
            int o = i * 5;
            int x0 = Band.colData[o], z0 = Band.colData[o + 1];
            int x1 = Band.colData[o + 2], z1 = Band.colData[o + 3];
            if (pos[0] + rad <= x0 || pos[0] - rad >= x1) continue;
            if (pos[2] + rad <= z0 || pos[2] - rad >= z1) continue;
            int ox0 = (pos[0] + rad) - x0;
            int ox1 = x1 - (pos[0] - rad);
            int oz0 = (pos[2] + rad) - z0;
            int oz1 = z1 - (pos[2] - rad);
            int m = ox0, axis = 0;
            if (ox1 < m) { m = ox1; axis = 1; }
            if (oz0 < m) { m = oz0; axis = 2; }
            if (oz1 < m) { m = oz1; axis = 3; }
            if (axis == 0) pos[0] -= m;
            else if (axis == 1) pos[0] += m;
            else if (axis == 2) pos[2] -= m;
            else pos[2] += m;
        }
        if (pos[0] < level.zoneX0) pos[0] = level.zoneX0;
        if (pos[0] > level.zoneX1) pos[0] = level.zoneX1;
        if (pos[2] < level.zoneZ0) pos[2] = level.zoneZ0;
        if (pos[2] > level.zoneZ1) pos[2] = level.zoneZ1;
    }

    public boolean isBlocked(int x, int z, int rad)
    {
        for (int i = 0; i < Band.ncol; i++)
        {
            int o = i * 5;
            if (x + rad > Band.colData[o] && x - rad < Band.colData[o + 2] &&
                z + rad > Band.colData[o + 1] && z - rad < Band.colData[o + 3]) return true;
        }
        return false;
    }

    // ------------------------------------------------------------------- loop
    public void update(Input in)
    {
        if (transition > 0)
        {
            transition--;
            if (transition == 0 && pendingLevel >= 0)
            {
                int l = pendingLevel;
                pendingLevel = -1;
                loadLevel(l);
                transition = 12;
                transitionOut = false;
                return;
            }
        }

        Music.update();
        if (state == S_TITLE)
        {
            updateTitle(in);
            in.endFrame();
            return;
        }
        if (state == S_NOVEL)
        {
            novel.update(in);
            if (novel.finished)
            {
                novel.finished = false;
                state = S_PLAY;
                afterNovel(novel.id);
            }
            in.endFrame();
            return;
        }
        if (state == S_MAP)
        {
            mapPanel.update(in, this);
            in.endFrame();
            return;
        }
        if (state == S_PAUSE)
        {
            updatePause(in);
            in.endFrame();
            return;
        }
        if (state == S_INVENTORY)
        {
            // `*` drinks a health drink - the one inventory action that is not
            // just "close the panel" (GBA/Play Novel muscle memory expects it).
            if (in.tapped(Input.STAR) && player.healthDrinks > 0 && player.hp < player.maxHp)
            {
                player.healthDrinks--;
                player.hp += 40;
                if (player.hp > player.maxHp) player.hp = player.maxHp;
                Sfx.play(Sfx.PICKUP, 70);
            }
            else if (in.tapped(Input.SOFT_LEFT) || in.tapped(Input.A)
                     || in.tapped(Input.SOFT_RIGHT))
            {
                Sfx.play(Sfx.MENU, 60);
                state = S_PLAY;
            }
            in.endFrame();
            return;
        }
        if (state == S_MESSAGE)
        {
            messageTimer--;
            if (in.tapped(Input.A) || in.tapped(Input.SOFT_RIGHT) || messageTimer <= 0)
            {
                state = S_PLAY;
            }
            in.endFrame();
            return;
        }
        if (state == S_DEAD)
        {
            if (in.tapped(Input.A) || in.tapped(Input.SOFT_RIGHT))
            {
                loadLevel(levelId);
                state = S_PLAY;
            }
            in.endFrame();
            return;
        }

        // ---- playing
        updatePlay(in);
        in.endFrame();
    }

    void updateTitle(Input in)
    {
        if (in.tapped(Input.A) || in.tapped(Input.SOFT_RIGHT))
        {
            Sfx.play(Sfx.MENU, 70);
            loadLevel(Levels.PROLOGUE);
            state = S_NOVEL;
            novel.start(Novel.INTRO, this);
        }
    }

    void updatePlay(Input in)
    {
        frames++;
        if (--fpsTimer <= 0)
        {
            fps = fpsAccum;
            fpsAccum = 0;
            fpsTimer = 20;
        }
        fpsAccum++;

        // ---- global buttons
        if (in.tapped(Input.POUND))
        {
            Sfx.play(Sfx.MENU, 60);
            mapPanel.open(this);
            state = S_MAP;
            return;
        }
        if (in.tapped(Input.SOFT_LEFT))
        {
            Sfx.play(Sfx.MENU, 60);
            state = S_INVENTORY;
            return;
        }
        if (state == S_INVENTORY && in.tapped(Input.STAR) && player.healthDrinks > 0)
        {
            player.healthDrinks--;
            player.heal(40);
            Sfx.play(Sfx.PICKUP, 80);
        }
        if (in.tapped(Input.SOFT_RIGHT))
        {
            Sfx.play(Sfx.MENU, 60);
            openPause();
            return;
        }
        if (in.tapped(Input.NUM0))
        {
            player.flashlight = !player.flashlight;
            Sfx.play(Sfx.MENU, 40);
        }
        if (in.tapped(Input.STAR))
        {
            if (player.hasGun || player.hasKnife)
            {
                player.aiming = !player.aiming;
                Sfx.play(Sfx.MENU, 40);
            }
        }
        // Zoom.  The projection is orthographic, so pulling the camera back does
        // not change the size of anything: the zoom key has to move `fov`.
        if (in.tapped(Input.K7))
        {
            if (fov > 20) fov -= 4;
        }
        if (in.tapped(Input.K9))
        {
            if (fov < 96) fov += 4;
        }

        // ---- camera orbit (1, 3) - horizontal, (7, 9) - zoom, above
        int orbit = 0;
        if (in.isDown(Input.K1) || (!tankControls && in.isDown(Input.LEFT) && player.aiming)) orbit -= 96;
        if (in.isDown(Input.K3) || (!tankControls && in.isDown(Input.RIGHT) && player.aiming)) orbit += 96;
        camYaw = (camYaw + orbit) & 4095;

        // ---- movement
        int dx = 0, dz = 0;
        boolean run = false;
        boolean aim = player.aiming;

        if (tankControls)
        {
            // 2 forward, 8 back, 1/3 strafe, 4/6 rotate the camera (handled above
            // with the orbit keys in this mode)
            if (in.isDown(Input.LEFT)) camYaw = (camYaw - 96) & 4095;
            if (in.isDown(Input.RIGHT)) camYaw = (camYaw + 96) & 4095;
            if (in.isDown(Input.UP))
            {
                dx = Core.sin(player.angle) >> 8;
                dz = Core.cos(player.angle) >> 8;
                run = in.doubleTap;
            }
            if (in.isDown(Input.DOWN))
            {
                dx = -Core.sin(player.angle) >> 8;
                dz = -Core.cos(player.angle) >> 8;
            }
            if (in.isDown(Input.K1))
            {
                dx = Core.sin(player.angle - 1024) >> 8;
                dz = Core.cos(player.angle - 1024) >> 8;
            }
            if (in.isDown(Input.K3))
            {
                dx = Core.sin(player.angle + 1024) >> 8;
                dz = Core.cos(player.angle + 1024) >> 8;
            }
        }
        else
        {
            // 8-way, camera relative
            if (in.isDown(Input.UP)) { dx += Core.sin(camYaw) >> 8; dz += Core.cos(camYaw) >> 8; }
            if (in.isDown(Input.DOWN)) { dx -= Core.sin(camYaw) >> 8; dz -= Core.cos(camYaw) >> 8; }
            if (in.isDown(Input.LEFT)) { dx -= Core.cos(camYaw) >> 8; dz += Core.sin(camYaw) >> 8; }
            if (in.isDown(Input.RIGHT)) { dx += Core.cos(camYaw) >> 8; dz -= Core.sin(camYaw) >> 8; }
            if (in.isDown(Input.UP) || in.isDown(Input.DOWN) ||
                in.isDown(Input.LEFT) || in.isDown(Input.RIGHT))
            {
                run = in.doubleTap;
            }
        }

        player.update(this, dx, dz, run, aim);
        player.godMode = godMode;

        // ---- actions
        if (in.tapped(Input.A))
        {
            doAction();
        }

        // ---- enemies.  Dead-but-still-dying enemies are inactive, so update
        //      every enemy that is not fully gone: killing one has to play its
        //      death animation instead of freezing it mid-pose.
        for (int i = 0; i < enemies.length; i++) enemies[i].update(this);

        // ---- the radio: static gets louder as something approaches
        updateRadio();

        // ---- triggers
        checkTriggers();

        // ---- death
        if (!player.alive && state == S_PLAY)
        {
            Music.stop();
            Sfx.play(Sfx.HURT, 100);
            state = S_DEAD;
        }

        // ---- ambient siren now and then in the street
        if (levelId == Levels.PROLOGUE && (frames % 900) == 0 && !player.flashlight)
        {
            Sfx.play(Sfx.SIREN, 25);
        }
    }

    void updateRadio()
    {
        if (!player.hasRadio) return;
        int nearest = 99999;
        for (int i = 0; i < enemies.length; i++)
        {
            Enemy e = enemies[i];
            if (!e.active) continue;
            int d = Core.dist2d(e.x - player.x, e.z - player.z);
            if (d < nearest) nearest = d;
        }
        if (nearest < 1400)
        {
            int v = 100 - (nearest * 70) / 1400;
            Sfx.radioStatic(true, v);
        }
        else
        {
            Sfx.radioStatic(false, 0);
        }
    }

    void checkTriggers()
    {
        for (int i = 0; i < Band.ntrig; i++)
        {
            if (Band.trigDone[i]) continue;
            int o = i * 7;
            int x0 = Band.trigData[o], z0 = Band.trigData[o + 1];
            int x1 = Band.trigData[o + 2], z1 = Band.trigData[o + 3];
            if (player.x < x0 || player.x > x1 || player.z < z0 || player.z > z1) continue;
            int kind = Band.trigData[o + 4];
            int id = Band.trigData[o + 5];
            int msg = Band.trigData[o + 6];
            if (kind == 3)
            {
                Band.trigDone[i] = true;
                startNovel(id);
                return;
            }
            if (kind == 4)
            {
                Band.trigDone[i] = true;
                spawnEnemy(id);
                return;
            }
            if (kind == 2)
            {
                // doors need the action button, they are not automatic
                continue;
            }
            // kind 0: message / interaction
            if (msg > 0)
            {
                Band.trigDone[i] = true;
                showMessage(msg);
                return;
            }
        }
    }

    void doAction()
    {
        if (player.attackTimer > 0) return;

        // 1) interact with a door / item in front of Harry
        int best = -1, bestD = 1 << 30;
        for (int i = 0; i < Band.ntrig; i++)
        {
            int o = i * 7;
            if (Band.trigData[o + 4] != 2) continue;
            int x0 = Band.trigData[o], z0 = Band.trigData[o + 1];
            int x1 = Band.trigData[o + 2], z1 = Band.trigData[o + 3];
            // nearest edge of the trigger, so a door is reachable from both sides
            int dx = player.x < x0 ? x0 - player.x : (player.x > x1 ? player.x - x1 : 0);
            int dz = player.z < z0 ? z0 - player.z : (player.z > z1 ? player.z - z1 : 0);
            int d = dx + dz;
            if (d > 60) continue;
            if (d < bestD) { bestD = d; best = i; }
        }
        if (best >= 0)
        {
            int o = best * 7;
            int door = Band.trigData[o + 5];
            Sfx.play(Sfx.DOOR, 80);
            if (door == 1)
            {
                if (!player.hasMap && levelId == Levels.PROLOGUE)
                {
                    showMessage(8);
                    return;
                }
                goTo(Levels.CAFE);
                return;
            }
            if (door == 0)
            {
                goTo(Levels.PROLOGUE);
                return;
            }
        }

        // 2) pick up an item within reach
        for (int bi = 0; bi < level.nbands; bi++)
        {
            Band b = level.bands[bi];
            if (b == null) continue;
            for (int i = 0; i < b.ni; i++)
            {
                if (b.taken[i]) continue;
                int o = i * 5;
                int d = Core.dist2d(b.it[o + 1] - player.x, b.it[o + 3] - player.z);
                if (d > 70) continue;
                b.taken[i] = true;
                pickUp(b.it[o], b.it[o + 4]);
                return;
            }
        }

        // 3) attack
        if (player.aiming && player.hasGun)
        {
            shoot();
            return;
        }
        int target = -1;
        int targetD = 9999;
        for (int i = 0; i < enemies.length; i++)
        {
            Enemy e = enemies[i];
            if (!e.active) continue;
            int d = e.hitTest(player);
            if (d < targetD) { targetD = d; target = i; }
        }
        if (target >= 0 && targetD < 200)
        {
            player.attackTimer = 12;
            Sfx.play(Sfx.HIT, 80);
            enemies[target].hurt(player.hasKnife ? 12 : 6);
            return;
        }
        // 4) just looked at something
        player.attackTimer = 8;
    }

    void shoot()
    {
        if (player.ammo <= 0)
        {
            Sfx.play(Sfx.MENU, 30);
            showMessage(9);
            return;
        }
        player.ammo--;
        player.attackTimer = 14;
        Sfx.play(Sfx.SHOT, 100);
        int best = -1, bestD = 9999;
        for (int i = 0; i < enemies.length; i++)
        {
            Enemy e = enemies[i];
            if (!e.active) continue;
            int d = e.hitTest(player);
            if (d < bestD) { bestD = d; best = i; }
        }
        if (best >= 0)
        {
            enemies[best].hurt(14);
        }
    }

    void pickUp(int tex, int msg)
    {
        Sfx.play(Sfx.PICKUP, 90);
        if (tex == Tex.KNIFE) { player.hasKnife = true; Music.queue(Music.CHASE, 60); }
        else if (tex == Tex.FLASHLIGHT) { player.hasFlashlightOn(); }
        else if (tex == Tex.PAPER) { player.hasMap = true; }
        else if (tex == Tex.RADIO) { player.hasRadio = true; }
        else if (tex == Tex.CAN) { player.healthDrinks++; player.heal(25); }
        if (msg > 0) showMessage(msg);
        if (levelId == Levels.CAFE && player.hasKnife && player.hasMap && !screamIntroDone)
        {
            spawnEnemy(2);
        }
    }

    void spawnEnemy(int id)
    {
        for (int i = 0; i < enemies.length; i++)
        {
            Enemy e = enemies[i];
            if (e.active) continue;
            // Spawn in front of Harry on the open floor: a fixed offset from the
            // player regularly lands inside a wall or a booth.
            int fx = Core.sin(camYaw);
            int fz = Core.cos(camYaw);
            int dist = id == 1 ? 420 : 340;
            int sx = player.x + ((fx * dist) >> 12);
            int sz = player.z + ((fz * dist) >> 12);
            int[] p = tmpPos;
            if (isBlocked(sx, sz, 26))
            {
                p[0] = sx; p[2] = sz;
                resolve(p, 26);
                sx = p[0];
                sz = p[2];
            }
            e.spawn(Enemy.KIND_SCREAMER, sx, sz);
            if (id != 1) screamIntroDone = true;
            Music.play(Music.CHASE);
            Sfx.play(Sfx.SIREN, 40);
            return;
        }
    }

    public void onEnemyKilled(Enemy e)
    {
        if (levelId == Levels.CAFE)
        {
            Music.play(Music.CAFE);
        }
        else
        {
            Music.play(Music.THEME);
        }
    }

    void startNovel(int id)
    {
        if (id == 1 && introDone) return;
        state = S_NOVEL;
        novel.start(id, this);
        Music.pause();
    }

    void afterNovel(int id)
    {
        Music.resume();
        if (id == Novel.INTRO)
        {
            introDone = true;
            player.hasMap = true;
            showMessage(20);
            Music.play(Music.THEME);
        }
        else if (id == Novel.CYBIL)
        {
            cafeCutsceneDone = true;
            Music.play(Music.CAFE);
        }
        else if (id == Novel.SCREAMER)
        {
            // Cybil runs out, the fight starts
            showCybil = false;
            Music.play(Music.CHASE);
        }
    }

    void showMessage(int id)
    {
        String t = null;
        if (id > 0 && id < messages.length) t = messages[id];
        if (t == null) return;
        messageId = id;
        messageText = t;
        state = S_MESSAGE;
        messageTimer = 60 + t.length() * 2;
        Sfx.play(Sfx.MENU, 40);
    }

    public void goTo(int lvl)
    {
        pendingLevel = lvl;
        transition = 14;
        transitionOut = true;
        Music.stop();
        Sfx.stopAll();
    }

    // ------------------------------------------------------------------- menus
    /** Live values for the pause menu's "LABEL: value" rows. */
    private final String[] pauseValues = new String[Texts.PAUSE.length];

    void refreshPauseValues()
    {
        pauseValues[3] = objMode == ss.core.Render3D.MODE_TEX ? "TEX" : "FLAT";
        pauseValues[4] = tankControls ? "TANK" : "CAM";
        pauseValues[6] = Music.isEnabled() ? "ON" : "OFF";
    }

    void openPause()
    {
        refreshPauseValues();
        menu.reset(Texts.PAUSE, pauseValues);
        menu.sel = 0;
        state = S_PAUSE;
        Music.pause();
        Sfx.radioStatic(false, 0);
    }

    void updatePause(Input in)
    {
        int r = menu.update(in);
        if (r == ss.ui.Menu.CANCELLED)
        {
            state = S_PLAY;
            Music.resume();
            return;
        }
        if (r < 0) return;
        if (state == S_DEV)
        {
            devAction(r);
            return;
        }
        switch (r)
        {
            case 0:
                state = S_PLAY;
                Music.resume();
                break;
            case 1:
                state = S_INVENTORY;
                break;
            case 2:
                mapPanel.open(this);
                state = S_MAP;
                break;
            case 3:
                objMode = (objMode + 1) % 2;
                refreshPauseValues();
                break;
            case 4:
                tankControls = !tankControls;
                refreshPauseValues();
                break;
            case 5:
                devMode = !devMode;
                break;
            case 6:
                // options: music / sfx toggles
                Music.setEnabled(!Music.isEnabled());
                Sfx.setEnabled(Music.isEnabled());
                refreshPauseValues();
                break;
            case 7:
                state = S_DEV;
                devMenu.reset(Texts.DEV);
                break;
            case 8:
                loadLevel(Levels.PROLOGUE);
                state = S_PLAY;
                Music.resume();
                break;
            case 9:
                loadLevel(Levels.CAFE);
                state = S_PLAY;
                Music.resume();
                break;
            default:
                break;
        }
    }

    void devAction(int r)
    {
        switch (r)
        {
            case 0: showFps = !showFps; break;
            case 1: showFps = !showFps; break;      // heap + coords share the flag
            case 2: godMode = !godMode; break;
            case 3: player.flashlight = !player.flashlight; break;
            case 4: loadLevel(Levels.PROLOGUE); state = S_PLAY; break;
            case 5: loadLevel(Levels.CAFE); state = S_PLAY; break;
            case 6: state = S_PAUSE; break;
            default: break;
        }
    }

    // ------------------------------------------------------------------- draw
    public void draw(Fb fb)
    {
        switch (state)
        {
            case S_TITLE:
                drawTitle(fb);
                break;
            case S_NOVEL:
                novel.draw(fb);
                break;
            case S_MAP:
                Scene.render(this, fb);
                mapPanel.draw(fb, this);
                break;
            case S_PAUSE:
            case S_DEV:
                Scene.render(this, fb);
                fb.fadeTo(0x0000, 110);
                if (state == S_DEV) devMenu.draw(fb, Texts.DEV_TITLE);
                else menu.draw(fb, Texts.PAUSE_TITLE);
                break;
            case S_INVENTORY:
                drawInventory(fb);
                break;
            case S_MESSAGE:
                Scene.render(this, fb);
                drawMessage(fb);
                break;
            case S_DEAD:
                Scene.render(this, fb);
                fb.fadeTo(0x6000, 60);
                center(fb, "YOU ARE DEAD", fb.h / 2 - 8, 0xE71C);
                center(fb, "PRESS 5", fb.h / 2 + 8, 0xC618);
                break;
            default:
                Scene.render(this, fb);
                drawHud(fb);
                if (devMode) drawDev(fb);
                break;
        }
        if (transition > 0)
        {
            int f = transitionOut ? (14 - transition) * 16 : transition * 16;
            fb.fadeTo(0x0000, f);
        }
    }

    void drawTitle(Fb fb)
    {
        fb.vgrad(0x0861, 0x2965);
        // fog band at the bottom
        for (int y = fb.h * 2 / 3; y < fb.h; y++)
        {
            int k = (y - fb.h * 2 / 3) * 3;
            fb.hline(0, fb.w - 1, y, Core.mix565(0x2965, 0x4208, k));
        }
        center(fb, "SILENT", fb.h / 3 - 20, 0xEF7D);
        center(fb, "SLOP", fb.h / 3 + 2, 0xEF7D);
        center(fb, "J2ME PROTOTYPE", fb.h / 3 + 24, 0x8C71);
        center(fb, "PROLOGUE + 5 TO 2", fb.h * 2 / 3, 0xBDF7);
        if ((frames / 8) % 2 == 0) center(fb, "PRESS 5 TO START", fb.h - 34, 0xFFE0);
        center(fb, "ISOMETRIC / AFFINE / FOG", fb.h - 18, 0x6B4D);
        frames++;
    }

    void center(Fb fb, String s, int y, int col)
    {
        Font6.draw(fb, s, (fb.w - Font6.width(s)) / 2, y, col, 0x0000);
    }

    void drawHud(Fb fb)
    {
        // health bar, top left
        int w = 46, h = 5;
        fb.rect(3, 3, 3 + w + 1, 3 + h + 1, 0x0000);
        int hpw = (w * player.hp) / player.maxHp;
        fb.rect(4, 4, 4 + hpw, 4 + h, player.hp > 40 ? 0x2FE0 : 0xE000);
        Font6.draw(fb, "HP", 3 + w + 5, 2, 0xC618, 0x0000);
        if (player.hasGun)
        {
            Font6.draw(fb, "AMMO " + player.ammo, 3, 12, 0xC618, 0x0000);
        }
        if (player.flashlight)
        {
            Font6.draw(fb, "LIGHT", fb.w - 36, 2, 0xFFE0, 0x0000);
        }
        if (player.aiming)
        {
            int cx = fb.w / 2, cy = fb.h / 2;
            fb.hline(cx - 6, cx - 2, cy, 0xF800);
            fb.hline(cx + 2, cx + 6, cy, 0xF800);
            fb.hline(cx, cx, cy - 6, 0xF800);
            fb.hline(cx, cx, cy - 2, 0xF800);
        }
        // soft key hints, bottom
        Font6.draw(fb, player.hasMap ? "ITEMS" : "ITEMS", 2, fb.h - 9, 0x8410, 0x0000);
        String r = "PAUSE";
        Font6.draw(fb, r, fb.w - Font6.width(r) - 2, fb.h - 9, 0x8410, 0x0000);
        if (devMode) Font6.draw(fb, "DEV", fb.w / 2 - 9, fb.h - 9, 0xF800, 0x0000);
    }

    void drawDev(Fb fb)
    {
        int y = 22;
        Font6.draw(fb, "FPS " + fps, 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, "TRI " + Render3D.statsTris(), 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, "BND " + Render3D.statsBands() + "/" + level.nbands, 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, "HEAP " + heapKb() + "K", 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, "POS " + (player.x / LevelU) + "," + (player.z / LevelU), 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, "CAM " + ((camYaw * 360) / 4096), 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, objMode == 0 ? "OBJ TEX" : "OBJ FLAT", 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, tankControls ? "CTRL TANK" : "CTRL 8WAY", 3, y, 0x07E0, 0x0000); y += 9;
        Font6.draw(fb, godMode ? "GOD ON" : "GOD OFF", 3, y, godMode ? 0xF800 : 0x07E0, 0x0000);
    }

    static final int LevelU = 40;

    public static int heapKb()
    {
        Runtime r = Runtime.getRuntime();
        return (int) ((r.totalMemory() - r.freeMemory()) / 1024);
    }

    void drawInventory(Fb fb)
    {
        fb.clear(0x1082);
        center(fb, "ITEMS", 10, 0xEF7D);
        int y = 30;
        Font6.draw(fb, player.hasKnife ? "* KITCHEN KNIFE" : "- KITCHEN KNIFE", 8, y, 0xFFFF, 0); y += 11;
        Font6.draw(fb, player.hasGun ? "* HANDGUN " + player.ammo : "- HANDGUN", 8, y, 0xFFFF, 0); y += 11;
        Font6.draw(fb, player.flashlight ? "* FLASHLIGHT ON" : "* FLASHLIGHT OFF", 8, y, 0xFFFF, 0); y += 11;
        Font6.draw(fb, player.hasRadio ? "* POCKET RADIO" : "- POCKET RADIO", 8, y, 0xFFFF, 0); y += 11;
        Font6.draw(fb, player.hasMap ? "* RESIDENTIAL MAP" : "- MAP", 8, y, 0xFFFF, 0); y += 11;
        Font6.draw(fb, "HEALTH DRINKS " + player.healthDrinks, 8, y, 0x9E79, 0); y += 14;
        Font6.draw(fb, "HP " + player.hp + "/" + player.maxHp, 8, y, 0x9E79, 0);
        if (player.healthDrinks > 0)
        {
            Font6.draw(fb, "PRESS * TO DRINK", 8, y + 12, 0xFFE0, 0);
        }
        Font6.draw(fb, "PRESS 5 TO CLOSE", 8, fb.h - 12, 0x8410, 0);
    }

    void drawMessage(Fb fb)
    {
        int h = 52;
        int y0 = fb.h - h - 16;
        fb.rect(4, y0, fb.w - 5, fb.h - 17, 0x0000);
        fb.rect(5, y0 + 1, fb.w - 6, fb.h - 18, 0x0841);
        // simple word wrap
        String s = messageText == null ? "" : messageText;
        int line = 0, x = 8;
        int yy = y0 + 6;
        int i = 0;
        while (i < s.length() && line < 4)
        {
            int space = s.indexOf(' ', i);
            if (space < 0) space = s.length();
            int w = (space - i) * Font6.W;
            if (x + w > fb.w - 8)
            {
                line++;
                x = 8;
                yy += 10;
                if (line >= 4) break;
            }
            Font6.drawRaw(fb, s.substring(i, space), x, yy, 0xE71C);
            x += w + Font6.W;
            i = space + 1;
        }
    }
}
