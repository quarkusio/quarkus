package io.quarkus.deployment.test;

public interface TestScopeSetup {

    void setup(boolean isSubstrateTest);

    void tearDown(boolean isSubstrateTest);
}
