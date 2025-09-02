package io.quarkus.hibernate.orm;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.generator.Generator;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;

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

    public static String getColumnTypeName(EntityManagerFactory entityManagerFactory, Class<?> entityType,
            String columnName) {
        MappingMetamodel domainModel = entityManagerFactory
                .unwrap(SessionFactoryImplementor.class).getRuntimeMetamodels().getMappingMetamodel();
        EntityPersister entityDescriptor = domainModel.findEntityDescriptor(entityType);
        var columnFinder = new SelectableConsumer() {
            private SelectableMapping found;

            @Override
            public void accept(int selectionIndex, SelectableMapping selectableMapping) {
                if (found == null && selectableMapping.getSelectableName().equals(columnName)) {
                    found = selectableMapping;
                }
            }
        };
        entityDescriptor.forEachSelectable(columnFinder);
        return columnFinder.found.getJdbcMapping().getJdbcType().getFriendlyName();
    }

    public static Generator getGenerator(EntityManagerFactory entityManagerFactory, Class<?> entityType) {
        MappingMetamodel domainModel = entityManagerFactory
                .unwrap(SessionFactoryImplementor.class).getRuntimeMetamodels().getMappingMetamodel();
        EntityPersister entityDescriptor = domainModel.findEntityDescriptor(entityType);
        return entityDescriptor.getGenerator();
    }
}
