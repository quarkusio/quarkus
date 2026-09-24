# Reproducible builds in Quarkus

Quarkus should produce the same build output when given the same sources, dependencies,
configuration, and build environment. This applies to generated and transformed classes,
recorded bytecode, serialized build inputs, metadata, and packaged application files.
A build that simply succeeds repeatedly is not necessarily reproducible - what makes it
reproducible is that the build output should match bit by bit.

This guide is for contributors writing build steps, build items, recorders, and packaging
code. The recurring rule is to fix all the sources of instability: collection order,
timestamps, unique identifiers, and unique file paths are the common offenders.

**TL;DR:** call recorders in a stable order with stable parameters, iterate over
collections in a stable order, derive identifiers from stable content, and use the
configured output timestamp instead of the current clock.

## Use collections with a stable iteration order

A build step that calls recorder methods must follow two rules:

1. Recorder methods must be called in the exact same order from one build to
   another. For example, when a recorder method is called in a loop, the loop
   iterations must be stable.
2. Parameters passed to a recorder method must be fully stable from one build to
   another.

Collections are the most common way to break both rules.

Any collection that is passed into a recorder can introduce unstable bytecode. This
could happen in the following ways:

- A collection is wrapped in a build item that can be used by an unknown build step.
- A collection is passed to a method annotated with `@Record`.
- A collection is initialized inside a method annotated with `@Record` or any method
  that is invoked from the recorded method.

The above list is not exhaustive and should be treated as a general outline of **how**
iterating over such collections may end up being recorded.

If a collection is not iterated over and is used only for operations such as
`Set#contains(...)`, then its order is unlikely to matter.

The fact that a collection by itself has a stable iteration order does not
necessarily mean that its order is stable: a list passed to a recorder could have been
initialized in a way that made its internal order unstable. General rules of thumb:

* It is fine to use `HashMap` and `HashSet` as long as their keys or elements produce
  stable hash codes. It's an implementation detail, but vanilla hash collections have
  a deterministic iteration order as long as keys have stable hash codes **and** elements
  are inserted into them in a stable order. `Class<?>` and objects inheriting
  `Object.hashCode()` are not suitable to use as keys in such collections.

* If you **need** to use a key with an unstable hash code, prefer using `LinkedHashMap`
  or `LinkedHashSet`.

* Use `TreeMap` to sort entries by key or `TreeSet` to sort elements.

* If you have no control over where the collection comes from and you think it **may**
  have an unstable order, you need to ensure its order is fixed. However, bear in mind
  that sometimes it's easier and cleaner to fix the order in the upstream build item
  producer once than it is to fix its order in multiple consumers.

* Immutability does not imply stable iteration order. In particular, `Set.copyOf()`,
  `Map.copyOf()`, `Collectors.toUnmodifiableSet()`, and `Collectors.toUnmodifiableMap()`
  can and will change the observed order across different JVM runs. When an ordered
  collection must also be immutable, wrap it with `Collections.unmodifiableSet()` or
  `Collections.unmodifiableMap()` instead of copying it into a JDK immutable collection.

## Build items and build-step order

When a build item extends the abstract `MultiBuildItem` class, it can be produced
by multiple build steps. The consumers of such a multi-build item will receive a list
of all produced items.

The build chain assigns stable producer ordinals to `MultiBuildItem` producers.
This prevents the order of items from different producers from depending on
which parallel build step finishes first. So, the build chain itself is stable,
and if every producer of a `MultiBuildItem` is stable, the order of the consumed
list should be stable as well. However, that is not always the case because
producers may produce such build items by iterating over a collection in an unstable
order.

In such cases, one possible solution (besides fixing the problem at its source,
which is frequently preferable) is to make the `MultiBuildItem` implement `Comparable`.
This way, consumers will receive a sorted list of items. However, bear in mind that
`compareTo()` must fully distinguish their ordering. Unresolved ties can still allow
unstable ordering.

## Try to eliminate other well-known sources of instability

Derive generated class names, proxy keys, identifiers, and serial numbers from
stable content. Avoid random UUIDs, identity hash codes, timestamps from the
current clock, and mutable global counters.

Use the configured package output timestamp for values embedded in build
artifacts, such as archive entries, generated build information, and SBOM
metadata. Do not call the current clock while generating such values when a
reproducible timestamp is available.

For a Maven build, the `project.build.outputTimestamp` property can provide Quarkus
with a fixed timestamp.

For a Gradle archive task, set `preserveFileTimestamps` to `false` and
`reproducibleFileOrder` to `true`. Consult
[this reference](https://docs.gradle.org/current/dsl/org.gradle.api.tasks.bundling/Jar.html)
for more information.

Once the fixed timestamp has been configured with the build tool, you can consume
`PackageConfig` in your build step and use its `outputTimestamp()` method to get
the configured timestamp.

## Run reproducibility checks locally

### JVM tests

In order to run reproducibility checks locally, build the affected modules and
their dependencies first. Then run the deployment tests, replacing the module name
below:

```bash
./mvnw install -f extensions/<name> -DskipTests
./mvnw test -f extensions/<name>/deployment -Dno-build-cache \
  -Dquarkus-internal.test.reproducibility-check=3
```

The property above causes the test infrastructure to run augmentation three
times and compare the generated and transformed in-memory class bytes. A passing
check aborts each test before its test methods run. It is a bytecode check, not a
replacement for the test's behavioral assertions. The same property also enables
generated-bytecode checks in `ArcTestContainer`; tests configured with `shouldFail`
are skipped by that check.

To catch values that differ between JVM processes, run the deployment tests
with an additional property:

```bash
-Dquarkus-internal.test.reproducibility-check.cross-jvm=true
```

This mode is slower because each augmentation runs in a separate JVM.

### Investigate a failure

On a mismatch, the reproducibility check writes class files and their decompiled
`*.java` counterparts under `target/debug/` in the module being checked. Start
with `mismatch.txt`, which identifies the changed, missing, and extra classes.
A typical failure produces the following structure:

```text
target/debug/<test>/<execution-id>/reproducibility-mismatch/
├── mismatch.txt
├── run-1/                 # reference class files
├── run-<n>/               # differing class files
├── run-1-decompiled/      # readable reference
└── run-<n>-decompiled/    # readable difference
```

For a changed class, which is often generated from a method annotated with `@Record`
in a deployment module, diff the matching Java files in the two `*-decompiled`
directories:

```bash
diff -u run-1-decompiled/<class>.java run-<n>-decompiled/<class>.java
```

Cross-JVM runs write their class dumps under `target/reproducibility/` in that module.

### Integration tests

The integration-test reproducibility check repeatedly packages integration-test
applications and compares their `target/quarkus-app` directories with
`diffoscope`. Before running the check, build and install the affected Quarkus
modules and install `diffoscope`. Installing `procyon-decompiler` is also
recommended because it lets `diffoscope` present class-file differences as
readable Java code.

The following example runs the check five times for the module of your choice:

```bash
NUM_RUNS=5 \
OUTPUT_DIR=<your-dir-of-choice> \
EXTRA_MAVEN_ARGS="--settings .github/mvn-settings.xml \
-Dformat.skip \
-Denforcer.skip \
-DskipDocs \
-Dforbiddenapis.skip \
-DskipExtensionValidation \
-DskipCodestartValidation \
-pl <your-module-of-choice>" \
.github/check-it-reproducibility.sh
```

The `-pl` argument selects the integration-test module. Replace
`<your-module-of-choice>` with the module you want to check. Set `NUM_RUNS=2`
for a quicker initial check.

The script writes its results to `OUTPUT_DIR`. The directory contains a
`report.md` summary, lists of checked and non-reproducible modules, the reference
outputs, and detailed HTML comparisons under `diffs/`.

**Note:** If you are running on macOS and the `diffoscope` output is not legible,
try running the script above with the
`DIFFOSCOPE="diffoscope --exclude-command=zipdetails"` environment variable set.

## CI setup

Reproducibility checks currently run nightly on the `main` branch. They are
not yet part of every pull request because their runtime needs to be reduced.
The goal is to include them in PR checks once they are fast enough.

For both the JVM and integration-test checks, CI uses five runs.

If you wish to see the CI status of the reproducibility checks after you merged a PR,
you can check the [Quarkus status website](https://status.quarkus.io/) the next day.
From the status page, you can follow the relevant workflow run to confirm that it
tested a commit containing your change and inspect its failure artifacts or report.
Scheduled runs may start or finish later than expected, so check the run's timestamp
and commit rather than relying only on the calendar day.

If the checks failed, it would be great if you could investigate the failure and
fix it.

If you cannot fix it, please report it on the
[Quarkus issue tracker](https://github.com/quarkusio/quarkus/issues).

- [Deployment tests and Jakarta REST TCK][repro-jvm]
  repeat augmentation and compare generated and transformed class bytes.
- [Integration tests][repro-it]
  rebuild packaged applications and compare `target/quarkus-app` outputs with
  `diffoscope`.

[repro-jvm]: https://github.com/quarkusio/quarkus/actions/workflows/reproducibility-checks.yml
[repro-it]: https://github.com/quarkusio/quarkus/actions/workflows/reproducibility-checks-it.yml

## Treat test-only exceptions as a last resort

`ReproducibilityCheckBuildItem` marks an augmentation that compares generated
bytecode. It allows a build step to become aware that the reproducibility check
is running and change its behavior accordingly.

This may be useful to make sure that the checks stay green even when the build
step generates volatile bytecode. That is why it is important to use this marker
only when the volatile bytecode is not generated for a production application.
For example, it is used where test/dev-only WebJar output needs a random temporary
path and substitutes it for a stable one if a marker build item is present.

First try to make the generated value stable. Use the marker only when the
volatile bytecode is genuinely limited to the test or dev environment and
cannot occur in the production artifact. Keep the exception narrow and
document why it is valid. Prefer fixing a specific source of variation over
excluding generated classes from comparison.

## Further information

If you need additional assistance, feel free to tag @reaver585 or @gsmet in the issue.
They are the maintainers of the reproducibility checks.
