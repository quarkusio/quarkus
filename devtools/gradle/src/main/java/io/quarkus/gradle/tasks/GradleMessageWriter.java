package io.quarkus.gradle.tasks;

import org.gradle.api.logging.Logger;

import io.quarkus.devtools.messagewriter.MessageWriter;

public class GradleMessageWriter implements MessageWriter {

    private final Logger logger;

    public GradleMessageWriter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void debug(String msg) {
        logger.debug(msg);
    }

    @Override
    public void error(String msg) {
        logger.error(msg);
    }

    @Override
    public void info(String msg) {
        logger.lifecycle(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public void warn(String msg) {
        logger.warn(msg);
    }
}
