package io.quarkus.bootstrap.resolver.maven;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Phaser;

import org.jboss.logging.Logger;

/**
 * Non-blocking task runner implementation
 */
class NonBlockingModelResolutionTaskRunner implements ModelResolutionTaskRunner {

    private static final Logger log = Logger.getLogger(NonBlockingModelResolutionTaskRunner.class);

    private final Phaser phaser = new Phaser(1);

    /**
     * Errors caught while running tasks
     */
    private final Collection<Exception> errors = new ConcurrentLinkedDeque<>();

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
                errors.add(e);
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
        assertNoErrors();
    }

    private void assertNoErrors() {
        if (!errors.isEmpty()) {
            var sb = new StringBuilder(
                    "The following errors were encountered while processing Quarkus application dependencies:");
            log.error(sb);
            var i = 1;
            for (var error : errors) {
                var prefix = i++ + ")";
                log.error(prefix, error);
                sb.append(System.lineSeparator()).append(prefix).append(" ").append(error.getLocalizedMessage());
                for (var e : error.getStackTrace()) {
                    sb.append(System.lineSeparator());
                    for (int j = 0; j < prefix.length(); ++j) {
                        sb.append(" ");
                    }
                    sb.append("at ").append(e);
                    if (e.getClassName().contains("io.quarkus")) {
                        sb.append(System.lineSeparator());
                        for (int j = 0; j < prefix.length(); ++j) {
                            sb.append(" ");
                        }
                        sb.append("...");
                        break;
                    }
                }
            }
            throw new RuntimeException(sb.toString());
        }
    }
}
