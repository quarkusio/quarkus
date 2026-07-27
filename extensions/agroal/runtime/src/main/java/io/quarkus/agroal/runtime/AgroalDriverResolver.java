package io.quarkus.agroal.runtime;

/**
 * Allows a JDBC driver extension to resolve the driver class name of a datasource at runtime,
 * overriding the driver class name resolved at build time.
 * <p>
 * Implementations must be exposed as CDI beans qualified with {@link JdbcDriver} for the database
 * kind they support. When no bean matches the resolved database kind of a datasource, the driver
 * class name resolved at build time is used as-is.
 * <p>
 * The returned class name is loaded from the {@link Thread#getContextClassLoader() TCCL} and passed
 * to Agroal which selects the proper connection provider implementation based on the actual type
 * ({@link java.sql.Driver}, {@link javax.sql.DataSource} or {@link javax.sql.XADataSource}).
 */
public interface AgroalDriverResolver {

    /**
     * Resolves the driver class name to use for the given datasource.
     *
     * @param dataSourceName the name of the datasource being created
     * @param buildTimeDriverClass the driver class name resolved at build time
     * @return the fully qualified name of the driver class to load, never {@code null}
     */
    String resolveDriverClassName(String dataSourceName, String buildTimeDriverClass);
}
