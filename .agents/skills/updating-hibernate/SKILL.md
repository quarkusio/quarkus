---
name: updating-hibernate
description: >
  Upgrade Hibernate ORM, Reactive, Search, and Tools dependencies in Quarkus:
  align related versions, check integration changes, validate the upgrade,
  and prepare migration guidance with a diff against the current wiki.
---

# Updating Hibernate

Use this skill for Hibernate dependency upgrades, whether the target versions
come from the user or a dependency-update pull request. Commands and paths
below are relative to the Quarkus checkout.

## Establish the upgrade scope

Identify the target branch and requested component versions. Read the current
values from that branch's `pom.xml`; these are the starting versions for the
PR's dependency table. Inspect the base branch and version table of a supplied
PR rather than assuming it targets `main`.

Keep Hibernate Reactive and Search compatible with the target ORM release.
Check their release metadata rather than inferring compatibility from matching
version numbers. If a compatible release is unavailable, explain the constraint
before treating the upgrade as complete. Leave components outside the requested
upgrade unchanged unless a compatibility requirement needs to be addressed.

## Align dependency versions

### Hibernate ORM

Read the **target release**, not Hibernate's development branch. Use an existing
source checkout with `git show <tag>:<path>`, or fetch the tagged source from
the upstream repository. Verify the tag name; final-release tags commonly omit
the `.Final` suffix.

Check `gradle/libs.versions.toml`, or `settings.gradle` on older ORM branches,
and the published `org.hibernate.orm:hibernate-platform` POM. Align the Quarkus
properties marked `version controlled by Hibernate ORM's needs`, including:

- Jakarta Persistence and Jakarta Data APIs
- ANTLR, Byte Buddy, and Geolatte
- Hibernate Models and Hibernate Accessor, when present

Check the dependency-alignment rules in `bom/application/pom.xml` as well.
Do not undo an intentional Quarkus override without understanding why it exists.
If the target branch manages Hibernate Tools separately, keep its version
aligned with ORM. Do not introduce a property absent from that branch merely
because an older upgrade used it.

For Reactive, also check the required ORM, Vert.x, and Mutiny versions against
Quarkus's managed versions.

### Hibernate Search

When upgrading Search, inspect the target tag's `build/parents/build/pom.xml`
for the Elasticsearch client and tested Elasticsearch/OpenSearch versions.
Align these Quarkus properties as appropriate for the target release:

| Property | File |
|----------|------|
| `elasticsearch-opensource-components.version` | `bom/application/pom.xml` |
| `elasticsearch-server.version` | `build-parent/pom.xml` |
| `opensearch-server.version` | `build-parent/pom.xml` |

Verify that both Elasticsearch client artifacts managed by the shared property
exist at the selected version. Preserve intentional client overrides.

Search test resources and Java tests for hardcoded `elasticsearch.version`
values. When the server's major/minor version changes, update the affected
ORM and standalone Search tests, including Dev Services tests. Values specifying
only the unchanged major version need no update.

## Check integration changes

Read upstream migration guides and relevant source changes between the old and
new versions. Resolve compilation failures using the target API contracts,
including Quarkus session delegates, persistence providers, annotation coverage,
and boot-time/runtime service registries. Add focused regression coverage for
behavioral fixes; see [writing-tests](../writing-tests/SKILL.md).

For upgrades that change service discovery, compare `META-INF/services` files
in the old and new ORM/Search releases. Quarkus disables GraalVM's automatic
ServiceLoader registration, so new providers may need explicit registration in
`ClassNames.SERVICE_PROVIDERS` or a `ServiceProviderBuildItem`. Check both ORM
and Reactive integration where a shared service contract changes.

Inspect ORM defaults that Quarkus overrides, particularly type mappings,
validation, schema management, and database versions. An upstream default does
not necessarily describe Quarkus's effective behavior.

## Build and test

**Ask whether to run local tests before executing them.** Offer to run the
relevant Hibernate tests or skip them and leave execution to CI. Briefly state
the proposed scope, including database/container and native-test requirements.
Honor an explicit choice already made for this upgrade; otherwise wait for the
answer. No answer is not consent to run tests. Continue independent dependency
and migration-guide work while the choice is pending.

If the user skips local tests, continue preparing the upgrade and PR. Do not
run JVM, integration, or native tests, including indirectly through a build;
use the appropriate test-skipping options for any necessary build. State
"Local tests not run at the user's request" in the PR and final response.
Do not report skipped tests as passing. This choice concerns test execution;
still add or update regression coverage when the code change requires it.

Follow [building-and-testing](../building-and-testing/SKILL.md) for Maven
commands, build flags, and evidence that tests actually ran.

1. Build Quarkus and check the Hibernate BOM dependency-alignment rules,
   skipping tests unless the user has chosen to run them.
2. If the user chose to run local tests, run the affected ORM and Reactive
   extension tests within the agreed scope, including annotation
   coverage, and relevant integration tests for JPA, Panache, XML mappings,
   converters, and database behavior changed by the upgrade.
3. When Search changes and local tests were selected, run the affected ORM and
   standalone Search extension and integration tests within the agreed scope,
   including Dev Services coverage for changed images.
4. Include relevant native integration tests for service registrations or
   other native-mode changes when these are part of the agreed scope.

Run test modules sequentially. Investigate failures before reporting success;
distinguish upgrade regressions from existing failures using the target base
branch. Report skipped or blocked checks explicitly.

## Prepare the pull request

Follow [pull-requests](../pull-requests/SKILL.md) for commit and PR conventions.
Keep independent compatibility fixes in separate commits when that helps review.
Exclude local Maven configuration, build outputs, and temporary guide artifacts.

Describe the upgrade and why compatibility fixes were needed. Include a table
of the actual dependency changes, including aligned dependencies and changed
Dev Services versions. Record the validation performed and any unavailable
compatible component release.

Read [Migration guide](references/migration-guide.md) to prepare the full wiki
replacement and, when an entry already exists, a diff against the latest wiki.
Include both in the PR body; a link to a local file is insufficient.

Create new PRs as drafts. When updating an existing PR, preserve its draft
status and unrelated description content. Mark it ready only when requested.

For an in-scope forward port or backport of the same upgrade, use the same shared
migration text and identify its intended wiki release in each PR. Investigate
apparent differences against each PR's base: unrelated changes already on a
branch do not belong to this upgrade's migration notes. Explain any actual
upgrade difference that requires different guidance.

Read back the published PR body to verify the full guide and applicable diff.
The final response should link the PR and briefly report validation and any
remaining limitations; the detailed migration comparison belongs in the PR.
