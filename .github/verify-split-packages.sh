#!/bin/bash

# Purpose: Fail when a Java package is present in more than one Maven module of the main sources,
# unless the package is listed in .github/split-packages-allowlist.txt.
# Java modules cannot share a package, see https://github.com/quarkusio/quarkus/issues/44710.
# Integration tests, TCKs and docs are not checked.

set -e -u -o pipefail

PRG_PATH=$( cd "$(dirname "$0")" ; pwd -P )
ALLOWLIST="${PRG_PATH}/split-packages-allowlist.txt"
cd "${PRG_PATH}/.."

# module directory -> package, one line per (module, package) pair
# only real modules: the directory before src/main/java must hold a pom.xml (templates under resources or test files do not)
PAIRS=$(find core extensions independent-projects devtools test-framework -path '*/src/main/java/*' \( -name '*.java' -o -name '*.kt' \) \
  | grep -v -E '/src/(main/resources|test)/' \
  | sed -E 's#^(.*)/src/main/java/(.*)/[^/]+$#\1 \2#' \
  | sort -u \
  | while read -r module pkg; do [ -f "${module}/pom.xml" ] && echo "${pkg//\//.} ${module}"; done)

# packages with more than one module
SPLIT=$(echo "${PAIRS}" | awk '{ count[$1]++; modules[$1] = modules[$1] " " $2 } END { for (p in count) if (count[p] > 1) print p modules[p] }' | sort)

STATUS=0
while read -r line; do
  [ -z "${line}" ] && continue
  pkg="${line%% *}"
  if ! grep -qx "${pkg}" "${ALLOWLIST}"; then
    echo "Package ${pkg} is split across modules:${line#${pkg}}"
    STATUS=1
  fi
done <<< "${SPLIT}"

while read -r pkg; do
  case "${pkg}" in ''|'#'*) continue ;; esac
  if ! echo "${SPLIT}" | grep -q "^${pkg} "; then
    echo "Package ${pkg} is no longer split and can be removed from ${ALLOWLIST}"
  fi
done < "${ALLOWLIST}"

if [ ${STATUS} -ne 0 ]; then
  echo "A Java package must not be present in more than one module; move the classes of one side to a package of its own."
  exit 1
fi
echo "No new split packages"
