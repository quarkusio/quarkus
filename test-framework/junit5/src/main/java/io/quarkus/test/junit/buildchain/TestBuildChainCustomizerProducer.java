package io.quarkus.test.junit.buildchain;

import java.util.function.Consumer;

import org.jboss.jandex.Index;

import io.quarkus.builder.BuildChainBuilder;

/**
 * Implementation of this class have the ability to add build items
 */
public interface TestBuildChainCustomizerProducer {

    Consumer<BuildChainBuilder> produce(Index testClassesIndex);
}
