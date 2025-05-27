package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.OracleDialect}
 *
 * @author Steve Ebersole
 */
@ConfigGroup
public interface OracleDialectConfig {

    /**
     * Support for Oracle's MAX_STRING_SIZE = EXTENDED.
     *
     * @see org.hibernate.cfg.DialectSpecificSettings#ORACLE_EXTENDED_STRING_SIZE
     */
    @ConfigDocDefault("false")
    Optional<Boolean> extended();

    /**
     * Specifies whether this database is running on an Autonomous Database Cloud Service.
     *
     * @see org.hibernate.cfg.DialectSpecificSettings#ORACLE_AUTONOMOUS_DATABASE
     */
    @ConfigDocDefault("false")
    Optional<Boolean> autonomous();

    /**
     * Specifies whether this database is accessed using a database service protected by Application Continuity.
     *
     * @see org.hibernate.cfg.DialectSpecificSettings#ORACLE_APPLICATION_CONTINUITY
     */
    @ConfigDocDefault("false")
    Optional<Boolean> applicationContinuity();

    default boolean isAnyPropertySet() {
        return extended().isPresent()
                || autonomous().isPresent()
                || applicationContinuity().isPresent();
    }
}
