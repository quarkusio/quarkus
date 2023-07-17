package io.quarkus.runtime.logging;

public final class JBossVersion {

    private static final String JBOSS_LOG_VERSION = "jboss.log-version";

    private JBossVersion() {
    }

    public static void disableVersionLogging() {
        System.setProperty(JBOSS_LOG_VERSION, "false");
    }
}
