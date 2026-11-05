import ss.core.Fb;
import ss.ui.Font6;
public class FontTest {
  public static void main(String[] a) {
    Fb fb = new Fb(120, 40);
    fb.clear(0);
    Font6.draw(fb, "HELLO", 2, 2, 0xFFFF, -1);
    Font6.draw(fb, "5 TO 2", 2, 12, 0xFFFF, -1);
    Font6.draw(fb, "abc123", 2, 22, 0xFFFF, -1);
    for (int y=0;y<40;y++){ StringBuilder sb=new StringBuilder();
      for(int x=0;x<120;x++) sb.append(fb.px[y*fb.w+x]!=0?'#':'.');
      System.out.println(sb);}
  }
}
