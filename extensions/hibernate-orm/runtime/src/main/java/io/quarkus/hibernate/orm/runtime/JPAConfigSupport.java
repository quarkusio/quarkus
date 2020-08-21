package io.quarkus.hibernate.orm.runtime;

import java.util.Map;
import java.util.Set;

import org.hibernate.MultiTenancyStrategy;

public class JPAConfigSupport {

    public Set<String> persistenceUnitNames;
    public Map<String, Set<String>> entityPersistenceUnitMapping;

    public MultiTenancyStrategy multiTenancyStrategy;
    public String multiTenancySchemaDataSource;

    public JPAConfigSupport() {
    }

    public JPAConfigSupport(Set<String> persistenceUnitNames,
            Map<String, Set<String>> entityPersistenceUnitMapping,
            MultiTenancyStrategy multiTenancyStrategy,
            String multiTenancySchemaDataSource) {
        this.persistenceUnitNames = persistenceUnitNames;
        this.entityPersistenceUnitMapping = entityPersistenceUnitMapping;
        this.multiTenancyStrategy = multiTenancyStrategy;
        this.multiTenancySchemaDataSource = multiTenancySchemaDataSource;
    }
}
