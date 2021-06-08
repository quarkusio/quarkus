package io.quarkus.deployment.dev.testing;

import java.util.function.BiConsumer;

import io.quarkus.builder.BuildResult;
import io.quarkus.dev.console.QuarkusConsole;

public class TestHandler implements BiConsumer<Object, BuildResult> {
    @Override
    public void accept(Object o, BuildResult buildResult) {
        QuarkusConsole.start();
        TestSupport.instance().get().start();
    }
}
