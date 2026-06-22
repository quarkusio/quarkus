package io.quarkus.hibernate.reactive.panache.common;

final class PanacheReactivePersistenceUnitDefaults {

    /**
     * Must match {@link io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil#DEFAULT_PERSISTENCE_UNIT_NAME}.
     */
    static final String DEFAULT_PERSISTENCE_UNIT_NAME = "<default>";

    private PanacheReactivePersistenceUnitDefaults() {
    }
}
