package io.quarkus.deployment.dev.testing;

import java.util.function.Consumer;

public interface TestListener {

    default void listenerRegistered(TestController testController) {

    }

    default void testsEnabled() {
    }

    default void testsDisabled() {

    }

    default void testRunStarted(Consumer<TestRunListener> listenerConsumer) {

    }

    default void setBrokenOnly(boolean bo) {

    }

    default void setTestOutput(boolean to) {

    }

    default void setInstrumentationBasedReload(boolean ibr) {

    }

    default void testCompileFailed(String message) {

    }

    default void testCompileSucceeded() {

    }
}
