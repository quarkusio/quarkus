package io.quarkus.hibernate.reactive.panache.runtime;

import java.util.Map;

import io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateReactiveRecorder {
    public void setEntityToPersistenceUnit(Map<String, String> entityToPersistenceUnit) {
        AbstractJpaOperations.setEntityToPersistenceUnit(entityToPersistenceUnit);
    }
}
