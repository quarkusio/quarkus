#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
QUARKUS_ROOT="${1:-$PWD}"

cd "$QUARKUS_ROOT"

# --- Step 1: OpenRewrite (in-place transforms on the old module) ---
# Scope to specific modules: without -pl, ChangePackage would also rename references
# to io.quarkus.hibernate.panache in Panache 1 extensions (which use a different
# package hierarchy but import classes from it).
mvn -B org.openrewrite.maven:rewrite-maven-plugin:run \
    -Drewrite.activeRecipes=io.quarkus.data.RenamePanacheNextToQuarkusDataJpa \
    -Drewrite.configLocation="$SCRIPT_DIR/rename-panache-next-to-quarkus-data.yml" \
    -Drewrite.plainTextMasks="**/*.adoc,**/*.yaml,**/*.yml,**/*.qute,**/*.qute.xml,**/*.qute.java,**/*.qute.md,**/__snapshots__/**" \
    -pl extensions/panache/hibernate-panache-next/runtime,extensions/panache/hibernate-panache-next/deployment,extensions/panache,bom/application,docs,devtools/bom-descriptor-json,devtools/project-core-extension-codestarts,independent-projects/tools/base-codestarts,integration-tests/devtools

# --- Step 2: Fix string literals in Java files (OpenRewrite FindAndReplace can't touch Java sources) ---
sed -i '' 's/quarkus-hibernate-panache-next/quarkus-data-jpa/g' \
    integration-tests/devtools/src/test/java/io/quarkus/devtools/codestarts/quarkus/QuarkusDataJpaCodestartIT.java

# --- Step 3: Add quarkus-data module to extensions/pom.xml ---
# OpenRewrite AddOrUpdateChildTag doesn't fire outside -pl scope
sed -i '' '/<module>panache<\/module>/a\
        <module>quarkus-data</module>
' extensions/pom.xml

# --- Step 4: Move to new directory ---
# rm -rf ensures clean move even if OpenRewrite created target/ dirs there
mkdir -p extensions/quarkus-data
rm -rf extensions/quarkus-data/quarkus-data-jpa
mv extensions/panache/hibernate-panache-next extensions/quarkus-data/quarkus-data-jpa

# --- Step 5: Fix parent POM reference ---
# OpenRewrite ChangeParentPom doesn't fire on the hibernate-panache-next parent POM
sed -i '' 's|<artifactId>quarkus-panache-parent</artifactId>|<artifactId>quarkus-data-parent</artifactId>|' \
    extensions/quarkus-data/quarkus-data-jpa/pom.xml
