package io.quarkus.produi.runtime;

/**
 * Recognises Prod UI's own artifacts so its views can optionally exclude themselves (the {@code exclude-self} option).
 * <p>
 * Prod UI reports on the running application, but it is itself part of that application - its loggers, beans and
 * dependencies would otherwise show up in its own Loggers and Dependencies views as noise. This pure, testable helper
 * centralises the "is this me?" decision so every view filters identically.
 */
public final class ProdUISelfFilter {

    /** Root of Prod UI's own logger names. */
    private static final String LOGGER_PREFIX = "io.quarkus.produi";
    /** Group id under which Prod UI's own modules are published. */
    private static final String SELF_GROUP_ID = "io.quarkus";
    /** Common prefix of Prod UI's own artifact ids ({@code quarkus-produi}, {@code quarkus-produi-deployment}, ...). */
    private static final String SELF_ARTIFACT_PREFIX = "quarkus-produi";

    private ProdUISelfFilter() {
    }

    /**
     * @return {@code true} if {@code loggerName} is one of Prod UI's own loggers.
     */
    public static boolean isSelfLogger(String loggerName) {
        if (loggerName == null) {
            return false;
        }
        return loggerName.equals(LOGGER_PREFIX) || loggerName.startsWith(LOGGER_PREFIX + ".");
    }

    /**
     * @return {@code true} if the given Maven coordinates belong to one of Prod UI's own modules.
     */
    public static boolean isSelfArtifact(String groupId, String artifactId) {
        return SELF_GROUP_ID.equals(groupId) && artifactId != null && artifactId.startsWith(SELF_ARTIFACT_PREFIX);
    }
}
