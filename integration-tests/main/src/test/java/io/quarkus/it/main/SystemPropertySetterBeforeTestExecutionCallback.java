package io.quarkus.it.main;

import io.quarkus.test.junit.callback.QuarkusTestBeforeTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class SystemPropertySetterBeforeTestExecutionCallback implements QuarkusTestBeforeTestExecutionCallback {

    @Override
    public void beforeTestExecution(QuarkusTestMethodContext context) {
        System.setProperty("quarkus.test.method", context.getTestMethod().getName());
    }
}
