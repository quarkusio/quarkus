package io.quarkus.hibernate.orm.deployment;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;

public record JpaPersistenceUnitModel(Set<String> entityClassNames,
        Set<String> allModelClassAndPackageNames,
        List<RecordableXmlMapping> xmlMappings) {
    public JpaPersistenceUnitModel() {
        this(new TreeSet<>(), new TreeSet<>(), new ArrayList<>());
    }
}
