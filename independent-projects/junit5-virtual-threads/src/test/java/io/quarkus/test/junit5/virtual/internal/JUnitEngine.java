package io.quarkus.test.junit5.virtual.internal;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;
import static org.junit.platform.testkit.engine.EventConditions.*;

import org.assertj.core.api.Condition;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

public class JUnitEngine {

    public static void runTestAndAssertFailure(Class<?> clazz, String methodName, String message) {
        runTest(clazz, methodName).assertThatEvents()
                .haveExactly(1, event(test(methodName),
                        finishedWithFailure(new Condition<>(
                                throwable -> throwable instanceof AssertionError && throwable.getMessage().contains(message),
                                ""))));
    }

    public static void runTestAndAssertSuccess(Class<?> clazz, String methodName) {
        runTest(clazz, methodName).assertThatEvents()
                .haveExactly(1, event(test(methodName),
                        finishedSuccessfully()));
    }

    public static Events runTest(Class<?> clazz, String methodName) {
        return EngineTestKit
                .engine("junit-jupiter")
                .selectors(selectMethod(clazz, methodName))
                .execute()
                .testEvents();
    }

}
