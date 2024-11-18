package io.quarkus.it.jpa.mapping.xml.legacy.app;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;

public final class SchemaUtil {

    private SchemaUtil() {
    }

    public static Set<String> getColumnNames(EntityManagerFactory entityManagerFactory, Class<?> entityType) {
        Set<String> result = new HashSet<>();
        var persister = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getMappingMetamodel()
                .getEntityDescriptor(entityType);
        if (persister == null) {
            return result;
        }
        for (String propertyName : persister.getPropertyNames()) {
            Collections.addAll(result, persister.getPropertyColumnNames(propertyName));
        }
        return result;
    }
}
