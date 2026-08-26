---
name: deprecation-cleanup
description: >
  Maintain @Deprecated code in the Quarkus codebase: remove code that has been
  deprecated for more than 12 months, and add the @Deprecated annotations that
  were missed when a related element was deprecated (e.g. a field is deprecated
  but its getters/setters/constructors are not). Use this skill whenever the
  user asks to clean up deprecations, remove old/long-deprecated code, purge
  deprecated APIs, "remove code deprecated for over a year", audit @Deprecated
  usage, or fix inconsistent/incomplete deprecation annotations — even if they
  don't name a specific class or module.
---

# Deprecation Cleanup

Quarkus accumulates `@Deprecated` code over time. This skill covers two
workflows, sharing one discovery tool and the same commit conventions:

- **Removal** — code deprecated for **at least 12 months** (the project's
  convention) is safe to delete; keeping it forever defeats the point of
  deprecating it.
- **Completion** — when one element is deprecated, everything that only exists
  to serve it should be deprecated too. A common miss: a field is marked
  `@Deprecated` but its getter, setter, constructor parameter, or builder method
  is not, so callers using the accessors get no warning.

## The discovery tool

`scripts/find-deprecated.sh` scans Java files for `@Deprecated` and prints each
entry **sorted by the date it was originally introduced**. It uses
`git blame -w -M -C -C -C` so a line's date reflects when it was first written,
not when a later refactor moved it — so the 12-month clock runs from the *real*
introduction date. Run it from the skill's `scripts/` directory (or use the full
path):

```bash
scripts/find-deprecated.sh core/deployment --removable --csv          # Workflow A: only entries ≥12 months old
scripts/find-deprecated.sh core/deployment --csv                      # Workflow B: ALL deprecations (default)
scripts/find-deprecated.sh core/deployment --min-age-months=18 --csv  # custom threshold
```

`--csv` emits CSV (easier to sort/filter) instead of the formatted table;
redirect to `$TMPDIR` to avoid re-scanning. Columns: `File Location`
(path:line), `Deprecated Entry` (the declaration), `Date` (YYYY-MM-DD
introduced), `Commit` (short hash).

Scan the narrowest path that covers the request — the whole repo is slow (one
`git blame` per hit).

## Workflow A — Remove long-deprecated code

The rule is age-based, but age alone is not sufficient to delete safely. Removal
of a public API can break callers, so verify before you cut.

### 1. Discover the removal candidates

Run `--removable` (see above); the list it prints is already the candidate set.
Entries dated `unknown` (blame couldn't resolve) are excluded because their age
can't be confirmed — run without `--removable` to review those individually.

### 2. Confirm each candidate is genuinely safe to remove

For every candidate, before deleting:

- **Search the whole repo for remaining usages** of the symbol (method name,
  constructor signature, field). Include tests, integration-tests, and docs.
  Deprecated does not mean unused.
- If usages remain, they were supposed to migrate to the replacement named in
  the `@deprecated` javadoc. **Migrate them first** (in the same change), or, if
  migration is non-trivial or out of scope, **leave that entry in place** and
  move on — don't delete something the codebase still depends on.
- Watch for **overrides and API contracts**: a deprecated method may implement
  an interface or be overridden elsewhere; removing it can break the hierarchy.

If in doubt about whether an external/public API can be dropped, surface it to
the user rather than guessing — some deprecated items are kept intentionally for
downstream compatibility.

### 3. Remove, one class per change

Delete the declaration and its javadoc/annotation block together. Preserve
surrounding comments that are not about the removed element (see the repo's
editing rules). Group all removals **within a single class into one commit**;
keep different classes in separate commits so history stays reviewable.

### 4. Build the affected module

Removing code can break compilation elsewhere in the module (or its dependents).
Build the module you touched — and rebuild its deployment module if you changed a
runtime module (see the `building-and-testing` skill):

```bash
./mvnw install -f <module-path> -DskipTests
```

Fix any fallout (usually leftover callers you can migrate to the replacement).
Only consider the removal done once it compiles.

### 5. Commit

One commit per class. See **Commit conventions** below.

## Workflow B — Complete missed deprecations

When you deprecate something, its satellites should be deprecated too. The
classic gap is a deprecated **field** whose **accessors** (getter/setter),
**constructors**, or **builder methods** were left un-annotated — so a caller
using the getter gets no warning.

### 1. Find the gaps

Run the tool **without an age filter** (the default) so every deprecation shows
up regardless of when it was introduced — the gaps you're hunting for are often
recent:

```bash
scripts/find-deprecated.sh <path> --csv
```

While reviewing a class (or when the user reports one), look for a deprecated
element whose companions are not deprecated:

- A `@Deprecated` field → is its getter/setter deprecated? Its constructor
  parameter? The builder method that sets it?
- A `@Deprecated` constructor/method → do overloads that only forward to it, or
  builder entry points that only exist for it, still lack the annotation?

`git blame` the un-annotated companion to understand when it was introduced and
whether it was meant to be deprecated alongside the element it serves. The commit
that deprecated the primary element is your reference.

### 2. Add the annotation + javadoc

Add both an `@Deprecated` annotation and a javadoc `@deprecated` tag that tells
the user what to use instead. The javadoc is what makes the deprecation
actionable — without a replacement pointer it's just noise.

```java
/**
 * @deprecated Use {@link GeneratedResourceBuildItem#GeneratedResourceBuildItem(String, byte[])} instead.
 *             If you want to serve static resources use
 *             {@link io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem} instead.
 */
@Deprecated(since = "4.0")
public GeneratedResourceBuildItem(String name, byte[] data, boolean excludeFromDevCL) {
    ...
}
```

### 3. Build and commit

Build the module (Workflow A, step 4) and commit — message form
*"Code deprecation in `X`"* (see below).

## `@Deprecated` annotation conventions

- Prefer the form **`@Deprecated(since = "<version>")`** over a bare
  `@Deprecated`. `since` records when the deprecation started (which
  drives the 12-month removal clock and helps future cleanup)
- Always pair the annotation with a javadoc **`@deprecated`** tag naming the
  replacement.
- **Choosing `since`:**
  - For a companion that *should have been* deprecated alongside an existing
    element, match the intent of that element — but note the actual value is a
    judgement call, ask the user if in doubt.
  - For a brand-new deprecation, use the **current in-development release**
    version. The main branch carries `999-SNAPSHOT`, so the real version isn't in
    `pom.xml` — determine it from recent release branches/tags (recent work used
    `4.0`). If you can't determine it confidently, ask the user rather than
    inventing a number.

## Commit conventions

One class per commit. The body cites the commit(s) that introduced the
deprecation, so a reviewer can verify the 12-month age without re-running blame.
Follow the repository's policies.

**Removal — single introducing commit:**

```
Remove deprecated code from `TransformedClassesBuildItem`

The removed code has been deprecated for at least 12 months in 469fd3ae
```

**Removal — multiple introducing commits:**

```
Remove deprecated code from `ReflectiveHierarchyBuildItem`

The removed code has been deprecated for at least 12 months in the
following commits:

* 498d936a
* aeeaa55d
```

**Completing a missed deprecation:**

```
Code deprecation in `GeneratedResourceBuildItem`

The removed code has been deprecated for at least 12 months in c76362ab.

The newly deprecated methods should have been deprecated as part of
41cd5830
```

Use backticks around the class name in the subject. Reference commits by their
short hash (from the discovery tool's `Commit` column).

## Common pitfalls

- **Don't trust `@Deprecated` as proof of disuse.** Always grep for callers
  first; deprecated APIs are frequently still used internally.
- **Don't remove based on a move-date.** The tool already uses `-M -C` to trace
  the true origin — but if you compute a date another way, a refactor can make
  old code look recent and vice-versa.
- **Don't skip the build.** Compilation is the cheapest check that a removal
  didn't strand a caller.
- **Don't reorder or hand-sort imports** after deletions — the formatter and
  `impsort-maven-plugin` handle that during compilation. Don't pass `-Dno-format`.
- **Keep unrelated comments.** Only delete the javadoc/annotation attached to the
  element you're removing.
