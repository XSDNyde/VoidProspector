#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./tools/setup.sh
source ./tools/java-classpath.sh
java -Djava.awt.headless=true -cp "$game_classpath" GameTests
