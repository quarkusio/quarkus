---
name: building-docs
description: >
  How to build, preview, and verify Quarkus documentation locally:
  root Maven build, docs rebuild, Jekyll preview via Podman/Docker.
---

# Building Documentation

## Supported Platforms

This workflow is supported on Linux, macOS, and Windows through WSL2.
Native Windows shells (PowerShell, CMD, Git Bash) are not supported.

## Quick Start

Run the full pipeline in one command:

```bash
just docs-preview
```

## Step 0 — Environment Setup

Source `detect-env.sh` to set `$CONTAINER_CMD`, `$VOL_FLAG`,
`$MVN_THREADS`, `$MAVEN_OPTS`, and `$BROWSER_CMD`:

```bash
. docs/detect-env.sh
```

The script computes everything automatically. For context, here is how
it adapts to different machines — no manual tuning is needed:

| Machine | Heap | Threads | Why |
|---------|------|---------|-----|
| 62 GB, 22 cores | 8 GB | `-T 0.8C` (17) | CPU is the limit |
| 16 GB, 8 cores | 4 GB | `-T 0.8C` (6) | CPU is the limit |
| 8 GB, 16 cores | 2 GB | `-T 5` | RAM caps it — 0.8C would be 12 |
| 4 GB, 4 cores | 2 GB | single-threaded | No room for parallel threads |

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
./mvnw clean install -DquicklyDocs -Dmaven.javadoc.skip=true
```

## Step 2 — Quick Docs Rebuild (from `docs/`)

```bash
cd docs
../mvnw package                # quick rebuild (~1 min)
../mvnw clean package          # if output looks stale
```

## Step 3 — Sync (from `docs/`)

First time: `./sync-web-site.sh`

Subsequent iterations — fast re-sync (<1 second). This duplicates the
rsync portion of `sync-web-site.sh` to skip its `rm -rf` + `git clone`
(~4 min). If `sync-web-site.sh` gains a `--no-clone` flag, prefer that.

```bash
rsync -r --delete \
    --exclude='**/*.html' --exclude='**/index.adoc' \
    --exclude='**/_attributes-local.adoc' --exclude='**/guides.md' \
    --exclude='**/_templates' \
    target/asciidoc/sources/ target/web-site/_versions/main/guides

[ -d target/quarkus-generated-doc/ ] && rsync -r --delete \
    --exclude='**/*.html' --exclude='**/index.adoc' \
    --exclude='**/_attributes.adoc' \
    target/quarkus-generated-doc/ target/web-site/_generated-doc/main

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

The `--config` flag chain loads `_only_latest_guides_config.yml` (excludes
old version directories) and `_config_dev.yml` (uses staging search
cluster). These files are created by `sync-web-site.sh` inside
`target/web-site/`.

```bash
cd target/web-site

$CONTAINER_CMD run -d --name quarkus-docs-preview \
  -p 127.0.0.1:4000:4000 -p 127.0.0.1:35729:35729 \
  -v "$(pwd):/site${VOL_FLAG}" \
  -v quarkus-jekyll-bundles:/usr/local/bundle \
  docker.io/bretfisher/jekyll-serve@sha256:db11b70736935b1a777b2ff2ae10f9ad191ee9fca6560eade1d5ad98b74e5f66 \
  bundle exec jekyll serve --host 0.0.0.0 \
  --livereload --incremental \
  --config _config.yml,_config_dev.yml,_only_latest_guides_config.yml
```

Stop: `$CONTAINER_CMD rm -f quarkus-docs-preview`

Fallback if `bretfisher/jekyll-serve` is unavailable:

```bash
$CONTAINER_CMD run -d --name quarkus-docs-preview \
  -p 127.0.0.1:4000:4000 -p 127.0.0.1:35729:35729 \
  --volume="$(pwd):/srv/jekyll${VOL_FLAG}" \
  -v quarkus-jekyll-bundles:/usr/local/bundle \
  docker.io/jekyll/jekyll@sha256:bb45414c3fefa80a75c5001f30baf1dff48ae31dc961b8b51003b93b60675334 \
  jekyll serve --host 0.0.0.0 \
  --livereload --incremental \
  --config _config.yml,_config_dev.yml,_only_latest_guides_config.yml
```

## Step 5 — Verify

The script auto-detects what you were working on and opens the right page:

| Content type | How detected | Preview URL |
|---|---|---|
| Guide | Recently modified `.adoc` in `docs/src/main/asciidoc/` | `/version/main/guides/` |
| Blog post | Recently modified `_posts/*.adoc`, tags do NOT contain `user-story` | `/blog/<slug>/` (deep-links to the post) |
| User story | Recently modified `_posts/*.adoc`, `tags:` contains `user-story` | `/blog/<slug>/` (deep-links to the story) |
| Can't determine | No recent `.adoc` changes found | `/` (homepage) |

## Iteration Loop

```
Edit .adoc → save → Step 2 (~1 min) → Step 3 re-sync (<1s) → browser auto-refreshes
```

Container stays running. Escalate to Step 1 when Step 2 fails or after
significant upstream changes.

## When to escalate to a root build

| Symptom | Action |
|---------|--------|
| Quick rebuild succeeds but content looks stale | Try `../mvnw clean package` in `docs/` |
| `clean package` still broken or fails | Root build (Step 1) |
| Pulled new upstream changes to `main` | Root build (Step 1) |
| Root build is roughly a week old | Root build (Step 1) |
| New config properties or extensions added | Root build (Step 1) |

## Troubleshooting

**`Failed to delete docs/.cache/formatter-maven-cache.properties`** —
Rootless Podman UID mapping. Fix: `podman unshare rm -rf docs/.cache/`.
With Docker: `rm -rf docs/.cache/`.

**Volume mount errors on macOS/Ubuntu** — SELinux `:z` flag applied on
a system without SELinux. Source `detect-env.sh` to set `$VOL_FLAG`
correctly.

**Container image pull fails** — Images are pinned by digest. To update:
`podman inspect docker.io/bretfisher/jekyll-serve:<new-tag> --format '{{.Digest}}'`
