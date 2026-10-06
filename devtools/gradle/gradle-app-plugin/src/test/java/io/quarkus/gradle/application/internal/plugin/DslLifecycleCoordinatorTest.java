package io.quarkus.gradle.application.internal.plugin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testing.base.TestingExtension;
import org.junit.jupiter.api.Test;

import io.quarkus.gradle.application.QuarkusApplicationPlugin;
import io.quarkus.gradle.application.dsl.QuarkusAotJarOutput;
import io.quarkus.gradle.application.dsl.QuarkusApplicationExtension;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmStartupArchive;
import io.quarkus.gradle.application.dsl.QuarkusApplicationJvmTestSuite;

class DslLifecycleCoordinatorTest {

    @Test
    void eventsDispatchOnceOutsideTheCoordinatorMonitorAndAllowReentrancy() {
        DslLifecycleCoordinator coordinator = new DslLifecycleCoordinator();
        QuarkusAotJarOutput build = aotBuild();
        AtomicInteger invocations = new AtomicInteger();
        coordinator.whenStartupOptimizedImageConfigured(build, ignored -> {
            invocations.incrementAndGet();
            coordinator.whenStartupOptimizedImageConfigured(build,
                    nested -> invocations.incrementAndGet());
        });

        coordinator.startupOptimizedImageConfigured(build);
        coordinator.startupOptimizedImageConfigured(build);

        assertThat(invocations).hasValue(2);
    }

    @Test
    void deliveredEventStaysDeliveredWhenAnActionThrows() {
        DslLifecycleCoordinator coordinator = new DslLifecycleCoordinator();
        QuarkusAotJarOutput build = aotBuild();
        coordinator.whenStartupOptimizedImageConfigured(build, ignored -> {
            throw new IllegalStateException("boom");
        });

        assertThatThrownBy(() -> coordinator.startupOptimizedImageConfigured(build))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");
        AtomicInteger late = new AtomicInteger();
        coordinator.whenStartupOptimizedImageConfigured(build, ignored -> late.incrementAndGet());
        assertThat(late).hasValue(1);
    }

    @Test
    void startupArchiveClaimsAreAtomicAndIdempotentForPackageBuild() throws Exception {
        DslLifecycleCoordinator coordinator = new DslLifecycleCoordinator();
        QuarkusApplicationJvmStartupArchive archive = aotBuild().getStartupArchive();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> packageClaim = executor.submit(() -> {
                await(start);
                coordinator.packageBuildConfigured(archive, "app");
            });
            Future<?> trainingClaim = executor.submit(() -> {
                await(start);
                coordinator.claimIntegrationTestTraining(archive, "app");
            });
            start.countDown();

            int failures = 0;
            for (Future<?> claim : java.util.List.of(packageClaim, trainingClaim)) {
                try {
                    claim.get();
                } catch (java.util.concurrent.ExecutionException expected) {
                    assertThat(expected.getCause())
                            .hasMessageContaining("cannot use package and integration-test");
                    failures++;
                }
            }
            assertThat(failures).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void selectedTestsDispatchExactlyOnceBeforeAndAfterAttachment() {
        Project project = project();
        DslLifecycleCoordinator coordinator = new DslLifecycleCoordinator();
        QuarkusApplicationExtension extension = project.getExtensions().getByType(QuarkusApplicationExtension.class);
        var first = project.getTasks().register("firstSelected", org.gradle.api.tasks.testing.Test.class);
        var second = project.getTasks().register("secondSelected", org.gradle.api.tasks.testing.Test.class);
        AtomicInteger selected = new AtomicInteger();
        AtomicInteger all = new AtomicInteger();

        coordinator.selectTestTask(extension.getTests(), first);
        coordinator.selectTestTask(extension.getTests(), first);
        coordinator.selectAllGradleTestTasks(extension.getTests());
        coordinator.attachTests(extension.getTests(), ignored -> selected.incrementAndGet(), all::incrementAndGet);
        coordinator.selectTestTask(extension.getTests(), second);
        coordinator.selectTestTask(extension.getTests(), second);
        coordinator.selectAllGradleTestTasks(extension.getTests());

        assertThat(selected).hasValue(2);
        assertThat(all).hasValue(1);
        assertThatThrownBy(() -> coordinator.attachTests(
                extension.getTests(), ignored -> {
                }, () -> {
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already have an internal configurator");
    }

    @Test
    void jvmSuiteTransitionsPreserveModeAndNativeConflicts() {
        QuarkusApplicationJvmTestSuite suite = quarkusSuite(project());
        DslLifecycleCoordinator coordinator = new DslLifecycleCoordinator();

        assertThat(coordinator.claimQuarkusTests(suite, "test")).isTrue();
        assertThat(coordinator.claimQuarkusTests(suite, "test")).isFalse();
        assertThatThrownBy(() -> coordinator.claimQuarkusIntegrationTests(suite, "test"))
                .hasMessageContaining("both Quarkus JVM and integration tests");

        QuarkusApplicationJvmTestSuite nativeSuite = quarkusSuite(project());
        coordinator.claimNamedNativeTestSuite(nativeSuite, "nativeTest");
        assertThatThrownBy(() -> coordinator.claimStartupArchiveTraining(nativeSuite, "nativeTest"))
                .hasMessageContaining("generated Quarkus named native-test suite");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static QuarkusAotJarOutput aotBuild() {
        QuarkusApplicationExtension extension = project().getExtensions().getByType(QuarkusApplicationExtension.class);
        return extension.getBuilds().aotJar("app").get();
    }

    private static QuarkusApplicationJvmTestSuite quarkusSuite(Project project) {
        JvmTestSuite suite = project.getExtensions().getByType(TestingExtension.class)
                .getSuites().withType(JvmTestSuite.class).getByName("test");
        return ((ExtensionAware) suite).getExtensions().getByType(QuarkusApplicationJvmTestSuite.class);
    }

    private static Project project() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply(QuarkusApplicationPlugin.class);
        return project;
    }
}
