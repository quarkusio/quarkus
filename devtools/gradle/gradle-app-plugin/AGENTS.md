# Gradle Application Plugin Hard Gates

This module owns the Gradle-native Quarkus application plugin. Keep it separate
from the legacy `io.quarkus` compatibility plugin.

Hard gates:

- All TestKit tests must use `--configuration-cache` and
  `-Dorg.gradle.unsafe.isolated-projects=true`.
- Use `--build-cache` for cacheable task-path tests unless the task is
  intentionally side-effecting or non-cacheable.
- Do not call `Task.getProject()` or equivalent mutable Gradle model APIs from
  task actions.
- Do not capture live `Project`, `Task`, `Configuration`, `SourceSet`,
  extension, task-container, or other mutable Gradle model objects in task
  actions, worker parameters, providers, or lazy callbacks.
- Do not access another project's mutable Gradle model.
- Do not expose internal helper methods or properties from DSL-facing types
  using Java `public` visibility.
- Do not generate task names that collide with legacy `io.quarkus` task names.
- Put expensive Quarkus, container-image, native-image, deployment, and model
  generation operations behind testable operation interfaces.
- Keep test-supporting implementations out of `src/main`.
