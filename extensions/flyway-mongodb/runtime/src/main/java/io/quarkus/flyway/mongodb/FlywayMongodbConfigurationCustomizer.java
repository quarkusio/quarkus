package io.quarkus.flyway.mongodb;

import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Implemented by a CDI bean to provide arbitrary customization of Flyway's
 * {@link FluentConfiguration} before the {@link org.flywaydb.core.Flyway} instance is created.
 * <p>
 * When used without a {@link FlywayMongodbClient} qualifier, the bean configures the default
 * MongoDB client's Flyway instance. When the qualifier is present, only the matching named
 * client is configured.
 */
public interface FlywayMongodbConfigurationCustomizer {

    void customize(FluentConfiguration configuration);
}
