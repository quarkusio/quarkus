package io.quarkus.runtime.test;

public interface TestScopeSetup {

    void setup(boolean isNativeImageTest);

    void tearDown(boolean isNativeImageTest);
}
