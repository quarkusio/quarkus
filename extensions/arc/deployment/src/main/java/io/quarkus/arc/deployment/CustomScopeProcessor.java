package io.quarkus.arc.deployment;

import io.quarkus.arc.RegisterContext;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;

public class CustomScopeProcessor {

    @BuildStep
    void registerCustomScopes(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<CustomScopeBuildItem> customScopes) {
        var contexts = combinedIndex.getIndex().getAnnotations(RegisterContext.class);
        for (var context : contexts) {
            var scope = context.value().asClass();
            customScopes.produce(new CustomScopeBuildItem(scope.name()));
        }
    }
}
