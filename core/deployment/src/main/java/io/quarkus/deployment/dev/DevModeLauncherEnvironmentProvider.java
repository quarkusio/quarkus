package io.quarkus.deployment.dev;

import java.util.Map;

/**
 * Allows extensions to contribute environment variables to the forked quarkus dev process.
 */
public interface DevModeLauncherEnvironmentProvider {

    /**
     * @param buildSystemProperties build-time properties passed to the dev mode process
     * @param applicationName the application artifact id
     * @return environment variables to set in the forked dev mode process
     */
    Map<String, String> provide(Map<String, String> buildSystemProperties, String applicationName);
}
