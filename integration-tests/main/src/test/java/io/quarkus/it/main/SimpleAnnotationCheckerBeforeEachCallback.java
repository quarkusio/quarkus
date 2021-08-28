package io.quarkus.it.main;

import java.lang.reflect.Method;

import io.quarkus.test.junit.callback.QuarkusTestBeforeEachCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class SimpleAnnotationCheckerBeforeEachCallback implements QuarkusTestBeforeEachCallback {

    static boolean testAnnotationChecked;

    @Override
    public void beforeEach(QuarkusTestMethodContext context) {
        // make sure that this comes into play only for the test we care about

        Method testMethod = context.getTestMethod();
        if (!testMethod.getDeclaringClass().getName().endsWith("QuarkusTestCallbacksTestCase")) {
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
