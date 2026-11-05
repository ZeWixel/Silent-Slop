package com.nokia.mid.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Graphics;

/** Compile-time stub of the Nokia UI API FullCanvas. */
public abstract class FullCanvas extends Canvas
{
    public static final int KEY_SOFTKEY1 = -6;
    public static final int KEY_SOFTKEY2 = -7;
    public static final int KEY_SOFTKEY3 = -5;
    public static final int KEY_UP_ARROW    = -1;
    public static final int KEY_DOWN_ARROW  = -2;
    public static final int KEY_LEFT_ARROW  = -3;
    public static final int KEY_RIGHT_ARROW = -4;

    protected FullCanvas() {}

    protected abstract void paint(Graphics g);
    public void addCommand(javax.microedition.lcdui.Command c) {}
    public void setCommandListener(CommandListener l) {}
}
