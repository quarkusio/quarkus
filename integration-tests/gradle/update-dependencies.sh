#!/bin/bash

# Purpose: Updates pom.xml with (minimal) dependencies to other modules to enforce a consistent build order.

set -e -u -o pipefail
shopt -s failglob

# path of this shell script (note: readlink -f does not work on Mac)
PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )

DEP_TEMPLATE='        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>XXX</artifactId>
            <version>\${project.version}</version>
        </dependency>'

DEP_TEMPLATE_DEPLOYMENT='        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>XXX-deployment</artifactId>
            <version>\${project.version}</version>
            <type>pom</type>
            <scope>test</scope>
            <exclusions>
                <exclusion>
                    <groupId>*</groupId>
                    <artifactId>*</artifactId>
                </exclusion>
            </exclusions>
        </dependency>'

echo ''
echo 'Building dependencies list from gradle build files...'
echo ''

# get the Quarkus artifact ids from all gradle build files in this repo
# pipefail is switched off briefly so that a better error can be logged when nothing is found
set +o pipefail
# note on sed: -deployment deps are added explicitly later and bom is upstream anyway
ARTIFACT_IDS=$(grep -EhR --include 'build*.gradle*' --exclude-dir=build '[iI]mplementation|api|quarkusDev' "${PRG_PATH}" | \
              grep -Eo 'quarkus-[a-z0-9-]+' | \
              sed -e '/-deployment/d' -e '/quarkus-bom/d' | sort | uniq)
set -o pipefail
if [ -z "${ARTIFACT_IDS}" ]
then
  echo -e '\033[0;31mError:\033[0m Could not find any artifact-ids. Please check the grep command. ' 1>&2
  exit 1
fi

# note: that bulky last sed in the following commands replaces newlines with \n so that the final sed calls accept ${DEPS} as input
# see also: https://stackoverflow.com/a/1252191/9529981

DEPS=$(echo "${ARTIFACT_IDS}" \
  | while read AID; do echo "${DEP_TEMPLATE/XXX/${AID}}"; done \
  | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\\n/g')

# https://superuser.com/a/307486
LF=$'\n'

DEPS+="\\${LF}"

# note on sed: Remove artifacts that are not extensions (since not -deployment sibling exists).
#              This is a bit fragile but without this -deployment deps would need to be added by hand.
DEPS+=$(echo "${ARTIFACT_IDS}" \
  | sed -e '/-bootstrap/d' -e '/-test/d' -e '/-junit/d' -e '/-devtools/d' -e '/-descriptor/d' -e '/-extension-codestarts/d' \
  | while read AID; do echo "${DEP_TEMPLATE_DEPLOYMENT/XXX/${AID}}"; done \
  | sed -e ':a' -e 'N' -e '$!ba' -e 's/\n/\\\n/g')

MARK_START='<!-- START update-dependencies.sh -->'
MARK_END='<!-- END update-dependencies.sh -->'
# note: line break after c command is required for MacOS compatibility: https://unix.stackexchange.com/a/52141
SED_EXPR="/${MARK_START}/,/${MARK_END}/c\\
        ${MARK_START}\\${LF}${DEPS}\\${LF}        ${MARK_END}"
# BSD sed (on MacOS) consumes one line break too much which will be fixed by the following additional expression:
SED_EXPR_BSD_FIXUP="s/${MARK_END}    <\/dependencies>/${MARK_END}\\${LF}    <\/dependencies>/"

echo ''
echo 'Updating pom.xml...'
echo ''
# note: the following sed command does not use -i because behavior on MacOS is different than on Linux
sed -e "${SED_EXPR}" -e "${SED_EXPR_BSD_FIXUP}" "${PRG_PATH}/pom.xml" > /tmp/gradle-pom.xml
mv /tmp/gradle-pom.xml "${PRG_PATH}/pom.xml"

echo ''
echo 'Sanity check...'
echo ''
# sanity check; make sure nothing stupid was added like non-existing deps
mvn dependency:resolve validate -Dsilent -q -f "${PRG_PATH}" $*

# CI only: verify that no pom.xml was touched (if changes are found, committer forgot to run script or to add changes)
if [ "${CI:-}" == true ] && [ $(git status -s -u no '*pom.xml' | wc -l) -ne 0 ]
then
  echo -e '\033[0;31mError:\033[0m Dependencies in integration-tests/gradle/pom.xml are outdated!' 1>&2
  echo -e '\033[0;31mError:\033[0m Run update-dependencies.sh in integration-tests/gradle and add the modified pom.xml file to your commit.' 1>&2
  echo -e '\033[0;31mError:\033[0m Diff is:' 1>&2
  git --no-pager diff '*pom.xml' 1>&2
  exit 1
fi
