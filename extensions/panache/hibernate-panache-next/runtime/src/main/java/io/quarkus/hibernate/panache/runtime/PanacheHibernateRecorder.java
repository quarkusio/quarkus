package io.quarkus.hibernate.panache.runtime;

import java.util.Map;

import io.quarkus.hibernate.orm.panache.common.runtime.AbstractJpaOperations;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class PanacheHibernateRecorder {
    public void setEntityToPersistenceUnit(Map<String, String> entityToPersistenceUnit, boolean incomplete,
            boolean hibernateReactivePresent) {
        AbstractJpaOperations.addEntityTypesToPersistenceUnit(entityToPersistenceUnit, incomplete);
        if (hibernateReactivePresent) {
            io.quarkus.hibernate.reactive.panache.common.runtime.AbstractJpaOperations
                    .addEntityTypesToPersistenceUnit(entityToPersistenceUnit);
        }
    }

    public void setRepositoryClassesToEntityClasses(Map<Class<?>, Class<?>> repositoryClassesToEntityClasses) {
        AbstractJpaOperations.setRepositoryClassesToEntityClasses(repositoryClassesToEntityClasses);
    }
}
