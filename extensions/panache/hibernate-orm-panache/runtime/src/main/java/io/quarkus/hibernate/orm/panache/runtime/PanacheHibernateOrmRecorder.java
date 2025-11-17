package io.quarkus.hibernate.orm.panache.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateOrmRecorder {
    public void addEntityTypesToPersistenceUnit(Map<String, String> entityToPersistenceUnit, boolean incomplete) {
        AbstractJpaOperations.addEntityTypesToPersistenceUnit(entityToPersistenceUnit, incomplete);
    }
}
