package io.quarkus.hibernate.reactive;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.generator.Generator;
import org.hibernate.metamodel.MappingMetamodel;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.reactive.mutiny.Mutiny;

public final class SchemaUtil {

    private SchemaUtil() {
    }

    public static Set<String> getColumnNames(
            Class<?> entityType,
            MappingMetamodel metamodel) {
        Set<String> result = new HashSet<>();

        var persister = metamodel.getEntityDescriptor(entityType);
        if (persister == null) {
            return result;
        }
        for (String propertyName : persister.getPropertyNames()) {
            Collections.addAll(result, persister.getPropertyColumnNames(propertyName));
        }
        return result;
    }

    public static String getColumnTypeName(
            Class<?> entityType,
            String columnName,
            MappingMetamodel mappingMetaModel) {
        EntityPersister entityDescriptor = mappingMetaModel.findEntityDescriptor(entityType);
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

    public static Generator getGenerator(Class<?> entityType, MappingMetamodel mappingMetamodel) {
        EntityPersister entityDescriptor = mappingMetamodel.findEntityDescriptor(entityType);
        return entityDescriptor.getGenerator();
    }

    public static MappingMetamodel mappingMetamodel(Mutiny.SessionFactory sessionFactory) {
        return (MappingMetamodel) sessionFactory.getMetamodel();
    }
}
