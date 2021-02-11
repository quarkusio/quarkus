package io.quarkus.logging.manager.deployment;

import io.quarkus.builder.item.SimpleBuildItem;

final class LoggingManagerBuildItem extends SimpleBuildItem {

    private final String loggingManagerFinalDestination;
    private final String loggingManagerPath;

    public LoggingManagerBuildItem(String loggingManagerFinalDestination, String loggingManagerPath) {
        this.loggingManagerFinalDestination = loggingManagerFinalDestination;
        this.loggingManagerPath = loggingManagerPath;
    }

    public String getLoggingManagerFinalDestination() {
        return loggingManagerFinalDestination;
    }

    public String getLoggingManagerPath() {
        return loggingManagerPath;
    }
}