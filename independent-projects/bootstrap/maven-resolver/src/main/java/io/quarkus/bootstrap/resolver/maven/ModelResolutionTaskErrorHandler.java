package io.quarkus.bootstrap.resolver.maven;

public interface ModelResolutionTaskErrorHandler {

    void handleError(ModelResolutionTask task, Exception error);

    default void allTasksFinished() {
    }
}
