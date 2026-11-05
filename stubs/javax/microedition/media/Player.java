package javax.microedition.media;

import java.io.InputStream;

/** Compile-time stub of JSR-135 Player. */
public interface Player
{
    int UNREALIZED  = 100;
    int REALIZED    = 200;
    int PREFETCHED  = 300;
    int STARTED     = 400;
    int CLOSED      = 0;

    void realize() throws MediaException;
    void prefetch() throws MediaException;
    void start() throws MediaException;
    void stop() throws MediaException;
    void deallocate();
    void close();
    void setLoopCount(int count);
    void setMediaTime(long now) throws MediaException;
    long getMediaTime();
    int getState();
    long getDuration();
    String getContentType();
    void addPlayerListener(PlayerListener l);
    void removePlayerListener(PlayerListener l);
    Control getControl(String type);
    Control[] getControls();
}
