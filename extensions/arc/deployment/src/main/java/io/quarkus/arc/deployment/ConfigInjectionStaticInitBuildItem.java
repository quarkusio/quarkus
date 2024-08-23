package io.quarkus.arc.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 *
 * @deprecated This build item is not used anymore
 */
@Deprecated(forRemoval = true)
public final class ConfigInjectionStaticInitBuildItem extends MultiBuildItem {
    private final DotName declaringCandidate;

    public ConfigInjectionStaticInitBuildItem(final DotName declaringCandidate) {
        this.declaringCandidate = declaringCandidate;
    }

    public DotName getDeclaringCandidate() {
        return declaringCandidate;
    }
}
