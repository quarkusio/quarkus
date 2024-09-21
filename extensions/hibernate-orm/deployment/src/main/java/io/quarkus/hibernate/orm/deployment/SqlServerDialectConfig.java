package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.SQLServerDialect}
 */
@ConfigGroup
public interface SqlServerDialectConfig {
    /**
     * The {@code compatibility_level} as defined in {@code sys.databases}.
     *
     * See link:{hibernate-orm-javadocs-url}/org/hibernate/cfg/DialectSpecificSettings.html#SQL_SERVER_COMPATIBILITY_LEVEL[SQL_SERVER_COMPATIBILITY_LEVEL]
     *
     * @asciidoctor
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> compatibilityLevel();

    default boolean isAnyPropertySet() {
        return compatibilityLevel().isPresent();
    }
}
