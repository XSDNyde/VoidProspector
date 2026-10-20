#!/usr/bin/env bash
# Use a real display if supplied; otherwise a private virtual display for cloud checks.
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ -x .local/java/usr/lib/jvm/java-21-openjdk-amd64/bin/java ]]; then
  export PATH="$PWD/.local/java/usr/lib/jvm/java-21-openjdk-amd64/bin:$PATH"
fi
if [[ -n "${DISPLAY:-}" ]]; then exec "$@"; fi
xvfb="$(command -v Xvfb || true)"
[[ -n "$xvfb" ]] || xvfb="$PWD/.local/xvfb/usr/bin/Xvfb"
[[ -x "$xvfb" ]] || { echo 'A display or Xvfb is required. See README.md.'; exit 1; }
mkdir -p build
# -displayfd asks Xvfb to choose a free display, avoiding collisions with other tasks.
display_file="$(mktemp)"
"$xvfb" -displayfd 3 -screen 0 1400x900x24 -nolisten tcp -ac 3>"$display_file" >build/xvfb.log 2>&1 &
xvfb_pid=$!
cleanup() { kill "$xvfb_pid" 2>/dev/null || true; rm -f "$display_file"; }
trap cleanup EXIT INT TERM
for ((i=0;i<100;i++)); do
  [[ -s "$display_file" ]] && break
  kill -0 "$xvfb_pid" 2>/dev/null || { cat build/xvfb.log; exit 1; }
  sleep .05
done
[[ -s "$display_file" ]] || { echo 'Virtual display did not become ready'; exit 1; }
export DISPLAY=":$(cat "$display_file")"
"$@"
