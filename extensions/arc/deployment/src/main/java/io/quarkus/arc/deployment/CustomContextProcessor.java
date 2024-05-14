package io.quarkus.arc.deployment;

import io.quarkus.arc.RegisterContext;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class CustomContextProcessor {

    @BuildStep
    void registerCustomContexts(
            CombinedIndexBuildItem combinedIndex,
            ContextRegistrationPhaseBuildItem phase,
            BuildProducer<ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem> customContexts) {
        var contexts = combinedIndex.getIndex().getAnnotations(RegisterContext.class);
        for (var context : contexts) {
            var scope = context.value().asClass();
            customContexts.produce(
                    new ContextRegistrationPhaseBuildItem.ContextConfiguratorBuildItem(
                            phase.getContext().configure(scope)
                                    .contextClass(context.target().asClass().name())));
        }
    }
}
