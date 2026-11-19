#!/bin/bash

# Purpose: Prints the paths of all the modules of the Maven reactor, one per line, relative to the repository root.
#          The root module is printed as '.'.
#          Any argument is passed to Maven (e.g. --settings ...).
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

ROOT_PATH=$(pwd -P)
MARKER='REACTOR_MODULE:'

# -Dtcks: the TCKs are tested on CI so they have to be part of the list
# the trailing slash makes sure the expression is evaluated as a String and not as a File
./mvnw -q -T1C "$@" -Dscan=false -Dtcks -Dscalpel.enabled=false \
    org.codehaus.mojo:exec-maven-plugin:exec -Dexec.executable=echo "-Dexec.args=${MARKER}\${project.basedir}/" \
  | grep "^${MARKER}" \
  | sed -e "s|^${MARKER}${ROOT_PATH}/||" -e 's|/$||' -e 's|^$|.|' \
  | sort
