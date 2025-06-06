package io.quarkus.hibernate.orm.runtime.customized;

import java.util.List;
import java.util.Set;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.SqlTypes;
import org.jboss.logging.Logger;

@Deprecated(since = "3.24", forRemoval = true)
public enum BuiltinFormatMapperBehaviour {
    /**
     * The Quarkus preconfigured mappers are ignored and if there is no user provided one,
     * Hibernate ORM will create a mapper according to its own rules.
     */
    IGNORE {
        @Override
        protected void action(String puName, String type) {
        }
    },
    /**
     * Currently the default one, uses a Quarkus preconfigured format mappers. If a format mapper operation is invoked a
     * warning is logged.
     */
    WARN {
        @Override
        protected void action(String puName, String type) {
            LOGGER.warnf(
                    "Persistence unit [%1$s] uses the Quarkus' builtin format mappers for %2$s properties. This may lead to undesired behavior. "
                            + "These mappers will be removed in the future version of Quarkus. See the migration guide for more details and how to proceed.",
                    puName, type);
        }
    },
    /**
     * If there is no user provided format mapper, a Quarkus preconfigured one will fail at runtime.
     * Will become the default in the future versions of Quarkus.
     */
    FAIL {
        @Override
        protected void action(String puName, String type) {
            throw new IllegalStateException(
                    "Using the Quarkus' builtin format mappers for JSON/XML properties is not allowed.");
        }
    };

    private static final Logger LOGGER = Logger.getLogger(BuiltinFormatMapperBehaviour.class);

    public static boolean hasJsonProperties(MetadataImplementor metadata) {
        return hasXxxProperties(metadata, Set.of(SqlTypes.JSON, SqlTypes.JSON_ARRAY));
    }

    public static boolean hasXmlProperties(MetadataImplementor metadata) {
        return hasXxxProperties(metadata, Set.of(SqlTypes.SQLXML, SqlTypes.XML_ARRAY));
    }

    private static boolean hasXxxProperties(MetadataImplementor metadata, Set<Integer> propertyTypeNames) {
        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            for (Property property : persistentClass.getProperties()) {
                List<Column> columns = property.getColumns();
                if (columns.isEmpty()) {
                    if (property.getValue() instanceof Collection c) {
                        columns = c.getElement().getColumns();
                    }
                }
                for (Column column : columns) {
                    if (propertyTypeNames.contains(column.getSqlTypeCode(metadata))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void jsonApply(MetadataImplementor metadata, String puName) {
        if (hasJsonProperties(metadata)) {
            action(puName, "JSON");
        }
    }

    public void xmlApply(MetadataImplementor metadata, String puName) {
        if (hasXmlProperties(metadata)) {
            action(puName, "XML");
        }
    }

    protected abstract void action(String puName, String type);
}
