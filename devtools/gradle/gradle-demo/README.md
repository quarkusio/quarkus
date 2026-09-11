# Quarkus Gradle demo

This is a small composite Gradle build for demonstrating and reviewing the new Quarkus Gradle application and extension plugins.
It is commit-stack material, not a Quarkus reactor module or a normal CI fixture.

The build uses only the new plugins.
It does not apply the legacy `io.quarkus` application plugin and does not demonstrate migration.

## What it contains

* `app`: a Quarkus REST application using `io.quarkus.application`.
* `api`: a plain Java API project.
* `dogs-service`: a plain Java project that implements the API.
* `demo-extension`: an included build with extension runtime and deployment projects.

The app declares independent `fast`, `native`, `container`, and `aot` builds.
The `aot` build has a package-backed JVM test suite for startup-archive training.

## Prerequisites

* Java 21.
* A HotSpot/OpenJDK 25 or newer runtime for AOT training.
* Gradle 9.6 or newer, provided by the sibling `devtools/gradle/gradlew` wrapper.
* Locally installed Quarkus 4 SNAPSHOT artifacts and Gradle plugins from this checkout.
* A supported native-image toolchain for native tasks.
* A working container-image environment for image tasks.
* `curl` and `ed` for the small recording helper scripts.

If the local Quarkus artifacts are not available, build and install the relevant Quarkus and Gradle-plugin modules first.
This demo resolves the local `999-SNAPSHOT` artifacts from `mavenLocal()`.

All examples below run from this directory.

```shell
cd devtools/gradle/gradle-demo
```

Use the sibling wrapper so the demo does not need to carry another wrapper distribution:

```shell
../gradlew -p . tasks
```

## Fast development demo

Start Gradle-owned dev mode:

```shell
../gradlew -p . :app:quarkusApplicationDev --continuous --no-configuration-cache
```

In a second screen region in the same terminal window, call the endpoint:

```shell
./scripts/curl-dogs.sh
```

Then perform one visible change at a time:

```shell
./scripts/demo-edit.sh app
./scripts/curl-dogs.sh

./scripts/demo-edit.sh build
./scripts/curl-dogs.sh
```

The first edit is the recommended recording path.
The build-script edit is an optional follow-up beat.
Reset the sources after a take:

```shell
./scripts/demo-edit.sh reset
```

`screen` works well for a one-window recording.
Start the dev command in one region, create a second region for the commands above, and keep the Gradle output visible.
The recording needs no browser because `curl` makes the changed response visible.

### Using the prepared screen layout

Start the layout from this directory:

```shell
./scripts/start-demo-screen.sh
```

It creates two vertical panes.
The right pane runs Gradle continuous dev mode and shows the exact Gradle command first.
The left pane is an interactive shell for the request and edit commands.

`screen` commands begin with `Ctrl-a`.
Press and release `Ctrl-a`, then press the second key.
For example, `Ctrl-a` followed by `Tab` moves the keyboard focus to the other pane.

| What to do | Key or command |
| --- | --- |
| Move between the Gradle and command panes | `Ctrl-a`, then `Tab` |
| Read old Gradle output | `Ctrl-a`, then `[`; use arrow keys or Page Up/Down; press `Esc` to return |
| See screen's built-in key help | `Ctrl-a`, then `?` |
| Detach and leave dev mode running | `Ctrl-a`, then `d` |
| Reattach later | `screen -r quarkus-gradle-demo` |
| List running screen sessions | `screen -ls` |

The current pane is where typed commands go.
Do not type `curl` or edit commands into the Gradle pane.
If that happens, use `Ctrl-a`, then `Tab` to return to the command pane.
Avoid `Ctrl-a`, then `0` or `1` during the demo: those replace the window shown in the focused pane instead of merely moving focus.

In the command pane, use this sequence:

```shell
./scripts/curl-dogs.sh
./scripts/demo-edit.sh app
./scripts/curl-dogs.sh

./scripts/demo-edit.sh build
./scripts/curl-dogs.sh
./scripts/demo-edit.sh reset
```

To stop the demo, focus the Gradle pane and press `Ctrl-c`.
Then type `exit` in any remaining shell, or use `Ctrl-a`, then `\` and confirm that `screen` should terminate.

To produce a short MP4 automatically, use the included `screen` layout and recording script:

```shell
./scripts/record-demo.sh
```

It opens a full-screen terminal, records the dev boot and both edits, then writes a Chrome-playable H.264 MP4 under `recording/`.
It requires `gnome-terminal`, GNU `screen`, `ffmpeg`, and a real X11 session.
In a Wayland session, use a native Wayland recorder with the same two-pane `screen` layout instead:

```shell
./scripts/start-demo-screen.sh
```

The right pane is ready for the `curl-dogs.sh` and `demo-edit.sh` commands above.
The video is deliberately ignored by Git.

## Named build commands

```shell
# Fast JAR
../gradlew -p . :app:quarkusFastBuild

# Native executable and its generated native-test suite
../gradlew -p . :app:quarkusNativeBuild :app:quarkusNativeNativeTest

# Jib container image
../gradlew -p . :app:quarkusContainerImageBuild

# AOT-JAR, package-backed training, and its optimized image
../gradlew -p . :app:aotTraining :app:quarkusAotStartupOptimizedImageBuild
```

The native, image, and AOT commands deliberately remain outside the fast recording path.
They can require a native-image toolchain, container runtime or registry access, JDK 25 for AOT training, and extra time.

The included extension is intentionally part of the composite build, but its runtime artifact is not live reloadable.
Use the application-source and build-script edits for the recording.

See [PLAN.md](PLAN.md) for the intended demo and recording sequence.
