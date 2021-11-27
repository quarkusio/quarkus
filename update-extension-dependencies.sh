#!/bin/bash

# Purpose: Updates pom.xml files with minimal dependencies to extensions to enforce a consistent build order.

set -e -u -o pipefail
shopt -s failglob

# path of this shell script (note: readlink -f does not work on Mac)
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

# CI env var => https://docs.github.com/en/actions/reference/environment-variables#default-environment-variables
if [ "${CI:-}" == true ]
then
  echo ''
  echo 'Building bom-descriptor-json...'
  echo ''
  mvn -q -e clean package -f "${PRG_PATH}/devtools/bom-descriptor-json" -Denforcer.skip $*
else
  read -n1 -p 'Did you build the entire project? [y/n] ' ANSWER
  echo ''
  if [ "${ANSWER}" != y ]
  then
    echo ''
    echo 'Building entire project...'
    echo ''
    mvn -q -e -Dquickly -T0.8C -f "${PRG_PATH}" $*
  fi
fi

# note: OFFSET is replaced later on with an individual amount of spaces
DEP_TEMPLATE='OFFSET<dependency>
OFFSET    <groupId>io.quarkus</groupId>
OFFSET    <artifactId>XXX</artifactId>
OFFSET    <version>\${project.version}</version>
OFFSET    <type>pom</type>
OFFSET    <scope>test</scope>
OFFSET    <exclusions>
OFFSET        <exclusion>
OFFSET            <groupId>*</groupId>
OFFSET            <artifactId>*</artifactId>
OFFSET        </exclusion>
OFFSET    </exclusions>
OFFSET</dependency>'

echo ''
echo 'Building dependencies list from bom-descriptor-json...'
echo ''

# get all "artifact-id" values from the generated json file
# pipefail is switched off briefly so that a better error can be logged when nothing is found
set +o pipefail
ARTIFACT_IDS=$(cd "${PRG_PATH}" && grep '^    "artifact"' devtools/bom-descriptor-json/target/quarkus-bom-quarkus-platform-descriptor-*.json | grep -Eo 'quarkus-[a-z0-9-]+' | sort)
set -o pipefail
if [ -z "${ARTIFACT_IDS}" ]
then
  echo -e '\033[0;31mError:\033[0m Could not find any artifact-ids. Please check the grep command. ' 1>&2
  exit 1
fi

# note: that bulky last sed in the following commands replaces newlines with \n so that the final sed calls accept ${DEPS_*} as input
# see also: https://stackoverflow.com/a/1252191/9529981

DEPS_RUNTIME=$(echo "${ARTIFACT_IDS}" \
  | while read AID; do echo "${DEP_TEMPLATE/XXX/${AID}}"; done \
  | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\\n/g')

DEPS_DEPLOYMENT=$(echo "${ARTIFACT_IDS}" \
  | while read AID; do echo "${DEP_TEMPLATE/XXX/${AID}-deployment}"; done \
  | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\\n/g')

MARK_START='<!-- START update-extension-dependencies.sh -->'
MARK_END='<!-- END update-extension-dependencies.sh -->'
# https://superuser.com/a/307486
LF=$'\n'
# note: line breaks after c command are required for MacOS compatibility: https://unix.stackexchange.com/a/52141
SED_EXPR_DEPS_RUNTIME="/${MARK_START}/,/${MARK_END}/c\\
OFFSET${MARK_START}\\${LF}${DEPS_RUNTIME}\\${LF}OFFSET${MARK_END}"
SED_EXPR_DEPS_DEPLOYMENT="/${MARK_START}/,/${MARK_END}/c\\
OFFSET${MARK_START}\\${LF}${DEPS_DEPLOYMENT}\\${LF}OFFSET${MARK_END}"
# BSD sed (on MacOS) consumes one line break too much which will be fixed by the following additional expression:
SED_EXPR_BSD_FIXUP="s/${MARK_END}([ ]*)<\/dependencies>/${MARK_END}\\${LF}\1<\/dependencies>/"

# note: the following sed commands do not use -i because behavior on MacOS is different than on Linux

echo ''
echo 'Updating devtools/bom-descriptor-json/pom.xml...'
echo ''
sed "${SED_EXPR_DEPS_RUNTIME}" "${PRG_PATH}/devtools/bom-descriptor-json/pom.xml" > /tmp/bom-descriptor-json-pom.xml
# note: more indentation here since bom-descriptor-json has a profile for the deps
sed -r -e 's/OFFSET/                /' -e "${SED_EXPR_BSD_FIXUP}" /tmp/bom-descriptor-json-pom.xml > "${PRG_PATH}/devtools/bom-descriptor-json/pom.xml"
rm -f /tmp/bom-descriptor-json-pom.xml

echo ''
echo 'Updating docs/pom.xml...'
echo ''
sed "${SED_EXPR_DEPS_DEPLOYMENT}" "${PRG_PATH}/docs/pom.xml" > /tmp/docs-pom.xml
sed -r -e 's/OFFSET/        /' -e "${SED_EXPR_BSD_FIXUP}" /tmp/docs-pom.xml > "${PRG_PATH}/docs/pom.xml"
rm -f /tmp/docs-pom.xml

echo ''
echo 'Sanity check...'
echo ''
# sanity check; make sure nothing stupid was added like non-existing deps
mvn dependency:resolve validate -Dsilent -q -f "${PRG_PATH}" -pl devtools/bom-descriptor-json,docs $*

# CI only: verify that no pom.xml was touched (if changes are found, committer forgot to run script or to add changes)
if [ "${CI:-}" == true ] && [ $(git status -s -u no '*pom.xml' | wc -l) -ne 0 ]
then
  echo -e '\033[0;31mError:\033[0m Dependencies to extension artifacts are outdated!' 1>&2
  echo -e '\033[0;31mError:\033[0m Run ./update-extension-dependencies.sh and add the modified pom.xml files to your commit.' 1>&2
  echo -e '\033[0;31mError:\033[0m Diff is:' 1>&2
  git --no-pager diff '*pom.xml' 1>&2
  exit 1
fi
