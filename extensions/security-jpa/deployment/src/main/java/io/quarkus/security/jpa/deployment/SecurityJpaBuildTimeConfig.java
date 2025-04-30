package io.quarkus.security.jpa.deployment;

import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigRoot
@ConfigMapping(prefix = "quarkus.security-jpa")
public interface SecurityJpaBuildTimeConfig {

    /**
     * Selects the Hibernate ORM persistence unit. Default persistence unit is used when no value is specified.
     */
    @WithDefault(DEFAULT_PERSISTENCE_UNIT_NAME)
    String persistenceUnitName();

}
