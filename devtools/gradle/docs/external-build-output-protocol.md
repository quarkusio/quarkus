# External build-output protocol

## Audience and scope

This page is for contributors changing the build-tool-neutral exchange between
an external compiler/build loop and Quarkus dev mode. It owns wire framing,
DTO exchange, apply status, sequence/correlation, replay, and rebaseline
semantics. Gradle session/process ownership belongs to
[development and remote lifecycle](development-and-remote-lifecycle.md).

The public DTO and server/policy types form shared Quarkus implementation API
for current build-tool integrations. This page documents current producer and
consumer behavior without promising cross-version protocol compatibility to
arbitrary third-party clients.

This is not the HTTP remote-dev protocol. It is a loopback transport between a
local build-tool process and the local Quarkus dev-mode child.

## Ownership and flow

The build tool is the server and producer of output changes. Quarkus dev mode
is the connecting client and consumer:

```text
build tool                         Quarkus dev-mode child
-----------                        ----------------------
bind loopback server
launch child with URI + token  --> connect and authenticate
CHANGES(request, batch)        --> apply to RuntimeUpdatesProcessor
                             <-- APPLY_RESULT(request, status)
                             <-- LIVE_RELOAD_STATE(generation, enabled)
```

`DevModeContext.ExternalBuildOutputTransport` carries the URI and
authentication token into the child launch. The TCP implementation binds only
to loopback and compares the random hello token without logging it.

## Framing and correlation

Protocol version 3 starts with a bounded text hello and then uses
length-prefixed UTF-8 frames. A frame is limited to 1 MiB. Negative,
oversized, or truncated lengths fail the connection before allocation or
decode.

Each `CHANGES` message has a non-negative request ID. Exactly one matching
`APPLY_RESULT` completes it. Unknown, duplicate, out-of-order, or exhausted
request IDs close the connection rather than guessing which caller to wake.
The current server serializes sends and permits one in-flight request.

Live-reload state is not a response to one changes request. It is an
asynchronous message with a monotonically increasing generation. The server
coalesces state callbacks and invokes the listener outside its connection
lock, so slow or failing listener code cannot block response correlation.

## Change batch

`BuildOutputChanges` contains:

- a monotonic build sequence;
- build status and failure category;
- main class/resource and test class/resource path changes;
- optional failure summary and diagnostics path;
- user-initiated and force-restart flags; and
- delivery kind: `DELTA` or `REBASELINE`.

Each path change carries an output root, changed path, and
`ADDED`/`MODIFIED`/`DELETED` kind. A rebaseline is a successful batch without
individual path changes; it tells the consumer to converge by rescanning the
complete declared output trees.

Build failures, cancellations, and superseded builds are statuses rather than
synthetic file changes. Runtime-JAR or other structural changes use the
restart path.

## Apply statuses

| Status | Meaning for the producer |
| --- | --- |
| `APPLIED` | Quarkus accepted the batch into current runtime state. |
| `NOT_APPLIED` | The batch was valid but did not update current state; retain policy state as appropriate. |
| `LIVE_RELOAD_DISABLED` | Preserve/coalesce successful changes for replay when re-enabled. |
| `REJECTED` | Drop the rejected delivery; a later convergence may require rebaseline. |

Transport failure is distinct from every apply status. It fails the current
generation so its lifecycle owner can clean up or recover it.

## Sequence and coalescing policy

`BuildOutputChangesPolicy` accepts only sequences newer than the last accepted
sequence. It coalesces path changes by output category, root, and path:

- add followed by delete cancels;
- delete followed by add becomes modify;
- later modification preserves an earlier add; and
- failures replace pending success while successful recovery clears failure
  only after delivery.

The policy keeps pending successful changes while live reload is disabled.
When their serialized payload would exceed the configured delta budget, it
replaces the delta with a rebaseline. The hard transport limit remains fixed;
an internal lower-only system property exists for diagnostics and tests and
is not user configuration.

A startup baseline establishes sequence ownership but is intentionally not
hot-reloaded. A restart-required outcome also advances the sequence.

## Replay state transition

Live-reload notifications and apply results are read by the transport's
receiver thread. State callbacks reach the build-tool integration
asynchronously. When live reload becomes enabled after a disabled result:

1. pending changes remain owned by the policy;
2. the integration schedules another build iteration;
3. that iteration asks the policy to deliver again; and
4. the policy sends the coalesced delta or a rebaseline.

The asynchronous notification prevents users from having to make an unrelated
file edit. Generation monotonicity prevents an older disable notification
from pausing a newer enabled state. The Gradle trigger used to schedule that
iteration belongs to
[development and remote lifecycle](development-and-remote-lifecycle.md#live-reload-disable-and-replay).

## Shutdown and failure behavior

Closing the server completes any pending request exceptionally, closes
authenticating and connected sockets, wakes callback waiters, and joins the
accept, receiver, and callback threads with bounded timeouts. Expected close
completes `termination()` normally; unexpected transport termination
completes it exceptionally.

Callbacks run outside connection locks. Request completion is also performed
after removing the pending request under the lock, preventing callbacks from
re-entering locked transport state.

## Platform and environment qualifications

Only loopback TCP is currently supported. Frame size counts encoded UTF-8
bytes, not Java characters or changed files. Paths in DTOs use the local
process filesystem; this protocol is not a remote package-transfer format.

## Source and test ownership

Primary source owners:

- `io.quarkus.deployment.dev.BuildOutputChanges`
- `BuildOutputChangesPolicy`
- `BuildOutputChangesTransports`
- `BuildOutputChangesTcpServer` and `BuildOutputChangesTcpClient`
- frame, protocol, and JSON codecs in `core/deployment`
- `RuntimeUpdatesProcessor` as runtime consumer

Primary test owners:

- `BuildOutputChangesPolicyTest`
- protocol/frame/JSON codec tests
- TCP server/client lifecycle and replay tests
- Gradle dev session and continuous-build replay tests
