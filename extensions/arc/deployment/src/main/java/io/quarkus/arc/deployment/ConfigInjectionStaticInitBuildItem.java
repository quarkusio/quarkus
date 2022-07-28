package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

public final class ConfigInjectionStaticInitBuildItem extends MultiBuildItem {
    private final DotName declaringCandidate;

    public ConfigInjectionStaticInitBuildItem(final DotName declaringCandidate) {
        this.declaringCandidate = declaringCandidate;
    }

    public DotName getDeclaringCandidate() {
        return declaringCandidate;
    }
}
