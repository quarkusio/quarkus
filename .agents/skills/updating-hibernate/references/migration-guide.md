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
or a refreshed wiki checkout. Record the baseline revision, source filename,
and section boundaries so the comparison is reproducible. If raw content
disagrees with a user-supplied revision, retrieve that exact revision or refresh
the wiki checkout and resolve the discrepancy before editing. Do not silently
use stale raw content. Determine the destination markup from the source file;
an AsciiDoc wiki remains AsciiDoc even though the PR body uses Markdown.

Establish the starting Hibernate versions and effective Quarkus behavior from
the previous Quarkus release's POM, configuration, and history, even when a wiki
entry already exists. Preserve applicable Quarkus-specific configuration and
compatibility guidance.

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

**Check existing claims as well as new changes.** Compare effective Quarkus
behavior in the previous release with the target release, including defaults,
supported APIs, property names, compatibility statements, and workarounds.
Include a change only when it requires application adjustments or awareness of
a concrete compatibility risk. An upstream migration-guide entry is a candidate
to investigate, not automatic justification for a Quarkus entry.

Apply these filters to additions and existing entries alike:

| Case | Treatment |
|------|-----------|
| Unchanged behavior | Omit it, including upstream changes neutralized by Quarkus configuration. |
| Prerelease feature reverted before the target release | Remove the note when the target restores the previous release's behavior. Keep the investigation out of the migration guide. |
| Ordinary bug fix | Omit it unless investigation identifies a concrete migration impact, such as an application adjustment or compatibility risk; describe that impact when included. |
| Unsupported usage | Omit custom persistence providers and settings Quarkus does not support, after checking whether supported usage, such as query hints, is also affected. Document any affected supported usage. |
| Breaking change affecting supported usage | Include the application impact and required action or relevant upstream guidance. |

Correct superseded guidance, remove duplicates and obsolete notes, and preserve
entries that still apply. Remove subsections left empty by this review. If
sources conflict, inspect the target code and history; report anything that
remains unresolved.

In particular:

- For direct JDBC handling of `java.time`, check both ORM's and Quarkus's
  defaults against the previous Quarkus release. If an enabled prerelease
  default was reverted and the target restores the baseline, remove the note
  instead of explaining the reversion or unchanged opt-in behavior.
- Quarkus may override upstream validation/DDL or type-mapping behavior. If
  the effective behavior is unchanged, omit the entry rather than documenting
  why the upstream change does not affect Quarkus.
- Hibernate's supported minimum database versions and Quarkus's default target
  versions are different. Compare the former with the release baseline and
  verify the latter against Quarkus configuration.

## Match the wiki style

Use [Quarkus 3.35's Hibernate section](https://github.com/quarkusio/quarkus/wiki/Migration-Guide-3.35#hibernate-orm)
as the reference for concise linked bullets. API, behavioral, and DDL bullets
normally contain one descriptive link and punctuation. Put the short change
summary inside the link; leave detailed explanations and examples upstream.
Keep extra prose where needed for a Quarkus-specific qualification.

Put qualifications and workarounds inside the relevant entry, including
conditions about older starting versions and fallback configuration. Do not
append detached notes that readers must match to earlier bullets. Prefer series
versions, such as ORM `8.0` and Reactive `5.1`; use a precise prerelease version
only when essential to explain a migration requirement.

Preserve the wiki's source format, heading hierarchy, anchor names, and
cross-references. The output must be the full reconciled sections, not an
addendum asking readers to combine them with earlier migration notes.

For an AsciiDoc destination, these compact examples show the native syntax;
retain the actual guide's anchors and heading levels:

```asciidoc
[[hibernate-orm]]
== Hibernate ORM

[[hibernate-orm-api-changes]]
=== API changes

* https://docs.hibernate.org/orm/7.3/migration-guide/#columns-annotation[The obsolete `@Columns` annotation was removed].
** Remove remaining references to this annotation.

See <<hibernate-orm-api-changes,API changes>>.

NOTE: Apply the relevant API changes before upgrading.
```

Keep anchors on their own line before the heading, and use `**` for nested
bullets. Use `URL[label]` links, `<<anchor,label>>` cross-references, and native
admonitions such as `NOTE:` or `[WARNING]` followed by a delimited block. Do not
substitute Markdown links, blockquotes, headings, or HTML anchors in the wiki
source. Admonition labels belong to their admonition, not the preceding entry.

Use exact, verified upstream anchors. If no migration-guide section covers an
API change, link to an appropriate API reference. Check the rendered link's
actual URL fragment against the destination anchor, not just whether the page
loads. In Javadoc varargs fragments, `...` can become a Unicode ellipsis during
rendering; encode the dots as `%2E%2E%2E` when necessary and verify the resulting
link. Avoid unverified backwards-compatibility claims.

Render the replacement using the destination's markup renderer, then check
links and fragments, missing or changed anchors, literal HTML, leftover
Markdown syntax, flattened nested lists, and misplaced admonition labels.
Compare with the captured source for accidental content loss or movement,
including unaffected entries and references from elsewhere on the wiki page.
If rendering or link checks are unavailable, report that limitation explicitly.

## Include the full guide and diff in the PR

Under `## Migration guide`, include the complete replacement as raw source in a
single fenced `asciidoc` block for an AsciiDoc wiki, or the corresponding native
source block for another wiki format. Use a fence longer than any fences inside
the guide, so the source remains directly copyable. Keep PR headings and any
collapsible wrappers outside the source block.

When the wiki already has the affected sections, also include
`### Diff from the current wiki` with a link identifying the baseline revision,
page, and sections, followed by an actual unified diff in a fenced `diff` block. A
collapsed `<details>` block can keep the PR readable.

1. Refresh the wiki before preparing the comparison. Reconcile any edits made
   since the initial read and update the recorded baseline revision. If the
   user requested a pinned revision, compare against it and identify it as such
   rather than claiming it is the latest wiki.
2. Compare only the affected existing sections with the complete replacement.
   Do not compare against the entire wiki page, Hibernate tag delta, or previous
   PR body.
3. Use the unconverted native wiki source as the baseline and preserve its
   markup and heading hierarchy in the replacement. Do not normalize or convert
   the baseline before diffing.
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
the rest of the PR body. After an authorized PR creation or update, read the
stored body back, extract the replacement and applicable diff from their
fences, and compare both verbatim with the prepared source. Check that fences
and wrappers remain intact; a visual inspection alone does not prove the source
survived unchanged. Report any unavailable read-back or comparison check.
Keep the shared guide and comparison aligned across in-scope
forward-port/backport PRs.

Generating this content does not publish it to the wiki. Report the PR link and
any unresolved inconsistency briefly in the final response; the reviewable
comparison is already in the PR body.
