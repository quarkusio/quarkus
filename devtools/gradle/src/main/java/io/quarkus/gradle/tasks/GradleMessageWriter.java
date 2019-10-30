package io.quarkus.gradle.tasks;

import org.gradle.api.logging.Logger;

import io.quarkus.platform.tools.MessageWriter;

public class GradleMessageWriter implements MessageWriter {

    private final Logger logger;

    public GradleMessageWriter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String arg0) {
        logger.debug(arg0);
    }

    @Override
    public void error(String arg0) {
        logger.error(arg0);
    }

    @Override
    public void info(String arg0) {
        logger.info(arg0);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void warn(String arg0) {
        logger.warn(arg0);
    }
}
