#!/bin/bash

# Purpose: Verify that a JSON file such as native-tests.json only contains valid references in its "test-modules" fields
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

# path of this shell script
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

if (( $# != 2 ))
then
  echo 'Invalid parameters'
  echo 'Usage: $0 <path from .github/ to json file to check> <path from repository root to integration test directory>'
  exit 1
fi

JSON_PATH="${PRG_PATH}/$1"
IT_DIR_PATH="$2"

echo "Checking JSON file $JSON_PATH against modules in directory $IT_DIR_PATH"

INVALID_REFS=($(
  # Print unmatched names from the JSON file
  # Input 1: List all test modules from the JSON file, one per line, trimmed, sorted
  # Input 2: List all Maven modules, one per line, sorted
  join -v 1 \
    <(jq -r '.include[] | ."test-modules"' ${PRG_PATH}/$1 | tr ',' $'\n' | sed 's|^\s*||;s|\s*$||;' | grep -v '^$' | sort) \
    <(find "$IT_DIR_PATH" -mindepth 2 -name pom.xml -exec realpath --relative-to "$IT_DIR_PATH" '{}' \; | xargs -d $'\n' -n 1 dirname | sort)
))

if [[ ${#INVALID_REFS[@]} = 0 ]]
then
    echo "$JSON_PATH is valid when checked against $IT_DIR_PATH"
    exit 0
else
    echo "$JSON_PATH is invalid when checked against $IT_DIR_PATH"
    echo "'test-modules' that cannot be resolved as paths to Maven modules relative to $IT_DIR_PATH: ${INVALID_REFS[*]}"
    exit 1
fi