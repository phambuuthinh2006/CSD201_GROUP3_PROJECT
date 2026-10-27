#!/usr/bin/env bash
set -euo pipefail
csd_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)
cd "$csd_root"
csd_maven=${CSD_MAVEN_BIN:-}
if [[ -z "$csd_maven" ]]; then
  if command -v mvn >/dev/null 2>&1; then csd_maven=$(command -v mvn)
  else csd_maven="$HOME/.cache/csd-tools/apache-maven-3.9.9/bin/mvn"; fi
fi
[[ -x "$csd_maven" ]] || { echo 'Set CSD_MAVEN_BIN to your Maven executable.' >&2; exit 1; }
mkdir -p results
csd_output=$(mktemp -d "$csd_root/results/codec-comparison-$(date +%Y%m%d-%H%M%S)-XXXXXX")
printf '%s\n' "$csd_output" > /tmp/csd-latest-comparison-path
"$csd_maven" -q -DskipTests compile
mkdir -p target/codec-comparison
javac --release 8 -encoding UTF-8 -cp target/classes -d target/codec-comparison scripts/benchmark/CodecComparison.java
{
  date --iso-8601=seconds
  java -version
  uname -srmo
  lscpu
  free -m
  cat /proc/loadavg
  git log -1 --oneline
  git status --short
} > "$csd_output/environment.txt" 2>&1
sha256sum src/main/java/vn/csd/compression/codec/*.java src/main/java/vn/csd/compression/io/*.java \
  scripts/benchmark/CodecComparison.java scripts/benchmark/run-comparison.sh > "$csd_output/source-sha256.txt"
cp scripts/benchmark/CodecComparison.java scripts/benchmark/run-comparison.sh "$csd_output/"
for csd_fork in 1 2 3; do
  java -Xms512m -Xmx1g -XX:+UseG1GC -cp target/classes:target/codec-comparison \
    CodecComparison "$csd_output" "$csd_fork" | tee "$csd_output/fork-$csd_fork.log"
done
cat /proc/loadavg >> "$csd_output/environment.txt"
printf '\nResults: %s\n' "$csd_output"
