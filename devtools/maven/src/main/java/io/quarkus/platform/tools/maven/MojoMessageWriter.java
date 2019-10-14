package io.quarkus.platform.tools.maven;

import org.apache.maven.plugin.logging.Log;

import io.quarkus.platform.tools.MessageWriter;

public class MojoMessageWriter implements MessageWriter {

    private final Log log;

    public MojoMessageWriter(Log log) {
        this.log = log;
    }

    @Override
    public void info(String msg) {
        log.info(msg);
    }

    @Override
    public void error(String msg) {
        log.error(msg);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void debug(String msg) {
        log.debug(msg);
    }

    @Override
    public void warn(String msg) {
        log.warn(msg);
    }
}
