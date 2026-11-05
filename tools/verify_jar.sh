#!/bin/sh
# Smoke-tests the shipped JAR, not the build tree: unpacks releases/SilentSlop.jar,
# loads the MIDlet class, renders both levels and checks the MIDI resources.
# The javax.microedition stubs are compiled to a separate directory and put on the
# classpath last, exactly like a phone would provide them.
set -e
JAVA_HOME=${JAVA_HOME:-/tmp/tools_linux_jdk8}
ROOT=$(cd "$(dirname "$0")/.." && pwd)
TMP=${TMPDIR:-/tmp}/ssverify
rm -rf "$TMP"
mkdir -p "$TMP/jar" "$TMP/out" "$TMP/stubs"

JAR="$ROOT/releases/SilentSlop.jar"
[ -f "$JAR" ] || { echo "no $JAR - run build.sh first"; exit 1; }

(cd "$TMP/jar" && unzip -q "$JAR")

# the JAR must not carry compile-only stubs
if find "$TMP/jar" -path '*/javax/*' -o -path '*/com/nokia/*' | grep -q .; then
  echo "FAIL: compile-only stubs leaked into the JAR"
  exit 1
fi

(cd "$ROOT" && find stubs -name '*.java' > "$TMP/stubs.txt")
"$JAVA_HOME/bin/javac" -nowarn -d "$TMP/stubs" @"$TMP/stubs.txt"

cat > "$TMP/Harness.java" <<'EOF'
import ss.core.*; import ss.game.*;
public class Harness {
  public static void main(String[] a) throws Exception {
    System.out.println("MIDlet: " + Class.forName("ss.Midlet").getName());
    Game.initAssets();
    Game g = new Game(); Fb fb = new Fb(176,208);
    for (int id = 0; id < 2; id++) {
      g.loadLevel(id); g.state = Game.S_PLAY;
      for (int i = 0; i < 30; i++) { g.update(new Input()); g.draw(fb); }
      System.out.println("  level " + g.level.name + " ok, tris=" + Render3D.statsTris());
    }
    for (int i = 0; i < 4; i++) {
      String f = new String[]{"/m/t_theme.mid","/m/t_cafe.mid","/m/t_chase.mid","/m/t_siren.mid"}[i];
      java.io.InputStream in = ss.snd.Music.class.getResourceAsStream(f);
      if (in == null) { System.out.println("FAIL: missing " + f); System.exit(1); }
      in.close();
    }
    System.out.println("  all four MIDI tracks present");
    System.out.println("JAR OK");
  }
}
EOF

"$JAVA_HOME/bin/javac" -nowarn -cp "$TMP/jar" -d "$TMP/out" "$TMP/Harness.java"
"$JAVA_HOME/bin/java" -cp "$TMP/jar:$TMP/out:$TMP/stubs" Harness
