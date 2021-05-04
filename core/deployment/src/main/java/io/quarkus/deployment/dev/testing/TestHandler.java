package io.quarkus.deployment.dev.testing;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;

public class TestHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        TestSupport.instance().get().start();

    }
}
