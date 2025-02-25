package io.quarkus.panache.hibernate.common.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class HibernateModelClassCandidatesForFieldAccessBuildItem extends SimpleBuildItem {
    private final Set<String> managedClassNames;

    public HibernateModelClassCandidatesForFieldAccessBuildItem(Set<String> managedClassNames) {
        this.managedClassNames = managedClassNames;
    }

    public Set<String> getManagedClassNames() {
        return managedClassNames;
    }
}
