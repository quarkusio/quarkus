package io.quarkus.it.main;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class TestContextCheckerBeforeEachCallback implements QuarkusTestBeforeEachCallback {

    public static final List<Object> OUTER_INSTANCES = new ArrayList<>();
    public static QuarkusTestMethodContext CONTEXT;

    static boolean testAnnotationChecked;

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        OUTER_INSTANCES.clear();
        OUTER_INSTANCES.addAll(context.getOuterInstances());

        // continue only if this comes into play only for the test we care about
        TestContextCheckerBeforeEachCallback.CONTEXT = context;
        // make sure that this comes into play only for the test we care about

        Method testMethod = context.getTestMethod();
        if (!testMethod.getDeclaringClass().getName().startsWith("io.quarkus.it.main.QuarkusTestCallbacks")) {
            return;
        }

        if (!testMethod.getName().equals("testTestMethodHasAnnotation")) {
            return;
        }

        QuarkusTestCallbacksTestCase.TestAnnotation annotation = testMethod
                .getAnnotation(QuarkusTestCallbacksTestCase.TestAnnotation.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Expected to find annotation @TestAnnotation on method test method testTestMethodHasAnnotation");
        }
        testAnnotationChecked = true;
    }
}
