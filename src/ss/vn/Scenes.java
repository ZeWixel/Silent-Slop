package ss.vn;

/**
 * The cut-scene scripts.
 *
 * Written from the decompiled trigger tables: the intro is the opening of
 * map0_s00 (Harry wakes by the crashed car, finds no Cheryl, sees her walk into
 * the fog), the second is the cafe meeting with Cybil (map0_s01 state machine),
 * the third is the Air Screamer breaking through the window.
 */
public final class Scenes
{
    public static final class Beat
    {
        public final String[] lines;
        public final String speaker;
        public final int portrait;
        public final int effect;

        Beat(String speaker, int portrait, int effect, String[] lines)
        {
            this.speaker = speaker;
            this.portrait = portrait;
            this.effect = effect;
            this.lines = lines;
        }
    }

    private Scenes() {}

    // ---- opening of the game -------------------------------------------
    static final Beat[] INTRO = {
        new Beat("HARRY", 1, Novel.FX_NONE,
                 new String[]{ "Was I dreaming?", "", "", "" }),
        new Beat("HARRY", 1, Novel.FX_NONE,
                 new String[]{ "The car... It's wrecked.", "Cheryl?", "", "" }),
        new Beat("HARRY", 2, Novel.FX_NONE,
                 new String[]{ "Cheryl... Where could you be?", "", "", "" }),
        new Beat("HARRY", 2, Novel.FX_SIREN,
                 new String[]{ "It's strange...", "It's quiet. Too quiet.", "", "" }),
        new Beat("HARRY", 3, Novel.FX_NONE,
                 new String[]{ "This place is like a ghost town.", "", "", "" }),
        new Beat("HARRY", 3, Novel.FX_SHAKE,
                 new String[]{ "Footsteps?", "", "", "" }),
        new Beat("CHERYL", 2, Novel.FX_NONE,
                 new String[]{ "...", "", "", "" }),
        new Beat("HARRY", 2, Novel.FX_NONE,
                 new String[]{ "Cheryl? Is that Cheryl!?", "Where are you going?", "", "" }),
        new Beat("HARRY", 2, Novel.FX_NONE,
                 new String[]{ "Hey wait... stop!", "", "", "" }),
        new Beat("", 3, Novel.FX_NONE,
                 new String[]{ "I'd better follow Cheryl...", "", "", "" }),
        new Beat("", 3, Novel.FX_SIREN,
                 new String[]{ "That's strange, it's getting darker.", "", "", "" }),
        new Beat("", 3, Novel.FX_FLASH,
                 new String[]{ "A dead end? What the hell!?", "", "", "" }),
        new Beat("", 3, Novel.FX_NONE,
                 new String[]{ "The fog is everywhere.", "I can't see more than ten feet ahead.", "", "" }),
        new Beat("", 1, Novel.FX_NONE,
                 new String[]{ "There is a map in the car.", "I'd better take it with me.", "", "" }),
    };

    // ---- meeting Cybil in the cafe ---------------------------------------
    static final Beat[] CYBIL = {
        new Beat("", 4, Novel.FX_NONE,
                 new String[]{ "The cafe is empty.", "Coffee still warm in the pot.", "", "" }),
        new Beat("CYBIL", 5, Novel.FX_NONE,
                 new String[]{ "Hold it!", "Where do you think you're going?", "", "" }),
        new Beat("HARRY", 5, Novel.FX_NONE,
                 new String[]{ "My daughter...", "I've gotta find her!", "", "" }),
        new Beat("CYBIL", 5, Novel.FX_NONE,
                 new String[]{ "No way! It's dangerous out there.", "", "", "" }),
        new Beat("HARRY", 5, Novel.FX_NONE,
                 new String[]{ "In that case, I need to find her now!", "Cheryl's my little girl.", "", "" }),
        new Beat("CYBIL", 5, Novel.FX_NONE,
                 new String[]{ "Have you got a gun?", "", "", "" }),
        new Beat("HARRY", 5, Novel.FX_NONE,
                 new String[]{ "Ummm... no.", "", "", "" }),
        new Beat("CYBIL", 5, Novel.FX_NONE,
                 new String[]{ "Take this.", "And hope you don't have to use it.", "", "" }),
        new Beat("CYBIL", 5, Novel.FX_NONE,
                 new String[]{ "Before you pull the trigger,", "know who you're shooting.", "", "" }),
        new Beat("CYBIL", 5, Novel.FX_NONE,
                 new String[]{ "The phones are all dead, and the radio too.", "I'm going back for reinforcements.", "", "" }),
        new Beat("", 4, Novel.FX_NONE,
                 new String[]{ "Maybe I should check for something useful", "before leaving.", "", "" }),
        new Beat("", 6, Novel.FX_SHAKE,
                 new String[]{ "What's that?", "Huh? Radio?", "", "" }),
        new Beat("", 4, Novel.FX_NONE,
                 new String[]{ "There is a kitchen knife.", "There is a flashlight.", "There is a map.", "" }),
    };

    // ---- the Air Screamer ------------------------------------------------
    static final Beat[] SCREAMER = {
        new Beat("", 4, Novel.FX_NONE,
                 new String[]{ "A sound. Glass.", "", "", "" }),
        new Beat("", 7, Novel.FX_FLASH,
                 new String[]{ "Something came through the window!", "", "", "" }),
        new Beat("", 7, Novel.FX_SHAKE,
                 new String[]{ "This is not a dream!", "What's happening to this place!?", "", "" }),
        new Beat("", 7, Novel.FX_SIREN,
                 new String[]{ "I've got to beat that monster!", "", "", "" }),
    };

    static Beat[][] all = { null, INTRO, CYBIL, SCREAMER };

    public static Beat get(int scene, int beat)
    {
        if (scene < 1 || scene >= all.length) return null;
        Beat[] s = all[scene];
        if (beat < 0 || beat >= s.length) return null;
        return s[beat];
    }

    public static int length(int scene)
    {
        if (scene < 1 || scene >= all.length) return 0;
        return all[scene].length;
    }
}
