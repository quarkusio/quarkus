package io.quarkus.resteasy.reactive.common.deployment;

import java.util.Set;

import org.jboss.jandex.DotName;

import io.quarkus.builder.item.SimpleBuildItem;

public final class AggregatedParameterContainersBuildItem extends SimpleBuildItem {

    /**
     * This contains all the parameter containers (bean param classes and records) as well as resources/endpoints
     */
    private final Set<DotName> classNames;
    /**
     * This contains all the non-record parameter containers (bean param classes only) as well as resources/endpoints
     */
    private final Set<DotName> nonRecordClassNames;

    public AggregatedParameterContainersBuildItem(Set<DotName> classNames, Set<DotName> nonRecordClassNames) {
        this.classNames = classNames;
        this.nonRecordClassNames = nonRecordClassNames;
    }

    /**
     * All class names
     */
    public Set<DotName> getClassNames() {
        return classNames;
    }

    /**
     * All class names minus the records
     */
    public Set<DotName> getNonRecordClassNames() {
        return nonRecordClassNames;
    }
}
