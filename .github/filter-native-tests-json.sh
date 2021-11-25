#!/bin/bash

# Purpose: Prints a filtered version of native-tests.json, with "test-modules" reduced to the ones passed in as the first argument.
#          This first argument is expected to the define one module per line.
#          "include" elements that (after filtering) have no "test-modules" anymore are deleted entirely!
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

# path of this shell script
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

JSON=$(cat ${PRG_PATH}/native-tests.json)

# Step 0: print unfiltered json and exit in case the parameter is empty (assumption: full build)
if [ -z "$1" ]
then
  echo "${JSON}"
  exit 0
fi

# Step 1: build an expression for grep that will only extract the given modules from each "test-modules" list,
#         including a trailing comma (if exists). Note: mvn doesn't mind something like -pl 'foo,'.
EXPR='((?:(?<=^)|(?<=,)|(?<=, ))('
while read -r impacted
do
  EXPR+="${impacted}|"
done < <(echo -n "$1" | grep -Po '(?<=integration-tests/).+')
EXPR+=')(,|$))+'

# Step 2: apply the filter expression via grep to each "test-modules" list and replace each original list with the filtered one
while read -r modules
do
  # Notes:
  # - trailing "|" (after EXPR) avoids grep return code > 0 if nothing matches (which is a valid case)
  # - "paste" joins all matches to get a single line
  FILTERED=$(echo -n "${modules}" | grep -Po "${EXPR}|" | paste -sd " " -)
  JSON=$(echo -n "${JSON}" | sed "s|${modules}|${FILTERED}|")
done < <(echo -n "${JSON}" | jq -r '.include[] | ."test-modules"')

# Step 3: delete entire elements from "include" array that now have an empty "test-modules" list
JSON=$(echo "${JSON}" | jq 'del(.include[] | select(."test-modules" == ""))')

# Step 4: echo final result, printing only {} in case _all_ elements were removed from "include" array
if [ -z "$(echo "${JSON}" | jq '.include[]')" ]
then
  echo -n '{}'
else
  echo -n "${JSON}"
fi
