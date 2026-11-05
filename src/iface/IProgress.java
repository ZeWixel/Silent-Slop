package iface;

/** Loading progress callback, kept as an interface so levels stay UI-free. */
public interface IProgress
{
    /** Called between build steps; implementations may show a bar. */
    void step(String what);

    /** True when the user asked to abort the load. */
    boolean cancelled();
}
