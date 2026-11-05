import ss.core.*;
import ss.ui.Font6;
public class Wrap {
  static final String[] STREET = {
    "My car... too wrecked to drive. Where is Cheryl?",
    "The street is blocked. I can't leave town this way.",
    "A door. The sign says 5 TO 2. It's open.",
    "The fog is getting thicker. I can't see the end of the road.",
    "I need the map before I go in there.",
    "The radio is screaming. Something is close.",
    "The car is dead. Cheryl couldn't have gone far.",
    "A wheelchair, broken. What happened here?",
    "It's getting darker. The flashlight still works.",
    "I have the map now. The cafe is just ahead.",
    "The radio... is it broken? Nothing but static.",
    "This is not a dream. What's happening to this place!?",
  };
  public static void main(String[] a) {
    int w = 176, h = 208;
    System.out.println("--- messages: box is 52px tall, 3 rows of 10px, ~28 cols");
    for (String s : STREET) {
      int rows = Font6.wrapRows(s, w - 12);
      String flag = rows > 3 ? "  <-- CLIPPED" : "";
      System.out.println(rows + " rows | " + s + flag);
    }
    System.out.println("--- novel: box is 62px, 3 wrapped rows max");
    for (String s : STREET) {
      int rows = Font6.wrapRows(s, w - 8);
      if (rows > 3) System.out.println("CLIP " + rows + " | " + s);
    }
    System.out.println("--- hud labels (28 cols)");
    String[] hud = {"* KITCHEN KNIFE","* HANDGUN 12","* FLASHLIGHT ON","* POCKET RADIO",
      "* RESIDENTIAL MAP","HEALTH DRINKS 1","PRESS * TO DRINK","PRESS 5 TO CLOSE",
      "5 TO 2 - GROUND FLOOR","RESIDENTIAL AREA","# CLOSE   0 ZOOM   * FLOOR 1",
      "CAM 1234","ITEMS","PAUSE","OBJ FLAT","+ KITCHEN KNIFE"};
    for (String s : hud) {
      int px = Font6.width(s);
      System.out.println((px > 176 ? "TOO WIDE " : "ok       ") + px + " | " + s);
    }
  }
}
