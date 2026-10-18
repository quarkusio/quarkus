package io.quarkus.bootstrap.app;

import io.quarkus.bootstrap.resolver.maven.EffectiveModelResolver;

public class DependencyInfoProviderBuilder {

    private EffectiveModelResolver mavenModelResolver;

    public DependencyInfoProviderBuilder setMavenModelResolver(EffectiveModelResolver mavenModelResolver) {
        this.mavenModelResolver = mavenModelResolver;
        return this;
    }

    public DependencyInfoProvider build() {
        var mmr = mavenModelResolver;
        return new DependencyInfoProvider() {
            @Override
            public EffectiveModelResolver getMavenModelResolver() {
                return mmr;
            }
        };
    }
}
