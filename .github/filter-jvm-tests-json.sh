#!/bin/bash

# Purpose: Prints the matrix of the JVM tests jobs: the entries of matrix-jvm-tests.json,
#          combined with the groups of the test plan (see compute-test-plan.sh) they have to run.
#          The groups an entry has to run are defined by its category, see "matrix-categories" in test-plan-rules.json:
#          a job is created for each group that is part of the test plan, its name is the name of the entry
#          with the "name-qualifier" of the group inserted after the first element of the name,
#          e.g. "JVM Tests - JDK 21" becomes "JVM Tests - Heavy modules - JDK 21".
#          The qualifier has to be before the JDK: the build reporter considers what is before the JDK
#          as the label of the job and uses it to group the jobs testing the same thing on various platforms.
#          A group can be restricted to some of the entries of its category with "entries" (the names of the entries).
#          An entry is dropped if none of its groups is part of the test plan.
#          The groups flagged with "gate" are tested first, the other test jobs only run if their tests pass:
#          the second parameter selects either the jobs of these groups (gate) or the other ones (main).
#          Nothing is printed if there is no entry left.
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

if (( $# != 2 )) || [[ "$2" != 'gate' && "$2" != 'main' ]]
then
  echo 'Invalid parameters' >&2
  echo "Usage: $0 <path to test-plan.json> <gate|main>" >&2
  exit 1
fi

# path of this shell script
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

TEST_PLAN="$1"
JOBS="$2"

JSON=$(cat ${PRG_PATH}/matrix-jvm-tests.json)
if [[ "${GITHUB_REPOSITORY:-DEFINED_ON_CI}" != "quarkusio/quarkus" ]]; then
  # Filter out mac os from a matrix of build configurations.
  # The reason we do this is that running mac on a fork won't work; the fork won't have a self-hosted runner
  # See https://stackoverflow.com/questions/65384420/how-to-make-a-github-action-matrix-element-conditional
  JSON=$(echo -n "$JSON" | jq 'map(. | select(.["os-name"]!="macos-arm64-latest"))')
fi

JSON=$(echo -n "$JSON" | jq -c --arg jobs "${JOBS}" --slurpfile plan "${TEST_PLAN}" --slurpfile config "${PRG_PATH}/test-plan-rules.json" '
  $config[0]["matrix-categories"] as $categories
  | map(
      . as $entry
      | ($categories[$entry.category] // error("No groups defined for category: " + $entry.category))[]
      | select($plan[0][.group] != null)
      | select(.entries == null or (.entries | index($entry.name)) != null)
      | select((.gate // false) == ($jobs == "gate"))
      | .["name-qualifier"] as $qualifier
      | $entry + {
          name: (if $qualifier != null then ($entry.name | sub(" - "; " - " + $qualifier + " - ")) else $entry.name end),
          group: .group
        }
    )
  | map(
      . + {
        tag: (
          .name
          | ascii_downcase
          | gsub(" "; "-")
          | gsub("-+"; "-")
        )
      }
    )
')

if [ "$JSON" == '[]' ]; then
  echo -n ''
  exit 0
fi

echo \{java: ${JSON}\}
