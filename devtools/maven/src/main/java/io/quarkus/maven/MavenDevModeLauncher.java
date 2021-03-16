package io.quarkus.maven;

import org.apache.maven.plugin.logging.Log;

import io.quarkus.deployment.dev.QuarkusDevModeLauncher;

public class MavenDevModeLauncher extends QuarkusDevModeLauncher {

    /**
     * Initializes the launcher builder
     *
     * @param java path to the java, may be null
     * @param log the logger
     * @return launcher builder
     */
    public static Builder builder(String java, Log log) {
        return new MavenDevModeLauncher(log).new Builder(java);
    }

    public class Builder extends QuarkusDevModeLauncher.Builder<MavenDevModeLauncher, Builder> {

        private Builder(String java) {
            super(java);
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
