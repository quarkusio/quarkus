package io.quarkus.flyway;

import org.flywaydb.core.api.configuration.FluentConfiguration;

/**
 * Meant to be implemented by a CDI bean that provides arbitrary customization for Flyway's {@link FluentConfiguration}
 * <p>
 * When used without a {@link FlywayDataSource} qualifier, the bean configured the Flyway object which is applied to the
 * default datasource. If the qualifier is used, then only the Flyway object that applies to the corresponding named
 * datasource will be configured by the customizer.
 */
public interface FlywayConfigurationCustomizer {

    void customize(FluentConfiguration configuration);
}
