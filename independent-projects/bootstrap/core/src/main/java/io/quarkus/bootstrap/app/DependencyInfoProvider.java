package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.resolver.maven.EffectiveModelResolver;

public interface DependencyInfoProvider {

    static DependencyInfoProviderBuilder builder() {
        return new DependencyInfoProviderBuilder();
    }

    default String getId() {
        return "default";
    }

    EffectiveModelResolver getMavenModelResolver();
}
