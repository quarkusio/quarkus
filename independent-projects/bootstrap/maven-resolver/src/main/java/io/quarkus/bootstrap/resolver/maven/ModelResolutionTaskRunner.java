package io.quarkus.bootstrap.resolver.maven;

/**
 * Task runner
 */
public interface ModelResolutionTaskRunner {

    /**
     * Returns an instance of a non-blocking task runner.
     *
     * @return an instance of a non-blocking task runner
     */
    static ModelResolutionTaskRunner getNonBlockingTaskRunner() {
        return new NonBlockingModelResolutionTaskRunner();
    }

    /**
     * Returns an instance of a blocking task runner.
     *
     * @return an instance of a blocking task runner
     */
    static ModelResolutionTaskRunner getBlockingTaskRunner() {
        return BlockingModelResolutionTaskRunner.getInstance();
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
