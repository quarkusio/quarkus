package io.quarkus.gradle.tooling;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.GradleException;
import org.gradle.api.logging.Logger;
import org.gradle.util.GradleVersion;

public final class GradleVersionEnforcer {

    private static final GradleVersion MINIMUM_GRADLE_VERSION = GradleVersion.version("6.1");
    private static final GradleVersion TESTED_GRADLE_VERSION = GradleVersion.version("8.14");
    private static final GradleVersion QUARKUS_4_MINIMUM_GRADLE_VERSION = GradleVersion.version("9.6");

    private GradleVersionEnforcer() {
    }

    public static void verifyGradleVersion(Logger logger) {
        verifyGradleVersion(GradleVersion.current(), logger);
    }

    static void verifyGradleVersion(GradleVersion current, Logger logger) {
        if (current.compareTo(MINIMUM_GRADLE_VERSION) < 0) {
            throw new GradleException("Quarkus plugin requires Gradle 6.1 or later. Current version is: " + current);
        }

        warningsFor(current).forEach(logger::warn);
    }

    static List<String> warningsFor(GradleVersion current) {
        List<String> warnings = new ArrayList<>();
        if (current.compareTo(TESTED_GRADLE_VERSION) < 0) {
            warnings.add("This version of Quarkus is tested with Gradle 8.14 or later. This build is using Gradle "
                    + current.getVersion() + ". Please upgrade the Gradle wrapper.");
        }
        if (current.compareTo(QUARKUS_4_MINIMUM_GRADLE_VERSION) < 0) {
            warnings.add("Quarkus 4 will require Gradle 9.6 or later. This build is using Gradle "
                    + current.getVersion() + ". Please upgrade the Gradle wrapper before upgrading to Quarkus 4.");
        }
        return warnings;
    }
}
