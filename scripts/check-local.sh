#!/bin/sh
# Compile against the assignment's minimum Java version and verify source integrity.
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
    JAVA_HOME=$(/usr/libexec/java_home)
    export JAVA_HOME
fi
JAVAC=${JAVA_HOME:+$JAVA_HOME/bin/}javac
JAVA=${JAVA_HOME:+$JAVA_HOME/bin/}java
mkdir -p work/classes
"$JAVAC" --release 17 -Xlint:all -d work/classes suite/lib/*.java suite/*.java
"$JAVA" -cp work/classes -Dtest.src=suite SourceIntegrityTest
