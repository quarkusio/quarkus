package io.quarkus.deployment.dev.testing;

import org.junit.platform.launcher.TestIdentifier;

public interface TestRunListener {

    default void runStarted(long toRun) {

    }

    default void testComplete(TestResult result) {

    }

    default void runComplete(TestRunResults results) {

    }

    default void runAborted() {

    }

    default void testStarted(TestIdentifier testIdentifier, String className) {

    }

    default void noTests(TestRunResults results) {

    }
}
