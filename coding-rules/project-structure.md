# Project Structure

Quarkus is a large multi-module Maven project. Understanding the layout is essential.

## Top-Level Directories

- `core/` - Core framework: builder, deployment, runtime, launcher
- `extensions/` - 200+ extensions (HTTP, data, messaging, security, cloud, etc.)
- `devtools/` - Maven/Gradle plugins, CLI
- `bom/` - Bill of Materials (dependency management)
- `build-parent/` - Shared build config, dependency versions, profiles
- `independent-projects/` - Standalone sub-projects: ArC (CDI), Qute (templating), RESTEasy Reactive, bootstrap, tools
- `integration-tests/` - Integration tests for extensions and features
- `test-framework/` - JUnit 5 test utilities (`@QuarkusTest`, etc.)
- `docs/` - Documentation (Asciidoc sources in `docs/src/main/asciidoc/`)
- `adr/` - Architecture Decision Records

## Extension Module Layout

Every extension follows this structure (see also `adr/0009-extension-structure.adoc`):

```
extensions/<name>/
  runtime/          # Runtime classes, recorders, beans (artifactId: quarkus-<name>)
  runtime-api/      # (optional) Public APIs for loose coupling (artifactId: quarkus-<name>-api)
  deployment/       # @BuildStep processors (artifactId: quarkus-<name>-deployment)
  deployment-spi/   # (optional) Public build items for cross-extension use (artifactId: quarkus-<name>-deployment-spi)
  runtime-dev/      # (optional) Dev UI/Shell components, dev mode only (artifactId: quarkus-<name>-dev)
  codegen/          # (optional) Code generation utilities (artifactId: quarkus-<name>-codegen)
  cli/              # (optional) CLI commands (artifactId: quarkus-<name>-cli)
  codestart/        # (optional) Codestart templates (artifactId: quarkus-<name>-codestart)
```

Only `runtime` and `deployment` are mandatory; the rest are added based on the
extension's needs.

## Key Dependency Rules

- **Deployment depends on runtime**, never the reverse.
- External consumers should use `runtime-api` or `deployment-spi` for loose coupling.
- External consumers should use `runtime` or `deployment` for tight coupling (requiring
  the extension to be present).
- Both the `runtime/` and `deployment/` POMs must include `quarkus-extension-processor`
  as an annotation processor path.

## Package Naming

- Runtime: `io.quarkus.<extension-name>` (or `io.quarkus.<extension-name>.runtime.internal` for internals)
- Runtime API: `io.quarkus.<extension-name>.api`
- Deployment: `io.quarkus.<extension-name>.deployment`
- Deployment SPI: `io.quarkus.<extension-name>.deployment.spi`
- Runtime Dev: `io.quarkus.<extension-name>.dev`
- Hyphenated names become underscored: `quarkus-foo-bar` -> `io.quarkus.foo_bar`

## Artifact Naming

- Runtime: `quarkus-<name>`
- Deployment: `quarkus-<name>-deployment`
- See `adr/0009-extension-structure.adoc` for the full naming table.

## Dev UI

- Extensions provide `CardPageBuildItem` and `JsonRPCProvidersBuildItem`
  in deployment processors guarded by `@BuildSteps(onlyIf = IsLocalDevelopment.class)`
- Dev-only classes go in `runtime-dev/` modules, loaded only in dev mode

## Independent Projects

Some sub-projects in `independent-projects/` have their own build lifecycle:
- `arc/` - The CDI implementation
- `resteasy-reactive/` - RESTEasy Reactive
- `qute/` - The templating engine
- `tools/` - Shared tooling utilities

These can be built standalone, but are also included in the main Quarkus build.
