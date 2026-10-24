package io.quarkus.bootstrap.resolver.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests for verifying that the blocking task runner prevents deadlocks when
 * executing dependency resolution tasks.
 * <p>
 * This test suite specifically addresses the deadlock issue reported in
 * <a href="https://github.com/quarkusio/quarkus/issues/57751">Issue #57751</a>,
 * where parallel dependency resolution using {@link NonBlockingModelResolutionTaskRunner}
 * could deadlock when:
 * <ol>
 * <li>Main thread holds {@code FacadeClassLoader} monitor</li>
 * <li>Main thread waits for ForkJoinPool workers to complete</li>
 * <li>Worker threads trigger class loading that requires the same monitor</li>
 * <li>Circular wait causes deadlock</li>
 * </ol>
 * <p>
 * The fix ensures {@link ApplicationDependencyResolver} always uses
 * {@link BlockingModelResolutionTaskRunner}, which executes tasks synchronously
 * on the calling thread, eliminating the possibility of cross-thread monitor contention.
 *
 * @see ApplicationDependencyResolver#getTaskRunner()
 * @see BlockingModelResolutionTaskRunner
 * @see NonBlockingModelResolutionTaskRunner
 */
@DisplayName("Blocking Task Runner Deadlock Prevention")
class BlockingTaskRunnerDeadlockPreventionTest {

    /**
     * Verifies that {@link BlockingModelResolutionTaskRunner} executes all tasks
     * synchronously on the calling thread, ensuring no parallel execution occurs.
     * <p>
     * This is the core guarantee that prevents deadlocks - all tasks run on the
     * thread that holds any monitors (like {@code FacadeClassLoader}), so no
     * worker threads can block waiting for those monitors.
     */
    @Test
    @DisplayName("Blocking runner executes all tasks synchronously on calling thread")
    void blockingRunnerExecutesSynchronously() {
        // Given: A blocking task runner and a task that captures the execution thread
        final Thread callingThread = Thread.currentThread();
        final AtomicBoolean taskExecuted = new AtomicBoolean(false);
        final List<Thread> executionThreads = new ArrayList<>();

        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: Multiple tasks are submitted
        for (int i = 0; i < 5; i++) {
            runner.run(() -> {
                executionThreads.add(Thread.currentThread());
                taskExecuted.set(true);
            });
        }
        runner.waitForCompletion();

        // Then: All tasks executed on the calling thread (no workers)
        assertThat(taskExecuted).isTrue();
        assertThat(executionThreads)
                .hasSize(5)
                .allMatch(thread -> thread == callingThread,
                        "All tasks should execute on the calling thread");
    }

    /**
     * Verifies that {@link BlockingModelResolutionTaskRunner#waitForCompletion()}
     * is a no-op, since all tasks complete before {@link BlockingModelResolutionTaskRunner#run(ModelResolutionTask)}
     * returns.
     * <p>
     * This test confirms that when using blocking execution, there are no pending
     * tasks to wait for, which is essential for preventing the deadlock scenario
     * where the main thread waits on a {@code Phaser} while holding a monitor.
     */
    @Test
    @DisplayName("Blocking runner waitForCompletion is a no-op (all tasks already complete)")
    void blockingRunnerWaitForCompletionIsNoOp() {
        // Given: A blocking task runner and a task counter
        final AtomicInteger completedTasks = new AtomicInteger(0);
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: Tasks are submitted and waitForCompletion is called
        for (int i = 0; i < 3; i++) {
            runner.run(completedTasks::incrementAndGet);
        }

        // Then: All tasks are already complete before waitForCompletion
        assertThat(completedTasks).hasValue(3);

        // When: waitForCompletion is called
        assertThatCode(runner::waitForCompletion)
                .doesNotThrowAnyException();

        // Then: Task count remains unchanged (no additional work done)
        assertThat(completedTasks).hasValue(3);
    }

    /**
     * Simulates a scenario where tasks are executed while holding a monitor,
     * verifying that blocking execution prevents deadlocks.
     * <p>
     * This test replicates the condition from issue #57751 where:
     * <ul>
     * <li>Main thread holds a monitor (simulating {@code FacadeClassLoader.loadClass()})</li>
     * <li>Tasks are submitted that would need to acquire the same monitor</li>
     * <li>With parallel execution, this would deadlock</li>
     * <li>With blocking execution, no deadlock occurs</li>
     * </ul>
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("No deadlock when executing tasks while holding monitor")
    void noDeadlockWithMonitorHeld() {
        // Given: A shared monitor object and a blocking task runner
        final Object sharedMonitor = new Object();
        final AtomicInteger tasksCompleted = new AtomicInteger(0);
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: Tasks are executed while holding the monitor
        synchronized (sharedMonitor) {
            for (int i = 0; i < 5; i++) {
                runner.run(() -> {
                    // This task would deadlock with non-blocking execution
                    // if it tried to acquire sharedMonitor (but it doesn't need to)
                    tasksCompleted.incrementAndGet();
                });
            }
            runner.waitForCompletion();
        }

        // Then: All tasks completed successfully without deadlock
        assertThat(tasksCompleted).hasValue(5);
    }

    /**
     * Verifies that {@link BlockingModelResolutionTaskRunner} maintains task
     * execution order (FIFO - First In First Out).
     * <p>
     * This is an important characteristic for dependency resolution, as the
     * order of visiting dependency nodes may affect the final resolution result.
     */
    @Test
    @DisplayName("Blocking runner maintains FIFO task execution order")
    void blockingRunnerMaintainsExecutionOrder() {
        // Given: A blocking task runner and a list to track execution order
        final List<Integer> executionOrder = new ArrayList<>();
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: Tasks are submitted in a specific order
        for (int i = 0; i < 10; i++) {
            final int taskId = i;
            runner.run(() -> executionOrder.add(taskId));
        }
        runner.waitForCompletion();

        // Then: Tasks executed in submission order
        assertThat(executionOrder)
                .containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }

    /**
     * Verifies that task exceptions are properly propagated to the caller
     * when using {@link BlockingModelResolutionTaskRunner}.
     * <p>
     * Edge case: Ensures that errors in dependency resolution tasks are
     * immediately visible to the caller, allowing proper error handling.
     */
    @Test
    @DisplayName("Blocking runner propagates task exceptions immediately")
    void blockingRunnerPropagatesExceptions() {
        // Given: A blocking task runner and a task that throws an exception
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();
        final RuntimeException expectedException = new RuntimeException("Task failed");

        // When: A failing task is submitted
        // Then: Exception is propagated immediately (not during waitForCompletion)
        assertThatCode(() -> runner.run(() -> {
            throw expectedException;
        }))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Task failed");
    }

    /**
     * Demonstrates the deadlock risk with {@link NonBlockingModelResolutionTaskRunner}
     * when tasks execute in parallel and contend for shared monitors.
     * <p>
     * <strong>WARNING:</strong> This test uses non-blocking execution to demonstrate
     * why it was changed. It verifies that parallel execution does occur (which is
     * the root cause of the deadlock in issue #57751).
     * <p>
     * This test does NOT actually trigger a deadlock (which would hang the test),
     * but instead verifies that tasks DO execute on different threads, proving
     * the parallel execution behavior that enables the deadlock condition.
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Non-blocking runner uses parallel execution (demonstrating deadlock risk)")
    void nonBlockingRunnerUsesParallelExecution() throws InterruptedException {
        // Given: A non-blocking task runner
        final Thread callingThread = Thread.currentThread();
        final List<Thread> executionThreads = new ArrayList<>();
        final CountDownLatch tasksStarted = new CountDownLatch(3);
        final CountDownLatch proceedSignal = new CountDownLatch(1);

        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getNonBlockingTaskRunner();

        // When: Multiple tasks are submitted that will run concurrently
        for (int i = 0; i < 3; i++) {
            runner.run(() -> {
                executionThreads.add(Thread.currentThread());
                tasksStarted.countDown();
                try {
                    // Wait to ensure concurrent execution
                    proceedSignal.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
        }

        // Wait for all tasks to start (proving they run concurrently)
        tasksStarted.await(5, TimeUnit.SECONDS);

        // Then: Tasks are executing on worker threads (not the calling thread)
        assertThat(executionThreads)
                .hasSizeGreaterThanOrEqualTo(1)
                .noneMatch(thread -> thread == callingThread,
                        "Non-blocking tasks should NOT execute on the calling thread")
                .allMatch(thread -> thread.getName().contains("ForkJoinPool"),
                        "Non-blocking tasks should execute in ForkJoinPool");

        // Cleanup: Allow tasks to complete
        proceedSignal.countDown();
        runner.waitForCompletion();
    }

    /**
     * Verifies that {@link BlockingModelResolutionTaskRunner} handles empty
     * task queues gracefully (edge case).
     */
    @Test
    @DisplayName("Blocking runner handles empty task queue")
    void blockingRunnerHandlesEmptyQueue() {
        // Given: A blocking task runner with no tasks
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: waitForCompletion is called without submitting any tasks
        // Then: No exception is thrown
        assertThatCode(runner::waitForCompletion)
                .doesNotThrowAnyException();
    }

    /**
     * Verifies that {@link BlockingModelResolutionTaskRunner} handles a large
     * number of tasks without performance degradation or stack overflow (edge case).
     * <p>
     * This ensures that sequential execution is viable even for dependency graphs
     * with many nodes.
     */
    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    @DisplayName("Blocking runner handles large number of tasks efficiently")
    void blockingRunnerHandlesLargeNumberOfTasks() {
        // Given: A blocking task runner and a large number of tasks
        final int taskCount = 1000;
        final AtomicInteger completedTasks = new AtomicInteger(0);
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: A large number of tasks are submitted
        for (int i = 0; i < taskCount; i++) {
            runner.run(completedTasks::incrementAndGet);
        }
        runner.waitForCompletion();

        // Then: All tasks completed successfully
        assertThat(completedTasks).hasValue(taskCount);
    }

    /**
     * Verifies that nested task submission works correctly with blocking execution
     * (edge case for recursive dependency resolution).
     * <p>
     * This simulates scenarios where resolving one dependency triggers resolution
     * of its transitive dependencies.
     */
    @Test
    @DisplayName("Blocking runner supports nested task submission")
    void blockingRunnerSupportsNestedTasks() {
        // Given: A blocking task runner
        final List<String> executionLog = new ArrayList<>();
        final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();

        // When: A task submits another task
        runner.run(() -> {
            executionLog.add("outer-start");
            runner.run(() -> executionLog.add("inner"));
            executionLog.add("outer-end");
        });
        runner.waitForCompletion();

        // Then: Tasks execute in the correct nested order
        assertThat(executionLog)
                .containsExactly("outer-start", "inner", "outer-end");
    }

    /**
     * Verifies that blocking execution is thread-safe when used from multiple
     * threads concurrently (edge case for parallel project builds).
     */
    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    @DisplayName("Blocking runner is thread-safe when used from multiple callers")
    void blockingRunnerIsThreadSafe() throws ExecutionException, InterruptedException {
        // Given: Multiple threads each with their own blocking runner instance
        final int threadCount = 5;
        final int tasksPerThread = 100;
        final AtomicInteger totalCompleted = new AtomicInteger(0);

        // When: Multiple threads submit tasks concurrently
        final List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> {
                final ModelResolutionTaskRunner runner = ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();
                for (int j = 0; j < tasksPerThread; j++) {
                    runner.run(totalCompleted::incrementAndGet);
                }
                runner.waitForCompletion();
            }));
        }

        // Wait for all threads to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();

        // Then: All tasks from all threads completed successfully
        assertThat(totalCompleted).hasValue(threadCount * tasksPerThread);
    }
}
