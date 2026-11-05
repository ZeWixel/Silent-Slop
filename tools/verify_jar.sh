#!/bin/sh
# Smoke-tests the shipped JAR, not the build tree: unpacks releases/SilentSlop.jar,
# loads the MIDlet class, renders both levels and checks the MIDI resources.
# The javax.microedition stubs are compiled to a separate directory and put on the
# classpath last, exactly like a phone would provide them.
set -e
JAVA_HOME=${JAVA_HOME:-/tmp/tools_linux_jdk8}
JAVA="$JAVA_HOME/bin/java"
JAVAC="$JAVA_HOME/bin/javac"
ECJ=${ECJ:-$JAVA_HOME/ecj.jar}
# compile() picks a Java 8 javac when there is one, otherwise the Eclipse batch
# compiler from $ECJ (any JDK 9+ can run it and it still emits class file 45.3)
compile() {
  if [ -x "$JAVAC" ] && "$JAVAC" -version 2>&1 | grep -q '^1\.'; then
    "$JAVAC" -nowarn "$@"
  else
    "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main -nowarn "$@"
  fi
}
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
compile -d "$TMP/stubs" @"$TMP/stubs.txt"

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

compile -cp "$TMP/jar" -d "$TMP/out" "$TMP/Harness.java"

# Conformance: every javax.microedition reference in the JAR must exist in a
# real implementation.  A wrong package name in stubs/ (GameCanvas in
# javax.microedition.lcdui instead of ...lcdui.game) passes javac and then
# NoClassDefFoundErrors on the device - this catches it before shipping.
API=${J2ME_API_DIR:-/tmp/jl/J2ME-Loader-master/app/src/main/java}
if [ -d "$API" ]; then
  python3 "$ROOT/tools/conformance/check.py" "$TMP/jar" "$API" || exit 1
else
  echo "  (skipped API conformance: no J2ME sources at $API)"
fi

"$JAVA" -cp "$TMP/jar:$TMP/out:$TMP/stubs" Harness
