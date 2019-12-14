package io.quarkus.kubernetes.deployment;

import org.eclipse.jkube.kit.common.KitLogger;
import org.jboss.logging.Logger;

public class JKubeLogger implements KitLogger {

    private final Logger log;

    JKubeLogger(Logger log) {
        this.log = log;
    }

    @Override
    public void debug(String format, Object... params) {
        log.debugf(format, params);
    }

    @Override
    public void info(String format, Object... params) {
        log.infof(format, params);
    }

    @Override
    public void warn(String format, Object... params) {
        log.warnf(format, params);
    }

    @Override
    public void error(String format, Object... params) {
        log.errorf(format, params);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }
}
