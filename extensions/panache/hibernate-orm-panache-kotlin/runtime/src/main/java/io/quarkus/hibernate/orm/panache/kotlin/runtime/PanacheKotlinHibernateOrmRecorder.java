package io.quarkus.hibernate.orm.panache.kotlin.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheKotlinHibernateOrmRecorder {
    public void setEntityToPersistenceUnit(Map<String, String> entityToPersistenceUnit) {
        JpaOperations.setEntityToPersistenceUnit(entityToPersistenceUnit);
    }
}
