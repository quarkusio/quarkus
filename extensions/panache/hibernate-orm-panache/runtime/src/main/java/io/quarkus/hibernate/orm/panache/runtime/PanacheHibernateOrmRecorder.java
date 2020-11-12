package io.quarkus.hibernate.orm.panache.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateOrmRecorder {
    public void setEntityToPersistenceUnit(Map<String, String> entityToPersistenceUnit) {
        AbstractJpaOperations.setEntityToPersistenceUnit(entityToPersistenceUnit);
    }
}
