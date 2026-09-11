#!/usr/bin/env bash
set -euo pipefail

demo_dir=$(cd "$(dirname "$0")/.." && pwd)
session_name="quarkus-gradle-demo-recording"
display=${DISPLAY:?An X11 DISPLAY is required for recording.}
recording_dir="$demo_dir/recording"
timestamp=$(date +%Y%m%d-%H%M%S)
output=${1:-"$recording_dir/quarkus-gradle-demo-$timestamp.mp4"}

if [[ "${XDG_SESSION_TYPE:-}" == "wayland" ]]; then
    echo "This recorder needs a real X11 session; XWayland capture produces a black video." >&2
    echo "Use a native Wayland recorder, or log in to an X11 session before running it." >&2
    exit 1
fi

screen_size=$(xrandr --current | sed -n 's/.*current \([0-9][0-9]*\) x \([0-9][0-9]*\).*/\1x\2/p' | head -n 1)
if [[ -z "$screen_size" ]]; then
    echo "Could not determine the X11 screen size." >&2
    exit 1
fi

mkdir -p "$recording_dir"
"$demo_dir/scripts/demo-edit.sh" reset
printf '%s\n' '== Recording the Quarkus Gradle demo =='
printf '%s\n' 'The recording shows dev boot, a source edit, a build-script input edit, and curl responses.'

recorder_pid=
cleanup() {
    "$demo_dir/scripts/demo-edit.sh" reset || true
    screen -S "$session_name" -X quit >/dev/null 2>&1 || true
    if [[ -n "${recorder_pid:-}" ]]; then
        kill -INT "$recorder_pid" >/dev/null 2>&1 || true
        wait "$recorder_pid" >/dev/null 2>&1 || true
    fi
}
trap cleanup EXIT INT TERM

gnome-terminal \
    --display "$display" \
    --full-screen \
    --hide-menubar \
    --zoom=1.35 \
    --title "Quarkus Gradle demo" \
    --working-directory "$demo_dir" \
    -- bash -lc "exec env QUARKUS_GRADLE_DEMO_DIR='$demo_dir' screen -S '$session_name' -c '$demo_dir/scripts/demo.screenrc'"

for _ in {1..100}; do
    if screen -S "$session_name" -Q select . >/dev/null 2>&1; then
        break
    fi
    sleep 0.1
done
screen -S "$session_name" -Q select . >/dev/null

ffmpeg -y \
    -f x11grab \
    -framerate 30 \
    -video_size "$screen_size" \
    -i "$display+0,0" \
    -vf "scale=2560:-2" \
    -an \
    -c:v libx264 \
    -crf 20 \
    -preset veryfast \
    -pix_fmt yuv420p \
    -movflags +faststart \
    "$output" >/dev/null 2>&1 &
recorder_pid=$!

send_command() {
    screen -S "$session_name" -p commands -X stuff "$1"
    screen -S "$session_name" -p commands -X stuff $'\n'
}

# Let the initial dev-mode boot remain visible in the left pane.
sleep 12
send_command "printf '\\n== baseline request ==\\n'"
send_command "./scripts/curl-dogs.sh"
sleep 3

send_command "printf '\\n== application source edit ==\\n'"
send_command "./scripts/demo-edit.sh app"
sleep 6
send_command "./scripts/curl-dogs.sh"
sleep 3

send_command "printf '\\n== build-script edit ==\\n'"
send_command "./scripts/demo-edit.sh build"
sleep 6
send_command "./scripts/curl-dogs.sh"
sleep 3

send_command "./scripts/demo-edit.sh reset"
sleep 2

echo "Recording written to $output"
