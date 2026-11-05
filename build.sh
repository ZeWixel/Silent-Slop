#!/bin/sh
# Silent-Slop build: JDK 8 targeting Java 1.3 / CLDC 1.0 bytecode.
set -e
JAVA_HOME=${JAVA_HOME:-/tmp/tools_linux_jdk8}
JAVA="$JAVA_HOME/bin/java"
JAVAC="$JAVA_HOME/bin/javac"
PROGUARD=${PROGUARD:-/tmp/LLM-J2ME-SUITE/libs/proguard.jar}
# Newer JDKs (9+) dropped -source/-target 1.3, so the build falls back to the
# Eclipse batch compiler on the classpath, which still emits class file 45.3.
ECJ=${ECJ:-$JAVA_HOME/ecj.jar}

rm -rf build/classes build/ss_raw.jar
mkdir -p build/classes releases

find src stubs -name '*.java' > build/sources.txt
if [ -x "$JAVAC" ] && "$JAVAC" -version 2>&1 | grep -q '^1\.'; then
  "$JAVAC" -source 1.3 -target 1.3 -Xlint:-options -g:none -nowarn \
      -d build/classes @build/sources.txt
elif [ -f "$ECJ" ]; then
  "$JAVA" -cp "$ECJ" org.eclipse.jdt.internal.compiler.batch.Main \
      -1.3 -nowarn -g:none -d build/classes @build/sources.txt
else
  echo "need a JDK 8 javac or an Eclipse batch compiler at \$ECJ" >&2
  exit 1
fi

# Only the MIDI tracks are read at runtime.  res/ss_data.bin and the plan PNGs
# are assetgen output used by the tools; shipping them would add ~115 kB of dead
# weight to a MIDlet that gets ~700 kB of heap on an SE handset.
mkdir -p build/classes/m
cp res/m/*.mid build/classes/m/

# JAD + manifest: SE phones refuse to install a JAR whose MIDlet attributes are
# only in the manifest, and most launchers want the JAD next to it.
mkdir -p build/classes/META-INF
cat > build/classes/META-INF/MANIFEST.MF <<'MF'
Manifest-Version: 1.0
MIDlet-Name: Silent Slop
MIDlet-Vendor: Silent-Slop
MIDlet-Version: 1.0.0
MIDlet-1: Silent Slop, , ss.Midlet
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.0
MF
# the compile-only stubs must never ship: the phone provides these classes
rm -rf build/classes/javax build/classes/com
cat > build/MANIFEST.MF <<'MF'
Manifest-Version: 1.0
MIDlet-Name: Silent Slop
MIDlet-Vendor: Silent-Slop
MIDlet-Version: 1.0.0
MIDlet-1: Silent Slop, , ss.Midlet
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.0
MF
cat > releases/SilentSlop.jad <<'JAD'
MIDlet-Name: Silent Slop
MIDlet-Vendor: Silent-Slop
MIDlet-Version: 1.0.0
MIDlet-1: Silent Slop, , ss.Midlet
MIDlet-Jar-URL: SilentSlop.jar
MicroEdition-Profile: MIDP-2.0
MicroEdition-Configuration: CLDC-1.0
JAD


# stackmap pass so the jar runs on real KVM/hotspot phones
if [ -f "$PROGUARD" ]; then
  "$JAVA" -jar "$PROGUARD" \
    -injars build/classes -outjars build/ss_raw.jar \
    -dontshrink -dontoptimize -dontobfuscate \
    -target 1.3 -microedition \
    -dontwarn -dontnote -ignorewarnings \
    -keepparameternames >/dev/null 2>&1 || true
fi

if [ ! -f build/ss_raw.jar ]; then
  # zip(1) cannot inject a manifest, so the MIDlet attributes are written as an
  # ordinary entry: KVM reads META-INF/MANIFEST.MF from the archive either way.
  ( cd build/classes && \
    zip -q -r ../ss_raw.jar . -x 'javax/*' 'com/*' )
fi

# small JAR: strip nothing else, the resources are already minimal
cp build/ss_raw.jar releases/SilentSlop.jar
# JAR size has to agree with the JAD or the installer rejects it
SIZE=$(wc -c < releases/SilentSlop.jar)
echo "MIDlet-Jar-Size: $SIZE" >> releases/SilentSlop.jad
ls -l releases/SilentSlop.jar releases/SilentSlop.jad
