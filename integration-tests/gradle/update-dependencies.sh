#!/bin/bash

# Purpose: Updates pom.xml with (minimal) dependencies to other modules to enforce a consistent build order.

set -e -u -o pipefail
shopt -s failglob

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
ARTIFACT_IDS=`grep -hR --include 'build*.gradle' --exclude-dir=build '[iI]mplementation' | \
              grep -Po '(?<=io\.quarkus:)quarkus-[a-z0-9-]+' | \
              sed -e '/-deployment/d' -e '/quarkus-bom/d' | sort | uniq`
set -o pipefail
if [ -z "${ARTIFACT_IDS}" ]
then
  echo -e '\033[0;31mError:\033[0m Could not find any artifact-ids. Please check the grep command. ' 1>&2
  exit 1
fi

# to replace newlines with \n so that the final sed calls accept ${DEPS} as input
SED_EXPR_NEWLINES=':a;N;$!ba;s/\n/\\\n/g'

DEPS=`echo "${ARTIFACT_IDS}" \
  | xargs -i sh -c "echo \"${DEP_TEMPLATE}\" | sed 's/XXX/{}/'" \
  | sed "${SED_EXPR_NEWLINES}"`

DEPS+='\n'

# note on sed: Remove artifacts that are not extensions (since not -deployment sibling exists).
#              This is a bit fragile but without this -deployment deps would need to be added by hand.
DEPS+=`echo "${ARTIFACT_IDS}" \
  | sed -e '/-bootstrap/d' -e '/-test/d' -e '/-junit/d' -e '/-devtools/d' -e '/-descriptor/d' \
  | xargs -i sh -c "echo \"${DEP_TEMPLATE_DEPLOYMENT}\" | sed 's/XXX/{}/'" \
  | sed "${SED_EXPR_NEWLINES}"`

MARK_START='<!-- START update-dependencies.sh -->'
MARK_END='<!-- END update-dependencies.sh -->'
SED_EXPR="/${MARK_START}/,/${MARK_END}/c\        ${MARK_START}\n${DEPS}\n        ${MARK_END}"

echo ''
echo 'Updating pom.xml...'
echo ''
sed -i "${SED_EXPR}" pom.xml

echo ''
echo 'Sanity check...'
echo ''
# sanity check; make sure nothing stupid was added like non-existing deps
mvn dependency:resolve validate -Dsilent -q $*