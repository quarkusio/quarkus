---
name: building-docs
description: >
  How to build, preview, and verify Quarkus documentation locally:
  root Maven build, docs rebuild, Roq (Quarkus dev mode) preview.
---

# Building Documentation

## Supported Platforms

This workflow is supported on Linux, macOS, and Windows through WSL2.
Native Windows shells (PowerShell, CMD, Git Bash) are not supported.

No container runtime is required — the website is a Quarkus application
(built with [Roq](https://docs.quarkiverse.io/quarkus-roq/dev/index.html))
served directly via `mvnw quarkus:dev`. Java 21+ is required for that step.

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
* `docs/.docs-preview-server.pid` — PID of the backgrounded `quarkus:dev`
  process. Used to detect and reuse an already-running preview server.
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

The script computes Maven heap, thread count, and browser command
automatically based on your machine.

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

This clones `quarkusio.github.io` into `target/web-site/` and copies the
built guides into its Roq content tree (`target/web-site/content/versions/main/guides`),
plus generated docs and index data, then runs Roq-specific post-processing
(moving static assets under `assets/`, rewriting asset links, adding
placeholder `index.html` files, and escaping Qute syntax in
`*qute-reference.adoc`).

Subsequent iterations — fast re-sync (<1 second). This duplicates the
rsync and post-processing portions of `sync-web-site.sh` to skip its
`rm -rf` + `git clone` (~4 min). If `sync-web-site.sh` gains a
`--no-clone` flag, prefer that.

```bash
TARGET_GUIDES=target/web-site/content/versions/main/guides

rsync -rt --delete \
    --exclude='**/*.html' --exclude='**/index.adoc' \
    --exclude='**/_attributes-local.adoc' --exclude='**/guides.md' \
    --exclude='**/_templates' \
    target/asciidoc/sources/ "$TARGET_GUIDES"

# Roq post-processing (see sync-web-site.sh for the full, current version)
for ext in py sh zip jar tar.gz; do
    find "$TARGET_GUIDES" -maxdepth 1 -name "*.$ext" -exec bash -c \
        'mkdir -p "$(dirname "$1")/assets"; mv "$1" "$(dirname "$1")/assets/"' _ {} \;
    find "$TARGET_GUIDES" -maxdepth 1 -name "*.adoc" \
        -exec sed -i "s|link:\([a-zA-Z0-9_-]*\.$ext\)|link:../assets/\1|g" {} \;
done
find "$TARGET_GUIDES" -name "*qute-reference.adoc" -exec sed -i 's/|}/|\\}/g; s/{|/\\{|/g' {} \;

[ -d target/quarkus-generated-doc/ ] && rsync -rt --delete \
    --exclude='**/*.html' --exclude='**/index.adoc' \
    --exclude='**/_attributes.adoc' \
    target/quarkus-generated-doc/ target/web-site/content/_generated-doc/main

if [ -f target/indexByType.yaml ]; then
    mkdir -p target/web-site/_data/versioned/main/index
    { echo "# Generated file. Do not edit"; cat target/indexByType.yaml
    } > target/web-site/_data/versioned/main/index/quarkus.yaml
fi

if [ -f target/relations.yaml ]; then
    mkdir -p target/web-site/_data/versioned/main/index
    { echo "# Generated file. Do not edit"; cat target/relations.yaml
    } > target/web-site/_data/versioned/main/index/relations.yaml
fi
```

## Step 4 — Serve (from `docs/target/web-site`)

The website is a Quarkus Roq site. It ships its own `serve-only-latest-guides.sh`,
which runs `mvnw quarkus:dev` with the `only-latest-guides` profile
(`config/application-only-latest-guides.properties`, which excludes old
version directories so dev-mode startup stays fast). This comes from the
`quarkusio.github.io` repository, cloned into `target/web-site/` by
`sync-web-site.sh`.

```bash
cd target/web-site
QUARKUS_HTTP_PORT=8042 ./serve-only-latest-guides.sh
```

Quarkus dev mode watches the content tree and live-reloads the browser on
change — there is no separate build-then-restart step and no livereload
port to manage. `QUARKUS_HTTP_PORT` (or any `quarkus.*` property, via its
env-var form) can be set to change the port; it defaults to `8042`
(`config/application.properties`).

To background it and reuse it across `docs-preview.sh` runs:

```bash
nohup ./serve-only-latest-guides.sh > server.log 2>&1 &
echo $! > .server.pid
```

Stop: `kill $(cat .server.pid)`

Other profiles available in that repo (see `.agents/skills/building-blog/SKILL.md`
there for the blog-focused equivalent): `./serve.sh` (full site, all guide
versions) and `./serve-noguides.sh` (fastest, no guides at all).

## Step 5 — Verify

The script auto-detects what you were working on and opens the right page:

| Content type | How detected | Preview URL |
|---|---|---|
| 1 guide | Recently modified `.adoc` in `docs/src/main/asciidoc/` | `/version/main/guides/<name>/` (direct) |
| 2-4 guides | Multiple `.adoc` files modified | Opens a tab for each guide |
| 5+ guides | Many files modified | `/version/main/guides/` (listing) |
| Blog post | Recently modified `content/posts/*.adoc`, no `user-story` tag | `/blog/<slug>/` (deep-link) |
| User story | Recently modified `content/posts/*.adoc`, has `user-story` tag | `/blog/<slug>/` (deep-link) |
| No changes | No recent `.adoc` changes found | `/` (homepage) |

## Iteration Loop

```
Edit .adoc → save → Step 2 (~1 min) → Step 3 re-sync (<1s) → browser auto-refreshes
```

The dev server stays running (backgrounded, tracked via
`docs/.docs-preview-server.pid`). Re-running `just docs-preview` reuses it
if it's still up and just re-syncs + re-opens the browser. Escalate to
Step 1 when Step 2 fails or after significant upstream changes.

## When to escalate to a root build

| Symptom | Action |
|---------|--------|
| Quick rebuild succeeds but content looks stale | Try `../mvnw -ntp clean package -Dasciidoctor.fail-if=ERROR` in `docs/` |
| `clean package` still broken or fails | Root build (Step 1) |
| Pulled new upstream changes to `main` | Root build (Step 1) |
| Root build is roughly a week old | Root build (Step 1) |
| New config properties or extensions added | Root build (Step 1) |

## Troubleshooting

**Port 8042 already in use** — Another process (possibly a stale dev server
from a previous run) is occupying the port. `docs-preview.sh` detects and
stops a stale server tracked via `docs/.docs-preview-server.pid`
automatically; for anything else, free the port or set `QUARKUS_HTTP_PORT`
to a different value.

**Preview shows partial or stale content** — Stop the dev server
(`kill $(cat docs/.docs-preview-server.pid)`) and run `just docs-preview`
again. If that doesn't help, delete `docs/target/web-site` to force a full
re-clone and re-sync.

**Changes not appearing** — Quarkus dev mode watches the content tree
under `target/web-site/`. If a change is not picked up, press `s` in the
terminal running the dev server to force a restart (or kill and restart it
via `docs-preview.sh`).

**Force a full root build** — Set `QUARKUS_DOCS_PREVIEW_FULL=1` before
running: `QUARKUS_DOCS_PREVIEW_FULL=1 just docs-preview`
