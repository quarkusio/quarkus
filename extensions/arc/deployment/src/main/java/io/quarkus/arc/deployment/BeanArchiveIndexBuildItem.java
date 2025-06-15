package io.quarkus.arc.deployment;

import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represent a Jandex {@link IndexView} on the whole deployment that has a complete CDI-related information. As such,
 * this index should be used for any CDI-oriented work. Compared to
 * {@link io.quarkus.deployment.builditem.CombinedIndexBuildItem} this index can contain additional classes that were
 * indexed while bean discovery was in progress.
 *
 * @see GeneratedBeanBuildItem
 * @see io.quarkus.deployment.builditem.CombinedIndexBuildItem
 */
public final class BeanArchiveIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;
    private final IndexView immutableIndex;
    private final Set<DotName> generatedClassNames;

    public BeanArchiveIndexBuildItem(IndexView index, IndexView immutableIndex, Set<DotName> generatedClassNames) {
        this.index = index;
        this.immutableIndex = immutableIndex;
        this.generatedClassNames = generatedClassNames;
    }

    /**
     * This index is built on top of the immutable index.
     *
     * @return the computing index that can also index classes on demand
     */
    public IndexView getIndex() {
        return index;
    }

    /**
     * @return an immutable index that represents the bean archive
     */
    public IndexView getImmutableIndex() {
        return immutableIndex;
    }

    /**
     * @return the set of classes generated via {@link GeneratedBeanBuildItem}
     */
    public Set<DotName> getGeneratedClassNames() {
        return generatedClassNames;
    }

}
