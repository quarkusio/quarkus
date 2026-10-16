#!/bin/bash

# Build the whole integration-tests reactor NUM_RUNS times and check every produced
# target/quarkus-app is byte-identical across builds (diffoscope). CI only; needs diffoscope.

set -u -o pipefail

NUM_RUNS="${NUM_RUNS:-5}"
OUTPUT_DIR="${OUTPUT_DIR:-reproducibility-checks/$(date -u +%Y-%m-%dT%H-%M-%SZ)}"
EXTRA_MAVEN_ARGS="${EXTRA_MAVEN_ARGS:-}"
# Allows to use a custom diffoscope command for testing
DIFFOSCOPE="${DIFFOSCOPE:-diffoscope}"

command -v "${DIFFOSCOPE%% *}" >/dev/null || { echo "ERROR: '${DIFFOSCOPE%% *}' not found on PATH" >&2; exit 2; }

REF_DIR="$OUTPUT_DIR/reference"
DIFF_DIR="$OUTPUT_DIR/diffs"
REPORT="$OUTPUT_DIR/report.md"
MODULES="$OUTPUT_DIR/modules.txt"
FAILED="$OUTPUT_DIR/not-reproducible.txt"
mkdir -p "$REF_DIR" "$DIFF_DIR"
: > "$FAILED"

build() {
  ./mvnw -e -B --fail-at-end -T2C -f integration-tests/pom.xml \
    -DskipTests -Dquarkus.build.skip=false -Dno-build-cache \
    -Dproject.build.outputTimestamp=2024-01-01T00:00:00Z \
    $EXTRA_MAVEN_ARGS clean package
}

for ((run = 1; run <= NUM_RUNS; run++)); do
  echo "::group::Reactor build $run/$NUM_RUNS"
  build || echo "!! build $run reported failures (continuing)"
  echo "::endgroup::"

  if [[ "$run" -eq 1 ]]; then
    : > "$MODULES"
    while IFS= read -r app; do
      module="${app%/target/quarkus-app}"
      cp -a "$app" "$REF_DIR/${module//\//__}"
      echo "$module" >> "$MODULES"
    done < <(find integration-tests -type d -name quarkus-app | sort)
    echo ">> reference: $(wc -l < "$MODULES") modules"
    continue
  fi

  # Diff each reference module against this build. A vanished quarkus-app (build failed
  # this run) makes diffoscope error, which also counts as not reproducible.
  while IFS= read -r module; do
    grep -qxF "$module" "$FAILED" && continue
    safe="${module//\//__}"
    if ! $DIFFOSCOPE --exclude-directory-metadata=recursive --html "$DIFF_DIR/$safe.html" \
        "$REF_DIR/$safe" "$module/target/quarkus-app"; then
      echo "!! [$module] not reproducible"
      echo "$module" >> "$FAILED"
    fi
  done < "$MODULES"
done

n_failed=$(wc -l < "$FAILED" | tr -d ' ')
{
  echo "# Integration test reproducibility report"
  echo
  echo "- Reactor builds: \`$NUM_RUNS\`"
  echo "- Modules checked: \`$(wc -l < "$MODULES" | tr -d ' ')\`"
  echo "- Not reproducible: \`$n_failed\`"
  if [[ "$n_failed" -gt 0 ]]; then
    echo
    echo "## Not reproducible"
    echo
    while IFS= read -r m; do echo "- \`$m\`"; done < "$FAILED"
  fi
} > "$REPORT"

echo "Report written to $REPORT"
[[ "$n_failed" -eq 0 ]]
