#!/bin/bash

# Purpose: Prints a comma-separated list of the core and extension deployment modules,
#          suitable for passing to Maven's -pl/--projects option.
# Note: This script is only for CI and does therefore not aim to be compatible with BSD/macOS.

set -e -u -o pipefail
shopt -s failglob

# An extension can be disabled by commenting out its <module> in extensions/pom.xml
# (e.g. smallrye-reactive-messaging-mqtt). Such a module is still tracked on disk, so
# git ls-files would list it, but it is not part of Maven's reactor and passing it to
# -pl fails the build. Collect the commented-out extensions and skip them.
disabled=$(grep -oE '<!--[[:space:]]*<module>[^<]+</module>[[:space:]]*-->' extensions/pom.xml \
           | sed -E 's|.*<module>([^<]+)</module>.*|extensions/\1/|')

# Note: git's ** matches one or more path segments, never zero,
# so core/deployment/pom.xml has to be listed explicitly.
deployment_modules=()
while IFS= read -r pom; do
  module="${pom%/pom.xml}"
  skip=
  for d in $disabled; do
    [[ "$module" == "$d"* ]] && { skip=1; break; }
  done
  if [[ -n "$skip" ]]; then
    printf 'Skipping disabled module: %s\n' "$module" >&2
  else
    deployment_modules+=("$module")
  fi
done < <(git ls-files 'core/deployment/pom.xml' 'core/**/deployment/pom.xml' 'extensions/**/deployment/pom.xml')

if (( ${#deployment_modules[@]} == 0 )); then
  echo "No deployment module found, this is not expected." >&2
  exit 1
fi

printf 'Found %d deployment modules:\n' "${#deployment_modules[@]}" >&2
printf '%s\n' "${deployment_modules[@]}" >&2

(IFS=,; printf '%s' "${deployment_modules[*]}")
