#!/bin/sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"
if [ -z "${JAVA_HOME:-}" ]; then
    if [ -x /usr/libexec/java_home ]; then
        JAVA_HOME=$(/usr/libexec/java_home)
        export JAVA_HOME
    else
        echo 'Set JAVA_HOME to a JDK 17 or newer.' >&2
        exit 2
    fi
fi
export PATH="$JAVA_HOME/bin:$PATH"
JTREG=${JTREG:-jtreg}
if ! command -v "$JTREG" >/dev/null 2>&1; then
    echo 'Set JTREG to the jtreg executable or add jtreg to PATH.' >&2
    exit 2
fi
mkdir -p work
HAS_TEST_TARGET=false
for ARGUMENT in "$@"; do
    case "$ARGUMENT" in
        -*) ;;
        *) HAS_TEST_TARGET=true ;;
    esac
done
if [ "$HAS_TEST_TARGET" = false ]; then
    set -- "$@" suite
fi
"$JTREG" -jdk:"$JAVA_HOME" -concurrency:1 -verbose:summary \
    -w:work/jtreg -r:work/jtreport "$@"
