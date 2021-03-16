package io.quarkus.panache.common.deployment;

import java.util.Set;

import io.quarkus.builder.item.SimpleBuildItem;

public final class HibernateModelClassCandidatesForFieldAccessBuildItem extends SimpleBuildItem {
    private final Set<String> allModelClassNames;

    public HibernateModelClassCandidatesForFieldAccessBuildItem(Set<String> allModelClassNames) {
        this.allModelClassNames = allModelClassNames;
    }

    public Set<String> getAllModelClassNames() {
        return allModelClassNames;
    }
}
