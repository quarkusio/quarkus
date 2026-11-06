package io.quarkus.hibernate.search.orm.elasticsearch.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class HibernateSearchOrmRootMappedClassesBuildItem extends SimpleBuildItem {

    private final Set<String> rootAnnotationMappedClassNames;

    public HibernateSearchOrmRootMappedClassesBuildItem(Set<String> rootAnnotationMappedClassNames) {
        this.rootAnnotationMappedClassNames = rootAnnotationMappedClassNames;
    }

    public Set<String> getRootAnnotationMappedClassNames() {
        return rootAnnotationMappedClassNames;
    }
}
