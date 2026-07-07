package io.quarkus.deployment.dev;

import static io.quarkus.deployment.dev.BuildOutputChangeKind.ADDED;
import static io.quarkus.deployment.dev.BuildOutputChangeKind.DELETED;
import static io.quarkus.deployment.dev.BuildOutputChangeKind.MODIFIED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_CANCELLED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_FAILED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_SUCCEEDED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_SUPERSEDED;
import static io.quarkus.deployment.dev.BuildOutputChangesApplyStatus.APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesApplyStatus.LIVE_RELOAD_DISABLED;
import static io.quarkus.deployment.dev.BuildOutputChangesApplyStatus.NOT_APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesApplyStatus.REJECTED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.BASELINE_DROPPED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.DISCARDED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.NON_RELOADABLE_STATUS;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.NOTHING_TO_SEND;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.NO_RELOADABLE_CHANGES;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.PENDING;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.RESTART_REQUIRED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SEND_FAILED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_LIVE_RELOAD_DISABLED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_NOT_APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_REJECTED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.STALE_REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class BuildOutputChangesPolicyTest {

    private final Path classes = Path.of("build/classes/java/main");
    private final Path resources = Path.of("build/resources/main");
    private final Path otherClasses = Path.of("other/classes");

    @Test
    void coalescesRepeatedModifiedChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));
        policy.accept(success(2, classChange("com/acme/Foo.class", MODIFIED)));

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(MODIFIED);
    }

    @Test
    void coalescesAddedModifiedAndAddedDeletedChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Added.class", ADDED)));
        policy.accept(success(2, classChange("com/acme/Added.class", MODIFIED)));
        policy.accept(success(3, resourceChange("temporary.txt", ADDED)));
        policy.accept(success(4, resourceChange("temporary.txt", DELETED)));

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(ADDED);
        assertThat(delivered.mainResourceChanges()).isEmpty();
    }

    @Test
    void coalescesDeleteAddModifyAsModified() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", DELETED)));
        policy.accept(success(2, classChange("com/acme/Foo.class", ADDED)));
        policy.accept(success(3, classChange("com/acme/Foo.class", MODIFIED)));

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(MODIFIED);
    }

    @Test
    void coalescesModifiedDeletedAndDeletedAddedChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Removed.class", MODIFIED)));
        policy.accept(success(2, classChange("com/acme/Removed.class", DELETED)));
        policy.accept(success(3, resourceChange("recreated.txt", DELETED)));
        policy.accept(success(4, resourceChange("recreated.txt", ADDED)));

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(DELETED);
        assertThat(delivered.mainResourceChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(MODIFIED);
    }

    @Test
    void coalescingKeysIncludeCategoryRootAndPath() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1,
                classChange(classes, "same/path.txt", MODIFIED),
                resourceChange(resources, "same/path.txt", MODIFIED),
                classChange(otherClasses, "same/path.txt", MODIFIED)));

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.mainClassChanges())
                .extracting(BuildOutputPathChange::outputRoot)
                .containsExactly(classes, otherClasses);
        assertThat(delivered.mainResourceChanges())
                .extracting(BuildOutputPathChange::outputRoot)
                .containsExactly(resources);
    }

    @Test
    void rejectsStaleSequences() {
        var policy = new BuildOutputChangesPolicy();

        assertThat(policy.accept(success(2, classChange("com/acme/Foo.class", MODIFIED))).outcome()).isEqualTo(PENDING);
        assertThat(policy.accept(success(2, classChange("com/acme/Bar.class", MODIFIED))).outcome()).isEqualTo(STALE_REJECTED);
        assertThat(policy.accept(success(1, classChange("com/acme/Baz.class", MODIFIED))).outcome()).isEqualTo(STALE_REJECTED);
    }

    @Test
    void newerBuildStatusReplacesPendingSuccessfulChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        var failed = policy.accept(changes(2, BUILD_FAILED, List.of(classChange("com/acme/Bad.class", MODIFIED)), List.of()));
        assertThat(failed.outcome()).isEqualTo(NON_RELOADABLE_STATUS);
        assertThat(failed.changes().status()).isEqualTo(BUILD_FAILED);
        assertThat(policy.accept(changes(3, BUILD_CANCELLED, List.of(classChange("com/acme/Other.class", MODIFIED)),
                List.of())).outcome())
                .isEqualTo(NON_RELOADABLE_STATUS);
        assertThat(policy.accept(changes(4, BUILD_SUPERSEDED, List.of(classChange("com/acme/Superseded.class", MODIFIED)),
                List.of())).outcome())
                .isEqualTo(NON_RELOADABLE_STATUS);

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.sequence()).isEqualTo(4);
        assertThat(delivered.status()).isEqualTo(BUILD_SUPERSEDED);
        assertThat(delivered.mainClassChanges()).isEmpty();
    }

    @Test
    void appliedDeliveryClearsPendingChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        assertThat(policy.deliver(ignored -> APPLIED).outcome()).isEqualTo(SENT_APPLIED);

        assertThat(policy.hasPendingChanges()).isFalse();
        assertThat(policy.deliver(ignored -> APPLIED).outcome()).isEqualTo(NOTHING_TO_SEND);
    }

    @Test
    void explicitPathlessRebaselineIsDelivered() {
        var policy = new BuildOutputChangesPolicy();
        var rebaseline = new BuildOutputChanges(1, BUILD_SUCCEEDED, BuildOutputFailureKind.NONE,
                List.of(), List.of(), List.of(), List.of(), null, null, false, true,
                BuildOutputChangesDeliveryKind.REBASELINE);

        assertThat(policy.accept(rebaseline).outcome()).isEqualTo(PENDING);
        assertThat(deliver(policy, APPLIED)).isEqualTo(rebaseline);
    }

    @Test
    void rejectedDeliveryClearsPendingChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        assertThat(policy.deliver(ignored -> REJECTED).outcome()).isEqualTo(SENT_REJECTED);

        assertThat(policy.hasPendingChanges()).isFalse();
    }

    @Test
    void notAppliedDeliveryKeepsPendingChangesAndCoalescesLaterEvents() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        assertThat(policy.deliver(ignored -> NOT_APPLIED).outcome()).isEqualTo(SENT_NOT_APPLIED);
        assertThat(policy.hasPendingChanges()).isTrue();

        policy.accept(success(2, classChange("com/acme/Foo.class", DELETED)));
        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.sequence()).isEqualTo(2);
        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(DELETED);
    }

    @Test
    void liveReloadDisabledDeliveryKeepsPendingChangesAndCoalescesLaterEvents() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        assertThat(policy.deliver(ignored -> LIVE_RELOAD_DISABLED).outcome())
                .isEqualTo(SENT_LIVE_RELOAD_DISABLED);

        policy.accept(success(2, classChange("com/acme/Foo.class", DELETED)));
        BuildOutputChanges delivered = deliver(policy, APPLIED);
        assertThat(delivered.sequence()).isEqualTo(2);
        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(DELETED);
    }

    @Test
    void failureAfterDisabledDeltaRetainsCompactRecoveryObligation() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));
        assertThat(policy.deliver(ignored -> LIVE_RELOAD_DISABLED).outcome())
                .isEqualTo(SENT_LIVE_RELOAD_DISABLED);

        policy.accept(changes(2, BUILD_FAILED, List.of(), List.of()));
        assertThat(deliver(policy, APPLIED).status()).isEqualTo(BUILD_FAILED);
        assertThat(policy.accept(success(3)).outcome()).isEqualTo(PENDING);

        BuildOutputChanges recovered = deliver(policy, APPLIED);
        assertThat(recovered.deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(recovered.forceRestart()).isTrue();
    }

    @Test
    void cancellationOrSupersessionAfterDisabledDeltaRetainsCompactRecoveryObligation() {
        for (BuildOutputChangeStatus status : List.of(BUILD_CANCELLED, BUILD_SUPERSEDED)) {
            var policy = new BuildOutputChangesPolicy();
            policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));
            assertThat(policy.deliver(ignored -> LIVE_RELOAD_DISABLED).outcome())
                    .isEqualTo(SENT_LIVE_RELOAD_DISABLED);

            policy.accept(changes(2, status, List.of(), List.of()));
            assertThat(deliver(policy, APPLIED).status()).isEqualTo(status);
            assertThat(policy.accept(success(3)).outcome()).isEqualTo(PENDING);

            BuildOutputChanges recovered = deliver(policy, APPLIED);
            assertThat(recovered.deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
            assertThat(recovered.forceRestart()).isTrue();
        }
    }

    @Test
    void rejectedDisabledDeltaRetainsCompactRecoveryObligation() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));
        assertThat(policy.deliver(ignored -> LIVE_RELOAD_DISABLED).outcome())
                .isEqualTo(SENT_LIVE_RELOAD_DISABLED);
        assertThat(policy.deliver(ignored -> REJECTED).outcome()).isEqualTo(SENT_REJECTED);

        assertThat(policy.accept(success(2)).outcome()).isEqualTo(PENDING);
        assertThat(deliver(policy, APPLIED).deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
    }

    @Test
    void sendFailuresKeepPendingChangesAndCoalesceLaterEvents() {
        var policy = new BuildOutputChangesPolicy();
        var failure = new IOException("timed out");
        policy.accept(success(1, classChange("com/acme/Foo.class", ADDED)));

        var failed = policy.deliver(ignored -> {
            throw failure;
        });

        assertThat(failed.outcome()).isEqualTo(SEND_FAILED);
        assertThat(failed.failure()).isSameAs(failure);
        assertThat(policy.hasPendingChanges()).isTrue();

        policy.accept(success(2, classChange("com/acme/Foo.class", MODIFIED)));
        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::kind)
                .containsExactly(ADDED);
    }

    @Test
    void startupBaselineEventsProduceNoReloadableBatch() {
        var policy = new BuildOutputChangesPolicy();

        assertThat(policy.acceptStartupBaseline(success(1, classChange("com/acme/Foo.class", MODIFIED))).outcome())
                .isEqualTo(BASELINE_DROPPED);
        assertThat(policy.deliver(ignored -> APPLIED).outcome()).isEqualTo(NOTHING_TO_SEND);
        assertThat(policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED))).outcome())
                .isEqualTo(STALE_REJECTED);
        assertThat(policy.accept(success(2, classChange("com/acme/Foo.class", MODIFIED))).outcome()).isEqualTo(PENDING);
    }

    @Test
    void restartRequiredSnapshotsDoNotProduceNormalReloadBatchesOrErasePendingChanges() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        assertThat(policy.acceptRestartRequired(2).outcome()).isEqualTo(RESTART_REQUIRED);

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.sequence()).isEqualTo(1);
        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(classes.resolve("com/acme/Foo.class"));
    }

    @Test
    void busyDeliveryKeepsCoalescingUntilDeliveryIsAllowed() {
        var policy = new BuildOutputChangesPolicy();

        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));
        policy.accept(success(2, resourceChange("application.properties", MODIFIED)));
        policy.accept(success(3, classChange("com/acme/Bar.class", MODIFIED)));

        var delivered = deliver(policy, APPLIED);

        assertThat(delivered.sequence()).isEqualTo(3);
        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(classes.resolve("com/acme/Foo.class"), classes.resolve("com/acme/Bar.class"));
        assertThat(delivered.mainResourceChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(resources.resolve("application.properties"));
    }

    @Test
    void discardPendingChangesIsDeterministic() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));

        var discarded = policy.discardPending("session closed");

        assertThat(discarded.outcome()).isEqualTo(DISCARDED);
        assertThat(discarded.message()).isEqualTo("session closed");
        assertThat(discarded.changes().sequence()).isEqualTo(1);
        assertThat(policy.hasPendingChanges()).isFalse();
        assertThat(policy.discardPending("again").outcome()).isEqualTo(NOTHING_TO_SEND);
    }

    @Test
    void testOutputOnlySuccessfulBuildsAreDelivered() {
        var policy = new BuildOutputChangesPolicy();

        var result = policy.accept(new BuildOutputChanges(1, BUILD_SUCCEEDED, null, null,
                List.of(classChange("org/acme/FooTest.class", MODIFIED)), null, null, null, false, false));

        assertThat(result.outcome()).isEqualTo(PENDING);
        BuildOutputChanges delivered = deliver(policy, APPLIED);
        assertThat(delivered.testClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(classes.resolve("org/acme/FooTest.class"));
    }

    @Test
    void successfulEmptyBuildAfterFailureIsDeliveredToClearFailureState() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(changes(1, BUILD_FAILED, List.of(), List.of()));

        assertThat(policy.accept(success(2)).outcome()).isEqualTo(PENDING);

        BuildOutputChanges delivered = deliver(policy, APPLIED);
        assertThat(delivered.status()).isEqualTo(BUILD_SUCCEEDED);
        assertThat(delivered.sequence()).isEqualTo(2);
    }

    @Test
    void successfulEmptyBuildAfterDeliveredFailureIsDeliveredToClearFailureState() {
        var policy = new BuildOutputChangesPolicy();
        policy.accept(changes(1, BUILD_FAILED, List.of(), List.of()));
        assertThat(deliver(policy, APPLIED).status()).isEqualTo(BUILD_FAILED);

        assertThat(policy.accept(success(2)).outcome()).isEqualTo(PENDING);

        BuildOutputChanges delivered = deliver(policy, APPLIED);
        assertThat(delivered.status()).isEqualTo(BUILD_SUCCEEDED);
        assertThat(delivered.sequence()).isEqualTo(2);
        assertThat(policy.accept(success(3)).outcome()).isEqualTo(NO_RELOADABLE_CHANGES);
    }

    @Test
    void cancelledAndSupersededBuildsDoNotRequireARecoverySuccess() {
        for (BuildOutputChangeStatus status : List.of(BUILD_CANCELLED, BUILD_SUPERSEDED)) {
            var policy = new BuildOutputChangesPolicy();
            policy.accept(changes(1, status, List.of(), List.of()));
            deliver(policy, APPLIED);

            assertThat(policy.accept(success(2)).outcome()).isEqualTo(NO_RELOADABLE_CHANGES);
        }
    }

    @Test
    void cancelledAndSupersededBuildsDoNotHideAnEarlierFailureRecovery() {
        for (BuildOutputChangeStatus status : List.of(BUILD_CANCELLED, BUILD_SUPERSEDED)) {
            var policy = new BuildOutputChangesPolicy();
            policy.accept(changes(1, BUILD_FAILED, List.of(), List.of()));
            deliver(policy, APPLIED);
            policy.accept(changes(2, status, List.of(), List.of()));
            deliver(policy, APPLIED);

            assertThat(policy.accept(success(3)).outcome()).isEqualTo(PENDING);
            assertThat(deliver(policy, APPLIED).status()).isEqualTo(BUILD_SUCCEEDED);
            assertThat(policy.accept(success(4)).outcome()).isEqualTo(NO_RELOADABLE_CHANGES);
        }
    }

    @Test
    void pendingCancelledAndSupersededBuildsDoNotRequireARecoverySuccess() {
        for (BuildOutputChangeStatus status : List.of(BUILD_CANCELLED, BUILD_SUPERSEDED)) {
            var policy = new BuildOutputChangesPolicy();
            policy.accept(changes(1, status, List.of(), List.of()));

            assertThat(policy.accept(success(2)).outcome()).isEqualTo(NO_RELOADABLE_CHANGES);
            assertThat(policy.deliver(ignored -> APPLIED).outcome()).isEqualTo(NOTHING_TO_SEND);
        }
    }

    @Test
    void deltaAtConfiguredByteLimitIsPreservedAndLargerDeltaRebaselines() {
        BuildOutputChanges candidate = success(1, classChange("com/acme/Foo.class", MODIFIED));
        int encodedBytes = BuildOutputChangesProtocol.completeChangesPayloadBytes(candidate);
        var exactPolicy = new BuildOutputChangesPolicy(encodedBytes);
        var smallerPolicy = new BuildOutputChangesPolicy(encodedBytes - 1);

        exactPolicy.accept(candidate);
        smallerPolicy.accept(candidate);

        assertThat(deliver(exactPolicy, APPLIED).deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.DELTA);
        BuildOutputChanges rebaseline = deliver(smallerPolicy, APPLIED);
        assertThat(rebaseline.deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(rebaseline.mainClassChanges()).isEmpty();
        assertThat(rebaseline.forceRestart()).isTrue();
    }

    @Test
    void failedRebaselineDeliveryStaysCompactAndAbsorbsLaterChanges() {
        var policy = new BuildOutputChangesPolicy(1);
        policy.accept(success(1, classChange("com/acme/Foo.class", ADDED)));

        var failed = policy.deliver(ignored -> {
            throw new IOException("offline");
        });
        assertThat(failed.outcome()).isEqualTo(SEND_FAILED);
        assertThat(failed.changes().deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);

        policy.accept(success(2, resourceChange("application.properties", MODIFIED)));
        BuildOutputChanges delivered = deliver(policy, APPLIED);

        assertThat(delivered.sequence()).isEqualTo(2);
        assertThat(delivered.deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
        assertThat(delivered.mainClassChanges()).isEmpty();
        assertThat(delivered.mainResourceChanges()).isEmpty();
    }

    @Test
    void cancelledAndSupersededBuildsDoNotEraseRequiredRebaseline() {
        for (BuildOutputChangeStatus status : List.of(BUILD_CANCELLED, BUILD_SUPERSEDED)) {
            var policy = new BuildOutputChangesPolicy(1);
            policy.accept(success(1, classChange("com/acme/Foo.class", MODIFIED)));
            policy.accept(changes(2, status, List.of(), List.of()));

            assertThat(deliver(policy, APPLIED).status()).isEqualTo(status);
            assertThat(policy.accept(success(3)).outcome()).isEqualTo(PENDING);

            BuildOutputChanges recovered = deliver(policy, APPLIED);
            assertThat(recovered.sequence()).isEqualTo(3);
            assertThat(recovered.deliveryKind()).isEqualTo(BuildOutputChangesDeliveryKind.REBASELINE);
            assertThat(policy.accept(success(4)).outcome()).isEqualTo(NO_RELOADABLE_CHANGES);
        }
    }

    @Test
    void internalDeltaLimitIsLowerOnly() {
        assertThat(BuildOutputChangesPolicy.configuredDeltaMaxBytes(null))
                .isEqualTo(BuildOutputChangesFrameCodec.MAX_FRAME_BYTES);
        assertThat(BuildOutputChangesPolicy.configuredDeltaMaxBytes("1")).isEqualTo(1);
        assertThat(BuildOutputChangesPolicy.configuredDeltaMaxBytes(
                Integer.toString(BuildOutputChangesFrameCodec.MAX_FRAME_BYTES)))
                .isEqualTo(BuildOutputChangesFrameCodec.MAX_FRAME_BYTES);
        assertThatThrownBy(() -> BuildOutputChangesPolicy.configuredDeltaMaxBytes("0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1");
        assertThatThrownBy(() -> BuildOutputChangesPolicy.configuredDeltaMaxBytes(
                Integer.toString(BuildOutputChangesFrameCodec.MAX_FRAME_BYTES + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 1");
        assertThatThrownBy(() -> BuildOutputChangesPolicy.configuredDeltaMaxBytes("not-a-number"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integer");
    }

    @Test
    void policySurfaceDoesNotExposeGradleTypes() {
        assertNoGradleType(BuildOutputChangesPolicy.class);
        assertNoGradleType(BuildOutputChangesPolicy.Result.class);
        assertNoGradleType(BuildOutputChangesPolicy.Sender.class);
    }

    private BuildOutputChanges deliver(BuildOutputChangesPolicy policy, BuildOutputChangesApplyStatus status) {
        var delivered = new ArrayList<BuildOutputChanges>();
        var result = policy.deliver(changes -> {
            delivered.add(changes);
            return status;
        });
        assertThat(result.outcome())
                .isEqualTo(status == APPLIED ? SENT_APPLIED
                        : status == REJECTED ? SENT_REJECTED
                                : status == LIVE_RELOAD_DISABLED ? SENT_LIVE_RELOAD_DISABLED : SENT_NOT_APPLIED);
        assertThat(delivered).hasSize(1);
        assertThat(result.changes()).isSameAs(delivered.get(0));
        return delivered.get(0);
    }

    private BuildOutputChanges success(long sequence, BuildOutputPathChange... classChanges) {
        var mainClassChanges = new ArrayList<BuildOutputPathChange>();
        var mainResourceChanges = new ArrayList<BuildOutputPathChange>();
        for (BuildOutputPathChange change : classChanges) {
            if (change.outputRoot().equals(resources)) {
                mainResourceChanges.add(change);
            } else {
                mainClassChanges.add(change);
            }
        }
        return changes(sequence, BUILD_SUCCEEDED, mainClassChanges, mainResourceChanges);
    }

    private BuildOutputChanges changes(long sequence, BuildOutputChangeStatus status,
            List<BuildOutputPathChange> classChanges, List<BuildOutputPathChange> resourceChanges) {
        return new BuildOutputChanges(sequence, status, classChanges, resourceChanges, null, null, null, null, false, false);
    }

    private BuildOutputPathChange classChange(String path, BuildOutputChangeKind kind) {
        return classChange(classes, path, kind);
    }

    private BuildOutputPathChange classChange(Path root, String path, BuildOutputChangeKind kind) {
        return new BuildOutputPathChange(root, root.resolve(path), kind);
    }

    private BuildOutputPathChange resourceChange(String path, BuildOutputChangeKind kind) {
        return resourceChange(resources, path, kind);
    }

    private BuildOutputPathChange resourceChange(Path root, String path, BuildOutputChangeKind kind) {
        return new BuildOutputPathChange(root, root.resolve(path), kind);
    }

    private static void assertNoGradleType(Class<?> type) {
        for (Method method : type.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName()).doesNotContain("org.gradle");
            for (Class<?> parameterType : method.getParameterTypes()) {
                assertThat(parameterType.getName()).doesNotContain("org.gradle");
            }
        }
    }
}
