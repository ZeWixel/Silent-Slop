package ss.core;

/** Tiny linear congruential generator - no java.util.Random needed in the hot path. */
public final class Rng
{
    private int s;

    public Rng(int seed)
    {
        s = seed == 0 ? 123456789 : seed;
    }

    public void seed(int v)
    {
        s = v == 0 ? 123456789 : v;
    }

    public int next()
    {
        s = s * 1103515245 + 12345;
        return (s >>> 8) & 0x7FFFFF;
    }

    /** 0 .. n-1 */
    public int range(int n)
    {
        if (n <= 0) return 0;
        return next() % n;
    }

    /** -n .. n */
    public int pm(int n)
    {
        return range(2 * n + 1) - n;
    }
}
