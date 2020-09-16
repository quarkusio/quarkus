package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.Set;

public class JPAConfigSupport {

    public Set<String> persistenceUnitNames;
    public Map<String, Set<String>> entityPersistenceUnitMapping;

    public JPAConfigSupport() {
    }

    public JPAConfigSupport(Set<String> persistenceUnitNames,
            Map<String, Set<String>> entityPersistenceUnitMapping) {
        this.persistenceUnitNames = persistenceUnitNames;
        this.entityPersistenceUnitMapping = entityPersistenceUnitMapping;
    }
}
