#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./tools/setup.sh
exec java "-Dvoid.save=$PWD/.local/save.properties" ${JAVA_OPTS:-} -cp 'build/classes:lib/core-4.3.1.jar' VoidProspector "$@"
