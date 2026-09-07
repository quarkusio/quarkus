---
name: building-docs
description: >
  How to build, preview, and verify Quarkus documentation locally:
  root Maven build, docs rebuild, Roq dev server preview.
---

# Building Documentation

## Supported Platforms

This workflow is supported on Linux, macOS, and Windows through WSL2.
Native Windows shells (PowerShell, CMD, Git Bash) are not supported.
Java 21+ is required. The repository includes a `./mvnw` wrapper — no
separate Maven install is needed.

## Quick Start

Run the full pipeline in one command:

```bash
just docs-preview
```

## How It Works

The script uses local marker files to keep the preview workflow fast:

* `docs/.docs-preview-root-build-last-run` — last successful root build.
  Used to decide whether Step 1 can be skipped.
* `docs/.docs-preview-root-build-head` — Git HEAD at last root build.
  Detects branch switches, checkouts, and pulls.
* `docs/.docs-preview-docs-build-last-run` — last successful docs rebuild.
  Used to decide whether Step 2 can be skipped.
* `docs/.docs-preview-last-run` — last preview run.
  Used to detect recently changed files and open the right preview URL.
* `docs/.docs-preview-times` — cached execution times for progress estimates.

All marker files are gitignored. Delete them to force a full rebuild.

**Caveat:** The script detects changes to docs sources and `pom.xml` files,
but does not detect uncommitted changes outside `docs/` (e.g., new config
properties, extension metadata, or generated-doc producers). If you modify
code that affects generated documentation, force a root build manually:
`QUARKUS_DOCS_PREVIEW_FULL=1 just docs-preview`

## Step 0 — Environment Setup

Source `detect-env.sh` to set `$MVN_THREADS`, `$MAVEN_OPTS`, and
`$BROWSER_CMD`:

```bash
. docs/detect-env.sh
```

The script computes Maven heap, thread count, and browser automatically
based on your machine.

## Step 1 — Root Build (from repo root)

Run **once**, then re-run roughly once a week, after pulling significant
upstream changes, or when Step 2 fails.

Use **`-DquicklyDocs`**, not **`-Dquickly`** (which sets `skipDocs=true`).
The extra flags are not covered by the profile and are needed to skip
integration-test modules, Gradle plugin build, and javadoc generation.

```bash
./mvnw $MVN_THREADS clean install -DquicklyDocs \
  -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true
```

Fallback (single-threaded) if the parallel build fails:

```bash
./mvnw clean install -DquicklyDocs \
  -Dno-test-modules -Dskip.gradle.build=true -Dmaven.javadoc.skip=true
```

## Step 2 — Quick Docs Rebuild (from `docs/`)

```bash
cd docs
../mvnw -ntp package -Dasciidoctor.fail-if=ERROR    # quick rebuild (~1 min)
../mvnw -ntp clean package -Dasciidoctor.fail-if=ERROR  # if output looks stale
```

The `-Dasciidoctor.fail-if=ERROR` override lets the build succeed despite
AsciiDoctor warnings (the default `WARN` level fails on cross-reference
or attribute warnings that are harmless for local preview).

## Step 3 — Sync (from `docs/`)

First time: `./sync-web-site.sh`

Subsequent iterations — fast re-sync (<1 second). The script invokes
`sync-web-site.sh` with the existing website checkout as the target
directory, skipping the `rm -rf` + `git clone` (~4 min) but running
all post-processing (asset moves, link rewrites, `index.html` creation,
and Qute escaping) through the same code path as a full sync.

To re-sync manually while the Roq dev server is still running (e.g.
after `just docs-preview` is done and you are iterating), run from
the `docs/` directory:

```bash
cd docs
./sync-web-site.sh main "$(pwd)/target/web-site"
```

Do not re-run `docs-preview.sh` while the server is still up — the
port check will reject it. Use the command above instead.

## Step 4 — Serve (from `docs/target/web-site`)

The site is built with [Quarkus Roq](https://docs.quarkiverse.io/quarkus-roq/dev/index.html),
a Quarkus-based static site generator. `./mvnw quarkus:dev` starts a
live-reload dev server — edits to content files are picked up
automatically without a server restart.

```bash
cd target/web-site
./mvnw quarkus:dev -DskipTests
```

The server starts on port 8042 by default (set in `config/application.properties`). To use a different port:

```bash
QUARKUS_HTTP_PORT=8081 ./mvnw quarkus:dev -DskipTests
```

Stop: `Ctrl-C` in the terminal running the dev server, or kill the background
process printed at the end of `just docs-preview`.

## Step 5 — Verify

The script auto-detects what you were working on and opens the right page:

| Content type | How detected | Preview URL |
|---|---|---|
| 1 guide | Recently modified `.adoc` in `docs/src/main/asciidoc/` | `/version/main/guides/<name>.html` (direct) |
| 2-4 guides | Multiple `.adoc` files modified | Opens a tab for each guide |
| 5+ guides | Many files modified | `/version/main/guides/` (listing) |
| Blog post | Recently modified `.adoc` in `content/posts/` | `/blog/<slug>/` (deep-link) |
| No changes | No recent `.adoc` changes found | `/` (homepage) |

## Iteration Loop

```
Edit .adoc → save → Step 2 (~1 min) → Step 3 re-sync (<1s) → browser auto-refreshes
```

Roq dev mode watches for file changes and triggers an incremental rebuild
automatically. Escalate to Step 1 when Step 2 fails or after significant
upstream changes.

## When to escalate to a root build

| Symptom | Action |
|---------|--------|
| Quick rebuild succeeds but content looks stale | Try `../mvnw -ntp clean package -Dasciidoctor.fail-if=ERROR` in `docs/` |
| `clean package` still broken or fails | Root build (Step 1) |
| Pulled new upstream changes to `main` | Root build (Step 1) |
| Root build is roughly a week old | Root build (Step 1) |
| New config properties or extensions added | Root build (Step 1) |

## Troubleshooting

**Port 8042 already in use** — Another process is using port 8042.
First, check whether it is a leftover preview from a previous run
(e.g. a `java` process launched by `mvnw`). If so, kill it and retry.
**Always tell the user before using a different port** — do not
silently switch ports without informing them. If the conflict cannot
be resolved, ask the user which port to use, then set the environment
variable:

```bash
QUARKUS_HTTP_PORT=8081 bash docs/docs-preview.sh
```

**Server process exited unexpectedly** — Maven started but then died.
Scroll up in the terminal to find the build error, or check the log
printed at the end of the run. Common causes: corrupted local Maven
repository, a missing or broken dependency, or a compile error.

**Changes not appearing** — Roq dev mode watches source files. If a
change is not picked up, stop and restart the dev server.

**Preview shows stale content after full sync** — Delete
`docs/target/web-site` and re-run `just docs-preview` to force a fresh
clone and sync.

**`./mvnw` not executable** — The Maven wrapper exists in the website
checkout but lacks the execute bit:

```bash
chmod +x docs/target/web-site/mvnw
```

Do not install Maven system-wide as a workaround. The repository-provided
wrapper pins the correct Maven version.

**`./mvnw` missing** — The website checkout inside `docs/target/web-site`
is incomplete or corrupted. Delete it and re-run to force a fresh clone:

```bash
rm -rf docs/target/web-site
just docs-preview
```

**Force a full root build** — Set `QUARKUS_DOCS_PREVIEW_FULL=1` before
running: `QUARKUS_DOCS_PREVIEW_FULL=1 just docs-preview`

