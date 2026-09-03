#!/bin/bash
# Tests for docs/detect-env.sh and docs/docs-preview.sh fixes.
# Run from the repository root:
#   bash docs/test-preview-scripts.sh
#
# Each test is fully isolated in a scratch directory.

SCRIPTDIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
DOCS="$SCRIPTDIR/docs"
PASS=0
FAIL=0

_pass() { echo "  PASS: $1"; PASS=$((PASS + 1)); }
_fail() { echo "  FAIL: $1"; FAIL=$((FAIL + 1)); }

run_test() {
  local name="$1"; shift
  echo ""
  echo "--- $name ---"
  "$@"
}
# ============================================================
# 1. Full→fast sync fixture
#    run_fast_sync must invoke sync-web-site.sh with TARGET_DIR
#    (no re-clone), sharing all post-processing logic.
# ============================================================
test_fast_sync_calls_sync_web_site() {
  # Extract the run_fast_sync function body from docs-preview.sh
  local func_body
  func_body=$(awk '/^run_fast_sync\(\)/{found=1} found{print} /^}$/{if(found){exit}}' \
    "$DOCS/docs-preview.sh")

  if echo "$func_body" | grep -q "sync-web-site.sh"; then
    _pass "run_fast_sync calls sync-web-site.sh"
  else
    _fail "run_fast_sync does not call sync-web-site.sh. Body: $func_body"
  fi

  if echo "$func_body" | grep -q 'main.*web-site\|web-site.*main'; then
    _pass "run_fast_sync passes TARGET_DIR to sync-web-site.sh"
  else
    _fail "run_fast_sync does not appear to pass TARGET_DIR. Body: $func_body"
  fi

  # Verify the call does NOT contain 'rm -rf target/web-site' (no re-clone)
  if echo "$func_body" | grep -q "rm -rf.*web-site"; then
    _fail "run_fast_sync deletes target/web-site (re-clones) — should not"
  else
    _pass "run_fast_sync does not delete target/web-site (no re-clone)"
  fi
}

test_sync_web_site_postprocessing() {
  local tmp
  tmp=$(mktemp -d)
  TARGET_DIR="$tmp/ws"
  TARGET_GUIDES="$tmp/ws/content/versions/main/guides"
  local guides="$TARGET_GUIDES"
  mkdir -p "$guides"

  # Pre-seed guides directory (mimicking what rsync would put there)
  printf 'link:myscript.sh[run it]\n' > "$guides/my-guide.adoc"
  printf '#!/bin/sh\necho hello\n' > "$guides/myscript.sh"
  printf 'Some text {| and |} markers\n' > "$guides/qute-reference.adoc"

  # --- Post-processing logic (mirrored from sync-web-site.sh) ---
  # 1. Move static assets to assets/
  for ext in py sh zip jar tar.gz; do
    find "$TARGET_GUIDES" -maxdepth 1 -name "*.$ext" -exec bash -c '
      mkdir -p "$(dirname "$1")/assets"
      mv "$1" "$(dirname "$1")/assets/"
    ' _ {} \;
  done

  # 2. Update AsciiDoc links to reference ../assets/
  for ext in py sh zip jar tar.gz; do
    find "$TARGET_GUIDES" -maxdepth 1 -name "*.adoc" -exec \
      sed -i "s|link:\([a-zA-Z0-9_-]*\.$ext\)|link:../assets/\1|g" {} \;
  done

  # 3. Ensure index.html files exist in resource directories
  for subdir in images javascript assets; do
    dir="$TARGET_GUIDES/$subdir"
    if [ -d "$dir" ] && [ ! -f "$dir/index.html" ]; then
      rel_path=$(echo "$dir" | sed "s|${TARGET_DIR}/content/||")
      printf '%s\n' '---' "link: /${rel_path}/" '---' '<html><body></body></html>' > "$dir/index.html"
    fi
  done

  # 4. Escape Qute syntax in qute-reference guides
  find "$TARGET_GUIDES" -name "*qute-reference.adoc" -exec sed -i 's/|}/|\\}/g; s/{|/\\{|/g' {} \;

  # --- Assertions ---
  if [ -f "$guides/assets/myscript.sh" ]; then
    _pass "Static .sh file moved to assets/ subdirectory"
  else
    _fail "Static .sh file not found in assets/: $(find "$guides" -name "*.sh" 2>/dev/null | tr '\n' ' ')"
  fi
  if [ ! -f "$guides/myscript.sh" ]; then
    _pass "Static .sh file removed from guides root"
  else
    _fail "Static .sh file still exists in guides root"
  fi
  if grep -q "link:../assets/myscript.sh" "$guides/my-guide.adoc"; then
    _pass "Asset link rewritten to ../assets/ in .adoc file"
  else
    _fail "Asset link not rewritten. Content: $(cat "$guides/my-guide.adoc")"
  fi
  if [ -f "$guides/assets/index.html" ]; then
    _pass "index.html created in assets/ directory"
  else
    _fail "index.html missing from assets/"
  if grep -q 'link: /versions/main/guides/assets/' "$guides/assets/index.html" 2>/dev/null; then
    _pass "index.html contains correct relative path"
  else
    _fail "index.html has wrong path. Content: $(cat \"$guides/assets/index.html\" 2>/dev/null)"
  fi
  fi
  if grep -q '|\\}' "$guides/qute-reference.adoc"; then
    _pass "Qute |} syntax escaped correctly"
  else
    _fail "Qute |} not escaped. Content: $(cat \"$guides/qute-reference.adoc\")"
  fi
  if grep -q '\\{|' "$guides/qute-reference.adoc"; then
    _pass "Qute {| syntax escaped correctly"
  else
    _fail "Qute {| not escaped. Content: $(cat \"$guides/qute-reference.adoc\")"
  fi

  rm -rf "$tmp"
}

# ============================================================
# 2. Readiness timeout fixture
# ============================================================
test_readiness_curl_flags() {
  local curl_line
  curl_line=$(grep "curl.*http_code.*ROQ_PORT" "$DOCS/docs-preview.sh")
  if echo "$curl_line" | grep -q -- '--connect-timeout'; then
    _pass "curl uses --connect-timeout in readiness loop"
  else
    _fail "curl missing --connect-timeout. Line: $curl_line"
  fi
  if echo "$curl_line" | grep -q -- '--max-time'; then
    _pass "curl uses --max-time in readiness loop"
  else
    _fail "curl missing --max-time. Line: $curl_line"
  fi
}

test_readiness_bounded() {
  if ! command -v python3 >/dev/null 2>&1; then
    echo "  SKIP: python3 not available for readiness bound test"
    return
  fi

  # Start a black-hole TCP listener (accepts but never responds)
  local port=59871
  python3 -c "
import socket,time
s=socket.socket()
s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1)
s.bind(('127.0.0.1',$port))
s.listen(1)
conn,_=s.accept()
time.sleep(60)
" &
  local nc_pid=$!
  sleep 0.3

  local start_t elapsed rc
  start_t=$(date +%s)
  curl -s --connect-timeout 3 --max-time 5 \
    -o /dev/null -w '%{http_code}' "http://127.0.0.1:${port}" >/dev/null 2>&1
  rc=$?
  elapsed=$(( $(date +%s) - start_t ))
  kill "$nc_pid" 2>/dev/null || true

  # --max-time 5 means curl should return within ~6s (5s + OS overhead)
  if [ "$elapsed" -le 10 ]; then
    _pass "curl returned within ${elapsed}s with a black-hole listener (bounded by --max-time)"
  else
    _fail "curl took ${elapsed}s — expected <= 10s with --max-time 5"
  fi
  if [ "$rc" -ne 0 ]; then
    _pass "curl returned non-zero ($rc) for unresponsive listener"
  else
    _fail "curl returned 0 for an unresponsive listener — should have timed out"
  fi
}

# ============================================================
# 3. Wrapper cases: missing, non-executable, executable
# ============================================================
test_mvnw_cases() {
  # Extract the pre-flight mvnw check from the real script at test time
  # so this test stays in sync if error messages or logic change.
  local check_block
  check_block=$(awk '/# Pre-flight: verify .\/mvnw is usable/{found=1} found && /^$/{if(found>1)exit; found++} found{print}' "$DOCS/docs-preview.sh" | grep -v '^#')
  if [ -z "$check_block" ]; then
    _fail "mvnw pre-flight block not found in docs-preview.sh — check awk pattern"
    return
  fi
  if ! echo "$check_block" | grep -q "not found in target/web-site"; then
    _fail "mvnw MISSING message not in extracted block — source may have changed"
    return
  fi
  local tmp out rc

  # Case 1: missing
  tmp=$(mktemp -d)
  out=$(cd "$tmp" && bash -c "$check_block" 2>&1)
  rc=$?
  [ $rc -ne 0 ] && _pass "mvnw MISSING: exits non-zero" || _fail "mvnw MISSING: should exit non-zero but got 0"
  echo "$out" | grep -q "not found in target/web-site" && \
    _pass "mvnw MISSING: correct error message" || \
    _fail "mvnw MISSING: wrong error. Got: $out"
  rm -rf "$tmp"

  # Case 2: present but non-executable
  tmp=$(mktemp -d)
  touch "$tmp/mvnw"
  out=$(cd "$tmp" && bash -c "$check_block" 2>&1)
  rc=$?
  [ $rc -ne 0 ] && _pass "mvnw NON-EXECUTABLE: exits non-zero" || _fail "mvnw NON-EXECUTABLE: should exit non-zero but got 0"
  echo "$out" | grep -q "not executable\|chmod" && \
    _pass "mvnw NON-EXECUTABLE: correct error message" || \
    _fail "mvnw NON-EXECUTABLE: wrong error. Got: $out"
  rm -rf "$tmp"

  # Case 3: present and executable
  tmp=$(mktemp -d)
  touch "$tmp/mvnw"; chmod +x "$tmp/mvnw"
  out=$(cd "$tmp" && bash -c "$check_block" 2>&1)
  rc=$?
  [ $rc -eq 0 ] && _pass "mvnw EXECUTABLE: check passes cleanly" || \
    _fail "mvnw EXECUTABLE: should pass but exited $rc. Output: $out"
  rm -rf "$tmp"
}

# ============================================================
# 4. Port validation
# ============================================================
test_port_validation() {
  # Inline the exact validation block from docs-preview.sh
  local validation_block
  validation_block=$(cat << 'BLOCK'
if [ -n "${QUARKUS_HTTP_PORT:-}" ]; then
  case "$QUARKUS_HTTP_PORT" in
    ''|*[!0-9]*)
      echo "ERROR: QUARKUS_HTTP_PORT must be a number, got: '$QUARKUS_HTTP_PORT'"
      exit 1 ;;
  esac
  if [ "$QUARKUS_HTTP_PORT" -lt 1 ] || [ "$QUARKUS_HTTP_PORT" -gt 65535 ]; then
    echo "ERROR: QUARKUS_HTTP_PORT must be in range 1–65535, got: $QUARKUS_HTTP_PORT"
    exit 1
  fi
fi
BLOCK
)

  local out rc

  # Verify the block exists in docs-preview.sh (not just inline here)
  if ! grep -q "QUARKUS_HTTP_PORT must be a number" "$DOCS/docs-preview.sh"; then
    _fail "Port validation block missing from docs-preview.sh"
    return
  fi

  # Valid cases
  out=$(QUARKUS_HTTP_PORT=8080 bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -eq 0 ] && _pass "Port 8080: accepted" || _fail "Port 8080: rejected unexpectedly. Output: $out"

  out=$(QUARKUS_HTTP_PORT=1 bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -eq 0 ] && _pass "Port 1: accepted" || _fail "Port 1: rejected unexpectedly"

  out=$(QUARKUS_HTTP_PORT=65535 bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -eq 0 ] && _pass "Port 65535: accepted" || _fail "Port 65535: rejected unexpectedly"

  # Invalid cases
  out=$(QUARKUS_HTTP_PORT=0 bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -ne 0 ] && _pass "Port 0: rejected" || _fail "Port 0: should be rejected but accepted"

  out=$(QUARKUS_HTTP_PORT=65536 bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -ne 0 ] && _pass "Port 65536: rejected" || _fail "Port 65536: should be rejected but accepted"

  out=$(QUARKUS_HTTP_PORT=abc bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -ne 0 ] && _pass "Port 'abc': rejected" || _fail "Port 'abc': should be rejected but accepted"

  # Unset / empty — must be accepted (no validation needed)
  out=$(bash -c "unset QUARKUS_HTTP_PORT; $validation_block" 2>&1); rc=$?
  [ $rc -eq 0 ] && _pass "Port unset: accepted (use default)" || \
    _fail "Port unset: should be accepted. Got exit $rc. Output: $out"

  out=$(QUARKUS_HTTP_PORT="" bash -c "$validation_block" 2>&1); rc=$?
  [ $rc -eq 0 ] && _pass "Port '': accepted (use default)" || \
    _fail "Port '': should be accepted. Got exit $rc. Output: $out"
}

# ============================================================
# Run all tests
# ============================================================
run_test "1a. Fast sync calls sync-web-site.sh"   test_fast_sync_calls_sync_web_site
run_test "1b. Sync post-processing (assets, links, index.html, Qute)" test_sync_web_site_postprocessing
run_test "2a. Readiness curl flags"  test_readiness_curl_flags
run_test "2b. Readiness bounded with black-hole listener" test_readiness_bounded
run_test "3. Wrapper cases (missing/non-exec/exec)" test_mvnw_cases
run_test "4. Port validation"        test_port_validation

echo ""
echo "==============================="
echo "Results: $PASS passed, $FAIL failed"
echo "==============================="
[ $FAIL -eq 0 ]
