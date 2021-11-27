#!/bin/bash

# Purpose: Updates resteasy-reactive-testsuite.repo.ref in pom.xmls (after changes were pushed to the testuite repo).
#          First parameter is the new ref (mandatory).

set -e -u -o pipefail
shopt -s failglob

# path of this shell script (note: readlink -f does not work on Mac)
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

# note: the following sed command does not use -i because behavior on MacOS is different than on Linux
sed -e "s/<resteasy-reactive-testsuite.repo.ref>.*</<resteasy-reactive-testsuite.repo.ref>$1</" \
  "${PRG_PATH}/pom.xml" > /tmp/tcks-resteasy-reactive-pom.xml
mv /tmp/tcks-resteasy-reactive-pom.xml "${PRG_PATH}/pom.xml"
