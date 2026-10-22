package io.quarkus.bootstrap.resolver.maven;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests verifying that {@link ApplicationDependencyResolver} uses the blocking
 * task runner to prevent deadlocks.
 * <p>
 * This test suite ensures the fix for
 * <a href="https://github.com/quarkusio/quarkus/issues/57751">Issue #57751</a>
 * is correctly implemented: {@link ApplicationDependencyResolver#getTaskRunner()}
 * must always return a {@link BlockingModelResolutionTaskRunner} instance.
 * <p>
 * <strong>Why this matters:</strong>
 * <ul>
 * <li>Using {@link NonBlockingModelResolutionTaskRunner} can cause deadlocks when
 * main thread holds {@code FacadeClassLoader} monitor while waiting for ForkJoinPool workers</li>
 * <li>Workers attempting class loading block on the same monitor → circular wait → deadlock</li>
 * <li>Using {@link BlockingModelResolutionTaskRunner} executes all tasks on calling thread,
 * eliminating the possibility of cross-thread monitor contention</li>
 * </ul>
 *
 * @see ApplicationDependencyResolver#getTaskRunner()
 * @see <a href="https://github.com/quarkusio/quarkus/issues/57751">Issue #57751</a>
 */
@DisplayName("ApplicationDependencyResolver Task Runner Configuration")
class ApplicationDependencyResolverTaskRunnerTest {

    /**
     * Verifies that {@link ApplicationDependencyResolver#getTaskRunner()} returns
     * a {@link BlockingModelResolutionTaskRunner} instance.
     * <p>
     * This is the core fix for issue #57751. The method must return a blocking
     * runner to prevent parallel execution that can cause deadlocks.
     * <p>
     * <strong>Test Approach:</strong> Uses reflection to access the private
     * {@code getTaskRunner()} method since it's an internal implementation detail
     * that's critical for deadlock prevention.
     */
    @Test
    @DisplayName("getTaskRunner() returns BlockingModelResolutionTaskRunner instance")
    void getTaskRunnerReturnsBlockingRunner() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given: Access to the private getTaskRunner() method via reflection
        final Method getTaskRunnerMethod = ApplicationDependencyResolver.class
                .getDeclaredMethod("getTaskRunner");
        getTaskRunnerMethod.setAccessible(true);

        // When: getTaskRunner() is called
        final ModelResolutionTaskRunner taskRunner = (ModelResolutionTaskRunner) getTaskRunnerMethod.invoke(null);

        // Then: The returned runner is a BlockingModelResolutionTaskRunner
        assertThat(taskRunner)
                .as("ApplicationDependencyResolver must use BlockingModelResolutionTaskRunner to prevent deadlocks")
                .isInstanceOf(BlockingModelResolutionTaskRunner.class);
    }

    /**
     * Verifies that {@link ApplicationDependencyResolver#getTaskRunner()} does NOT
     * return a {@link NonBlockingModelResolutionTaskRunner} instance.
     * <p>
     * This negative test ensures we haven't accidentally reverted to the problematic
     * non-blocking implementation that caused issue #57751.
     */
    @Test
    @DisplayName("getTaskRunner() does NOT return NonBlockingModelResolutionTaskRunner")
    void getTaskRunnerDoesNotReturnNonBlockingRunner()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given: Access to the private getTaskRunner() method via reflection
        final Method getTaskRunnerMethod = ApplicationDependencyResolver.class
                .getDeclaredMethod("getTaskRunner");
        getTaskRunnerMethod.setAccessible(true);

        // When: getTaskRunner() is called
        final ModelResolutionTaskRunner taskRunner = (ModelResolutionTaskRunner) getTaskRunnerMethod.invoke(null);

        // Then: The returned runner is NOT a NonBlockingModelResolutionTaskRunner
        assertThat(taskRunner)
                .as("ApplicationDependencyResolver must NOT use NonBlockingModelResolutionTaskRunner (causes deadlocks)")
                .isNotInstanceOf(NonBlockingModelResolutionTaskRunner.class);
    }

    /**
     * Verifies that the factory method consistently returns blocking runners.
     * <p>
     * Edge case: Ensures that multiple invocations don't accidentally switch
     * to non-blocking execution based on thread state, system properties, or
     * other runtime conditions.
     */
    @Test
    @DisplayName("getTaskRunner() consistently returns blocking runners across multiple calls")
    void getTaskRunnerConsistentlyReturnsBlockingRunner()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given: Access to the private getTaskRunner() method
        final Method getTaskRunnerMethod = ApplicationDependencyResolver.class
                .getDeclaredMethod("getTaskRunner");
        getTaskRunnerMethod.setAccessible(true);

        // When: getTaskRunner() is called multiple times
        final int iterations = 10;
        for (int i = 0; i < iterations; i++) {
            final ModelResolutionTaskRunner taskRunner = (ModelResolutionTaskRunner) getTaskRunnerMethod.invoke(null);

            // Then: Each call returns a BlockingModelResolutionTaskRunner
            assertThat(taskRunner)
                    .as("Call #%d should return BlockingModelResolutionTaskRunner", i + 1)
                    .isInstanceOf(BlockingModelResolutionTaskRunner.class)
                    .isNotInstanceOf(NonBlockingModelResolutionTaskRunner.class);
        }
    }

    /**
     * Verifies that the blocking runner is used even when the system property
     * {@code quarkus.bootstrap.blocking-task-runner} is not explicitly set.
     * <p>
     * This ensures the fix makes blocking execution the <em>default</em> behavior,
     * not just an opt-in workaround.
     */
    @Test
    @DisplayName("Blocking runner is used by default without system property")
    void blockingRunnerIsDefaultWithoutSystemProperty()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given: System property is not set (or is set to false)
        final String originalValue = System.getProperty("quarkus.bootstrap.blocking-task-runner");
        try {
            System.clearProperty("quarkus.bootstrap.blocking-task-runner");

            // When: getTaskRunner() is called
            final Method getTaskRunnerMethod = ApplicationDependencyResolver.class
                    .getDeclaredMethod("getTaskRunner");
            getTaskRunnerMethod.setAccessible(true);
            final ModelResolutionTaskRunner taskRunner = (ModelResolutionTaskRunner) getTaskRunnerMethod.invoke(null);

            // Then: Blocking runner is still returned (it's the new default)
            assertThat(taskRunner)
                    .as("Blocking runner should be the default even without system property")
                    .isInstanceOf(BlockingModelResolutionTaskRunner.class);

        } finally {
            // Cleanup: Restore original system property value
            if (originalValue != null) {
                System.setProperty("quarkus.bootstrap.blocking-task-runner", originalValue);
            }
        }
    }

    /**
     * Verifies that {@link ModelResolutionTaskRunnerFactory#getBlockingTaskRunner()}
     * actually returns a blocking runner (factory method contract test).
     * <p>
     * This tests the factory that {@code ApplicationDependencyResolver} relies on,
     * ensuring the factory contract is fulfilled.
     */
    @Test
    @DisplayName("ModelResolutionTaskRunnerFactory.getBlockingTaskRunner() returns correct type")
    void factoryReturnsBlockingRunner() {
        // When: Getting a blocking runner from the factory
        final ModelResolutionTaskRunner taskRunner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // Then: The runner is actually a BlockingModelResolutionTaskRunner
        assertThat(taskRunner)
                .as("Factory must return a BlockingModelResolutionTaskRunner instance")
                .isInstanceOf(BlockingModelResolutionTaskRunner.class)
                .isNotNull();
    }

    /**
     * Verifies that {@link ModelResolutionTaskRunnerFactory#getNonBlockingTaskRunner()}
     * returns a non-blocking runner (factory contract test for comparison).
     * <p>
     * This confirms the factory can produce both types, and we're explicitly
     * choosing the blocking one in {@code ApplicationDependencyResolver}.
     */
    @Test
    @DisplayName("ModelResolutionTaskRunnerFactory.getNonBlockingTaskRunner() returns correct type")
    void factoryReturnsNonBlockingRunner() {
        // When: Getting a non-blocking runner from the factory
        final ModelResolutionTaskRunner taskRunner = ModelResolutionTaskRunnerFactory.getNonBlockingTaskRunner();

        // Then: The runner is a NonBlockingModelResolutionTaskRunner
        assertThat(taskRunner)
                .as("Factory must return a NonBlockingModelResolutionTaskRunner instance")
                .isInstanceOf(NonBlockingModelResolutionTaskRunner.class)
                .isNotNull();
    }

    /**
     * Verifies that the blocking runner returned is not null (basic sanity check).
     * <p>
     * Edge case: Ensures factory doesn't return null under any circumstances.
     */
    @Test
    @DisplayName("getTaskRunner() never returns null")
    void getTaskRunnerNeverReturnsNull()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Given: Access to the private getTaskRunner() method
        final Method getTaskRunnerMethod = ApplicationDependencyResolver.class
                .getDeclaredMethod("getTaskRunner");
        getTaskRunnerMethod.setAccessible(true);

        // When: getTaskRunner() is called
        final ModelResolutionTaskRunner taskRunner = (ModelResolutionTaskRunner) getTaskRunnerMethod.invoke(null);

        // Then: The result is never null
        assertThat(taskRunner)
                .as("getTaskRunner() must never return null")
                .isNotNull();
    }
}
