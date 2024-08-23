package io.quarkus.hibernate.orm.runtime.integration;

import java.util.List;
import java.util.function.BiConsumer;

import org.hibernate.boot.registry.StandardServiceInitiator;

public interface HibernateOrmIntegrationRuntimeInitListener {

    void contributeRuntimeProperties(BiConsumer<String, Object> propertyCollector);

    /**
     * Contributes services to be (re-)initiated at runtime.
     */
    default List<StandardServiceInitiator<?>> contributeServiceInitiators() {
        // Defaults to no-op
        return List.of();
    }

}
