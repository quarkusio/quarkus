# Development and remote lifecycle

## Audience and scope

This page is for contributors changing Gradle continuous execution, dev-mode
process supervision, continuous testing, remote-development sessions,
reconnect behavior, or cleanup. It owns Gradle and HTTP remote session
lifecycle. The build-tool-neutral TCP messages and apply-state machine belong
to the [external build-output protocol](external-build-output-protocol.md).

Dev and remote-dev task types and documented command-line options are
user-facing. Deployment handles, trigger files, snapshots, and internal
receipts are current implementation contracts between plugin-owned
components, not general SPIs.

## Gradle-owned continuous execution

The standalone plugin makes Gradle the owner of compilation and resource
processing. `quarkusApplicationDev` and
`quarkusApplicationContinuousTest` run as Gradle continuous-build tasks:

1. Gradle executes normal compile/resource/code-generation dependencies.
2. The task snapshots the declared main, dependency, test, resource, and
   runtime-JAR outputs.
3. A successful iteration converts Gradle file changes into output-root-aware
   changes.
4. Compilation/resource failures are reported separately by build event
   listeners because the long-lived launch task is skipped when a dependency
   fails.
5. The dev session sends a successful delta, recovery rebaseline, restart
   requirement, or failure status to Quarkus.

The dedicated continuous-test task prevents the ordinary `test` task from
running concurrently when explicitly selected. Dev mode can still enable
Quarkus continuous testing within the same long-lived process.

Gradle continuous builds require `--continuous`. Configuration-cache reuse
between continuous iterations is not assumed; each configuration must still
be cache-compatible and Isolated-Projects-safe.

## Output snapshots and recovery

Each task iteration observes immutable resolved output-root descriptors and a
snapshot stored under `build/`. A missing or corrupt prior snapshot requests a
complete rebaseline rather than guessing a delta.

Successful observation is persisted before delivery so later iterations
compare against what Gradle actually produced. Compilation failure does not
advance the successful baseline. Recovery after a reported failure is
distinguished from an ordinary successful delta so Quarkus can clear failure
state even when no single changed path explains the recovered output.

Runtime-JAR changes request restart/rebootstrap. Class changes are restricted
to class files, directory resource events are filtered, and every path remains
associated with its declared output root.

## Dev process supervision

Gradle's deployment registry holds one supervisor per effective dev
configuration. The supervisor owns at most one current child process and
external-output transport generation.

Task iterations acquire the current healthy generation. Unexpected transport
or child-process failure is detected and cleaned up before one leader creates
a replacement generation; concurrent followers await the same recovery.
Automatic recovery is bounded within a failure window so a crash loop becomes
a terminal, actionable Gradle failure.

Stopping the deployment closes the transport, stops the child, releases the
output tree, terminates scenario-owned executors, and writes the close
receipt. Slow or failed cleanup is bounded and reported instead of leaking a
process. The lifecycle does not stop the shared Gradle daemon.

The task parses the first Quarkus listen-address log lines and logs the
browser Dev UI URL at lifecycle level on subsequent continuous invocations.
Log parsing is a bridge until the running Quarkus side exposes structured
listen-address metadata.

## Live-reload disable and replay

The build-output protocol owns live-reload state, sequence, coalescing, and
rebaseline semantics. See
[external build-output protocol](external-build-output-protocol.md#sequence-and-coalescing-policy).
The Gradle-specific reaction is to touch the session's replay trigger when an
asynchronous notification enables live reload, causing another continuous
iteration without requiring the user to edit a file.

## HTTP remote development

`quarkusApplicationRemoteDev` is a different lifecycle and protocol:

1. a mutable-JAR build is materialized;
2. the task captures a complete local package snapshot;
3. the HTTP client connects with the configured live-reload URL and password;
4. the server may request an initial subset of files;
5. subsequent iterations send changed files and deletion paths; and
6. a background `/dev` poll detects a stale server session and requests a
   Gradle reconnect iteration.

The session, rather than one Gradle task instance, owns the client and last
accepted snapshot. Reconnect is serialized: one trigger asks Gradle for a new
iteration, which establishes the replacement session and converges from the
complete current snapshot. An application-model update is sent last because
it causes the remote application to restart.

The HTTP client serializes request counters, including background poll
requests. It treats a stale-session response as a reconnect requirement, does
not expose the password in diagnostics, bounds restart probing, and interrupts
and joins its polling thread on close.

The runtime-side HTTP handler in `extensions/vertx-http/runtime` pauses and
bounds each request body before deserializing or materializing it. It rejects
invalid or oversized declared
lengths, enforces aggregate byte and active-collector admission, spools
accepted bodies outside event-loop work, applies an object-input filter to
serialized state, and releases reservations and temporary files on success,
rejection, disconnect, and shutdown. These concrete limits are defensive
implementation policy rather than an application-plugin compatibility
contract.

## Path and trust boundary

Remote snapshots contain normalized relative package paths. Paths that are
absolute, escape the package root, or use a disallowed symlink are rejected.
Before upload, the client re-reads attributes without following links and
verifies size and hash against the captured change. A mismatch reports that
the file changed after capture instead of uploading different bytes under an
authenticated request.

The password authenticates the existing Quarkus remote-dev HTTP exchange; it
does not make an untrusted build directory safe. Local build output and the
remote endpoint remain trusted inputs to a development session.

## Platform and environment qualifications

Continuous-build cancellation and output-tree release are covered on Unix and
Windows, but filesystem timing and path canonicalization remain
platform-sensitive. HTTP remote dev needs a reachable endpoint with the
matching Quarkus remote-dev configuration. Containerized remote applications
may expose a management address different from the main HTTP address.

## Source and test ownership

Primary source owners:

- `io.quarkus.gradle.application.tasks.QuarkusApplicationDevTask`
- `io.quarkus.gradle.application.internal.dev`
- `io.quarkus.gradle.application.tasks.QuarkusApplicationRemoteDevTask`
- `io.quarkus.gradle.application.internal.remotedev`
- `io.quarkus.deployment.dev.remotedev` for the HTTP client
- `io.quarkus.vertx.http.runtime.devmode.RemoteSyncHandler` and its
  request-body collector in `extensions/vertx-http/runtime`

Primary test owners:

- `QuarkusApplicationDevContinuousBuildTest`
- `QuarkusApplicationContinuousTestingFailureRecoveryTest`
- `QuarkusApplicationDevUiContinuousBuildTest`
- `QuarkusApplicationContinuousTestingParityTest`
- dev deployment/session and replay unit tests
- remote-dev task/session tests in `gradle-app-plugin`
- `HttpRemoteDevPackageClientTest`
- remote sync request-body admission, spool, and session tests in
  `extensions/vertx-http`
- Tooling API continuous-build and platform cancellation journeys
