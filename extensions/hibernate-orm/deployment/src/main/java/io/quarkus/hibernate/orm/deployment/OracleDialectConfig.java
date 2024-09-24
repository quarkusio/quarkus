package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.OracleDialect}
 */
@ConfigGroup
public interface OracleDialectConfig {

    /**
     * Support for Oracle's MAX_STRING_SIZE = EXTENDED.
     *
     * See
     * link:{hibernate-orm-javadocs-url}/org/hibernate/cfg/DialectSpecificSettings.html#ORACLE_EXTENDED_STRING_SIZE[ORACLE_EXTENDED_STRING_SIZE]
     *
     * @asciidoctor
     */
    @ConfigDocDefault("false")
    Optional<Boolean> extended();

    /**
     * Specifies whether this database is running on an Autonomous Database Cloud Service.
     *
     * See
     * link:{hibernate-orm-javadocs-url}/org/hibernate/cfg/DialectSpecificSettings.html#ORACLE_AUTONOMOUS_DATABASE[ORACLE_AUTONOMOUS_DATABASE]
     *
     * @asciidoctor
     */
    @ConfigDocDefault("false")
    Optional<Boolean> autonomous();

    /**
     * Specifies whether this database is accessed using a database service protected by Application Continuity.
     *
     * See
     * link:{hibernate-orm-javadocs-url}/org/hibernate/cfg/DialectSpecificSettings.html#ORACLE_APPLICATION_CONTINUITY[ORACLE_APPLICATION_CONTINUITY]
     *
     * @asciidoctor
     */
    @ConfigDocDefault("false")
    Optional<Boolean> applicationContinuity();

    default boolean isAnyPropertySet() {
        return extended().isPresent()
                || autonomous().isPresent()
                || applicationContinuity().isPresent();
    }
}
