#!/bin/bash

BASEDIR=$(dirname "$0")

# Filter out mac os from a matrix of build configurations.
# The reason we do this is that running mac on a fork won't work; the fork won't have a self-hosted runner

# See https://stackoverflow.com/questions/65384420/how-to-make-a-github-action-matrix-element-conditional

repoName=${GITHUB_REPOSITORY}

if [[ $repoName == "quarkusio/quarkus" ]]
then
    matrix=$(cat $BASEDIR/matrix-jvm-tests.json)
else
  # Use jq to read in a json file that represents the matrix configuration.
  matrix=$(jq 'map(. | select(.["os-name"]!="macos-arm64-latest"))' $BASEDIR/matrix-jvm-tests.json)
fi

echo \{java: ${matrix}\}