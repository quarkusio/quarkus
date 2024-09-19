package io.quarkus.hibernate.orm.deployment;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocDefault;
import io.quarkus.runtime.annotations.ConfigGroup;

/**
 * Configuration specific to the Hibernate ORM {@linkplain org.hibernate.dialect.HANADialect}
 *
 * @author Steve Ebersole
 */
@ConfigGroup
public interface HANADialectConfig {
    /**
     * Specifies the LOB prefetch size.
     *
     * @see org.hibernate.cfg.DialectSpecificSettings#HANA_MAX_LOB_PREFETCH_SIZE
     */
    @ConfigDocDefault("1024")
    Optional<Integer> maxLobPrefetchSize();

    default boolean isAnyPropertySet() {
        return maxLobPrefetchSize().isPresent();
    }
}
