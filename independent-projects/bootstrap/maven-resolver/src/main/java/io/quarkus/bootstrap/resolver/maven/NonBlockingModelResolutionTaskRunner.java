package io.quarkus.bootstrap.resolver.maven;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;

/**
 * Non-blocking task runner implementation
 */
class NonBlockingModelResolutionTaskRunner implements ModelResolutionTaskRunner {

    private final Phaser phaser = new Phaser(1);

    private final ModelResolutionTaskErrorHandler errorHandler;

    NonBlockingModelResolutionTaskRunner(ModelResolutionTaskErrorHandler errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler cannot be null");
    }

    /**
     * Runs a model resolution task asynchronously. This method may return before the task has completed.
     *
     * @param task task to run
     */
    @Override
    public void run(ModelResolutionTask task) {
        phaser.register();
        CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                errorHandler.handleError(task, e);
            } finally {
                phaser.arriveAndDeregister();
            }
        });
    }

    /**
     * Blocks until all the tasks have completed.
     * <p>
     * In case some tasks failed with errors, this method will log each error and throw a {@link RuntimeException}
     * with a corresponding message.
     */
    @Override
    public void waitForCompletion() {
        phaser.arriveAndAwaitAdvance();
        errorHandler.allTasksFinished();
    }
}
