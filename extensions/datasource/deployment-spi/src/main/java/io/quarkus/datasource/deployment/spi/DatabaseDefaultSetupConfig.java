package io.quarkus.datasource.deployment.spi;

public final class DatabaseDefaultSetupConfig {

    private DatabaseDefaultSetupConfig() {
    }

    public static final String DEFAULT_DATABASE_USERNAME = "quarkus";

    public static final String DEFAULT_DATABASE_PASSWORD = "quarkus";
    // mssql container enforces a 'strong' password with min 8 chars, upper/lowercase, number or special char
    public static final String DEFAULT_DATABASE_STRONG_PASSWORD = "Quarkus123";

    public static final String DEFAULT_DATABASE_NAME = "quarkus";
}
