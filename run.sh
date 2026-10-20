#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./tools/setup.sh
source ./tools/java-classpath.sh
exec java '-Dvoid.save=.local/save.properties' ${JAVA_OPTS:-} -cp "$game_classpath" VoidProspector "$@"
