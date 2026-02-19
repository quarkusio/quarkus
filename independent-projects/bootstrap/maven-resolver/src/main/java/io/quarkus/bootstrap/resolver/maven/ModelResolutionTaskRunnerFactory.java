package io.quarkus.bootstrap.resolver.maven;

public class ModelResolutionTaskRunnerFactory {

    /**
     * Whether to use a blocking or non-blocking dependency resolution and processing task runner
     */
    private static final boolean BLOCKING_TASK_RUNNER = Boolean.getBoolean("quarkus.bootstrap.blocking-task-runner");

    public static boolean isDefaultRunnerBlocking() {
        return BLOCKING_TASK_RUNNER;
    }

    /**
     * New instance of {@link ModelResolutionTaskRunner}. Unless system property {@code quarkus.bootstrap.blocking-task-runner}
     * is set to {@code true}, the returned runner will not be blocking.
     *
     * @return new task runner instance
     */
    public static ModelResolutionTaskRunner newTaskRunner() {
        return BLOCKING_TASK_RUNNER ? getBlockingTaskRunner() : getNonBlockingTaskRunner();
    }

    /**
     * Returns an instance of a non-blocking task runner with an error handler
     * that collects all task execution errors and throws a single error when all the tasks
     * have finished.
     *
     * @return an instance of a non-blocking task runner
     */
    public static ModelResolutionTaskRunner getNonBlockingTaskRunner() {
        return getNonBlockingTaskRunner(new FailAtCompletionErrorHandler());
    }

    /**
     * Returns an instance of a non-blocking task runner with a custom error handler.
     *
     * @param errorHandler error handler
     * @return an instance of non-blocking task runner
     */
    public static ModelResolutionTaskRunner getNonBlockingTaskRunner(ModelResolutionTaskErrorHandler errorHandler) {
        return new NonBlockingModelResolutionTaskRunner(errorHandler);
    }

    /**
     * Returns an instance of a blocking task runner with no error handler.
     * Exceptions thrown from tasks will be immediately propagated to the caller.
     *
     * @return an instance of a blocking task runner
     */
    public static ModelResolutionTaskRunner getBlockingTaskRunner() {
        return BlockingModelResolutionTaskRunner.getInstance();
    }

    /**
     * Returns an instance of a blocking task runner with a custom error handler.
     *
     * @return an instance of a blocking task runner
     */
    public static ModelResolutionTaskRunner getBlockingTaskRunner(ModelResolutionTaskErrorHandler errorHandler) {
        return BlockingModelResolutionTaskRunner.getInstance(errorHandler);
    }
}
