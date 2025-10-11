#!/bin/bash

# Purpose: Prints a filtered version of matrix-jvm-tests.json
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

export IFS=$'\n'

# path of this shell script
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

JSON=$(cat ${PRG_PATH}/matrix-jvm-tests.json)
if [[ "${GITHUB_REPOSITORY:-DEFINED_ON_CI}" != "quarkusio/quarkus" ]]; then
  # Filter out mac os from a matrix of build configurations.
  # The reason we do this is that running mac on a fork won't work; the fork won't have a self-hosted runner
  # See https://stackoverflow.com/questions/65384420/how-to-make-a-github-action-matrix-element-conditional
  JSON=$(echo -n "$JSON" | jq 'map(. | select(.["os-name"]!="macos-arm64-latest"))')
fi

JSON=$(echo "$JSON" | jq '
  map(
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

# Step 0: print unfiltered json and exit in case the parameter is '_all_' (full build) or print nothing if empty (no changes)
if [ "$1" == '_all_' ]
then
  echo \{java: ${JSON}\}
  exit 0
elif [ -z "$1" ]
then
  echo -n ''
  exit 0
fi

RUNTIME_MODULES=$(echo -n "$1" | grep -Pv '^(integration-tests|tcks|docs)($|/.*)' || echo '')
INTEGRATION_TESTS=$(echo -n "$1" | grep -Po '^integration-tests/.+' | grep -Pv '^integration-tests/(devtools|gradle|maven|devmode|kubernetes)($|/.*)' || echo '')

if [ -z "$RUNTIME_MODULES" ] && [ -z "$INTEGRATION_TESTS" ]; then
  echo -n ''
  exit 0
fi

if [ -z "$RUNTIME_MODULES" ]; then
  JSON=$(echo -n $JSON | jq --arg category Runtime 'del( .[] | select(.category == $category) )')
else
  RUNTIME_MODULES_COMMAND="-pl\n"
  for RUNTIME_MODULE in $RUNTIME_MODULES; do
    RUNTIME_MODULES_COMMAND+="${RUNTIME_MODULE},"
  done
  JSON=$(echo -n $JSON | jq --arg category Runtime --arg modules "${RUNTIME_MODULES_COMMAND}" '( .[] | select(.category == $category) ).modules = $modules')
fi

if [ -z "$INTEGRATION_TESTS" ]; then
  JSON=$(echo -n $JSON | jq --arg category Integration 'del( .[] | select(.category == $category) )')
else
  INTEGRATION_TESTS_COMMAND="-pl\n"
  for INTEGRATION_TEST in $INTEGRATION_TESTS; do
    INTEGRATION_TESTS_COMMAND+="${INTEGRATION_TEST},"
  done
  JSON=$(echo -n $JSON | jq --arg category Integration --arg modules "${INTEGRATION_TESTS_COMMAND}" '( .[] | select(.category == $category) ).modules = $modules')
fi

echo \{java: ${JSON}\}
