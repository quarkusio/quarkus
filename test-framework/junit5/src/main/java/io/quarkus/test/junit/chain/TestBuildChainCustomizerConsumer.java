package io.quarkus.test.junit.chain;

import java.util.function.Consumer;

import io.quarkus.builder.BuildChainBuilder;

public interface TestBuildChainCustomizerConsumer extends Consumer<BuildChainBuilder> {

}
