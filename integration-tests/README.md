# Quarkus integration tests

These modules exercise Quarkus extensions end to end. By default they are built against the
Quarkus artifacts produced by the surrounding reactor, so running one normally means building
Quarkus locally first.

They can also be built against an **already released Quarkus**, without rebuilding Quarkus at
all. That is useful to:

- validate a branch's integration tests against an assembled Quarkus Platform BOM, and
- isolate a regression by running the same test against several released versions of a stream.

## Running against a released Quarkus

Set `quarkus.version` to the release you want to test against:

```shell
./mvnw verify -f integration-tests/rest-client-reactive -Dquarkus.version=4.0.0.Beta1
```

Every `io.quarkus` artifact is then resolved from the repository at that version, and nothing
needs to have been built locally.

To build against an assembled **platform** BOM rather than the Quarkus core BOM, also set the
platform coordinates:

```shell
./mvnw verify -f integration-tests/rest-client-reactive \
  -Dquarkus.version=4.0.0.Beta1 \
  -Dquarkus.platform.group-id=io.quarkus.platform \
  -Dquarkus.platform.version=4.0.0.Beta1
```

### Properties

| Property | Default | Meaning |
|---|---|---|
| `quarkus.version` | `${project.version}` | Version of the `io.quarkus:*` artifacts to build against |
| `quarkus.platform.group-id` | `io.quarkus` | Group id of the BOM to import |
| `quarkus.platform.artifact-id` | `quarkus-bom` | Artifact id of the BOM to import |
| `quarkus.platform.version` | `${quarkus.version}` | Version of the BOM to import |

`quarkus.platform.version` is kept separate from `quarkus.version` because downstream
distributions can ship a platform whose version differs from the Quarkus core version it
aligns `io.quarkus:*` to. Keep `quarkus.version` equal to that core version, otherwise the
`requiresMinimalDeploymentDependency` enforcer rule will report mismatches; `-Denforcer.skip`
is the escape hatch if you need one.

> **Do not pass `quarkus.version` to a full repository build.** It is meant for building the
> integration tests on their own. At the repository root it would also redirect the BOM import
> of every extension, which is not what you want.

### Modules that depend on integration test helpers

Some integration tests depend on helper artifacts that live under `integration-tests/` itself,
such as `quarkus-integration-test-shared-library` or
`quarkus-integration-test-class-transformer`. Those are never published, so they always have to
be built from source. Select the module from the integration test root with `-am` instead of
pointing `-f` straight at it:

```shell
./mvnw verify -f integration-tests -pl packaging -am -Dquarkus.version=4.0.0.Beta1
```

The helpers are then built from the reactor at `999-SNAPSHOT` while everything else resolves at
the released version. This is deliberate: those artifacts belong to the integration tests, not
to the Quarkus release being tested.

### Verifying that nothing resolved locally

If you have ever run `./mvnw -Dquickly`, your local repository already contains
`io.quarkus:*:999-SNAPSHOT`, which can silently satisfy a dependency that should have come from
the release. Use a scratch repository to prove otherwise:

```shell
./mvnw verify -f integration-tests/rest-client-reactive \
  -Dquarkus.version=4.0.0.Beta1 \
  -Dmaven.repo.local=/tmp/m2-scratch \
  -Dno-build-cache
```

`-Dno-build-cache` disables the Develocity build cache, so a cache hit cannot mask a resolution
problem.

## Limitations

**Use a version from the same release stream.** The integration test sources track `main`, so
they can use APIs that an older release does not have. Cross-stream runs additionally break on
artifacts that have been renamed — on `main`, for instance, `quarkus-junit5` is only a
relocation to `quarkus-junit`.

**Artifacts newer than the release are not available.** When `main` adds a new artifact, the
integration tests that use it cannot be built against a release that predates it.

**Some modules cannot work this way at all**, because they test the build tooling itself rather
than an extension, and are therefore inseparable from the surrounding repository:

`maven`, `gradle`, `devtools`, `devtools-registry-client`, `kotlin-maven-invoker`, `packaging`,
`devmode`, `ide-launcher`, `istio`.

## Keeping this working

Dependencies on Quarkus artifacts that are *not* built under `integration-tests/` must use
`${quarkus.version}`; artifacts that *are* built here keep `${project.version}`. Both expand to
the same value in a normal build, so a mistake is invisible locally — it only shows up when the
integration tests are built against a release. `.github/check-integration-test-versions.py`
enforces the distinction in CI, and `--fix` applies it:

```shell
./.github/check-integration-test-versions.py --fix
```
