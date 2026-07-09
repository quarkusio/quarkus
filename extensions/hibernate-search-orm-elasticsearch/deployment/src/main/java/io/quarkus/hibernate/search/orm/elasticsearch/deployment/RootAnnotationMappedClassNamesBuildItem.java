package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

final class RootAnnotationMappedClassNamesBuildItem extends SimpleBuildItem {

    private final Set<String> classNames;

    RootAnnotationMappedClassNamesBuildItem(Set<String> classNames) {
        this.classNames = classNames;
    }

    Set<String> getClassNames() {
        return classNames;
    }
}
