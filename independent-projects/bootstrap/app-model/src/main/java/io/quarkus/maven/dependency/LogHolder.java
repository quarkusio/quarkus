package io.quarkus.maven.dependency;

import org.jboss.logging.Logger;

final class LogHolder {
    private LogHolder() {
    }

    static final Logger log = Logger.getLogger("io.quarkus.maven.dependency");
}
