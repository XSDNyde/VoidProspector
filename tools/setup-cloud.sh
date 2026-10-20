#!/usr/bin/env bash
# Optional Debian cloud display prerequisites; installs only inside this checkout.
set -euo pipefail
cd "$(dirname "$0")/.."
./tools/setup.sh
java_home="$(java -XshowSettings:properties -version 2>&1 | sed -n 's/^ *java.home = //p')"
need_java=0; need_xvfb=0
if [[ ! -f "$java_home/lib/libawt_xawt.so" && ! -x .local/java/usr/lib/jvm/java-21-openjdk-amd64/bin/java ]]; then need_java=1; fi
if ! command -v Xvfb >/dev/null && [[ ! -x .local/xvfb/usr/bin/Xvfb ]]; then need_xvfb=1; fi
if (( need_java || need_xvfb )); then
  command -v apt-get >/dev/null || { echo 'Install a desktop Java 21 runtime and Xvfb for this platform.'; exit 1; }
  task_tmp="$(mktemp -d)"
  trap 'rm -rf "$task_tmp"' EXIT
  mkdir -p "$task_tmp/lists/partial" "$task_tmp/cache/archives/partial" "$task_tmp/empty" "$task_tmp/debs"
  printf 'Dir::Etc::parts "%s/empty";\nDir::Etc::main "/dev/null";\n' "$task_tmp" > "$task_tmp/apt.conf"
  printf 'deb https://deb.debian.org/debian trixie main\n' > "$task_tmp/sources.list"
  export APT_CONFIG="$task_tmp/apt.conf"
  apt_opts=(-o "Dir::Etc::sourcelist=$task_tmp/sources.list" -o "Dir::Etc::sourceparts=$task_tmp/empty" -o "Dir::State::Lists=$task_tmp/lists" -o "Dir::Cache=$task_tmp/cache")
  # Debian's installed archive keyring verifies signed repository metadata.
  apt-get "${apt_opts[@]}" update
  packages=()
  (( !need_java )) || packages+=(openjdk-21-jre openjdk-21-jre-headless)
  (( !need_xvfb )) || packages+=(xvfb)
  for package in "${packages[@]}"; do
    version="$(apt-cache "${apt_opts[@]}" madison "$package" | awk 'NR==1 {print $3}')"
    [[ -n "$version" ]] || { echo "No package candidate: $package"; exit 1; }
    (cd "$task_tmp/debs"; apt-get "${apt_opts[@]}" download "$package=$version")
  done
  mkdir -p .local/java .local/xvfb
  for package in "$task_tmp/debs/"*.deb; do
    case "$(basename "$package")" in
      openjdk*) dpkg-deb -x "$package" .local/java ;;
      xvfb*) dpkg-deb -x "$package" .local/xvfb ;;
    esac
  done
fi
printf 'Cloud display prerequisites available. Use ./tools/with-display.sh ./run.sh\n'
