package io.quarkus.hibernate.orm.deployment;

import java.util.Set;
import java.util.TreeSet;

public record JpaPersistenceUnitModel(Set<String> entityClassNames,
        Set<String> allModelClassAndPackageNames) {
    public JpaPersistenceUnitModel() {
        this(new TreeSet<>(), new TreeSet<>());
    }
}
