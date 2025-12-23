package io.quarkus.bootstrap.resolver.maven;

/**
 * Blocking implementation of the {@link ModelResolutionTaskRunner}.
 * Each call to {@link #run(ModelResolutionTask)} will block until the task has finished running.
 * {@link #waitForCompletion()} is a no-op in this implementation.
 */
class BlockingModelResolutionTaskRunner implements ModelResolutionTaskRunner {

    static BlockingModelResolutionTaskRunner getInstance() {
        return new BlockingModelResolutionTaskRunner(null);
    }

    static BlockingModelResolutionTaskRunner getInstance(ModelResolutionTaskErrorHandler errorHandler) {
        return new BlockingModelResolutionTaskRunner(errorHandler);
    }

    private final ModelResolutionTaskErrorHandler errorHandler;

    BlockingModelResolutionTaskRunner(ModelResolutionTaskErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public void run(ModelResolutionTask task) {
        if (errorHandler == null) {
            task.run();
            return;
        }
        try {
            task.run();
        } catch (Exception e) {
            errorHandler.handleError(task, e);
        }
    }

    @Override
    public void waitForCompletion() {
        if (errorHandler != null) {
            errorHandler.allTasksFinished();
        }
    }
}
