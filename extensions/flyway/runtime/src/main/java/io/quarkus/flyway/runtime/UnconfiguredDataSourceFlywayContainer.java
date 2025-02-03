package io.quarkus.flyway.runtime;

import org.flywaydb.core.Flyway;

/**
 * @deprecated This is never instantiated by Quarkus. Do not use.
 */
@Deprecated(forRemoval = true)
public class UnconfiguredDataSourceFlywayContainer extends FlywayContainer {

    private final String message;
    private final Throwable cause;

    public UnconfiguredDataSourceFlywayContainer(String dataSourceName, String message, Throwable cause) {
        super(null, false, false, false, false, false, dataSourceName, "flyway", dataSourceName, false, false);
        this.message = message;
        this.cause = cause;
    }

    @Override
    public Flyway getFlyway() {
        throw new UnsupportedOperationException(message, cause);
    }
}
