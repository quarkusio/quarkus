---
name: creating-extensions
description: >
  How to create a new Quarkus extension: full module layout, package naming,
  artifact naming, dependency rules, and Dev UI setup.
---

# Creating Extensions

## Extension Module Layout

Every extension lives under `extensions/<name>/` with this structure:

```
extensions/<name>/
  runtime/          # Runtime classes, recorders, beans (quarkus-<name>)
  runtime-api/      # (optional) Public APIs for loose coupling (quarkus-<name>-api)
  deployment/       # @BuildStep processors (quarkus-<name>-deployment)
  deployment-spi/   # (optional) Public build items for cross-extension use (quarkus-<name>-deployment-spi)
  runtime-dev/      # (optional) Dev UI/Shell components, dev mode only (quarkus-<name>-dev)
  codegen/          # (optional) Code generation utilities (quarkus-<name>-codegen)
  cli/              # (optional) CLI commands (quarkus-<name>-cli)
  codestart/        # (optional) Codestart templates (quarkus-<name>-codestart)
```

Only `runtime` and `deployment` are mandatory.

## Dependency Rules

- **Deployment depends on runtime**, never the reverse
- External consumers use `runtime-api` or `deployment-spi` for loose coupling
- External consumers use `runtime` or `deployment` for tight coupling
- Both `runtime/` and `deployment/` POMs must include
  `quarkus-extension-processor` as an annotation processor path

## Package Naming

| Module | Package |
|--------|---------|
| Runtime | `io.quarkus.<extension-name>` |
| Runtime internals | `io.quarkus.<extension-name>.runtime.internal` |
| Runtime API | `io.quarkus.<extension-name>.api` |
| Deployment | `io.quarkus.<extension-name>.deployment` |
| Deployment SPI | `io.quarkus.<extension-name>.deployment.spi` |
| Runtime Dev | `io.quarkus.<extension-name>.dev` |

Hyphenated names become underscored: `quarkus-foo-bar` -> `io.quarkus.foo_bar`

## Artifact Naming

- Runtime: `quarkus-<name>`
- Deployment: `quarkus-<name>-deployment`
- See `adr/0009-extension-structure.adoc` for the full naming table

## Dev UI

- Provide `CardPageBuildItem` and `JsonRPCProvidersBuildItem` in deployment
  processors guarded by `@BuildSteps(onlyIf = IsLocalDevelopment.class)`
- Dev-only classes go in `runtime-dev/` modules
- See the `classloading-and-runtime-dev` skill for runtime-dev wiring details

## After Creating an Extension

- Run `./update-extension-dependencies.sh` after adding/removing/renaming —
  this ensures modules like `devtools/bom-descriptor-json` build in the
  correct order relative to the new extension
- Always `install` (not just `compile`) so dependent modules can find it
- If you change a runtime module, rebuild its deployment module too
- Run `mvn -Dowasp-check` in the extension directory to check for known
  security vulnerabilities in dependencies

## External Maven Repositories

Quarkus uses `--ignore-transitive-repositories` to ensure all dependencies
come from Maven Central. If your extension needs an external repository:

1. Discuss with the Quarkus Core team first — external repos are avoided
2. Add the repository to the specific extension module's `pom.xml`, not the
   root `pom.xml`
3. Declare Maven Central **first** in the `<repositories>` element
4. Add a `.mvn/rrf/groupId-<REPOSITORY-ID>.txt` file listing authorized
   `groupId`s (one per line)
5. Verify with: `./mvnw -Dquickly -Dmaven.repo.local=/tmp/temp-repo`

## Independent Projects

Some sub-projects in `independent-projects/` have their own build lifecycle:
- `arc/` — The CDI implementation
- `resteasy-reactive/` — RESTEasy Reactive
- `qute/` — The templating engine
- `tools/` — Shared tooling utilities

These can be built standalone but are also included in the main Quarkus build.
