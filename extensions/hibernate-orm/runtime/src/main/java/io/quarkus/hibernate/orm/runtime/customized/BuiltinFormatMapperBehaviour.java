package io.quarkus.hibernate.orm.runtime.customized;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.mapping.Collection;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Property;
import org.hibernate.type.SqlTypes;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

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
            LOGGER.warn(message(puName, type));
        }
    },
    /**
     * If there is no user provided format mapper, a Quarkus preconfigured one will fail at runtime.
     * Will become the default in the future versions of Quarkus.
     */
    FAIL {
        @Override
        protected void action(String puName, String type) {
            throw new IllegalStateException(message(puName, type));
        }
    };

    private static final Logger LOGGER = Logger.getLogger(BuiltinFormatMapperBehaviour.class);
    private static final String TYPE_JSON = "JSON";
    private static final String TYPE_XML = "XML";

    private static String message(String puName, String type) {
        return String.format(Locale.ROOT,
                "Persistence unit [%1$s] uses Quarkus' main formatting facilities for %2$s columns in the database. "
                        + "\nAs these facilities are primarily meant for REST endpoints, and they might have been customized for such use, "
                        + "this may lead to undesired behavior, up to and including data loss. "
                        + "\nTo address this:"
                        + "\n\t- If the application does not customize the %2$s serialization/deserialization, set \"quarkus.hibernate-orm.mapping.format.global=ignore\". This will be the default in future versions of Quarkus. "
                        + "\n\t- Otherwise, define a custom `FormatMapper` bean annotated with "
                        + (TYPE_JSON.equals(type) ? "@JsonFormat" : "@XmlFormat")
                        + " and @PersistenceUnitExtension"
                        + (PersistenceUnitUtil.isDefaultPersistenceUnit(puName) ? "" : "(\"%1$s\")")
                        + "to address your database serialization/deserialization needs."
                        + "\nSee the migration guide for more details and how to proceed.",
                puName, type);
    }

    public static boolean hasJsonProperties(MetadataImplementor metadata) {
        return hasXxxProperties(metadata, Set.of(SqlTypes.JSON, SqlTypes.JSON_ARRAY));
    }

    public static boolean hasXmlProperties(MetadataImplementor metadata) {
        return hasXxxProperties(metadata, Set.of(SqlTypes.SQLXML, SqlTypes.XML_ARRAY));
    }

    private static boolean hasXxxProperties(MetadataImplementor metadata, Set<Integer> sqlTypeCodes) {
        for (PersistentClass persistentClass : metadata.getEntityBindings()) {
            for (Property property : persistentClass.getProperties()) {
                List<Column> columns = property.getColumns();
                if (columns.isEmpty()) {
                    if (property.getValue() instanceof Collection c) {
                        columns = c.getElement().getColumns();
                    }
                }
                for (Column column : columns) {
                    if (sqlTypeCodes.contains(column.getSqlTypeCode(metadata))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public void jsonApply(MetadataImplementor metadata, String puName) {
        if (hasJsonProperties(metadata)) {
            action(puName, TYPE_JSON);
        }
    }

    public void xmlApply(MetadataImplementor metadata, String puName) {
        if (hasXmlProperties(metadata)) {
            action(puName, TYPE_XML);
        }
    }

    protected abstract void action(String puName, String type);
}
