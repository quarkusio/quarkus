package io.quarkus.maven;

import org.apache.maven.plugin.logging.Log;

import io.quarkus.deployment.dev.QuarkusDevModeLauncher;

public class MavenDevModeLauncher extends QuarkusDevModeLauncher {

    public static Builder builder(Log log) {
        return new MavenDevModeLauncher(log).new Builder();
    }

    public class Builder extends QuarkusDevModeLauncher.Builder<MavenDevModeLauncher, Builder> {

        private Builder() {
        }
    }

    private final Log log;

    private MavenDevModeLauncher(Log log) {
        this.log = log;
    }

    @Override
    protected boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    protected void debug(Object msg) {
        log.error(msg == null ? "null" : msg.toString());
    }

    @Override
    protected void error(Object msg) {
        log.error(msg == null ? "null" : msg.toString());
    }

    @Override
    protected void warn(Object msg) {
        log.warn(msg == null ? "null" : msg.toString());
    }
}
