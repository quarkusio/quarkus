#!/usr/bin/env bash
set -euo pipefail

demo_dir=$(cd "$(dirname "$0")/.." && pwd)
session_name="quarkus-gradle-demo"

screen -S "$session_name" -X quit >/dev/null 2>&1 || true
cd "$demo_dir"
printf '%s\n' '== Starting the Quarkus Gradle demo in screen =='
printf '%s\n' 'The right pane runs Gradle continuous dev mode.'
printf '%s\n' 'The left pane is for ./scripts/curl-dogs.sh and ./scripts/demo-edit.sh {app|build|reset}.'
exec env QUARKUS_GRADLE_DEMO_DIR="$demo_dir" screen \
    -S "$session_name" \
    -c "$demo_dir/scripts/demo.screenrc"
