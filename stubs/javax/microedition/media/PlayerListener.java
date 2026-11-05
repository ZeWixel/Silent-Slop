package javax.microedition.media;

/** Compile-time stub of JSR-135 PlayerListener. */
public interface PlayerListener
{
    String STARTED   = "started";
    String STOPPED   = "stopped";
    String END_OF_MEDIA = "endOfMedia";
    String DURATION_UPDATED = "durationUpdated";
    String VOLUME_CHANGED = "volumeChanged";
    String ERROR     = "error";
    String CLOSED    = "closed";

    void playerUpdate(Player player, String event, Object data);
}
