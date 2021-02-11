package io.quarkus.hibernate.orm.panache.kotlin.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheKotlinHibernateOrmRecorder {
    public void setEntityToPersistenceUnit(Map<String, String> entityToPersistenceUnit) {
        AbstractJpaOperations.setEntityToPersistenceUnit(entityToPersistenceUnit);
    }
}
