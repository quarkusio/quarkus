package io.quarkus.gradle;

import org.gradle.api.GradleException;
import org.gradle.util.GradleVersion;

public final class GradleVersionSupport {

    public static final String MINIMUM_GRADLE_VERSION = GeneratedGradleVersionSupport.MINIMUM_GRADLE_VERSION;

    public static final String SUPPORTED_GRADLE_VERSIONS = GeneratedGradleVersionSupport.SUPPORTED_GRADLE_VERSIONS;

    private static final GradleVersion MINIMUM_GRADLE = GradleVersion.version(MINIMUM_GRADLE_VERSION);

    private GradleVersionSupport() {
    }

    public static void requireMinimumGradleVersion() {
        requireMinimumGradleVersion(GradleVersion.current());
    }

    static void requireMinimumGradleVersion(GradleVersion currentVersion) {
        if (currentVersion.compareTo(MINIMUM_GRADLE) < 0) {
            throw new GradleException("Quarkus Gradle plugins require Gradle " + MINIMUM_GRADLE_VERSION
                    + " or later. Current version is: " + currentVersion);
        }
    }
}
