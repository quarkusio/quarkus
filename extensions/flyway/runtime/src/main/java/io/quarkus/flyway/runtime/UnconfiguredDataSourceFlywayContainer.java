package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

public class UnconfiguredDataSourceFlywayContainer extends FlywayContainer {

    public UnconfiguredDataSourceFlywayContainer(String dataSourceName) {
        super(null, false, false, false, false, false, dataSourceName, false, false);
    }

    @Override
    public Flyway getFlyway() {
        throw new UnsupportedOperationException(
                "Cannot get a Flyway instance for unconfigured datasource " + getDataSourceName());
    }
}
