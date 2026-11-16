#!/bin/bash

# Purpose: Computes the test plan of the CI: which test job tests which modules.
#
# The modules to test (one path per line, relative to the repository root) are dispatched to groups,
# according to the rules defined in test-plan-rules.json:
# - "ignored" is a list of regular expressions applied to the path of the module:
#   a matching module is not tested by any of the jobs relying on the test plan.
# - "rules" is an ordered list of rules, the first match wins. A rule matches a module either with "pattern",
#   a regular expression applied to the path of the module, or with "modules", an explicit list of paths.
#   The explicit lists are used to isolate the modules having the longest test suites in their own job:
#   they have to be rebalanced from time to time, from the durations reported by the builds.
# - "matrix-categories" maps the categories of matrix-jvm-tests.json to the groups they have to run,
#   see filter-jvm-tests-json.sh.
#
# The following files are written to the output directory:
# - test-plan.json: a JSON object, the keys are the groups and the values are the lists of modules.
#   A group with no module to test is not part of the test plan.
# - <group>.maven-config: the selection of the modules of the group, to append to .mvn/maven.config.
#   Using .mvn/maven.config is required as the list of modules can exceed the cmd.exe command line limit on Windows.
#   The format of this file is specific: it's one option per line,
#   see https://maven.apache.org/configure.html#a.mvn.2Fmaven.config_file.3A
#
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

if (( $# < 2 || $# > 3 ))
then
  echo 'Invalid parameters' >&2
  echo "Usage: $0 <file containing the modules to test, one per line> <output directory> [<origin of the modules to test, for reporting>]" >&2
  exit 1
fi

# path of this shell script
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

MODULES_FILE="$1"
OUTPUT_DIR="$2"
ORIGIN="${3:-}"
TEST_PLAN="${OUTPUT_DIR}/test-plan.json"

mkdir -p "${OUTPUT_DIR}"

# Make sure the modules explicitly listed in the rules exist: a module might have been moved or removed
INVALID_MODULES=()
while IFS= read -r MODULE; do
  if [ ! -f "${PRG_PATH}/../${MODULE}/pom.xml" ]; then
    INVALID_MODULES+=("${MODULE}")
  fi
done < <(jq -r '.rules[] | select(.modules != null) | .modules[]' "${PRG_PATH}/test-plan-rules.json")
if (( ${#INVALID_MODULES[@]} > 0 )); then
  echo "The following modules listed in test-plan-rules.json do not exist:" >&2
  printf '  %s\n' "${INVALID_MODULES[@]}" >&2
  exit 1
fi

jq -n --rawfile modules "${MODULES_FILE}" --slurpfile config "${PRG_PATH}/test-plan-rules.json" '
  $config[0].rules as $rules
  | $config[0].ignored as $ignored
  | [ $modules | split("\n")[] | select(length > 0) ]
  | unique
  | map(select(. as $module | any($ignored[]; .pattern as $pattern | $module | test($pattern)) | not))
  | map(
      . as $module
      | {
          module: $module,
          group: (
            (first($rules[] | select(
                if .modules != null then (.modules | index($module)) != null
                else (.pattern as $pattern | $module | test($pattern)) end))
              // error("No rule matching module: " + $module))
            | .group
          )
        }
    )
  | reduce .[] as $entry ({}; .[$entry.group] += [$entry.module])
' > "${TEST_PLAN}"

# IMPORTANT: we have a module with spaces in the name (to test spaces are supported) so we have to be careful with the separators
while IFS= read -r GROUP; do
  jq -r --arg group "${GROUP}" '"-pl\n" + (.[$group] | join(","))' "${TEST_PLAN}" > "${OUTPUT_DIR}/${GROUP}.maven-config"
done < <(jq -r 'keys[]' "${TEST_PLAN}")

# Print the test plan in the summary of the job and in the log (one collapsible section per group)
SUMMARY=$(jq -r --arg origin "${ORIGIN}" '
  "## Test plan\n",
  (select($origin != "") | "\($origin)\n"),
  if length == 0 then
    "No module to test."
  else
    "| Group | Modules |",
    "|---|---|",
    (to_entries[] | "| `\(.key)` | \(.value | length) |"),
    "",
    (to_entries[] | "<details><summary><code>\(.key)</code> (\(.value | length))</summary>\n\n```\n\(.value | join("\n"))\n```\n\n</details>\n")
  end
' "${TEST_PLAN}")

if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  echo "${SUMMARY}" >> "${GITHUB_STEP_SUMMARY}"
fi

if [ -n "${ORIGIN}" ]; then
  echo "Test plan: ${ORIGIN}" >&2
fi
jq -r '
  if length == 0 then
    "Test plan: no module to test"
  else
    to_entries[] | "::group::Test plan: \(.key) - \(.value | length) module(s)\n\(.value | join("\n"))\n::endgroup::"
  end
' "${TEST_PLAN}" >&2
