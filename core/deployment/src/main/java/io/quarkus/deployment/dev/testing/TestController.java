package io.quarkus.deployment.dev.testing;

public interface TestController {

    TestState currentState();

    void runAllTests();

    void setDisplayTestOutput(boolean displayTestOutput);

    void runFailedTests();
}
