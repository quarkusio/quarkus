package io.quarkus.deployment.steps;

import static io.quarkus.deployment.util.ServiceUtil.classNamesNamedIn;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ValueRegistryRuntimeInfoProviderBuildItem;
import io.quarkus.registry.RuntimeInfoProvider;

class ValueRegistryProcessor {
    @BuildStep
    @SuppressWarnings("unchecked")
    void discoverRuntimeInfos(BuildProducer<ValueRegistryRuntimeInfoProviderBuildItem> runtimeInfoProviders) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (String service : classNamesNamedIn(classLoader, "META-INF/services/" + RuntimeInfoProvider.class.getName())) {
            if (QuarkusClassLoader.isClassPresentAtRuntime(service)) {
                Class<? extends RuntimeInfoProvider> provider = (Class<? extends RuntimeInfoProvider>) classLoader
                        .loadClass(service);
                runtimeInfoProviders.produce(new ValueRegistryRuntimeInfoProviderBuildItem(provider));
            }
        }
    }
}
