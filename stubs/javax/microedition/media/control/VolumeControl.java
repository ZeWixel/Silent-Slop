package javax.microedition.media.control;

import javax.microedition.media.Control;

/** Compile-time stub of JSR-135 VolumeControl (implemented by players that support it). */
public interface VolumeControl extends Control
{
    int setLevel(int level);
    int getLevel();
    void setMute(boolean mute);
    boolean isMuted();
}
