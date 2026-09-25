#!/usr/bin/env bash

set -euo pipefail

demo_dir=$(cd "$(dirname "$0")/.." && pwd)
cd "$demo_dir"

replace() {
    local file=$1
    local old=$2
    local new=$3

    ed -s "$file" <<EOF
g/$old/s//$new/
wq
EOF
}

explain() {
    printf '\033[2m%s\033[0m\n' "$1"
}

case "${1:-}" in
    app)
	printf '\n%s\n\n' '== Demo action: change application source =='
	explain 'Editing: app/src/main/java/org/acme/gradledemo/DemoMessage.java'
	explain 'Change:  "Hello from the application" -> "Hello after the application edit"'
	printf '\n'
        replace app/src/main/java/org/acme/gradledemo/DemoMessage.java \
            "Hello from the application" "Hello after the application edit"
	printf '%s\n' 'Application source updated.'
	explain 'Next: Gradle recompiles the application and Quarkus restarts dev mode.'
	printf '\n'
        ;;
    build)
	printf '\n%s\n\n' '== Demo action: change a declared Gradle build input =='
	explain 'Editing: app/build.gradle.kts'
	explain 'Change:  "Build script says hello" -> "Build script says hello again"'
	printf '\n'
        replace app/build.gradle.kts "Build script says hello" "Build script says hello again"
	printf '%s\n' 'Build script updated.'
	explain 'Next: Gradle regenerates one source file and Quarkus restarts dev mode.'
	printf '\n'
        ;;
    reset)
	printf '\n%s\n\n' '== Reset demo edits =='
	explain 'Restoring the original application source and Gradle build-script values.'
	printf '\n'
        replace app/src/main/java/org/acme/gradledemo/DemoMessage.java \
            "Hello after the application edit" "Hello from the application"
        replace app/build.gradle.kts "Build script says hello again" "Build script says hello"
	printf '%s\n\n' 'Original values restored.'
        ;;
    *)
        echo "Usage: $0 {app|build|reset}" >&2
        exit 2
        ;;
esac
