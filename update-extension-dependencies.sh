#!/bin/bash

# Purpose: Updates pom.xml files with symbolic dependencies to extensions to enforce a consistent build order.

set -e -u -o pipefail
shopt -s failglob

echo ''
echo 'Building bom-descriptor-json...'
echo ''
mvn -e clean package -f devtools/bom-descriptor-json -Denforcer.skip $*

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
ARTIFACT_IDS=`grep '"artifact"' devtools/bom-descriptor-json/target/*.json | grep -Po '(?<=io.quarkus:)(?!quarkus-bom)[^:]+' | sort`
set -o pipefail
if [ -z "${ARTIFACT_IDS}" ]
then
  echo -e '\033[0;31mError:\033[0m Could not find any artifact-ids. Please check the grep command. ' 1>&2
  exit 1
fi

# to replace newlines with \n so that the final sed calls accept ${DEPS_*} as input
SED_EXPR_NEWLINES=':a;N;$!ba;s/\n/\\\n/g'

DEPS_RUNTIME=`echo "${ARTIFACT_IDS}" \
  | xargs -i sh -c "echo \"${DEP_TEMPLATE}\" | sed 's/XXX/{}/'" \
  | sed "${SED_EXPR_NEWLINES}"`

DEPS_DEPLOYMENT=`echo "${ARTIFACT_IDS}" \
  | xargs -i sh -c "echo \"${DEP_TEMPLATE}\" | sed 's/XXX/{}-deployment/'" \
  | sed "${SED_EXPR_NEWLINES}"`

MARK_START='<!-- START update-extension-dependencies.sh -->'
MARK_END='<!-- END update-extension-dependencies.sh -->'
SED_EXPR_DEPS_RUNTIME="/${MARK_START}/,/${MARK_END}/cOFFSET${MARK_START}\n${DEPS_RUNTIME}\nOFFSET${MARK_END}"
SED_EXPR_DEPS_DEPLOYMENT="/${MARK_START}/,/${MARK_END}/cOFFSET${MARK_START}\n${DEPS_DEPLOYMENT}\nOFFSET${MARK_END}"

echo ''
echo 'Updating devtools/bom-descriptor-json/pom.xml...'
echo ''
sed -i "${SED_EXPR_DEPS_RUNTIME}" devtools/bom-descriptor-json/pom.xml
# note: more indetation here since bom-descriptor-json has a profile for the deps
sed -i 's/OFFSET/                /' devtools/bom-descriptor-json/pom.xml

echo ''
echo 'Updating docs/pom.xml...'
echo ''
sed -i "${SED_EXPR_DEPS_DEPLOYMENT}" docs/pom.xml
sed -i 's/OFFSET/        /' docs/pom.xml
