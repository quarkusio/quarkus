package io.quarkus.panache.mock.impl;

import java.util.function.Consumer;

import org.jboss.jandex.Index;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.test.junit.buildchain.TestBuildChainCustomizerProducer;

public final class PanacheMockBuildChainCustomizer implements TestBuildChainCustomizerProducer {

    @Override
    public Consumer<BuildChainBuilder> produce(Index testClassesIndex) {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder buildChainBuilder) {
                buildChainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        LaunchModeBuildItem launchMode = context.consume(LaunchModeBuildItem.class);
                        if (launchMode.getLaunchMode() == LaunchMode.TEST) {
                            context.produce(new PanacheMethodCustomizerBuildItem(new PanacheMockMethodCustomizer()));
                        }
                    }
                }).produces(PanacheMethodCustomizerBuildItem.class)
                        .consumes(LaunchModeBuildItem.class)
                        .build();
            }
        };
    }
}
