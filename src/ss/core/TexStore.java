package ss.core;

/**
 * Texture bank. Textures are loaded once from /t.bin (palettized, packed) and
 * live for the whole game; the map and the props share the same bank.
 */
public final class TexStore
{
    public static final int MAX = 128;
    private static Tex[] tex = new Tex[MAX];
    private static int count;

    private TexStore() {}

    public static void reset()
    {
        count = 0;
        for (int i = 0; i < MAX; i++) tex[i] = null;
    }

    public static Tex get(int id)
    {
        if (id < 0 || id >= count) return null;
        return tex[id];
    }

    public static int count()
    {
        return count;
    }

    public static void add(Tex t)
    {
        if (count < MAX) tex[count++] = t;
    }
}
