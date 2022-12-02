package io.quarkus.hibernate.orm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.AbstractEntityPersister;

public final class SchemaUtil {

    private SchemaUtil() {
    }

    public static Set<String> getColumnNames(EntityManagerFactory entityManagerFactory, Class<?> entityType) {
        Set<String> result = new HashSet<>();
        AbstractEntityPersister persister = (AbstractEntityPersister) entityManagerFactory
                .unwrap(SessionFactoryImplementor.class)
                .getMetamodel().entityPersister(entityType);
        if (persister == null) {
            return result;
        }
        for (String propertyName : persister.getPropertyNames()) {
            Collections.addAll(result, persister.getPropertyColumnNames(propertyName));
        }
        return result;
    }
}
