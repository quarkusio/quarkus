# Hibernate Migration Guide

Prepare complete replacement sections for the Quarkus wiki, including the
application-facing changes across the release. The dependency table in the PR
and the migration guide have different baselines.

## Find the existing guide and release baseline

Identify the Quarkus release receiving the upgrade. Check the PR base branch
and release metadata; `main` uses `999-SNAPSHOT`, so consult its milestone when
the release is unclear. For a shared upgrade spanning release branches, identify
the intended wiki release explicitly.

Read the latest affected sections of `Migration-Guide-<release>` from the
[Quarkus wiki](https://github.com/quarkusio/quarkus/wiki), using its raw source
or a refreshed wiki checkout. Preserve applicable Quarkus-specific configuration
and compatibility guidance. If there is no entry yet, establish the starting
Hibernate versions from the previous Quarkus release's POM and history.

For a major upgrade, cover the entire transition from the Hibernate series in
the previous Quarkus release. A later Alpha/Beta/CR bump does not reset that
baseline. For example, an ORM `8.0.0.Beta4` to `8.0.0.CR1` bump for Quarkus 4.0
still needs the ORM 7.4 to 8.0 migration guide. The Beta4-to-CR1 delta belongs
in the dependency table and helps identify additions or corrections.

## Reconcile the content

Read the full migration guides for the target Hibernate releases. Online guides
can move ahead of the requested version; check their version and use tagged
source when necessary.

| Component | Upstream guide |
|-----------|----------------|
| ORM | `https://docs.hibernate.org/orm/<major.minor>/migration-guide/` |
| Reactive | `https://docs.hibernate.org/reactive/<major.minor>/migration-guide/` |
| Search | `https://docs.hibernate.org/search/<version>/migration/html_single/` |

Cover relevant API removals, application-facing SPI changes, behavioral
differences, DDL changes, and minimum database versions. For Reactive, include
its own breaking changes as well as inherited ORM changes. For Search, verify
whether reindexing or an outbox-polling schema update is needed. Include changed
Elasticsearch/OpenSearch Dev Services defaults when relevant.

**Check existing claims as well as new changes.** Compare defaults, property
names, compatibility statements, and workarounds with the target Hibernate tag
and effective Quarkus behavior. Correct superseded guidance, remove duplicates,
and preserve notes that still apply. If sources conflict, inspect the target
code and history; report anything that remains unresolved.

In particular:

- A behavior may have been introduced and then reverted during prereleases.
  Direct JDBC handling of `java.time`, for example, was enabled by default in
  ORM 8.0.0.Beta4 and disabled again in 8.0.0.CR1. Check both ORM's and Quarkus's
  defaults, the opt-in property, and timezone limitations.
- Quarkus may override upstream validation/DDL or type-mapping behavior.
  Do not turn an upstream change into a migration requirement that Quarkus
  already handles.
- Hibernate's supported minimum database versions and Quarkus's default target
  versions are different. Compare the former with the release baseline and
  verify the latter against Quarkus configuration.

## Match the wiki style

Use [Quarkus 3.35's Hibernate section](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.35#hibernate-orm)
as the reference for concise linked bullets. API, behavioral, and DDL bullets
normally contain one descriptive link and punctuation. Put the short change
summary inside the link; leave detailed explanations and examples upstream.
Keep extra prose where needed for a Quarkus-specific qualification.

For example, in AsciiDoc:

```asciidoc
* https://docs.hibernate.org/orm/7.3/migration-guide/#columns-annotation[The obsolete `@Columns` annotation was removed].
```

Use exact, verified upstream anchors. If no migration-guide section covers an
API change, link to an appropriate API reference. This is a synthesis of
breaking changes, not a list of new features or all upstream commits. Omit empty
subsections and avoid unverified backwards-compatibility claims.

Match the existing wiki's source format, heading hierarchy, anchors, and
cross-references. Use Markdown when requested, converting links and admonitions
without changing their meaning. The output must be the full reconciled sections,
not an addendum asking readers to combine them with earlier migration notes.

## Include the full guide and diff in the PR

Under `## Migration guide`, include the complete replacement as raw source in a
fenced `asciidoc` or `markdown` block. Use a fence longer than any fences inside
the guide, so the source remains directly copyable.

When the wiki already has the affected sections, also include
`### Diff from the current wiki` with a link identifying the baseline page and
sections, followed by an actual unified diff in a fenced `diff` block. A
collapsed `<details>` block can keep the PR readable.

1. Refresh the wiki before preparing the comparison. Reconcile any edits made
   since the initial read.
2. Compare only the affected existing sections with the complete replacement.
   Do not compare against the entire wiki page, Hibernate tag delta, or previous
   PR body.
3. Use the same markup and heading hierarchy on both sides. If the full guide
   is Markdown and the wiki uses AsciiDoc, convert the existing sections without
   rewriting their content first, and explain this next to the diff. Formatting
   conversion should not obscure the migration changes.
4. Generate a unified diff with additions, removals, and context, using
   `git diff --no-index` or `difflib.unified_diff`, for example. Include corrected
   claims and wording changes; an additions-only list is insufficient.
5. Verify that applying the diff to the captured baseline produces the full
   replacement exactly.

**If no existing entry exists for the relevant release/upgrade, include only
the full guide.** Do not manufacture a comparison with an unrelated release.
If the existing sections are unchanged, state that instead of adding an empty
diff block.

On later updates, replace both blocks without duplicating them, and preserve
the rest of the PR body. Read the published body back and verify that the full
guide and diff survived unchanged. Keep the shared guide and comparison aligned
across in-scope forward-port/backport PRs.

Generating this content does not publish it to the wiki. Report the PR link and
any unresolved inconsistency briefly in the final response; the reviewable
comparison is already in the PR body.
