#!/bin/bash

# Purpose: Updates pom.xml files with symbolic dependencies to extensions to enforce a consistent build order.

set -e -u -o pipefail
shopt -s failglob

echo ''
echo 'Building bom-descriptor-json...'
echo ''
mvn clean package -f devtools/bom-descriptor-json

DEP_TEMPLATE='        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>XXX</artifactId>
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
echo 'Building dependencies list from bom-descriptor-json...'
echo ''

ARTIFACT_IDS=`grep -Po '(?<="artifact-id": ")(?!quarkus-bom)[^"]+' devtools/bom-descriptor-json/target/*.json | sort`

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
SED_EXPR_DEPS_RUNTIME="/${MARK_START}/,/${MARK_END}/c\        ${MARK_START}\n${DEPS_RUNTIME}\n        ${MARK_END}"
SED_EXPR_DEPS_DEPLOYMENT="/${MARK_START}/,/${MARK_END}/c\        ${MARK_START}\n${DEPS_DEPLOYMENT}\n        ${MARK_END}"

echo ''
echo 'Updating devtools/bom-descriptor-json/pom.xml...'
echo ''
sed -i "${SED_EXPR_DEPS_RUNTIME}" devtools/bom-descriptor-json/pom.xml

echo ''
echo 'Updating docs/pom.xml...'
echo ''
sed -i "${SED_EXPR_DEPS_DEPLOYMENT}" docs/pom.xml
