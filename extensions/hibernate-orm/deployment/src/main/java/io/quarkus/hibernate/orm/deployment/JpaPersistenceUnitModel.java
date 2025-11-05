package io.quarkus.hibernate.orm.deployment;

import java.util.Set;
import java.util.TreeSet;

public record JpaPersistenceUnitModel(Set<String> entityClassNames,
        Set<String> allModelClassAndPackageNames, Set<String> allModelClassNamesOnly) {
    public JpaPersistenceUnitModel() {
        this(new TreeSet<>(), new TreeSet<>(), new TreeSet<>());
    }
}
