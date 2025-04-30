package io.quarkus.bootstrap.resolver.maven;

/**
 * Blocking implementation of the {@link ModelResolutionTaskRunner}.
 * Each call to {@link #run(ModelResolutionTask)} will block until the task has finished running.
 * {@link #waitForCompletion()} is a no-op in this implementation.
 */
class BlockingModelResolutionTaskRunner implements ModelResolutionTaskRunner {

    static BlockingModelResolutionTaskRunner getInstance() {
        return new BlockingModelResolutionTaskRunner();
    }

    @Override
    public void run(ModelResolutionTask task) {
        task.run();
    }

    @Override
    public void waitForCompletion() {
    }
}
