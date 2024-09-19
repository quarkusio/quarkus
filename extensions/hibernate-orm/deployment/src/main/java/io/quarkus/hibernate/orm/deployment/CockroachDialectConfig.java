package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.CockroachDialect}
 *
 * @author Steve Ebersole
 */
@ConfigGroup
public interface CockroachDialectConfig {
    /**
     * Specialized version string which can be passed into the {@linkplain org.hibernate.dialect.CockroachDialect}
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> versionString();

    default boolean isAnyPropertySet() {
        return versionString().isPresent();
    }
}
