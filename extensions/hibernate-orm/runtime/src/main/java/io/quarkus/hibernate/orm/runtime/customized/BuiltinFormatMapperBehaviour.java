package io.quarkus.hibernate.orm.runtime.customized;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.java.spi.JsonJavaType;
import org.hibernate.type.descriptor.java.spi.XmlJavaType;
import org.jboss.logging.Logger;

import io.quarkus.arc.ArcContainer;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;

public enum BuiltinFormatMapperBehaviour {
    /**
     * The Quarkus preconfigured mappers are ignored and if there is no user provided one,
     * Hibernate ORM will create a mapper according to its own rules.
     *
     * @asciidoclet
     */
    IGNORE {
        @Override
        protected void action(String puName, String type, List<String> causes) {
        }
    },
    /**
     * Uses a Quarkus preconfigured format mappers. If a format mapper operation is invoked a
     * warning is logged.
     *
     * @asciidoclet
     */
    WARN {
        @Override
        protected void action(String puName, String type, List<String> causes) {
            LOGGER.warn(message(puName, type, causes));
        }
    },
    /**
     * Currently the default one. If there is no user provided format mapper, a Quarkus preconfigured one will fail at runtime.
     *
     * @asciidoclet
     */
    FAIL {
        @Override
        protected void action(String puName, String type, List<String> causes) {
            throw new IllegalStateException(message(puName, type, causes));
        }
    };

    private static final Logger LOGGER = Logger.getLogger(BuiltinFormatMapperBehaviour.class);
    private static final String TYPE_JSON = "JSON";
    private static final String TYPE_XML = "XML";

    private static String message(String puName, String type, List<String> causes) {
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
                        + " to address your database serialization/deserialization needs."
                        + "\nThe precise causes for this failure are: \n\t- "
                        + String.join("\n\t- ", causes)
                        + "\nSee the migration guide for more details and how to proceed.",
                puName, type);
    }

    public static boolean hasJsonProperties(MetadataImplementor metadata) {
        AtomicBoolean hasJsonProperties = new AtomicBoolean(false);
        metadata.getTypeConfiguration().getJavaTypeRegistry().forEachDescriptor(javaType -> {
            if (javaType instanceof JsonJavaType<?>) {
                hasJsonProperties.set(true);
            }
        });
        if (hasJsonProperties.get()) {
            return true;
        } else {
            // for JSON_ARRAY we need to check the jdbc type registry instead
            return metadata.getTypeConfiguration().getJdbcTypeRegistry().hasRegisteredDescriptor(SqlTypes.JSON_ARRAY);
        }
    }

    public static boolean hasXmlProperties(MetadataImplementor metadata) {
        AtomicBoolean hasXmlProperties = new AtomicBoolean(false);
        metadata.getTypeConfiguration().getJavaTypeRegistry().forEachDescriptor(javaType -> {
            if (javaType instanceof XmlJavaType<?>) {
                hasXmlProperties.set(true);
            }
        });
        if (hasXmlProperties.get()) {
            return true;
        } else {
            // for XML_ARRAY we need to check the jdbc type registry instead
            return metadata.getTypeConfiguration().getJdbcTypeRegistry().hasRegisteredDescriptor(SqlTypes.XML_ARRAY);
        }
    }

    public void jsonApply(MetadataImplementor metadata, String puName, ArcContainer container,
            JsonFormatterCustomizationCheck check) {
        if (hasJsonProperties(metadata)) {
            List<String> causes = check.apply(container);
            if (!causes.isEmpty()) {
                action(puName, TYPE_JSON, causes);
            }
        }
    }

    public void xmlApply(MetadataImplementor metadata, String puName) {
        // XML mapper can only be a JAXB based one. With Hibernate ORM 7.0 there was a change in the mapper:
        // org.hibernate.type.format.jaxb.JaxbXmlFormatMapper -- where a new format was introduced
        // and legacy one would be currently used for Quarkus. If we just bypass the built-in one, we will break the user data.
        // There is:
        //   XML_FORMAT_MAPPER_LEGACY_FORMAT = "hibernate.type.xml_format_mapper.legacy_format"
        // for "migration" purposes.
        //
        // Let's fail and tell the user to migrate their data to the new format and before that is done: use a delegate to org.hibernate.type.format.jaxb.JaxbXmlFormatMapper()
        // using a legacy format:
        if (hasXmlProperties(metadata)) {
            action(puName, TYPE_XML,
                    List.of("The XML format mapper uses the legacy format. It is not compatible with the new default one."));
        }
    }

    protected abstract void action(String puName, String type, List<String> causes);
}
