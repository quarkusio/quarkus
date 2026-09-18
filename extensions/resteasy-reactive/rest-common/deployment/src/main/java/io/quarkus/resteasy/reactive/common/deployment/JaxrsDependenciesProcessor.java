package io.quarkus.resteasy.reactive.common.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.modular.spi.items.AddDependencyBuildItem;
import io.smallrye.modules.desc.Dependency;

public class JaxrsDependenciesProcessor {
    @BuildStep
    AddDependencyBuildItem addDependency() {
        // make sure that `jakarta.ws.rs` can see our implementation
        return new AddDependencyBuildItem("jakarta.ws.rs", "io.quarkus.resteasy.reactive.common",
            Dependency.Modifier.Set.of(Dependency.Modifier.SERVICES, Dependency.Modifier.OPTIONAL));
    }
}
