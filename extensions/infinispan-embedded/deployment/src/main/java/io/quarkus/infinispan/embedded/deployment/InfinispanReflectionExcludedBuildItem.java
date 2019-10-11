package io.quarkus.infinispan.embedded.deployment;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * BuildItem that is used by extensions on top of Infinispan Embedded that can
 * dictate what classes should not be added to reflection list. This can be
 * used when an extension has additional classes that implement a given interface
 * that embedded looks for, but doesn't want it added to via Reflection. The end
 * result is that the classes are not loaded as reachable and can eliminate many
 * code paths if necessary. An example may be if indexing is disabled they may want
 * to remove indexing based classes from reflection list.
 */
public final class InfinispanReflectionExcludedBuildItem extends MultiBuildItem {

    private final DotName excludedClass;

    public InfinispanReflectionExcludedBuildItem(DotName excludedClass) {
        this.excludedClass = excludedClass;
    }

    public DotName getExcludedClass() {
        return excludedClass;
    }
}
