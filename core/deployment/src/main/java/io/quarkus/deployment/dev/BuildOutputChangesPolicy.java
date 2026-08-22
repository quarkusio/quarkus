package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Build-tool-agnostic coalescing policy for externally produced Quarkus dev
 * build-output changes.
 * <p>
 * Quarkus build-tool integrations use this mutable, single-session state
 * machine to reject stale sequences, coalesce pending deltas, retain updates
 * while live reload is disabled, and request a compact rebaseline when a
 * delta would exceed the configured delta budget. That budget defaults to and
 * never exceeds the transport frame maximum. Callers must serialize access;
 * this type performs no internal synchronization and makes no cross-version
 * wire compatibility promise.
 */
public final class BuildOutputChangesPolicy {

    // Internal lower-only diagnostic/test override. The hard transport frame remains fixed.
    static final String DELTA_MAX_BYTES_PROPERTY = "io.quarkus.deployment.dev.build-output-delta-max-bytes";

    private final int deltaMaxBytes;
    private long lastAcceptedSequence = Long.MIN_VALUE;
    private PendingChanges pending;
    private boolean recoverySuccessRequired;
    private boolean rebaselineRequired;
    private boolean deferredReplayRequired;

    /**
     * Creates an empty session policy using the configured delta budget, which
     * defaults to the transport frame maximum.
     */
    public BuildOutputChangesPolicy() {
        this(configuredDeltaMaxBytes(System.getProperty(DELTA_MAX_BYTES_PROPERTY)));
    }

    BuildOutputChangesPolicy(int deltaMaxBytes) {
        if (deltaMaxBytes < 1 || deltaMaxBytes > BuildOutputChangesFrameCodec.MAX_FRAME_BYTES) {
            throw new IllegalArgumentException("External build-output delta maximum must be between 1 and "
                    + BuildOutputChangesFrameCodec.MAX_FRAME_BYTES + " bytes");
        }
        this.deltaMaxBytes = deltaMaxBytes;
    }

    /**
     * Accepts a non-stale startup candidate as the current baseline without
     * scheduling live reload. The caller is responsible for supplying the
     * successful output state that started the session; this method validates
     * sequence ordering, not build status or transition position.
     *
     * @param candidate startup build result
     * @return the acceptance outcome
     */
    public Result acceptStartupBaseline(BuildOutputChanges candidate) {
        requireNonNull(candidate, "candidate");
        if (isStale(candidate.sequence())) {
            return Result.stale(candidate.sequence());
        }
        lastAcceptedSequence = candidate.sequence();
        return Result.baselineDropped(candidate.sequence());
    }

    /**
     * Records that the current output state requires a full process restart.
     *
     * @param sequence build sequence
     * @return the acceptance outcome
     */
    public Result acceptRestartRequired(long sequence) {
        if (isStale(sequence)) {
            return Result.stale(sequence);
        }
        lastAcceptedSequence = sequence;
        return Result.restartRequired(sequence);
    }

    /**
     * Accepts a ready-session update, rejecting stale sequences and coalescing
     * it with any pending update.
     *
     * @param candidate update to accept
     * @return the acceptance outcome
     */
    public Result accept(BuildOutputChanges candidate) {
        requireNonNull(candidate, "candidate");
        if (isStale(candidate.sequence())) {
            return Result.stale(candidate.sequence());
        }
        lastAcceptedSequence = candidate.sequence();
        if (candidate.status() != BuildOutputChangeStatus.BUILD_SUCCEEDED) {
            if (deferredReplayRequired && pending != null
                    && pending.status == BuildOutputChangeStatus.BUILD_SUCCEEDED) {
                rebaselineRequired = true;
            }
            pending = new PendingChanges(candidate);
            return Result.nonReloadableStatus(candidate);
        }
        boolean replacesPendingFailure = pending != null && pending.status == BuildOutputChangeStatus.BUILD_FAILED;
        boolean recoversAppliedFailure = pending == null && recoverySuccessRequired;
        boolean replacesFailure = replacesPendingFailure || recoversAppliedFailure;
        boolean requiresRebaseline = rebaselineRequired;
        if (pending != null && pending.status != BuildOutputChangeStatus.BUILD_SUCCEEDED) {
            pending = null;
        }
        if (candidate.deliveryKind() == BuildOutputChangesDeliveryKind.DELTA
                && !replacesFailure && !requiresRebaseline
                && candidate.mainClassChanges().isEmpty() && candidate.mainResourceChanges().isEmpty()
                && candidate.testClassChanges().isEmpty() && candidate.testResourceChanges().isEmpty()) {
            return Result.noReloadableChanges(candidate.sequence());
        }
        PendingChanges next = pending == null || replacesFailure ? new PendingChanges(candidate)
                : pending.withSequence(candidate);
        if (next.deliveryKind == BuildOutputChangesDeliveryKind.DELTA) {
            coalesce(next.mainClassChanges, OutputCategory.MAIN_CLASSES, candidate.mainClassChanges());
            coalesce(next.mainResourceChanges, OutputCategory.MAIN_RESOURCES, candidate.mainResourceChanges());
            coalesce(next.testClassChanges, OutputCategory.TEST_CLASSES, candidate.testClassChanges());
            coalesce(next.testResourceChanges, OutputCategory.TEST_RESOURCES, candidate.testResourceChanges());
        }
        if (requiresRebaseline) {
            next = next.toRebaseline();
        }
        if (!next.hasOutputChanges() && !replacesFailure) {
            pending = null;
            return Result.noReloadableChanges(candidate.sequence());
        }
        pending = rebaselineIfOversized(next);
        return Result.pending(candidate.sequence());
    }

    /**
     * Attempts to deliver the current coalesced update.
     * <p>
     * An applied or rejected update clears pending state. A disabled,
     * not-applied, or failed delivery remains pending so the session owner can
     * retry it or converge with a later rebaseline.
     *
     * @param sender receiver callback
     * @return the delivery outcome
     */
    public Result deliver(Sender sender) {
        requireNonNull(sender, "sender");
        if (pending == null) {
            return Result.nothingToSend(lastAcceptedSequence);
        }
        BuildOutputChanges emitted = pending.toBuildOutputChanges();
        try {
            BuildOutputChangesApplyStatus status = requireNonNull(sender.send(emitted), "sender result");
            if (status == BuildOutputChangesApplyStatus.APPLIED) {
                if (emitted.status() == BuildOutputChangeStatus.BUILD_SUCCEEDED) {
                    rebaselineRequired = false;
                    deferredReplayRequired = false;
                }
                if (emitted.status() == BuildOutputChangeStatus.BUILD_FAILED) {
                    recoverySuccessRequired = true;
                } else if (emitted.status() == BuildOutputChangeStatus.BUILD_SUCCEEDED) {
                    recoverySuccessRequired = false;
                }
                pending = null;
                return Result.sentApplied(emitted);
            }
            if (status == BuildOutputChangesApplyStatus.LIVE_RELOAD_DISABLED) {
                deferredReplayRequired = true;
                return Result.sentLiveReloadDisabled(emitted);
            }
            if (status == BuildOutputChangesApplyStatus.REJECTED) {
                if (deferredReplayRequired && emitted.status() == BuildOutputChangeStatus.BUILD_SUCCEEDED) {
                    rebaselineRequired = true;
                }
                pending = null;
                return Result.sentRejected(emitted);
            }
            return Result.sentNotApplied(emitted);
        } catch (IOException e) {
            return Result.sendFailed(emitted, e);
        }
    }

    /**
     * Discards any pending update.
     *
     * @param reason diagnostic reason retained in the result
     * @return the discarded update, or {@link Outcome#NOTHING_TO_SEND}
     */
    public Result discardPending(String reason) {
        requireNonNull(reason, "reason");
        if (pending == null) {
            return Result.nothingToSend(lastAcceptedSequence);
        }
        BuildOutputChanges discarded = pending.toBuildOutputChanges();
        pending = null;
        return Result.discarded(discarded, reason);
    }

    /**
     * Returns whether an accepted update is waiting for delivery or replay.
     *
     * @return {@code true} when pending state exists
     */
    public boolean hasPendingChanges() {
        return pending != null;
    }

    static int configuredDeltaMaxBytes(String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return BuildOutputChangesFrameCodec.MAX_FRAME_BYTES;
        }
        try {
            int configured = Integer.parseInt(configuredValue);
            if (configured < 1 || configured > BuildOutputChangesFrameCodec.MAX_FRAME_BYTES) {
                throw new IllegalArgumentException("System property '" + DELTA_MAX_BYTES_PROPERTY
                        + "' must be between 1 and " + BuildOutputChangesFrameCodec.MAX_FRAME_BYTES);
            }
            return configured;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "System property '" + DELTA_MAX_BYTES_PROPERTY + "' must be an integer number of bytes", e);
        }
    }

    private PendingChanges rebaselineIfOversized(PendingChanges changes) {
        if (changes.deliveryKind == BuildOutputChangesDeliveryKind.REBASELINE) {
            rebaselineRequired = true;
            return changes;
        }
        if (!changes.hasPathChanges()) {
            return changes;
        }
        int encodedBytes = BuildOutputChangesProtocol.completeChangesPayloadBytes(changes.toBuildOutputChanges());
        if (encodedBytes <= deltaMaxBytes) {
            return changes;
        }
        rebaselineRequired = true;
        return changes.toRebaseline();
    }

    private boolean isStale(long sequence) {
        return sequence <= lastAcceptedSequence;
    }

    private static void coalesce(Map<ChangeKey, BuildOutputChangeKind> target, OutputCategory category,
            List<BuildOutputPathChange> changes) {
        for (BuildOutputPathChange change : changes) {
            var key = new ChangeKey(category, change.outputRoot(), change.changedPath());
            BuildOutputChangeKind merged = merge(target.get(key), change.kind());
            if (merged == null) {
                target.remove(key);
            } else {
                target.put(key, merged);
            }
        }
    }

    private static BuildOutputChangeKind merge(BuildOutputChangeKind previous, BuildOutputChangeKind next) {
        if (previous == null) {
            return next;
        }
        return switch (previous) {
            case ADDED -> switch (next) {
                case ADDED, MODIFIED -> BuildOutputChangeKind.ADDED;
                case DELETED -> null;
            };
            case MODIFIED -> switch (next) {
                case ADDED -> BuildOutputChangeKind.ADDED;
                case MODIFIED -> BuildOutputChangeKind.MODIFIED;
                case DELETED -> BuildOutputChangeKind.DELETED;
            };
            case DELETED -> switch (next) {
                case ADDED, MODIFIED -> BuildOutputChangeKind.MODIFIED;
                case DELETED -> BuildOutputChangeKind.DELETED;
            };
        };
    }

    /**
     * Delivers one coalesced update to the current receiver.
     */
    @FunctionalInterface
    public interface Sender {
        /**
         * Sends an update and reports how the receiver handled it.
         *
         * @param changes update to send
         * @return receiver status
         * @throws IOException when transport delivery fails
         */
        BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException;
    }

    /**
     * Policy transition outcomes.
     */
    public enum Outcome {
        /**
         * Startup output was recorded without scheduling live reload.
         */
        BASELINE_DROPPED,
        /**
         * The sequence was not newer than the last accepted sequence.
         */
        STALE_REJECTED,
        /**
         * A failure, cancellation, or supersession is pending.
         */
        NON_RELOADABLE_STATUS,
        /**
         * A successful update contained no effective reloadable changes.
         */
        NO_RELOADABLE_CHANGES,
        /**
         * The producer reported that the application process must restart.
         */
        RESTART_REQUIRED,
        /**
         * An update is pending delivery.
         */
        PENDING,
        /**
         * No update was available to deliver or discard.
         */
        NOTHING_TO_SEND,
        /**
         * The receiver applied the pending update.
         */
        SENT_APPLIED,
        /**
         * The receiver deferred the update because live reload is disabled.
         */
        SENT_LIVE_RELOAD_DISABLED,
        /**
         * The receiver rejected and caused the policy to discard the update.
         */
        SENT_REJECTED,
        /**
         * The receiver did not apply the update, which remains pending.
         */
        SENT_NOT_APPLIED,
        /**
         * Transport delivery failed and the update remains pending.
         */
        SEND_FAILED,
        /**
         * The session owner explicitly discarded the pending update.
         */
        DISCARDED
    }

    /**
     * Immutable result of one policy transition.
     *
     * @param outcome transition outcome
     * @param sequence associated build sequence
     * @param changes associated update, when the outcome concerns one
     * @param failure transport failure for {@link Outcome#SEND_FAILED}
     * @param message optional diagnostic message, currently used by
     *        {@link Outcome#DISCARDED}
     */
    public record Result(
            Outcome outcome,
            long sequence,
            BuildOutputChanges changes,
            IOException failure,
            String message) {

        private static Result baselineDropped(long sequence) {
            return new Result(Outcome.BASELINE_DROPPED, sequence, null, null, null);
        }

        private static Result stale(long sequence) {
            return new Result(Outcome.STALE_REJECTED, sequence, null, null, null);
        }

        private static Result nonReloadableStatus(BuildOutputChanges changes) {
            return new Result(Outcome.NON_RELOADABLE_STATUS, changes.sequence(), changes, null, null);
        }

        private static Result noReloadableChanges(long sequence) {
            return new Result(Outcome.NO_RELOADABLE_CHANGES, sequence, null, null, null);
        }

        private static Result restartRequired(long sequence) {
            return new Result(Outcome.RESTART_REQUIRED, sequence, null, null, null);
        }

        private static Result pending(long sequence) {
            return new Result(Outcome.PENDING, sequence, null, null, null);
        }

        private static Result nothingToSend(long sequence) {
            return new Result(Outcome.NOTHING_TO_SEND, sequence, null, null, null);
        }

        private static Result sentApplied(BuildOutputChanges changes) {
            return new Result(Outcome.SENT_APPLIED, changes.sequence(), changes, null, null);
        }

        private static Result sentNotApplied(BuildOutputChanges changes) {
            return new Result(Outcome.SENT_NOT_APPLIED, changes.sequence(), changes, null, null);
        }

        private static Result sentLiveReloadDisabled(BuildOutputChanges changes) {
            return new Result(Outcome.SENT_LIVE_RELOAD_DISABLED, changes.sequence(), changes, null, null);
        }

        private static Result sentRejected(BuildOutputChanges changes) {
            return new Result(Outcome.SENT_REJECTED, changes.sequence(), changes, null, null);
        }

        private static Result sendFailed(BuildOutputChanges changes, IOException failure) {
            return new Result(Outcome.SEND_FAILED, changes.sequence(), changes, failure, null);
        }

        private static Result discarded(BuildOutputChanges changes, String reason) {
            return new Result(Outcome.DISCARDED, changes.sequence(), changes, null, reason);
        }
    }

    private enum OutputCategory {
        MAIN_CLASSES,
        MAIN_RESOURCES,
        TEST_CLASSES,
        TEST_RESOURCES
    }

    private record ChangeKey(OutputCategory category, Path outputRoot, Path changedPath) {
    }

    private record PendingChanges(
            long sequence,
            BuildOutputChangeStatus status,
            BuildOutputFailureKind failureKind,
            String failureSummary,
            Path diagnosticsPath,
            boolean userInitiated,
            boolean forceRestart,
            BuildOutputChangesDeliveryKind deliveryKind,
            Map<ChangeKey, BuildOutputChangeKind> mainClassChanges,
            Map<ChangeKey, BuildOutputChangeKind> mainResourceChanges,
            Map<ChangeKey, BuildOutputChangeKind> testClassChanges,
            Map<ChangeKey, BuildOutputChangeKind> testResourceChanges) {

        private PendingChanges(BuildOutputChanges candidate) {
            this(candidate.sequence(), candidate.status(), candidate.failureKind(), candidate.failureSummary(),
                    candidate.diagnosticsPath(), candidate.userInitiated(), candidate.forceRestart(),
                    candidate.deliveryKind(), new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>());
        }

        private PendingChanges withSequence(BuildOutputChanges candidate) {
            return new PendingChanges(candidate.sequence(), candidate.status(), candidate.failureKind(),
                    candidate.failureSummary(), candidate.diagnosticsPath(), userInitiated || candidate.userInitiated(),
                    forceRestart || candidate.forceRestart(), deliveryKind, mainClassChanges, mainResourceChanges,
                    testClassChanges, testResourceChanges);
        }

        private boolean hasOutputChanges() {
            return deliveryKind == BuildOutputChangesDeliveryKind.REBASELINE || hasPathChanges();
        }

        private boolean hasPathChanges() {
            return !mainClassChanges.isEmpty() || !mainResourceChanges.isEmpty()
                    || !testClassChanges.isEmpty() || !testResourceChanges.isEmpty();
        }

        private PendingChanges toRebaseline() {
            return new PendingChanges(sequence, status, failureKind, null, null, userInitiated, true,
                    BuildOutputChangesDeliveryKind.REBASELINE, new LinkedHashMap<>(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        private BuildOutputChanges toBuildOutputChanges() {
            return new BuildOutputChanges(sequence, status, failureKind,
                    toPathChanges(mainClassChanges), toPathChanges(mainResourceChanges),
                    toPathChanges(testClassChanges), toPathChanges(testResourceChanges),
                    failureSummary, diagnosticsPath, userInitiated, forceRestart, deliveryKind);
        }

        private static List<BuildOutputPathChange> toPathChanges(Map<ChangeKey, BuildOutputChangeKind> changes) {
            var pathChanges = new ArrayList<BuildOutputPathChange>(changes.size());
            for (Map.Entry<ChangeKey, BuildOutputChangeKind> entry : changes.entrySet()) {
                ChangeKey key = entry.getKey();
                pathChanges.add(new BuildOutputPathChange(key.outputRoot(), key.changedPath(), entry.getValue()));
            }
            return pathChanges;
        }
    }
}
