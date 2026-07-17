package io.quarkus.deployment.dev;

import static io.quarkus.deployment.dev.BuildOutputChangeKind.ADDED;
import static io.quarkus.deployment.dev.BuildOutputChangeKind.DELETED;
import static io.quarkus.deployment.dev.BuildOutputChangeKind.MODIFIED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_CANCELLED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_FAILED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_SUCCEEDED;
import static io.quarkus.deployment.dev.BuildOutputChangeStatus.BUILD_SUPERSEDED;
import static io.quarkus.deployment.dev.BuildOutputChangesApplyStatus.APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesApplyStatus.NOT_APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.BASELINE_DROPPED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.DISCARDED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.NON_RELOADABLE_STATUS;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.NOTHING_TO_SEND;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.NO_RELOADABLE_CHANGES;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.PENDING;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.RESTART_REQUIRED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SEND_FAILED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.SENT_NOT_APPLIED;
import static io.quarkus.deployment.dev.BuildOutputChangesPolicy.Outcome.STALE_REJECTED;
import static org.assertj.core.api.Assertions.assertThat;

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
    void failedBuildsDoNotBecomeReloadsAndDoNotErasePendingSuccessfulChanges() {
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

        assertThat(delivered.sequence()).isEqualTo(1);
        assertThat(delivered.mainClassChanges()).extracting(BuildOutputPathChange::changedPath)
                .containsExactly(classes.resolve("com/acme/Foo.class"));
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
    void testOutputOnlySuccessfulBuildsAreNotReloadableInTheFirstSlice() {
        var policy = new BuildOutputChangesPolicy();

        var result = policy.accept(new BuildOutputChanges(1, BUILD_SUCCEEDED, null, null,
                List.of(classChange("org/acme/FooTest.class", MODIFIED)), null, null, null, false, false));

        assertThat(result.outcome()).isEqualTo(NO_RELOADABLE_CHANGES);
        assertThat(policy.hasPendingChanges()).isFalse();
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
        assertThat(result.outcome()).isEqualTo(status == APPLIED ? SENT_APPLIED : SENT_NOT_APPLIED);
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
