package io.quarkus.runtime.test;

public interface TestScopeSetup {

    void setup(boolean isIntegrationTest);

    void tearDown(boolean isIntegrationTest);
}
