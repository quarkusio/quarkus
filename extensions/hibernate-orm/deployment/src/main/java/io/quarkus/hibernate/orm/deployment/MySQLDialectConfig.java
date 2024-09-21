package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.configuration.TrimmedStringConverter;
import io.smallrye.config.WithConverter;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.MySQLDialect}
 * and {@linkplain org.hibernate.dialect.MariaDBDialect}.
 */
@ConfigGroup
public interface MySQLDialectConfig {
    /**
     * Specifies the bytes per character to use based on the database's configured
     * <a href="https://dev.mysql.com/doc/refman/8.0/en/charset-charsets.html">charset</a>.
     *
     * See link:{hibernate-orm-javadocs-url}/org/hibernate/cfg/DialectSpecificSettings.html#MYSQL_BYTES_PER_CHARACTER[MYSQL_BYTES_PER_CHARACTER]
     *
     * @asciidoctor
     */
    @ConfigDocDefault("4")
    Optional<Integer> bytesPerCharacter();

    /**
     * Specifies whether the {@code NO_BACKSLASH_ESCAPES} sql mode is enabled.
     *
     * See link:{hibernate-orm-javadocs-url}/org/hibernate/cfg/DialectSpecificSettings.html#MYSQL_NO_BACKSLASH_ESCAPES[MYSQL_NO_BACKSLASH_ESCAPES]
     *
     * @asciidoctor
     */
    @ConfigDocDefault("false")
    Optional<Boolean> noBackslashEscapes();

    /**
     * The storage engine to use.
     */
    @WithConverter(TrimmedStringConverter.class)
    Optional<String> storageEngine();

    default boolean isAnyPropertySet() {
        return bytesPerCharacter().isPresent()
                || noBackslashEscapes().isPresent()
                || storageEngine().isPresent();
    }
}
