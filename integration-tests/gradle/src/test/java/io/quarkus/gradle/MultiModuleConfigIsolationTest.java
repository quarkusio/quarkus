package io.quarkus.gradle;

import java.io.File;

import org.junit.jupiter.api.Test;

/**
 * Regression test for https://github.com/quarkusio/quarkus/issues/54095.
 *
 * <p>
 * Each module's {@code quarkusAppPartsBuild} in a multi-module Gradle build must only see its own
 * configuration. Stale system properties on a pooled worker JVM otherwise leak datasource (and
 * other extension) configuration from one module into the next module's bootstrap, making
 * {@code AgroalProcessor} try to load a driver absent from the classpath.
 */
public class MultiModuleConfigIsolationTest extends QuarkusGradleWrapperTestBase {

    @Test
    public void testForwardOrderBuild() throws Exception {
        File projectDir = getProjectDir("multi-module-config-isolation");
        // `clean` keeps the second test method from picking up build/ outputs left by this one.
        // Without it, Gradle marks both quarkusAppPartsBuild tasks up-to-date and skips them,
        // bypassing the worker JVM reuse path the test exercises. Explicit task ordering also
        // forces module-a first.
        runGradleWrapper(projectDir, "clean", ":module-a:quarkusAppPartsBuild", ":module-b:quarkusAppPartsBuild");
    }

    @Test
    public void testReverseOrderBuild() throws Exception {
        File projectDir = getProjectDir("multi-module-config-isolation");
        runGradleWrapper(projectDir, "clean", ":module-b:quarkusAppPartsBuild", ":module-a:quarkusAppPartsBuild");
    }
}
