package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.SQLServerDialect}
 *
 * @author Steve Ebersole
 */
@ConfigGroup
public interface SqlServerDialectConfig {
    /**
     * The {@code compatibility_level} as defined in {@code sys.databases}.
     *
     * @see org.hibernate.cfg.DialectSpecificSettings#SQL_SERVER_COMPATIBILITY_LEVEL
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> compatibilityLevel();

    default boolean isAnyPropertySet() {
        return compatibilityLevel().isPresent();
    }
}
