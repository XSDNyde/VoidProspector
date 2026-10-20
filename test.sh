#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
./tools/setup.sh
java -Djava.awt.headless=true -cp 'build/classes:lib/core-4.3.1.jar' GameTests
