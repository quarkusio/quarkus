package org.acme.examples;

import io.quarkus.deployment.annotations.BuildStep;

public class ClassWithBuildStep {
    @BuildStep
    ArbitraryBuildItem feature() {
        return new ArbitraryBuildItem();
    }
}
