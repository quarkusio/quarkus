package io.quarkus.bootstrap.resolver.maven;

/**
 * Task runner
 */
public interface ModelResolutionTaskRunner {

    /**
     * @deprecated in favor of {@link ModelResolutionTaskRunnerFactory#getNonBlockingTaskRunner()}
     *
     *             Returns an instance of a non-blocking task runner with an error handler
     *             that collects all task execution errors and throws a single error when all the tasks
     *             have finished.
     *
     * @return an instance of a non-blocking task runner
     */
    @Deprecated(forRemoval = true)
    static ModelResolutionTaskRunner getNonBlockingTaskRunner() {
        return ModelResolutionTaskRunnerFactory.getNonBlockingTaskRunner();
    }

    /**
     * @deprecated in favor of
     *             {@link ModelResolutionTaskRunnerFactory#getNonBlockingTaskRunner(ModelResolutionTaskErrorHandler)}
     *
     *             Returns an instance of a non-blocking task runner with a custom error handler.
     *
     * @param errorHandler error handler
     * @return an instance of non-blocking task runner
     */
    @Deprecated(forRemoval = true)
    static ModelResolutionTaskRunner getNonBlockingTaskRunner(ModelResolutionTaskErrorHandler errorHandler) {
        return ModelResolutionTaskRunnerFactory.getNonBlockingTaskRunner(errorHandler);
    }

    /**
     * @deprecated in favor of {@link ModelResolutionTaskRunnerFactory#getBlockingTaskRunner()}
     *
     *             Returns an instance of a blocking task runner with no error handler.
     *             Exceptions thrown from tasks will be immediately propagated to the caller.
     *
     * @return an instance of a blocking task runner
     */
    @Deprecated(forRemoval = true)
    static ModelResolutionTaskRunner getBlockingTaskRunner() {
        return ModelResolutionTaskRunnerFactory.getBlockingTaskRunner();
    }

    /**
     * @deprecated in favor of {@link ModelResolutionTaskRunnerFactory#getBlockingTaskRunner(ModelResolutionTaskErrorHandler)}
     *
     *             Returns an instance of a blocking task runner with a custom error handler.
     *
     * @return an instance of a blocking task runner
     */
    @Deprecated(forRemoval = true)
    static ModelResolutionTaskRunner getBlockingTaskRunner(ModelResolutionTaskErrorHandler errorHandler) {
        return ModelResolutionTaskRunnerFactory.getBlockingTaskRunner(errorHandler);
    }

    /**
     * Instructs a runner to run the passed in task.
     * <p>
     * Whether this method is blocking or not will depend on the implementation.
     *
     * @param task task to run
     */
    void run(ModelResolutionTask task);

    /**
     * Blocking method that will return once all the tasks submitted by calling the {@link #run(ModelResolutionTask)}
     * method have been executed.
     */
    void waitForCompletion();
}
