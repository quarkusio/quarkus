package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

public class UnconfiguredDataSourceFlywayContainer extends FlywayContainer {

    private final String message;
    private final Throwable cause;

    public UnconfiguredDataSourceFlywayContainer(String dataSourceName, String name, String message, Throwable cause) {
        super(null, null, false, false, false, false, false, dataSourceName, name, false, false, false);
        this.message = message;
        this.cause = cause;
    }

    @Override
    public Flyway getFlyway() {
        throw new UnsupportedOperationException(message, cause);
    }
}
