package org.jboss.shamrock.jpa;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shamrock.runtime.ConfigGroup;

import java.util.Optional;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ConfigGroup()
public class HibernateOrmConfig {
    /**
     * The hibernate ORM dialect class name
     */
    // TODO should it be dialects
    //TODO should it be shortcuts like "postgresql" "h2" etc
    @ConfigProperty(name = "dialect")
    public Optional<String> dialect;

    /**
     * Control how schema generation is happening in Hibernate ORM.
     * Same as JPA's javax.persistence.schema-generation.database.action.
     */
    @ConfigProperty(name = "schema-generation.database.action")
    public Optional<String> schemaGeneration;

    /**
     * Enable SQL logging (default to false)
     */
    @ConfigProperty(name="show_sql")
    public Optional<Boolean> showSql;

    public boolean isAnyPropertySet() {
        return dialect.isPresent() || schemaGeneration.isPresent() || showSql.isPresent();
    }

}
