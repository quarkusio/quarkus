package io.quarkus.test.spring;

import java.util.function.Consumer;

import org.jboss.jandex.Index;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.deployment.builditem.TestAnnotationBuildItem;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;

/**
 * Registers {@link SpringBootTest} as a Quarkus test annotation so that test classes
 * annotated with it are treated as CDI beans by the Quarkus build pipeline, exactly as
 * test classes annotated with {@code @QuarkusTest} are.
 */
public class SpringBootTestBuildChainCustomizerProducer implements TestBuildChainCustomizerProducer {

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return buildChainBuilder -> buildChainBuilder
                .addBuildStep(context -> context.produce(new TestAnnotationBuildItem(SpringBootTest.class.getName())))
                .produces(TestAnnotationBuildItem.class)
                .build();
    }
}