package ss.game;

/**
 * Message strings, indexed exactly like the original map message tables from
 * silent-hill-decomp where the text is reused from the game, and written fresh
 * where the demo/port needed a new line.  Kept short: 176 px wide screens fit
 * ~28 characters per line and the message box shows four lines.
 */
public final class Texts
{
    private Texts() {}

    // map0_s00 (street)
    public static final String[] STREET = {
        "",                                                       // 0
        "My car... too wrecked to drive. Where is Cheryl?",       // 1
        "The street is blocked. I can't leave town this way.",    // 2
        "",                                                       // 3
        "A door. The sign says 5 TO 2. It's open.",                // 4
        "A dead end. Just a fence and the fog.",                   // 5
        "A health drink. Better than nothing.",                    // 6
        "The fog is getting thicker. I can't see the end of the road.", // 7
        "I need the map before I go in there.",                    // 8
        "No bullets left.",                                       // 9
        "Nothing here.",                                          // 10
        "It's locked.",                                            // 11
        "The radio is screaming. Something is close.",             // 12
        "It's quiet. Too quiet.",                                  // 13
        "Cheryl? Is that you?",                                     // 14
        "The car is dead. Cheryl couldn't have gone far.",         // 15
        "A wheelchair, broken. What happened here?",               // 16
        "I'd better follow the road.",                             // 17
        "The sign reads: 5 TO 2. A cafe.",                         // 18
        "It's getting darker. The flashlight still works.",        // 19
        "I have the map now. The cafe is just ahead.",             // 20
        "Something moved in the fog.",                             // 21
        "I can't go that way.",                                    // 22
    };

    // map0_s01 (cafe)
    public static final String[] CAFE = {
        "",                                                        // 0
        "",                                                        // 1
        "",                                                        // 2
        "",                                                        // 3
        "",                                                        // 4
        "The counter. Cold coffee, no one behind it.",             // 5
        "A pinball machine. Nothing unusual.",                     // 6
        "Can't turn on the TV. No power.",                         // 7
        "",                                                        // 8
        "",                                                        // 9
        "There is a kitchen knife. Take it?",                      // 10
        "There is a flashlight. Take it?",                         // 11
        "There is a residential map. Take it?",                    // 12
        "There is a pocket radio. Take it?",                       // 13
        "A health drink. I'll need it.",                           // 14
        "A note: CLOSED UNTIL FURTHER NOTICE.",                    // 15
        "I'd better check for something useful before leaving.",   // 16
        "The radio... is it broken? Nothing but static.",          // 17
        "This is not a dream. What's happening to this place!?",   // 18
        "I've got to beat that monster!",                          // 19
        "The radio may be useful for something.",                  // 20
        "I better take it with me.",                               // 21
    };

    public static final String[] PAUSE = {
        "RESUME",
        "ITEMS",
        "MAP",
        "OBJECT RENDER: " ,
        "CONTROLS: ",
        "DEVELOPER MODE",
        "SOUND",
        "DEV MENU",
        "LOAD: PROLOGUE",
        "LOAD: 5 TO 2"
    };

    public static final String PAUSE_TITLE = "PAUSE";

    public static final String[] DEV = {
        "FPS / STATS",
        "HEAP + COORDS",
        "GOD MODE",
        "FLASHLIGHT",
        "LEVEL: PROLOGUE",
        "LEVEL: CAFE",
        "BACK"
    };

    public static final String DEV_TITLE = "DEVELOPER";
}
