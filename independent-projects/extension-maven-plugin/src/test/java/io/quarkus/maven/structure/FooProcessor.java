package io.quarkus.maven.structure;

import io.quarkus.deployment.annotations.BuildStep;

public class FooProcessor {

    @BuildStep
    void feature() {
    }

    static class Nested {
    }
}
