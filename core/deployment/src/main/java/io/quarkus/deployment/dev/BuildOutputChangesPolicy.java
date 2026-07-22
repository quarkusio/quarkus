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
 * build-output changes. Its public surface stays free of Gradle and Quarkus
 * Gradle plugin types so external build tools can share the same delivery
 * semantics.
 */
public final class BuildOutputChangesPolicy {

    private long lastAcceptedSequence = Long.MIN_VALUE;
    private PendingChanges pending;

    public Result acceptStartupBaseline(BuildOutputChanges candidate) {
        requireNonNull(candidate, "candidate");
        if (isStale(candidate.sequence())) {
            return Result.stale(candidate.sequence());
        }
        lastAcceptedSequence = candidate.sequence();
        return Result.baselineDropped(candidate.sequence());
    }

    public Result acceptRestartRequired(long sequence) {
        if (isStale(sequence)) {
            return Result.stale(sequence);
        }
        lastAcceptedSequence = sequence;
        return Result.restartRequired(sequence);
    }

    public Result accept(BuildOutputChanges candidate) {
        requireNonNull(candidate, "candidate");
        if (isStale(candidate.sequence())) {
            return Result.stale(candidate.sequence());
        }
        lastAcceptedSequence = candidate.sequence();
        if (candidate.status() != BuildOutputChangeStatus.BUILD_SUCCEEDED) {
            pending = new PendingChanges(candidate);
            return Result.nonReloadableStatus(candidate);
        }
        boolean replacesFailure = pending != null && pending.status != BuildOutputChangeStatus.BUILD_SUCCEEDED;
        if (!replacesFailure && candidate.mainClassChanges().isEmpty() && candidate.mainResourceChanges().isEmpty()
                && candidate.testClassChanges().isEmpty() && candidate.testResourceChanges().isEmpty()) {
            return Result.noReloadableChanges(candidate.sequence());
        }
        PendingChanges next = pending == null || replacesFailure ? new PendingChanges(candidate)
                : pending.withSequence(candidate);
        coalesce(next.mainClassChanges, OutputCategory.MAIN_CLASSES, candidate.mainClassChanges());
        coalesce(next.mainResourceChanges, OutputCategory.MAIN_RESOURCES, candidate.mainResourceChanges());
        coalesce(next.testClassChanges, OutputCategory.TEST_CLASSES, candidate.testClassChanges());
        coalesce(next.testResourceChanges, OutputCategory.TEST_RESOURCES, candidate.testResourceChanges());
        if (!next.hasOutputChanges() && !replacesFailure) {
            pending = null;
            return Result.noReloadableChanges(candidate.sequence());
        }
        pending = next;
        return Result.pending(candidate.sequence());
    }

    public Result deliver(Sender sender) {
        requireNonNull(sender, "sender");
        if (pending == null) {
            return Result.nothingToSend(lastAcceptedSequence);
        }
        BuildOutputChanges emitted = pending.toBuildOutputChanges();
        try {
            BuildOutputChangesApplyStatus status = requireNonNull(sender.send(emitted), "sender result");
            if (status == BuildOutputChangesApplyStatus.APPLIED) {
                pending = null;
                return Result.sentApplied(emitted);
            }
            if (status == BuildOutputChangesApplyStatus.REJECTED) {
                pending = null;
                return Result.sentRejected(emitted);
            }
            return Result.sentNotApplied(emitted);
        } catch (IOException e) {
            return Result.sendFailed(emitted, e);
        }
    }

    public Result discardPending(String reason) {
        requireNonNull(reason, "reason");
        if (pending == null) {
            return Result.nothingToSend(lastAcceptedSequence);
        }
        BuildOutputChanges discarded = pending.toBuildOutputChanges();
        pending = null;
        return Result.discarded(discarded, reason);
    }

    public boolean hasPendingChanges() {
        return pending != null;
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

    @FunctionalInterface
    public interface Sender {
        BuildOutputChangesApplyStatus send(BuildOutputChanges changes) throws IOException;
    }

    public enum Outcome {
        BASELINE_DROPPED,
        STALE_REJECTED,
        NON_RELOADABLE_STATUS,
        NO_RELOADABLE_CHANGES,
        RESTART_REQUIRED,
        PENDING,
        NOTHING_TO_SEND,
        SENT_APPLIED,
        SENT_REJECTED,
        SENT_NOT_APPLIED,
        SEND_FAILED,
        DISCARDED
    }

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
            Map<ChangeKey, BuildOutputChangeKind> mainClassChanges,
            Map<ChangeKey, BuildOutputChangeKind> mainResourceChanges,
            Map<ChangeKey, BuildOutputChangeKind> testClassChanges,
            Map<ChangeKey, BuildOutputChangeKind> testResourceChanges) {

        private PendingChanges(BuildOutputChanges candidate) {
            this(candidate.sequence(), candidate.status(), candidate.failureKind(), candidate.failureSummary(),
                    candidate.diagnosticsPath(), candidate.userInitiated(), candidate.forceRestart(), new LinkedHashMap<>(),
                    new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>());
        }

        private PendingChanges withSequence(BuildOutputChanges candidate) {
            return new PendingChanges(candidate.sequence(), candidate.status(), candidate.failureKind(),
                    candidate.failureSummary(), candidate.diagnosticsPath(), userInitiated || candidate.userInitiated(),
                    forceRestart || candidate.forceRestart(), mainClassChanges, mainResourceChanges,
                    testClassChanges, testResourceChanges);
        }

        private boolean hasOutputChanges() {
            return !mainClassChanges.isEmpty() || !mainResourceChanges.isEmpty()
                    || !testClassChanges.isEmpty() || !testResourceChanges.isEmpty();
        }

        private BuildOutputChanges toBuildOutputChanges() {
            return new BuildOutputChanges(sequence, status, failureKind,
                    toPathChanges(mainClassChanges), toPathChanges(mainResourceChanges),
                    toPathChanges(testClassChanges), toPathChanges(testResourceChanges),
                    failureSummary, diagnosticsPath, userInitiated, forceRestart);
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
