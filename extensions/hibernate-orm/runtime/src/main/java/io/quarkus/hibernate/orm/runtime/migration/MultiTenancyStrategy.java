package io.quarkus.hibernate.orm.runtime.migration;

import java.lang.invoke.MethodHandles;
import java.util.Locale;
import java.util.Map;

import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;

/**
 * This class is temporarily useful to facilitate the migration to Hibernate ORM 6;
 * we should get rid of it afterwards as we adapt to the new details of configuring multi-tenancy.
 * Copied from Hibernate ORM 5.x
 *
 * Describes the methods for multi-tenancy understood by Hibernate.
 *
 * @author Steve Ebersole
 * @deprecated This class should be removed after we're done migrating to Jakarta APIs and Hibernate ORM v6.
 */
@Deprecated
public enum MultiTenancyStrategy {
    /**
     * Multi-tenancy implemented by use of discriminator columns.
     */
    DISCRIMINATOR,
    /**
     * Multi-tenancy implemented as separate schemas.
     */
    SCHEMA,
    /**
     * Multi-tenancy implemented as separate databases.
     */
    DATABASE,
    /**
     * No multi-tenancy.
     */
    NONE;

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(MethodHandles.lookup(),
            CoreMessageLogger.class,
            MultiTenancyStrategy.class.getName());

    /**
     * Does this strategy indicate a requirement for the specialized
     * {@link org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider}, rather than the
     * traditional {@link org.hibernate.engine.jdbc.connections.spi.ConnectionProvider}?
     *
     * @return {@code true} indicates a MultiTenantConnectionProvider is required; {@code false} indicates it is not.
     */
    public boolean requiresMultiTenantConnectionProvider() {
        return this == DATABASE || this == SCHEMA;
    }

    /**
     * Extract the MultiTenancyStrategy from the setting map.
     *
     * @param properties The map of settings.
     *
     * @return The selected strategy. {@link #NONE} is always the default.
     */
    public static MultiTenancyStrategy determineMultiTenancyStrategy(Map properties) {
        final Object strategy = properties.get("hibernate.multiTenancy");//FIXME this property is meaningless in Hibernate ORM 6
        if (strategy == null) {
            return MultiTenancyStrategy.NONE;
        }

        if (MultiTenancyStrategy.class.isInstance(strategy)) {
            return (MultiTenancyStrategy) strategy;
        }

        final String strategyName = strategy.toString();
        try {
            return MultiTenancyStrategy.valueOf(strategyName.toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            LOG.warn("Unknown multi tenancy strategy [ " + strategyName + " ], using MultiTenancyStrategy.NONE.");
            return MultiTenancyStrategy.NONE;
        }
    }
}
