package ss.ui;

import ss.core.Fb;
import ss.game.Input;
import ss.snd.Sfx;

/**
 * Vertical menu, the one UI widget the game needs (pause, dev, inventory is
 * drawn by hand because it is a list of items, not actions).
 */
public final class Menu
{
    public static final int CANCELLED = -2;
    public static final int NOTHING = -1;

    public String[] items;
    public int sel;
    public int top;

    public void reset(String[] items)
    {
        this.items = items;
        sel = 0;
        top = 0;
    }

    public int update(Input in)
    {
        if (items == null) return NOTHING;
        if (in.tapped(Input.UP) || (!in.isDown(Input.UP) && in.repeated(Input.UP))) {}
        if (in.repeated(Input.UP) && sel > 0)
        {
            sel--;
            Sfx.play(Sfx.MENU, 25);
        }
        else if (in.repeated(Input.DOWN) && sel < items.length - 1)
        {
            sel++;
            Sfx.play(Sfx.MENU, 25);
        }
        if (in.tapped(Input.SOFT_RIGHT) || in.tapped(Input.POUND)) return CANCELLED;
        if (in.tapped(Input.A)) return sel;
        return NOTHING;
    }

    public void draw(Fb fb, String title)
    {
        int w = fb.w - 40;
        int x = 20;
        int h = 16 + items.length * 11;
        int y = (fb.h - h) / 2;
        fb.rect(x, y, x + w, y + h, 0x0000);
        fb.rect(x + 1, y + 1, x + w - 1, y + h - 1, 0x1082);
        fb.rect(x + 1, y + 1, x + w - 1, y + 13, 0x18C3);
        Font6.draw(fb, title, x + 4, y + 3, 0xCE79, 0x0000);
        for (int i = 0; i < items.length; i++)
        {
            int yy = y + 16 + i * 11;
            if (i == sel)
            {
                fb.rect(x + 2, yy - 1, x + w - 2, yy + 7, 0x2945);
                Font6.draw(fb, ">", x + 3, yy, 0xFFE0, 0x0000);
            }
            String s = items[i];
            if (s != null && s.length() > 22) s = s.substring(0, 22);
            Font6.draw(fb, s, x + 12, yy, i == sel ? 0xFFFF : 0x9E79, 0x0000);
        }
    }
}
