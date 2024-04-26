package org.acme.examples;

import io.quarkus.deployment.annotations.BuildStep;

public class ClassWithNoteworthyBuildItem {
    @BuildStep
    NoteworthyBuildItem devService() {
        return new NoteworthyBuildItem();
    }

    // This one shouldn't get recorded for use in the metadata
    @BuildStep
    ArbitraryBuildItem arbitrary() {
        return new ArbitraryBuildItem();
    }

    // This one shouldn't get recorded, obviously, and it should not cause runtime exceptions
    @BuildStep
    void boring() {
    }
}
