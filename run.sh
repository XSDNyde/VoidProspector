#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./tools/setup.sh
source ./tools/java-classpath.sh
case "${OSTYPE:-}" in
  msys*|cygwin*|win32*) launcher=javaw ;;
  *) launcher=java ;;
esac
exec "$launcher" '-Dvoid.save=.local/save.properties' ${JAVA_OPTS:-} -cp "$game_classpath" VoidProspector "$@"
