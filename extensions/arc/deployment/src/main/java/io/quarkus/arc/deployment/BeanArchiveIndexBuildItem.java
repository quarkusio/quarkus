package io.quarkus.arc.deployment;

import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Represent a Jandex {@link IndexView} on the whole deployment that has a complete CDI-related information.
 * As such, this index should be used for any CDI-oriented work.
 *
 * Compared to {@link io.quarkus.deployment.builditem.CombinedIndexBuildItem} this index can contain additional classes
 * that were indexed while bean discovery was in progress.
 *
 * It also holds information about all programmatically registered beans and all generated bean classes.
 * 
 * @see GeneratedBeanBuildItem
 * @see AdditionalBeanBuildItem
 * @see io.quarkus.deployment.builditem.CombinedIndexBuildItem
 */
public final class BeanArchiveIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;
    private final Set<DotName> generatedClassNames;

    public BeanArchiveIndexBuildItem(IndexView index, Set<DotName> generatedClassNames) {
        this.index = index;
        this.generatedClassNames = generatedClassNames;
    }

    public IndexView getIndex() {
        return index;
    }

    public Set<DotName> getGeneratedClassNames() {
        return generatedClassNames;
    }

}
