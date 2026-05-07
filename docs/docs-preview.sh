#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1

# Verify required commands before anything else
for cmd in awk curl rsync find stat mktemp grep sed sort head tail cut basename date seq tee; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $cmd"
    exit 1
  fi
done

# Keep LOGDIR for post-run debugging. The OS cleans /tmp on its own schedule.
LOGDIR=$(mktemp -d "${TMPDIR:-/tmp}/quarkus-docs-build-XXXXXX") || exit 1

# Two images pinned by digest: primary is a lightweight wrapper that auto-serves
# on port 4000; fallback is the official Jekyll image in case the primary is
# removed from Docker Hub (single-maintainer project).
JEKYLL_IMAGE="docker.io/bretfisher/jekyll-serve@sha256:db11b70736935b1a777b2ff2ae10f9ad191ee9fca6560eade1d5ad98b74e5f66"
JEKYLL_IMAGE_FALLBACK="docker.io/jekyll/jekyll@sha256:bb45414c3fefa80a75c5001f30baf1dff48ae31dc961b8b51003b93b60675334"

# Persistent marker from the previous successful preview run. Files newer
# than this marker are detected as the user's recent edits at Step 5.
# Stored outside docs/target/ because mvn clean deletes that directory.
PREVIEW_REF="$SCRIPTDIR/docs/.docs-preview-last-run"

# Step 0 — Environment detection
ENVSCRIPT="docs/detect-env.sh"
echo "=== Environment ==="
# shellcheck source=detect-env.sh
. "$ENVSCRIPT" || exit 1
echo ""

# QMVNCMD is trusted input from the user's environment (consistent with .justfile).
# Word splitting is intentional so users can pass a command with arguments.
MVNCMD="${QMVNCMD:-./mvnw $MVN_THREADS}"
MVNCMD_ST="${QMVNCMD:-./mvnw}"

# Pre-flight: check if ports 4000 or 35729 are already in use
port_in_use() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  # Fallback: any successful TCP connection means the port is occupied
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$1" 2>/dev/null
    return $?
  fi
  # Last resort: curl exit code 0 means TCP connection succeeded
  curl --max-time 1 -s -o /dev/null "http://127.0.0.1:$1" 2>/dev/null
}

if port_in_use 4000 || port_in_use 35729; then
  echo "Port 4000 or 35729 is already in use."
  echo "If a previous docs-preview is running: $CONTAINER_CMD rm -f quarkus-docs-preview"
  exit 1
fi

START_TOTAL=$(date +%s)

# Rootless Podman creates files with a mapped UID that the host user cannot
# delete. "podman unshare" enters that namespace to remove them. With Docker
# (which runs as root), the plain rm -rf is sufficient. Non-fatal cleanup.
if [ "$CONTAINER_CMD" = "podman" ]; then
  podman unshare rm -rf docs/.cache/ 2>/dev/null || rm -rf docs/.cache/ 2>/dev/null || true
else
  rm -rf docs/.cache/ 2>/dev/null || true
fi

# Step 1 — Root Build
echo "=== Step 1: Root Build ==="
STEP_START=$(date +%s)
# shellcheck disable=SC2086
$MVNCMD clean install -DquicklyDocs \
  -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true \
  > "$LOGDIR/step1-parallel.log" 2>&1
RC=$?
STEP_END=$(date +%s)
grep -E 'BUILD SUCCESS|BUILD FAILURE|Total time' "$LOGDIR/step1-parallel.log" | tail -3
echo "Step 1: $((STEP_END - STEP_START))s (exit $RC)"
if [ $RC -ne 0 ] && [ -n "$MVN_THREADS" ]; then
  echo "Parallel build failed, falling back to single-threaded..."
  STEP_START=$(date +%s)
  # shellcheck disable=SC2086
  $MVNCMD_ST clean install -DquicklyDocs \
    -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true \
    > "$LOGDIR/step1-fallback.log" 2>&1
  RC=$?
  STEP_END=$(date +%s)
  grep -E 'BUILD SUCCESS|BUILD FAILURE|Total time' "$LOGDIR/step1-fallback.log" | tail -3
  echo "Step 1 fallback: $((STEP_END - STEP_START))s (exit $RC)"
fi
[ $RC -ne 0 ] && echo "Root build failed. Check $LOGDIR/step1*.log" && exit 1

# Step 2 — Docs Rebuild
echo ""
echo "=== Step 2: Docs Rebuild ==="
STEP_START=$(date +%s)
cd docs || exit 1
../mvnw package > "$LOGDIR/step2.log" 2>&1
RC=$?
STEP_END=$(date +%s)
grep -E 'BUILD SUCCESS|BUILD FAILURE|Total time' "$LOGDIR/step2.log" | tail -3
echo "Step 2: $((STEP_END - STEP_START))s (exit $RC)"
[ $RC -ne 0 ] && echo "Docs rebuild failed. Check $LOGDIR/step2.log" && exit 1

# Step 3 — Sync
echo ""
echo "=== Step 3: Sync Web Site ==="
STEP_START=$(date +%s)

# Fast re-sync: copies built files into the website checkout without
# re-cloning the repo. Each critical command checks its exit code.
run_fast_sync() {
  rsync -r --delete \
      --exclude='**/*.html' --exclude='**/index.adoc' \
      --exclude='**/_attributes-local.adoc' --exclude='**/guides.md' \
      --exclude='**/_templates' \
      target/asciidoc/sources/ target/web-site/_versions/main/guides || return 1

  if [ -d target/quarkus-generated-doc/ ]; then
    rsync -r --delete \
        --exclude='**/*.html' --exclude='**/index.adoc' \
        --exclude='**/_attributes.adoc' \
        target/quarkus-generated-doc/ target/web-site/_generated-doc/main || return 1
  fi

  if [ -f target/indexByType.yaml ]; then
    mkdir -p target/web-site/_data/versioned/main/index || return 1
    {
      echo "# Generated file. Do not edit"
      cat target/indexByType.yaml
    } > target/web-site/_data/versioned/main/index/quarkus.yaml || return 1
  fi

  if [ -f target/relations.yaml ]; then
    mkdir -p target/web-site/_data/versioned/main/index || return 1
    {
      echo "# Generated file. Do not edit"
      cat target/relations.yaml
    } > target/web-site/_data/versioned/main/index/relations.yaml || return 1
  fi
}

# Helper: run sync-web-site.sh with visible progress via tee
run_full_sync() {
  local log="$1"
  ./sync-web-site.sh 2>&1 | tee "$log"
  [ "${PIPESTATUS[0]}" -eq 0 ]
}

if [ -d target/web-site/_versions ]; then
  # Validate that sync-web-site.sh output layout matches our expectations.
  SYNC_OK=true
  [ ! -d target/web-site/_versions/main/guides ] && SYNC_OK=false
  [ ! -f target/web-site/_config.yml ] && SYNC_OK=false
  [ ! -f target/web-site/_only_latest_guides_config.yml ] && SYNC_OK=false

  if [ "$SYNC_OK" = "true" ]; then
    echo "Website already cloned, using fast re-sync..."
    if ! run_fast_sync > "$LOGDIR/step3-fast-sync.log" 2>&1; then
      echo "Fast sync failed. Check $LOGDIR/step3-fast-sync.log"
      echo "Falling back to full sync..."
      rm -rf target/web-site || exit 1
      if ! run_full_sync "$LOGDIR/step3.log"; then
        echo "Sync failed. Check $LOGDIR/step3.log"
        exit 1
      fi
    fi
  else
    echo "WARNING: Website layout changed. Falling back to full sync..."
    rm -rf target/web-site || exit 1
    if ! run_full_sync "$LOGDIR/step3.log"; then
      echo "Sync failed. Check $LOGDIR/step3.log"
      exit 1
    fi
  fi
else
  echo "First run — cloning website repo (this takes ~4 min)..."
  if ! run_full_sync "$LOGDIR/step3.log"; then
    echo "Sync failed. Check $LOGDIR/step3.log"
    exit 1
  fi
fi
STEP_END=$(date +%s)
echo "Step 3: $((STEP_END - STEP_START))s"

# Step 4 — Serve
echo ""
echo "=== Step 4: Serve Locally ==="
$CONTAINER_CMD rm -f quarkus-docs-preview 2>/dev/null || true
cd target/web-site || exit 1
STEP_START=$(date +%s)

# Try primary image; fall back on pull failure or container crash.
# The run_log captures the container ID or CLI errors from "run -d".
# On failure, container logs (Jekyll output) are appended to the same file.
start_jekyll() {
  local image="$1" mount="$2" run_log="$3"
  if ! $CONTAINER_CMD run -d --name quarkus-docs-preview \
    -p 127.0.0.1:4000:4000 -p 127.0.0.1:35729:35729 \
    -v "$(pwd):${mount}${VOL_FLAG}" \
    -v quarkus-jekyll-bundles:/usr/local/bundle \
    "$image" \
    bundle exec jekyll serve --host 0.0.0.0 \
    --livereload --incremental \
    --config _config.yml,_config_dev.yml,_only_latest_guides_config.yml \
    > "$run_log" 2>&1; then
    return 1
  fi

  sleep 3
  if [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' quarkus-docs-preview 2>/dev/null)" != "true" ]; then
    $CONTAINER_CMD logs quarkus-docs-preview >> "$run_log" 2>&1
    return 1
  fi
}

PULL_OK=true
if ! $CONTAINER_CMD image inspect "$JEKYLL_IMAGE" > /dev/null 2>&1; then
  $CONTAINER_CMD pull "$JEKYLL_IMAGE" > /dev/null 2>&1 || PULL_OK=false
fi

STARTED=false
if [ "$PULL_OK" = "true" ]; then
  if start_jekyll "$JEKYLL_IMAGE" "/site" "$LOGDIR/step4-primary.log"; then
    STARTED=true
  else
    echo "Primary image failed to start. Check $LOGDIR/step4-primary.log"
    echo "Trying fallback..."
    $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
  fi
fi

if [ "$STARTED" != "true" ]; then
  echo "Using fallback image..."
  if ! start_jekyll "$JEKYLL_IMAGE_FALLBACK" "/srv/jekyll" "$LOGDIR/step4-fallback.log"; then
    echo "Fallback container also failed. Check $LOGDIR/step4-fallback.log"
    echo "Container logs:"
    $CONTAINER_CMD logs quarkus-docs-preview 2>&1 | tail -10
    $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
    exit 1
  fi
fi

echo "Waiting for Jekyll..."
for i in $(seq 1 150); do
  # Check if container is still running (catches delayed crashes)
  if [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' quarkus-docs-preview 2>/dev/null)" != "true" ]; then
    echo "Step 4: Container exited during startup. Logs:"
    $CONTAINER_CMD logs quarkus-docs-preview 2>&1 | tail -50 | tee "$LOGDIR/step4-crash.log"
    $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
    exit 1
  fi
  if [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:4000 2>/dev/null)" = "200" ]; then
    STEP_END=$(date +%s)
    echo "Step 4: Jekyll ready after $((STEP_END - STEP_START))s"
    break
  fi
  if [ "$i" -eq 150 ]; then
    echo "Step 4: Timeout after 300s. Logs:"
    $CONTAINER_CMD logs quarkus-docs-preview 2>&1 | tail -50
    $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
    exit 1
  fi
  sleep 2
done

# Step 5 — Detect content type and open browser
echo ""
END_TOTAL=$(date +%s)
echo "==============================="
echo "Total time: $((END_TOTAL - START_TOTAL))s"
echo "Build logs: $LOGDIR"
echo "==============================="
echo ""

# Portable stat helper: prints "mtime filepath" for sorting.
file_mtime_path() {
  if stat -c '%Y %n' "$1" >/dev/null 2>&1; then
    stat -c '%Y %n' "$1"
  else
    stat -f '%m %N' "$1"
  fi
}

# Find the most recently modified .adoc file newer than a reference file.
newest_adoc_after() {
  local dir="$1" ref="$2"
  [ -d "$dir" ] || return 0
  [ -f "$ref" ] || return 0
  find "$dir" -name '*.adoc' -newer "$ref" -print 2>/dev/null |
    while IFS= read -r f; do
      file_mtime_path "$f"
    done | sort -rn | head -1 | cut -d' ' -f2-
}

# Detect what the user was working on. PREVIEW_REF is a persistent marker
# from the previous successful preview run. On first run it won't exist,
# so detection falls through to the homepage.
PREVIEW_URL="http://127.0.0.1:4000"
RECENT_POST=""
RECENT_GUIDE=""

if [ -f "$PREVIEW_REF" ]; then
  RECENT_POST=$(newest_adoc_after "$SCRIPTDIR/docs/target/web-site/_posts" "$PREVIEW_REF")
  RECENT_GUIDE=$(newest_adoc_after "$SCRIPTDIR/docs/src/main/asciidoc" "$PREVIEW_REF")
fi

if [ -n "$RECENT_POST" ]; then
  SLUG=$(basename "$RECENT_POST" .adoc | sed 's/^[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}-//')
  PREVIEW_URL="http://127.0.0.1:4000/blog/${SLUG}/"
  # Detect user-story tag in YAML front matter (supports single-line and multi-line)
  if awk '/^---$/{c++; next} c==1' "$RECENT_POST" | grep -q 'user-story'; then
    echo "Detected: user story → /blog/${SLUG}/"
  else
    echo "Detected: blog post → /blog/${SLUG}/"
  fi
elif [ -n "$RECENT_GUIDE" ]; then
  PREVIEW_URL="http://127.0.0.1:4000/version/main/guides/"
  echo "Detected: guide ($(basename "$RECENT_GUIDE"))"
else
  echo "No recent changes detected, opening homepage."
fi

# Update the persistent marker after detection completes
touch "$PREVIEW_REF" || exit 1

[ -n "$BROWSER_CMD" ] && $BROWSER_CMD "$PREVIEW_URL" 2>/dev/null
echo "Docs preview: $PREVIEW_URL"
