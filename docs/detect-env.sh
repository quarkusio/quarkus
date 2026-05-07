#!/bin/bash
# Detects the machine environment and exports variables for the docs
# build pipeline. Source this script: . docs/detect-env.sh

if command -v podman &>/dev/null; then CONTAINER_CMD="podman"
elif command -v docker &>/dev/null; then CONTAINER_CMD="docker"
else echo "ERROR: neither podman nor docker found" && return 1 2>/dev/null || exit 1; fi

if command -v getenforce &>/dev/null && [ "$(getenforce 2>/dev/null)" != "Disabled" ]; then
  VOL_FLAG=":z"
else
  VOL_FLAG=""
fi

if command -v nproc &>/dev/null; then
  CORES=$(nproc)
elif command -v sysctl &>/dev/null; then
  CORES=$(sysctl -n hw.ncpu)
else
  CORES=4
fi
case "$CORES" in ''|*[!0-9]*) CORES=4 ;; esac

if command -v free &>/dev/null; then
  TOTAL_MEM_MB=$(free -m | awk '/^Mem:/{print $2}')
elif command -v sysctl &>/dev/null; then
  TOTAL_MEM_MB=$(sysctl -n hw.memsize | awk '{print int($1/1048576)}')
else
  TOTAL_MEM_MB=8192
fi
case "$TOTAL_MEM_MB" in ''|*[!0-9]*) TOTAL_MEM_MB=8192 ;; esac

HEAP_MB=$((TOTAL_MEM_MB / 4))
[ "$HEAP_MB" -lt 2048 ] && HEAP_MB=2048
[ "$HEAP_MB" -gt 8192 ] && HEAP_MB=8192
case "$MAVEN_OPTS" in
  *-Xmx*) ;;
  "") export MAVEN_OPTS="-Xmx${HEAP_MB}m" ;;
  *)  export MAVEN_OPTS="$MAVEN_OPTS -Xmx${HEAP_MB}m" ;;
esac

AVAIL_FOR_THREADS=$((TOTAL_MEM_MB - HEAP_MB - 2048))
[ "$AVAIL_FOR_THREADS" -lt 0 ] && AVAIL_FOR_THREADS=0
MAX_THREADS_BY_RAM=$((AVAIL_FOR_THREADS / 750))
MAX_THREADS_BY_CPU=$(awk -v cores="$CORES" 'BEGIN {printf "%d", cores * 0.8}')
if [ "$MAX_THREADS_BY_RAM" -lt "$MAX_THREADS_BY_CPU" ] && [ "$MAX_THREADS_BY_RAM" -ge 1 ]; then
  MVN_THREADS="-T $MAX_THREADS_BY_RAM"
elif [ "$MAX_THREADS_BY_RAM" -lt 1 ]; then
  MVN_THREADS=""
else
  MVN_THREADS="-T 0.8C"
fi

if command -v wslview &>/dev/null; then BROWSER_CMD="wslview"
elif command -v xdg-open &>/dev/null; then BROWSER_CMD="xdg-open"
elif command -v open &>/dev/null; then BROWSER_CMD="open"
else BROWSER_CMD=""; fi

echo "Container:  $CONTAINER_CMD"
echo "SELinux:    ${VOL_FLAG:-none}"
echo "Cores:      $CORES"
echo "RAM:        $((TOTAL_MEM_MB / 1024))GB total → heap ${HEAP_MB}MB, threads: ${MVN_THREADS:-single-threaded}"
echo "MAVEN_OPTS: $MAVEN_OPTS"
echo "Browser:    ${BROWSER_CMD:-manual}"
