package io.quarkus.hibernate.orm.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.hibernate.orm.HibernateMetadata;
import io.quarkus.hibernate.orm.PersistenceUnitMetadata;

public class DefaultHibernateMetadata implements HibernateMetadata {

    private final Map<String, PersistenceUnitMetadata> map;

    public DefaultHibernateMetadata(final Set<String> defaultPersistentUnitEntities) {
        this.map = Collections.singletonMap(DEFAULT_PERSISTENCE_UNIT_NAME, new PersistenceUnitMetadata() {
            @Override
            public Set<String> getEntityClassNames() {
                return defaultPersistentUnitEntities;
            }
        });
    }

    @Override
    public Optional<PersistenceUnitMetadata> getPersistenceUnitMetadata(String name) {
        PersistenceUnitMetadata persistenceUnitMetadata = map.get(name);
        return persistenceUnitMetadata != null ? Optional.of(persistenceUnitMetadata) : Optional.empty();
    }
}
