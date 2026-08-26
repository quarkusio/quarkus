#!/usr/bin/env bash
# find-deprecated.sh
#
# Scans Java files for @Deprecated annotations and lists them sorted by the
# date they were *originally* introduced, using git blame with move/copy
# detection to avoid attributing a line to a later refactor/move commit.
#
# Usage:
#   ./find-deprecated.sh [search_root] [--csv] [--removable | --min-age-months=N]
#
#   search_root         Directory to scan (default: current working directory)
#   --csv               Emit CSV instead of a formatted table
#   --removable         Only report entries introduced at least 12 months ago
#                       (i.e. candidates for removal). Shorthand for
#                       --min-age-months=12.
#   --min-age-months=N  Only report entries introduced at least N months ago.
#                       Default (0) reports ALL deprecations, which is what you
#                       want when completing missed deprecation annotations.
#
# Entries whose introduction date cannot be resolved (blame "unknown") are
# omitted when an age filter is active, because their age can't be confirmed.
#
# Output columns:
#   File Location   –  path:line of the @Deprecated annotation
#   Deprecated Entry –  first non-annotation declaration line below it
#   Date            –  YYYY-MM-DD when the line was first introduced
#   Commit          –  short commit hash from git blame
#
# Flags passed to git blame to minimise "false-late" dates caused by code moves:
#   -w          ignore whitespace-only changes
#   -M          detect lines moved/copied within the same file
#   -C -C -C    detect lines moved/copied from other files across all of history

set -euo pipefail

# ── Argument parsing ──────────────────────────────────────────────────────────
SEARCH_ROOT="."
OUTPUT_CSV=0
MIN_AGE_MONTHS=0

for arg in "$@"; do
    case "$arg" in
        --csv)                OUTPUT_CSV=1 ;;
        --removable)          MIN_AGE_MONTHS=12 ;;
        --min-age-months=*)   MIN_AGE_MONTHS="${arg#*=}" ;;
        *)                    SEARCH_ROOT="$arg" ;;
    esac
done

SEARCH_ROOT="$(cd "$SEARCH_ROOT" && pwd)"
REPO_ROOT="$(git -C "$SEARCH_ROOT" rev-parse --show-toplevel 2>/dev/null || echo "$SEARCH_ROOT")"

# ── Age cutoff ─────────────────────────────────────────────────────────────────
# When an age filter is requested, compute the cutoff timestamp. Entries whose
# introduction timestamp is > cutoff (newer than N months) are filtered out.
# Try GNU date first, then BSD/macOS date.
CUTOFF_TS=0
if [[ "$MIN_AGE_MONTHS" -gt 0 ]]; then
    CUTOFF_TS=$(date -d "-${MIN_AGE_MONTHS} months" +%s 2>/dev/null \
                || date -v-"${MIN_AGE_MONTHS}"m +%s 2>/dev/null \
                || echo 0)
    if [[ "$CUTOFF_TS" -eq 0 ]]; then
        echo "WARNING: could not compute a date cutoff; reporting all entries." >&2
    fi
fi

# ── Helpers ───────────────────────────────────────────────────────────────────

# get_context FILE LINE_NUM
# Reads the file and returns the first non-annotation, non-comment, non-blank
# line starting at LINE_NUM (i.e. the declaration that is being deprecated).
get_context() {
    local file="$1"
    local start="$2"
    local full_path="$REPO_ROOT/$file"

    [[ -f "$full_path" ]] || { echo ""; return; }

    awk -v start="$start" '
        NR >= start {
            # skip blank, annotation, javadoc/comment lines
            gsub(/^[[:space:]]+/, "")
            if ($0 == "")            { next }
            if ($0 ~ /^@/)           { next }
            if ($0 ~ /^\/\//)        { next }
            if ($0 ~ /^\*/)          { next }
            if ($0 ~ /^\/\*/)        { next }
            # strip trailing brace, collapse whitespace, truncate
            sub(/[[:space:]]*\{[[:space:]]*$/, "")
            # collapse internal whitespace
            gsub(/[[:space:]]+/, " ")
            printf "%s", substr($0, 1, 120)
            exit
        }
    ' "$full_path"
}

# blame_line FILE LINE_NUM  →  prints "COMMIT UNIX_TIMESTAMP"
#
# Uses -C -C -C so git traces the line back through file copies/renames across
# the full history, giving us the *original* introduction date rather than the
# date a refactor moved the line.
blame_line() {
    local file="$1"
    local line="$2"

    local porcelain
    porcelain=$(git -C "$REPO_ROOT" blame -w -M -C -C -C \
                    --porcelain -L "${line},${line}" -- "$file" 2>/dev/null) || true

    local commit="" ts=""
    while IFS= read -r bl; do
        if [[ -z "$commit" ]]; then
            commit="${bl%% *}"          # first token of first line = hash
        fi
        if [[ "$bl" == author-time\ * ]]; then
            ts="${bl#author-time }"
            break
        fi
    done <<< "$porcelain"

    echo "${commit:0:8} ${ts:-0}"
}

# unix_to_date UNIX_TIMESTAMP  →  YYYY-MM-DD
unix_to_date() {
    local ts="$1"
    if [[ "$ts" == "0" || -z "$ts" ]]; then
        echo "unknown"
    else
        date -d "@$ts" +"%Y-%m-%d" 2>/dev/null \
            || date -r "$ts" +"%Y-%m-%d" 2>/dev/null \
            || echo "unknown"
    fi
}

# ── Main ──────────────────────────────────────────────────────────────────────

echo "Scanning $SEARCH_ROOT …" >&2

# Collect all @Deprecated locations: "file:line"
mapfile -t locations < <(
    rg --line-number --no-heading --color=never -g '*.java' '@Deprecated' \
       "$SEARCH_ROOT" \
    | sed "s|^$REPO_ROOT/||"   # make paths relative to repo root
)

total=${#locations[@]}
echo "Found $total @Deprecated annotations, resolving blame …" >&2

# Temp file stores: UNIX_TS TAB file:line TAB context TAB commit
tmpfile=$(mktemp)
trap 'rm -f "$tmpfile"' EXIT

idx=0
for loc in "${locations[@]}"; do
    idx=$(( idx + 1 ))
    (( idx % 50 == 0 || idx == total )) && echo "  $idx/$total" >&2

    # Split "path/to/File.java:42:  @Deprecated" → file and line number
    file="${loc%%:*}"
    rest="${loc#*:}"
    line="${rest%%:*}"

    read -r commit ts < <(blame_line "$file" "$line")
    context=$(get_context "$file" "$(( line + 1 ))")

    printf '%s\t%s\t%s\t%s\n' \
        "${ts:-0}" "${file}:${line}" "$context" "${commit:-unknown}" \
        >> "$tmpfile"
done

# Sort by unix timestamp (column 1), ascending
sorted=$(sort -t$'\t' -k1,1n "$tmpfile")

# Apply the age filter, if requested: keep only entries with a known timestamp
# (column 1 != 0) that is at or before the cutoff (i.e. at least N months old).
if [[ "$MIN_AGE_MONTHS" -gt 0 && "$CUTOFF_TS" -gt 0 ]]; then
    sorted=$(awk -F'\t' -v cutoff="$CUTOFF_TS" '$1 != 0 && $1 <= cutoff' <<< "$sorted")
fi

# Count what will actually be shown (may differ from $total once filtered).
shown=$(grep -c . <<< "$sorted" 2>/dev/null || echo 0)
[[ -z "$sorted" ]] && shown=0

# ── Render output ─────────────────────────────────────────────────────────────

if [[ "$OUTPUT_CSV" -eq 1 ]]; then
    echo "File Location,Deprecated Entry,Date,Commit"
    while IFS=$'\t' read -r ts fileloc context commit; do
        date_str=$(unix_to_date "$ts")
        # CSV-escape fields (wrap in quotes, double any internal quotes)
        printf '"%s","%s","%s","%s"\n' \
            "${fileloc//\"/\"\"}" \
            "${context//\"/\"\"}" \
            "$date_str" \
            "$commit"
    done <<< "$sorted"
else
    FILE_W=70
    ENTRY_W=90
    DATE_W=12
    CMT_W=9

    sep_len=$(( FILE_W + ENTRY_W + DATE_W + CMT_W + 6 ))

    printf "%-${FILE_W}s  %-${ENTRY_W}s  %-${DATE_W}s  %-${CMT_W}s\n" \
        "File Location" "Deprecated Entry" "Date" "Commit"
    printf '%*s\n' "$sep_len" '' | tr ' ' '-'

    while IFS=$'\t' read -r ts fileloc context commit; do
        date_str=$(unix_to_date "$ts")
        printf "%-${FILE_W}s  %-${ENTRY_W}s  %-${DATE_W}s  %-${CMT_W}s\n" \
            "${fileloc:0:$FILE_W}" \
            "${context:0:$ENTRY_W}" \
            "$date_str" \
            "$commit"
    done <<< "$sorted"
fi

echo "" >&2
if [[ "$MIN_AGE_MONTHS" -gt 0 ]]; then
    echo "Total: $shown of $total entries (at least ${MIN_AGE_MONTHS} months old)" >&2
else
    echo "Total: $total entries" >&2
fi
