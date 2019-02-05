package org.jboss.shamrock.jpa;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

import java.util.Optional;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ConfigRoot
public class HibernateConfig {
    /**
     * The hibernate ORM dialect class name
     */
    // TODO should it be dialects
    //TODO should it be shortcuts like "postgresql" "h2" etc
    @ConfigItem
    public Optional<String> dialect;

    /**
     * Control how schema generation is happening in Hibernate ORM.
     * Same as JPA's javax.persistence.schema-generation.database.action.
     */
    @ConfigItem(name = "schema-generation.database.action")
    public Optional<String> schemaGeneration;

    /**
     * Enable SQL logging (default to false)
     */
    @ConfigItem
    public boolean showSql;

    /**
     * To populate the database tables with data before the application loads,
     * specify the location of a load script.
     * The location specified in this property is relative to the root of the persistence unit.
     */
    @ConfigItem
    public Optional<String> sqlLoadScriptSource;

    public boolean isAnyPropertySet() {
        return dialect.isPresent() || schemaGeneration.isPresent() || showSql || sqlLoadScriptSource.isPresent();
    }

}
