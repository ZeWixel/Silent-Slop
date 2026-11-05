package ss.game;

import javax.microedition.lcdui.Canvas;

/**
 * Key decoding for Sony Ericsson / S60 / S40 keypads.
 *
 * The SE keypad reports digits as KEY_NUM0..9, so the whole control scheme is
 * expressed in those, plus the two soft keys which are reported as negative
 * key codes on most handsets (and as -6/-7 in Nokia's UI API).
 */
public final class Input
{
    // logical buttons
    public static final int NONE = 0;
    public static final int UP = 1;
    public static final int DOWN = 2;
    public static final int LEFT = 3;
    public static final int RIGHT = 4;
    public static final int A = 5;      // 5 / fire / confirm
    public static final int STAR = 6;
    public static final int POUND = 7;
    public static final int NUM0 = 8;
    public static final int K1 = 9;
    public static final int K3 = 10;
    public static final int K7 = 11;
    public static final int K9 = 12;
    public static final int SOFT_LEFT = 13;
    public static final int SOFT_RIGHT = 14;

    public static final int MAX = 15;

    public final boolean[] down = new boolean[MAX + 1];
    public final boolean[] pressed = new boolean[MAX + 1];
    public final boolean[] released = new boolean[MAX + 1];
    public final int[] repeat = new int[MAX + 1];
    public final int[] lastPress = new int[MAX + 1];

    public int frames;

    /** Double tap detection for "run". */
    public int lastDirPressed;
    public int lastDirFrame;
    public boolean doubleTap;

    /** Maps a MIDP key code to a logical button. */
    public static int map(int keyCode, int gameAction)
    {
        switch (keyCode)
        {
            case Canvas.KEY_NUM2: return UP;
            case Canvas.KEY_NUM8: return DOWN;
            case Canvas.KEY_NUM4: return LEFT;
            case Canvas.KEY_NUM6: return RIGHT;
            case Canvas.KEY_NUM5: return A;
            case Canvas.KEY_STAR: return STAR;
            case Canvas.KEY_POUND: return POUND;
            case Canvas.KEY_NUM0: return NUM0;
            case Canvas.KEY_NUM1: return K1;
            case Canvas.KEY_NUM3: return K3;
            case Canvas.KEY_NUM7: return K7;
            case Canvas.KEY_NUM9: return K9;
            case -6: return SOFT_LEFT;      // Nokia / SE soft keys
            case -7: return SOFT_RIGHT;
            case -5: return A;              // select
            case -1: return UP;             // jog dial / arrows on some SE
            case -2: return DOWN;
            case -3: return LEFT;
            case -4: return RIGHT;
            default: break;
        }
        switch (gameAction)
        {
            case Canvas.UP: return UP;
            case Canvas.DOWN: return DOWN;
            case Canvas.LEFT: return LEFT;
            case Canvas.RIGHT: return RIGHT;
            case Canvas.FIRE: return A;
            case Canvas.GAME_A: return SOFT_LEFT;
            case Canvas.GAME_B: return SOFT_RIGHT;
            default: return NONE;
        }
    }

    public void clear()
    {
        for (int i = 0; i <= MAX; i++)
        {
            down[i] = false;
            pressed[i] = false;
            released[i] = false;
        }
    }

    public void keyDown(int b)
    {
        if (b <= 0) return;
        if (!down[b])
        {
            down[b] = true;
            pressed[b] = true;
            repeat[b] = 0;
            lastPress[b] = frames;
            if (b == UP || b == DOWN || b == LEFT || b == RIGHT)
            {
                if (lastDirPressed == b && frames - lastDirFrame < 14)
                {
                    doubleTap = true;
                }
                lastDirPressed = b;
                lastDirFrame = frames;
            }
        }
    }

    public void keyUp(int b)
    {
        if (b <= 0) return;
        if (down[b])
        {
            down[b] = false;
            released[b] = true;
        }
    }

    public boolean isDown(int b)
    {
        return down[b];
    }

    public boolean tapped(int b)
    {
        return pressed[b];
    }

    public boolean letGo(int b)
    {
        return released[b];
    }

    /** Auto repeat for menus: true every 5 frames while held. */
    public boolean repeated(int b)
    {
        if (!down[b]) return false;
        if (pressed[b]) return true;
        repeat[b]++;
        if (repeat[b] > 8)
        {
            repeat[b] = 5;
            return true;
        }
        return false;
    }

    /** Called at the end of every frame. */
    public void endFrame()
    {
        frames++;
        for (int i = 0; i <= MAX; i++)
        {
            pressed[i] = false;
            released[i] = false;
        }
        if (doubleTap && frames - lastDirFrame > 14) doubleTap = false;
    }
}
