package io.quarkus.deployment.dev;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Immutable result of one externally owned build iteration.
 * <p>
 * Successful delta updates carry categorized path changes relative to the
 * preceding baseline. A successful {@link BuildOutputChangesDeliveryKind#REBASELINE}
 * update carries no path changes and asks the receiver to converge from the
 * complete current output trees. Failure, cancellation, and supersession do
 * not advance the accepted successful baseline; they do not imply that a
 * failed producer left the physical output trees untouched.
 *
 * @param sequence monotonically increasing sequence within the producing
 *        build session
 * @param status build-iteration outcome
 * @param failureKind output category that failed, or
 *        {@link BuildOutputFailureKind#NONE}
 * @param mainClassChanges changes below main class-output roots
 * @param mainResourceChanges changes below main resource-output roots
 * @param testClassChanges changes below test class-output roots
 * @param testResourceChanges changes below test resource-output roots
 * @param failureSummary optional user-facing build failure summary
 * @param diagnosticsPath optional producer-local path to fuller diagnostics
 * @param userInitiated whether a user explicitly requested the iteration
 * @param forceRestart whether the update requires a dev-mode process restart
 *        instead of live reload
 * @param deliveryKind delta or complete-output rebaseline semantics
 */
public record BuildOutputChanges(
        long sequence,
        BuildOutputChangeStatus status,
        BuildOutputFailureKind failureKind,
        List<BuildOutputPathChange> mainClassChanges,
        List<BuildOutputPathChange> mainResourceChanges,
        List<BuildOutputPathChange> testClassChanges,
        List<BuildOutputPathChange> testResourceChanges,
        String failureSummary,
        Path diagnosticsPath,
        boolean userInitiated,
        boolean forceRestart,
        BuildOutputChangesDeliveryKind deliveryKind) {

    /**
     * Creates an immutable, normalized update and validates rebaseline shape.
     */
    public BuildOutputChanges {
        requireNonNull(status, "status");
        failureKind = failureKind == null ? BuildOutputFailureKind.NONE : failureKind;
        mainClassChanges = copyOrEmpty(mainClassChanges);
        mainResourceChanges = copyOrEmpty(mainResourceChanges);
        testClassChanges = copyOrEmpty(testClassChanges);
        testResourceChanges = copyOrEmpty(testResourceChanges);
        deliveryKind = deliveryKind == null ? BuildOutputChangesDeliveryKind.DELTA : deliveryKind;
        if (deliveryKind == BuildOutputChangesDeliveryKind.REBASELINE) {
            if (status != BuildOutputChangeStatus.BUILD_SUCCEEDED) {
                throw new IllegalArgumentException("Only a successful build can request output rebaseline");
            }
            if (!mainClassChanges.isEmpty() || !mainResourceChanges.isEmpty()
                    || !testClassChanges.isEmpty() || !testResourceChanges.isEmpty()) {
                throw new IllegalArgumentException("An output rebaseline must not contain path changes");
            }
        }
    }

    /**
     * Creates a delta update with an explicit failure category.
     */
    public BuildOutputChanges(long sequence, BuildOutputChangeStatus status, BuildOutputFailureKind failureKind,
            List<BuildOutputPathChange> mainClassChanges, List<BuildOutputPathChange> mainResourceChanges,
            List<BuildOutputPathChange> testClassChanges, List<BuildOutputPathChange> testResourceChanges,
            String failureSummary, Path diagnosticsPath, boolean userInitiated, boolean forceRestart) {
        this(sequence, status, failureKind, mainClassChanges, mainResourceChanges, testClassChanges, testResourceChanges,
                failureSummary, diagnosticsPath, userInitiated, forceRestart, BuildOutputChangesDeliveryKind.DELTA);
    }

    /**
     * Creates a delta update whose failure category defaults to
     * {@link BuildOutputFailureKind#NONE}.
     */
    public BuildOutputChanges(long sequence, BuildOutputChangeStatus status,
            List<BuildOutputPathChange> mainClassChanges, List<BuildOutputPathChange> mainResourceChanges,
            List<BuildOutputPathChange> testClassChanges, List<BuildOutputPathChange> testResourceChanges,
            String failureSummary, Path diagnosticsPath, boolean userInitiated, boolean forceRestart) {
        this(sequence, status, BuildOutputFailureKind.NONE, mainClassChanges, mainResourceChanges,
                testClassChanges, testResourceChanges, failureSummary, diagnosticsPath, userInitiated, forceRestart,
                BuildOutputChangesDeliveryKind.DELTA);
    }

    private static <T> List<T> copyOrEmpty(List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
