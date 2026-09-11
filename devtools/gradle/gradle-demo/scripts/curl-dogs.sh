#!/usr/bin/env bash

set -euo pipefail

demo_dir=$(cd "$(dirname "$0")/.." && pwd)
cd "$demo_dir"

printf '\n%s\n\n' '== GET /dogs =='
curl --fail --silent --show-error http://localhost:8081/dogs
printf '\n\n'
