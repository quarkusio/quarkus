#!/bin/bash
SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd) || exit 1
cd "$SCRIPTDIR" || exit 1

# Verify required commands before anything else
for cmd in awk curl rsync find stat mktemp grep sed sort head tail cut basename date seq tee mv; do
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "ERROR: required command not found: $cmd"
    exit 1
  fi
done

LOGDIR=$(mktemp -d "${TMPDIR:-/tmp}/quarkus-docs-build-XXXXXX") || exit 1

# Primary: build from the upstream jekyll-container/ Dockerfile (in the website checkout).
# Fallback: use pre-built images if the Dockerfile is not available (first run before sync).
JEKYLL_LOCAL_IMAGE="quarkus-docs-jekyll:local"
JEKYLL_IMAGE_FALLBACK="docker.io/bretfisher/jekyll-serve@sha256:db11b70736935b1a777b2ff2ae10f9ad191ee9fca6560eade1d5ad98b74e5f66"
JEKYLL_IMAGE_FALLBACK2="docker.io/jekyll/jekyll@sha256:bb45414c3fefa80a75c5001f30baf1dff48ae31dc961b8b51003b93b60675334"

PREVIEW_REF="$SCRIPTDIR/docs/.docs-preview-last-run"
ROOT_BUILD_REF="$SCRIPTDIR/docs/.docs-preview-root-build-last-run"
ROOT_BUILD_HEAD_REF="$SCRIPTDIR/docs/.docs-preview-root-build-head"
TIMES_CACHE="$SCRIPTDIR/docs/.docs-preview-times"
LOCK_DIR="$SCRIPTDIR/docs/.docs-preview.lock"

# --- Interrupt handling ---

ACTIVE_PID=""

# Prevent concurrent runs
if ! mkdir "$LOCK_DIR" 2>/dev/null; then
  echo "Another docs preview run is already active."
  echo "If this is stale, remove: $LOCK_DIR"
  exit 1
fi

cleanup_on_exit() {
  rm -rf "$LOCK_DIR"
}
cleanup_on_interrupt() {
  echo ""
  echo "Interrupted."
  if [ -n "$ACTIVE_PID" ] && kill -0 "$ACTIVE_PID" 2>/dev/null; then
    kill "$ACTIVE_PID" 2>/dev/null || true
  fi
  rm -rf "$LOCK_DIR"
  exit 130
}
trap cleanup_on_exit EXIT
trap cleanup_on_interrupt INT TERM

# --- Progress display helpers ---

estimate_step() {
  local step="$1"
  if [ -f "$TIMES_CACHE" ]; then
    local cached
    cached=$(grep "^${step}=" "$TIMES_CACHE" 2>/dev/null | cut -d= -f2)
    if [ -n "$cached" ] && [ "$cached" -gt 0 ] 2>/dev/null; then
      echo "$cached"
      return
    fi
  fi
  case "$step" in
    root)      awk -v c="$CORES" 'BEGIN {t=int(100+(22-c)*22.2); if(t<60)t=60; printf "%d",t}' ;;
    docs)      awk -v c="$CORES" 'BEGIN {t=int(75+(22-c)*1.7);  if(t<30)t=30; printf "%d",t}' ;;
    sync_full) echo 200 ;;
    sync_fast) echo 1 ;;
    jekyll)    awk -v c="$CORES" 'BEGIN {t=int(100+(22-c)*4.1);  if(t<60)t=60; printf "%d",t}' ;;
    *)         echo 120 ;;
  esac
}

save_step_time() {
  local step="$1" seconds="$2" tmp
  case "$seconds" in ''|*[!0-9]*) return 0 ;; esac
  tmp=$(mktemp "${TMPDIR:-/tmp}/docs-preview-times-XXXXXX") || return 1
  if [ -f "$TIMES_CACHE" ]; then
    awk -F= -v key="$step" -v val="$seconds" \
      'BEGIN{u=0} $1==key{print key"="val;u=1;next} {print} END{if(!u)print key"="val}' \
      "$TIMES_CACHE" > "$tmp" || { rm -f "$tmp"; return 1; }
  else
    echo "${step}=${seconds}" > "$tmp" || { rm -f "$tmp"; return 1; }
  fi
  mv "$tmp" "$TIMES_CACHE" || { rm -f "$tmp"; return 1; }
}

run_with_progress() {
  local label="$1" est="$2" log="$3"
  shift 3
  case "$est" in ''|*[!0-9]*) est=120 ;; esac
  [ "$est" -lt 1 ] && est=120

  "$@" > "$log" 2>&1 &
  ACTIVE_PID=$!
  local pid=$ACTIVE_PID
  local start
  start=$(date +%s)

  while kill -0 "$pid" 2>/dev/null; do
    local elapsed=$(( $(date +%s) - start ))
    local pct=$(( elapsed * 100 / est ))
    [ "$pct" -gt 99 ] && pct=99
    if [ -t 1 ]; then
      printf '\r  %s [%3d%%] %ds / ~%ds ' "$label" "$pct" "$elapsed" "$est"
    else
      printf '  %s: %ds / ~%ds\n' "$label" "$elapsed" "$est"
    fi
    sleep 2
  done

  wait "$pid"
  local rc=$?
  ACTIVE_PID=""
  local elapsed=$(( $(date +%s) - start ))

  if [ $rc -eq 0 ]; then
    if [ -t 1 ]; then
      printf '\r  %s ✓ %ds                          \n' "$label" "$elapsed"
    else
      printf '  %s: completed in %ds\n' "$label" "$elapsed"
    fi
  else
    if [ -t 1 ]; then
      printf '\r  %s ✗ %ds (failed)                  \n' "$label" "$elapsed"
    else
      printf '  %s: failed after %ds\n' "$label" "$elapsed"
    fi
    echo "  Last log lines:"
    tail -15 "$log" | sed 's/^/    /'
  fi
  return $rc
}

# --- Root build decision ---

root_build_needed() {
  [ "${QUARKUS_DOCS_PREVIEW_FULL:-}" = "1" ] && return 0
  [ ! -f "$ROOT_BUILD_REF" ] && return 0
  if [ -n "$(find "$ROOT_BUILD_REF" -mtime +7 -print 2>/dev/null)" ]; then
    return 0
  fi
  if find "$SCRIPTDIR" \
      -path "$SCRIPTDIR/.git" -prune -o \
      -path "$SCRIPTDIR/docs/target" -prune -o \
      -name 'pom.xml' -newer "$ROOT_BUILD_REF" -print 2>/dev/null | grep -q .; then
    return 0
  fi
  # Detect git pull, checkout, or branch switch since last root build
  if command -v git >/dev/null 2>&1 && git -C "$SCRIPTDIR" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    local current_head saved_head
    current_head=$(git -C "$SCRIPTDIR" rev-parse HEAD 2>/dev/null || true)
    saved_head=$(cat "$ROOT_BUILD_HEAD_REF" 2>/dev/null || true)
    [ -n "$current_head" ] && [ "$current_head" != "$saved_head" ] && return 0
  fi
  return 1
}

# --- Container reuse ---

container_running() {
  [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' quarkus-docs-preview 2>/dev/null)" = "true" ]
}

preview_ready() {
  [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:4000 2>/dev/null)" = "200" ]
}

# --- Step 0: Environment ---

ENVSCRIPT="docs/detect-env.sh"
echo "=== Environment ==="
# shellcheck source=detect-env.sh
. "$ENVSCRIPT" || exit 1

MVNCMD="${QMVNCMD:-./mvnw -ntp $MVN_THREADS}"
MVNCMD_ST="${QMVNCMD:-./mvnw -ntp}"

RUN_ROOT_BUILD=false
if root_build_needed; then
  RUN_ROOT_BUILD=true
fi

EST_ROOT=$(estimate_step root)
EST_DOCS=$(estimate_step docs)
EST_JEKYLL=$(estimate_step jekyll)

PREVIEW_ALREADY_RUNNING=false
if container_running && preview_ready; then
  PREVIEW_ALREADY_RUNNING=true
fi

echo ""
if [ "$RUN_ROOT_BUILD" = "true" ]; then
  EST_SYNC=$(estimate_step sync_full)
  EST_TOTAL=$((EST_ROOT + EST_DOCS + EST_SYNC + EST_JEKYLL))
  echo "Full root build needed."
  echo "Estimated time: ~$((EST_TOTAL / 60)) minutes on this machine."
  echo "After this completes, later docs updates take about 1 minute."
else
  echo "Root build output is ready."
  echo "Running the quick docs rebuild path."
  echo "Expected time: about 1 minute."
fi
echo "Logs: $LOGDIR"
echo ""

# Pre-flight: port check (skip if reusing container)
port_in_use() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$1" -sTCP:LISTEN >/dev/null 2>&1
    return $?
  fi
  if command -v nc >/dev/null 2>&1; then
    nc -z 127.0.0.1 "$1" 2>/dev/null
    return $?
  fi
  curl --max-time 1 -s -o /dev/null "http://127.0.0.1:$1" 2>/dev/null
}

if [ "$PREVIEW_ALREADY_RUNNING" != "true" ]; then
  if port_in_use 4000 || port_in_use 35729; then
    echo "Port 4000 or 35729 is already in use."
    echo "If a previous preview is running: $CONTAINER_CMD rm -f quarkus-docs-preview"
    exit 1
  fi
fi

START_TOTAL=$(date +%s)

# Non-fatal cache cleanup
if [ "$CONTAINER_CMD" = "podman" ]; then
  podman unshare rm -rf docs/.cache/ 2>/dev/null || rm -rf docs/.cache/ 2>/dev/null || true
else
  rm -rf docs/.cache/ 2>/dev/null || true
fi

# --- Step 1: Root Build ---

if [ "$RUN_ROOT_BUILD" = "true" ]; then
  echo "=== Step 1: Root build (~${EST_ROOT}s estimated) ==="
  STEP1_START=$(date +%s)
  # shellcheck disable=SC2086
  run_with_progress "Root build" "$EST_ROOT" "$LOGDIR/step1-parallel.log" \
    $MVNCMD clean install -DquicklyDocs \
    -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true
  RC=$?
  STEP1_TIME=$(( $(date +%s) - STEP1_START ))

  if [ $RC -ne 0 ] && [ -n "$MVN_THREADS" ]; then
    echo "  Parallel build failed. Retrying single-threaded..."
    STEP1_START=$(date +%s)
    # shellcheck disable=SC2086
    run_with_progress "Root build (single-threaded)" "$((EST_ROOT * 2))" "$LOGDIR/step1-fallback.log" \
      $MVNCMD_ST clean install -DquicklyDocs \
      -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true
    RC=$?
    STEP1_TIME=$(( $(date +%s) - STEP1_START ))
  fi

  if [ $RC -ne 0 ]; then
    echo "  Root build failed. Check $LOGDIR/step1*.log"
    exit 1
  fi
  save_step_time "root" "$STEP1_TIME" || true
  touch "$ROOT_BUILD_REF" || exit 1
  git -C "$SCRIPTDIR" rev-parse HEAD > "$ROOT_BUILD_HEAD_REF" 2>/dev/null || true
else
  echo "=== Step 1: Root build (skipped — output is fresh) ==="
fi

# --- Step 2: Docs Rebuild ---

echo ""
echo "=== Step 2: Docs rebuild (~${EST_DOCS}s estimated) ==="
cd docs || exit 1
STEP2_START=$(date +%s)
run_with_progress "Docs rebuild" "$EST_DOCS" "$LOGDIR/step2.log" \
  ../mvnw -ntp package -Dasciidoctor.fail-if=ERROR
RC=$?
STEP2_TIME=$(( $(date +%s) - STEP2_START ))

# If docs rebuild fails and we skipped root build, retry with root build
if [ $RC -ne 0 ] && [ "$RUN_ROOT_BUILD" != "true" ]; then
  echo "  Docs rebuild failed after skipping root build."
  echo "  Refreshing root build output, then retrying..."
  cd "$SCRIPTDIR" || exit 1
  STEP1_START=$(date +%s)
  # shellcheck disable=SC2086
  run_with_progress "Root build (recovery)" "$EST_ROOT" "$LOGDIR/step1-recovery.log" \
    $MVNCMD install -DquicklyDocs \
    -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true
  RC=$?
  if [ $RC -ne 0 ]; then
    echo "  Root build failed. Check $LOGDIR/step1-recovery.log"
    exit 1
  fi
  touch "$ROOT_BUILD_REF" || exit 1
  git -C "$SCRIPTDIR" rev-parse HEAD > "$ROOT_BUILD_HEAD_REF" 2>/dev/null || true
  save_step_time "root" "$(( $(date +%s) - STEP1_START ))" || true
  cd docs || exit 1
  STEP2_START=$(date +%s)
  run_with_progress "Docs rebuild (retry)" "$EST_DOCS" "$LOGDIR/step2.log" \
    ../mvnw -ntp package -Dasciidoctor.fail-if=ERROR
  RC=$?
  STEP2_TIME=$(( $(date +%s) - STEP2_START ))
fi

if [ $RC -ne 0 ]; then
  echo "  Docs rebuild failed. Check $LOGDIR/step2.log"
  exit 1
fi
save_step_time "docs" "$STEP2_TIME" || true

# --- Step 3: Sync ---

echo ""
echo "=== Step 3: Sync ==="
STEP3_START=$(date +%s)

run_fast_sync() {
  rsync -rt --delete \
      --exclude='**/*.html' --exclude='**/index.adoc' \
      --exclude='**/_attributes-local.adoc' --exclude='**/guides.md' \
      --exclude='**/_templates' \
      target/asciidoc/sources/ target/web-site/_versions/main/guides || return 1
  if [ -d target/quarkus-generated-doc/ ]; then
    rsync -rt --delete \
        --exclude='**/*.html' --exclude='**/index.adoc' \
        --exclude='**/_attributes.adoc' \
        target/quarkus-generated-doc/ target/web-site/_generated-doc/main || return 1
  fi
  if [ -f target/indexByType.yaml ]; then
    mkdir -p target/web-site/_data/versioned/main/index || return 1
    { echo "# Generated file. Do not edit"; cat target/indexByType.yaml
    } > target/web-site/_data/versioned/main/index/quarkus.yaml || return 1
  fi
  if [ -f target/relations.yaml ]; then
    mkdir -p target/web-site/_data/versioned/main/index || return 1
    { echo "# Generated file. Do not edit"; cat target/relations.yaml
    } > target/web-site/_data/versioned/main/index/relations.yaml || return 1
  fi
}

run_full_sync() {
  local log="$1"
  ./sync-web-site.sh 2>&1 | tee "$log"
  [ "${PIPESTATUS[0]}" -eq 0 ]
}

SYNC_TYPE="full"
if [ -d target/web-site/_versions ]; then
  SYNC_OK=true
  [ ! -d target/web-site/_versions/main/guides ] && SYNC_OK=false
  [ ! -f target/web-site/_config.yml ] && SYNC_OK=false
  [ ! -f target/web-site/_only_latest_guides_config.yml ] && SYNC_OK=false

  if [ "$SYNC_OK" = "true" ]; then
    SYNC_TYPE="fast"
    echo "  Running fast re-sync..."
    if ! run_fast_sync > "$LOGDIR/step3-fast-sync.log" 2>&1; then
      echo "  Fast sync failed. Falling back to full sync..."
      SYNC_TYPE="full"
      rm -rf target/web-site || exit 1
      if ! run_full_sync "$LOGDIR/step3.log"; then
        echo "  Sync failed. Check $LOGDIR/step3.log"
        exit 1
      fi
    else
      printf '  Sync ✓ fast\n'
    fi
  else
    echo "  Website layout changed. Full sync..."
    rm -rf target/web-site || exit 1
    if ! run_full_sync "$LOGDIR/step3.log"; then
      echo "  Sync failed. Check $LOGDIR/step3.log"
      exit 1
    fi
  fi
else
  echo "  First run — cloning quarkusio.github.io..."
  if ! run_full_sync "$LOGDIR/step3.log"; then
    echo "  Sync failed. Check $LOGDIR/step3.log"
    exit 1
  fi
fi
STEP3_TIME=$(( $(date +%s) - STEP3_START ))
if [ "$SYNC_TYPE" = "fast" ]; then
  save_step_time "sync_fast" "$STEP3_TIME" || true
else
  save_step_time "sync_full" "$STEP3_TIME" || true
  # Full sync recreated the mount directory — container must restart
  PREVIEW_ALREADY_RUNNING=false
fi

# --- Step 4: Serve ---

echo ""
echo "=== Step 4: Preview server ==="

if [ "$PREVIEW_ALREADY_RUNNING" = "true" ]; then
  if container_running && preview_ready; then
    echo "  Preview server is already running. Reusing it."
  else
    echo "  Previously running preview server is no longer ready. Restarting."
    PREVIEW_ALREADY_RUNNING=false
  fi
fi

if [ "$PREVIEW_ALREADY_RUNNING" != "true" ]; then
  $CONTAINER_CMD rm -f quarkus-docs-preview 2>/dev/null || true
  cd target/web-site || exit 1
  STEP4_START=$(date +%s)

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

  # Primary: build from the upstream jekyll-container/ Dockerfile
  STARTED=false
  if [ -d "jekyll-container" ] && [ -f "jekyll-container/Dockerfile" ]; then
    echo "  Checking Jekyll image (build cache)..."
    if ! $CONTAINER_CMD build -t "$JEKYLL_LOCAL_IMAGE" jekyll-container/ > "$LOGDIR/step4-build.log" 2>&1; then
      echo "  Image build failed. Falling back to pre-built image..."
    fi
    if $CONTAINER_CMD image inspect "$JEKYLL_LOCAL_IMAGE" > /dev/null 2>&1; then
      if start_jekyll "$JEKYLL_LOCAL_IMAGE" "/site" "$LOGDIR/step4-primary.log"; then
        STARTED=true
      else
        echo "  Local image failed to start. Trying fallback..."
        $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
      fi
    fi
  fi

  # Fallback 1: bretfisher/jekyll-serve (pinned by digest)
  if [ "$STARTED" != "true" ]; then
    PULL_OK=true
    if ! $CONTAINER_CMD image inspect "$JEKYLL_IMAGE_FALLBACK" > /dev/null 2>&1; then
      echo "  Pulling fallback Jekyll image (first time only)..."
      $CONTAINER_CMD pull "$JEKYLL_IMAGE_FALLBACK" > /dev/null 2>&1 || PULL_OK=false
    fi
    if [ "$PULL_OK" = "true" ]; then
      if start_jekyll "$JEKYLL_IMAGE_FALLBACK" "/site" "$LOGDIR/step4-fallback.log"; then
        STARTED=true
      else
        $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
      fi
    fi
  fi

  # Fallback 2: jekyll/jekyll (pinned by digest)
  if [ "$STARTED" != "true" ]; then
    PULL_OK2=true
    if ! $CONTAINER_CMD image inspect "$JEKYLL_IMAGE_FALLBACK2" > /dev/null 2>&1; then
      echo "  Pulling second fallback image..."
      $CONTAINER_CMD pull "$JEKYLL_IMAGE_FALLBACK2" > /dev/null 2>&1 || PULL_OK2=false
    fi
    if [ "$PULL_OK2" = "true" ]; then
      if start_jekyll "$JEKYLL_IMAGE_FALLBACK2" "/srv/jekyll" "$LOGDIR/step4-fallback2.log"; then
        STARTED=true
      else
        $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
      fi
    fi
  fi

  if [ "$STARTED" != "true" ]; then
    echo "  All images failed. Check $LOGDIR/step4*.log"
    $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
    exit 1
  fi

  echo "  Waiting for Jekyll to generate the site..."
  for i in $(seq 1 150); do
    if [ "$($CONTAINER_CMD inspect -f '{{.State.Running}}' quarkus-docs-preview 2>/dev/null)" != "true" ]; then
      echo "  Container exited during startup."
      $CONTAINER_CMD logs quarkus-docs-preview 2>&1 | tail -50 | tee "$LOGDIR/step4-crash.log"
      $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
      exit 1
    fi
    if [ "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:4000 2>/dev/null)" = "200" ]; then
      STEP4_TIME=$(( $(date +%s) - STEP4_START ))
      printf '  Preview server ✓ %ds\n' "$STEP4_TIME"
      save_step_time "jekyll" "$STEP4_TIME" || true
      break
    fi
    if [ "$i" -eq 150 ]; then
      echo "  Timeout after 300s."
      $CONTAINER_CMD logs quarkus-docs-preview 2>&1 | tail -50 | tee "$LOGDIR/step4-timeout.log"
      $CONTAINER_CMD rm -f quarkus-docs-preview >/dev/null 2>&1 || true
      exit 1
    fi
    local_elapsed=$(( $(date +%s) - STEP4_START ))
    local_pct=$(( local_elapsed * 100 / (EST_JEKYLL > 0 ? EST_JEKYLL : 120) ))
    [ "$local_pct" -gt 99 ] && local_pct=99
    if [ -t 1 ]; then
      printf '\r  Jekyll [%3d%%] %ds / ~%ds ' "$local_pct" "$local_elapsed" "$EST_JEKYLL"
    else
      printf '  Jekyll: %ds / ~%ds\n' "$local_elapsed" "$EST_JEKYLL"
    fi
    sleep 2
  done
fi

# --- Step 5: Detect and open ---

echo ""
END_TOTAL=$(date +%s)
TOTAL_TIME=$((END_TOTAL - START_TOTAL))

echo "==============================="
echo "Done in ${TOTAL_TIME}s"
echo "==============================="
echo ""

file_mtime_path() {
  if stat -c '%Y %n' "$1" >/dev/null 2>&1; then
    stat -c '%Y %n' "$1"
  else
    stat -f '%m %N' "$1"
  fi
}

newest_adoc_after() {
  local dir="$1" ref="$2"
  [ -d "$dir" ] || return 0
  [ -f "$ref" ] || return 0
  find "$dir" -name '*.adoc' -newer "$ref" -print 2>/dev/null |
    while IFS= read -r f; do file_mtime_path "$f"; done |
    sort -rn | head -1 | cut -d' ' -f2-
}

all_adoc_after() {
  local dir="$1" ref="$2"
  [ -d "$dir" ] || return 0
  [ -f "$ref" ] || return 0
  find "$dir" -name '*.adoc' -newer "$ref" -print 2>/dev/null
}

PREVIEW_URLS=()
RECENT_POST=""

if [ -f "$PREVIEW_REF" ]; then
  RECENT_POST=$(newest_adoc_after "$SCRIPTDIR/docs/target/web-site/_posts" "$PREVIEW_REF")
  GUIDE_COUNT=$(all_adoc_after "$SCRIPTDIR/docs/src/main/asciidoc" "$PREVIEW_REF" | awk 'END { print NR }')
fi

if [ -n "$RECENT_POST" ]; then
  SLUG=$(basename "$RECENT_POST" .adoc | sed 's/^[0-9]\{4\}-[0-9]\{2\}-[0-9]\{2\}-//')
  PREVIEW_URLS=("http://127.0.0.1:4000/blog/${SLUG}/")
  if awk '/^---$/{c++; next} c==1' "$RECENT_POST" | grep -q 'user-story'; then
    echo "Detected: user story ($(basename "$RECENT_POST"))"
  else
    echo "Detected: blog post ($(basename "$RECENT_POST"))"
  fi
elif [ "${GUIDE_COUNT:-0}" -eq 1 ]; then
  GUIDE_FILE=$(all_adoc_after "$SCRIPTDIR/docs/src/main/asciidoc" "$PREVIEW_REF" | head -1)
  GUIDE_SLUG=$(basename "$GUIDE_FILE" .adoc)
  PREVIEW_URLS=("http://127.0.0.1:4000/version/main/guides/${GUIDE_SLUG}.html")
  echo "Detected: guide ($GUIDE_SLUG.adoc)"
elif [ "${GUIDE_COUNT:-0}" -ge 2 ] && [ "${GUIDE_COUNT:-0}" -le 4 ]; then
  echo "Detected: $GUIDE_COUNT guides modified"
  while IFS= read -r gf; do
    GS=$(basename "$gf" .adoc)
    PREVIEW_URLS+=("http://127.0.0.1:4000/version/main/guides/${GS}.html")
    echo "  - $GS.adoc"
  done < <(all_adoc_after "$SCRIPTDIR/docs/src/main/asciidoc" "$PREVIEW_REF")
elif [ "${GUIDE_COUNT:-0}" -ge 5 ]; then
  PREVIEW_URLS=("http://127.0.0.1:4000/version/main/guides/")
  echo "Detected: $GUIDE_COUNT guides modified. Opening listing."
else
  PREVIEW_URLS=("http://127.0.0.1:4000")
  echo "No recent changes detected. Opening homepage."
fi

touch "$PREVIEW_REF" || exit 1

echo ""
if [ -n "$BROWSER_CMD" ]; then
  for url in "${PREVIEW_URLS[@]}"; do
    echo "Opening: $url"
    "$BROWSER_CMD" "$url" 2>/dev/null
    sleep 1
  done
else
  echo "Open in your browser:"
  for url in "${PREVIEW_URLS[@]}"; do
    echo "  $url"
  done
fi
echo ""
echo "Preview is ready."
echo "Container: quarkus-docs-preview"
echo "Stop:      $CONTAINER_CMD rm -f quarkus-docs-preview"
echo "Logs:      $LOGDIR"
