package io.quarkus.hibernate.orm.runtime;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import io.quarkus.hibernate.orm.HibernateMetadata;
import io.quarkus.hibernate.orm.PersistenceUnitMetadata;

public class DefaultHibernateMetadata implements HibernateMetadata {

    private final Map<String, PersistenceUnitMetadata> map = new HashMap<>();

    public DefaultHibernateMetadata(final Set<String> defaultPersistentUnitEntityNames) {
        this.map.put(DEFAULT_PERSISTENCE_UNIT_NAME, new DefaultPersistenceUnitMetadata(defaultPersistentUnitEntityNames));
    }

    @Override
    public Optional<PersistenceUnitMetadata> getPersistenceUnitMetadata(String name) {
        PersistenceUnitMetadata persistenceUnitMetadata = map.get(name);
        return persistenceUnitMetadata != null ? Optional.of(persistenceUnitMetadata) : Optional.empty();
    }

}
