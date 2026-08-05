package io.quarkus.gradle.tooling.taskrunner;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Phaser;

/**
 * This is a copy of io.quarkus.bootstrap.resolver.maven.NonBlockingModelResolutionTaskRunner
 * with the same implementation, with Task and ErrorHandler interfaces defined as inner interfaces to avoid
 * dependency on the maven-resolver module.
 */
public class MavenModelResolutionTaskRunner {

    private final Phaser phaser = new Phaser(1);

    private final ErrorHandler errorHandler;

    public MavenModelResolutionTaskRunner(ErrorHandler errorHandler) {
        this.errorHandler = Objects.requireNonNull(errorHandler, "errorHandler cannot be null");
    }

    public void run(Task task) {
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

    public void waitForCompletion() {
        phaser.arriveAndAwaitAdvance();
        errorHandler.allTasksFinished();
    }

    public interface Task {

        void run();
    }

    public interface ErrorHandler {
        void handleError(Task task, Exception error);

        default void allTasksFinished() {
        }
    }

}
