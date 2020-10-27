package io.quarkus.gradle.tasks;

import org.gradle.api.logging.Logger;

import io.quarkus.deployment.dev.QuarkusDevModeLauncher;

public class GradleDevModeLauncher extends QuarkusDevModeLauncher {

    /**
     * Initializes the launcher builder
     *
     * @param logger the logger
     * @return launcher builder
     */
    public static Builder builder(Logger logger) {
        return new GradleDevModeLauncher(logger).new Builder();
    }

    public class Builder extends QuarkusDevModeLauncher.Builder<GradleDevModeLauncher, Builder> {

        private Builder() {
            super(null);
        }
    }

    private final Logger logger;

    private GradleDevModeLauncher(Logger logger) {
        this.logger = logger;
    }

    @Override
    protected boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    protected void debug(Object msg) {
        logger.warn(msg == null ? "null" : msg.toString());
    }

    @Override
    protected void error(Object msg) {
        logger.error(msg == null ? "null" : msg.toString());
    }

    @Override
    protected void warn(Object msg) {
        logger.warn(msg == null ? "null" : msg.toString());
    }
}
