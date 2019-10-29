package io.quarkus.deployment.test;

public interface TestScopeSetup {

    void setup(boolean isNativeImageTest);

    void tearDown(boolean isNativeImageTest);
}
