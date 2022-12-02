package io.quarkus.test;

import java.util.Map;

import io.quarkus.builder.BuildStep;

// needs to be in a class of it's own in order to avoid java.lang.IncompatibleClassChangeError
public abstract class ProdModeTestBuildStep implements BuildStep {

    private final Map<String, Object> testContext;

    public ProdModeTestBuildStep(Map<String, Object> testContext) {
        this.testContext = testContext;
    }

    public Map<String, Object> getTestContext() {
        return testContext;
    }
}
