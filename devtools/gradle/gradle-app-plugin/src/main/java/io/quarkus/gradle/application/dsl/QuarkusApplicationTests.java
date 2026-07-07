package io.quarkus.gradle.application.dsl;

import javax.inject.Inject;

import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.testing.Test;

import io.quarkus.gradle.application.internal.plugin.DslLifecycleCoordinator;

/**
 * Selects finite Gradle {@link Test} tasks that receive Quarkus test setup.
 * <p>
 * The standard {@code test} task is configured automatically. Additional tasks can be selected individually or by
 * type. Selection is lazy and idempotent. When the legacy {@code io.quarkus} plugin coexists and owns test execution,
 * these selections do not reconfigure its tasks.
 */
public class QuarkusApplicationTests {

    private final DslLifecycleCoordinator lifecycle;

    /**
     * Creates the Gradle-managed test-selection DSL.
     *
     * @param lifecycle the plugin's internal DSL lifecycle coordinator
     */
    @Inject
    public QuarkusApplicationTests(Object lifecycle) {
        if (!(lifecycle instanceof DslLifecycleCoordinator coordinator)) {
            throw new IllegalArgumentException("Quarkus application tests require their internal lifecycle coordinator");
        }
        this.lifecycle = coordinator;
    }

    /**
     * Selects one lazily registered Gradle test task for Quarkus test setup.
     *
     * @param taskProvider the test task provider
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void task(TaskProvider<? extends Test> taskProvider) {
        lifecycle.selectTestTask(this, taskProvider);
    }

    /**
     * Selects every present and future Gradle {@link Test} task in the project.
     */
    @SuppressWarnings("unused") // publicly documented DSL
    public void allGradleTestTasks() {
        lifecycle.selectAllGradleTestTasks(this);
    }
}
