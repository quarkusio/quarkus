package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.SybaseDialect}
 *
 * @author Steve Ebersole
 */
@ConfigGroup
public interface SybaseDialectConfig {

    /**
     * Whether the database's {@code ansinull} setting is enabled
     *
     * @see org.hibernate.cfg.DialectSpecificSettings#SYBASE_ANSI_NULL
     */
    @SuppressWarnings("SpellCheckingInspection")
    @ConfigDocDefault("false")
    Optional<Boolean> ansinull();

    default boolean isAnyPropertySet() {
        return ansinull().isPresent();
    }
}
