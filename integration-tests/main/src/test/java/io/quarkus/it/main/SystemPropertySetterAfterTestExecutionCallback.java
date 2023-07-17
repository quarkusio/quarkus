package io.quarkus.it.main;

import io.quarkus.test.junit.callback.QuarkusTestAfterTestExecutionCallback;
import io.quarkus.test.junit.callback.QuarkusTestMethodContext;

public class SystemPropertySetterAfterTestExecutionCallback implements QuarkusTestAfterTestExecutionCallback {

    @Override
    public void afterTestExecution(QuarkusTestMethodContext context) {
        System.clearProperty("quarkus.test.method");
    }
}
