#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
command -v java >/dev/null || { echo 'Java 21+ is required (JRE is sufficient).'; exit 1; }
mkdir -p lib tools build/classes
fetch() {
  local url="$1" dest="$2" expected="$3"
  if [[ -f "$dest" ]] && printf '%s  %s\n' "$expected" "$dest" | sha256sum --check --status; then return; fi
  curl --fail --location --retry 2 --proto '=https' "$url" -o "$dest.tmp"
  printf '%s  %s\n' "$expected" "$dest.tmp" | sha256sum --check --status || { rm -f "$dest.tmp"; echo "Checksum failed: $dest"; exit 1; }
  mv "$dest.tmp" "$dest"
}
fetch https://repo.maven.apache.org/maven2/org/processing/core/4.3.1/core-4.3.1.jar lib/core-4.3.1.jar 245f454ceb89c2cd03bc9b1ea85159769495279af8293ffab688f330c6a80ce1
fetch https://repo.maven.apache.org/maven2/org/eclipse/jdt/ecj/3.40.0/ecj-3.40.0.jar tools/ecj-3.40.0.jar 05cc22a24e7982970f63a405fc6c820bc80b806f27f3c5a6236fc475f8f7152b
java -jar tools/ecj-3.40.0.jar -21 -cp lib/core-4.3.1.jar -d build/classes src/*.java
printf 'Processing Java demo compiled. Run ./run.sh\n'
