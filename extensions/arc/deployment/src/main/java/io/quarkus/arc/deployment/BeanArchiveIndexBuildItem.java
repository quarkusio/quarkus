package io.quarkus.arc.deployment;

import java.util.List;
import java.util.Set;

import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;

import io.quarkus.builder.item.SimpleBuildItem;

public final class BeanArchiveIndexBuildItem extends SimpleBuildItem {

    private final IndexView index;
    private final Set<DotName> generatedClassNames;
    private final List<String> additionalBeans;

    public BeanArchiveIndexBuildItem(IndexView index, Set<DotName> generatedClassNames,
            List<String> additionalBeans) {
        this.index = index;
        this.generatedClassNames = generatedClassNames;
        this.additionalBeans = additionalBeans;
    }

    public IndexView getIndex() {
        return index;
    }

    public Set<DotName> getGeneratedClassNames() {
        return generatedClassNames;
    }

    public List<String> getAdditionalBeans() {
        return additionalBeans;
    }

}
