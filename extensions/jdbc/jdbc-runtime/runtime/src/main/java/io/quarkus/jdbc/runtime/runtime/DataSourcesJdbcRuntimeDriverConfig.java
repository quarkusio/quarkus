package io.quarkus.jdbc.runtime.runtime;

import java.util.Map;
import java.util.Optional;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefaults;
import io.smallrye.config.WithParentName;
import io.smallrye.config.WithUnnamedKey;

@ConfigMapping(prefix = "quarkus.datasource")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DataSourcesJdbcRuntimeDriverConfig {

    /**
     * Datasources.
     */
    @ConfigDocMapKey("datasource-name")
    @WithParentName
    @WithDefaults
    @WithUnnamedKey(DataSourceUtil.DEFAULT_DATASOURCE_NAME)
    Map<String, DataSourceJdbcRuntimeDriverOuterNamedConfig> dataSources();

    @ConfigGroup
    interface DataSourceJdbcRuntimeDriverOuterNamedConfig {

        /**
         * The configuration of the JDBC driver resolved at runtime.
         */
        DataSourceJdbcRuntimeDriverConfig jdbcRuntime();
    }

    @ConfigGroup
    interface DataSourceJdbcRuntimeDriverConfig {

        /**
         * The fully qualified name of the JDBC driver class to use for this datasource.
         * <p>
         * The class must be present in the application classpath and is loaded with reflection
         * from the TCCL when the datasource is created. Depending on the actual type of the class
         * ({@code java.sql.Driver}, {@code javax.sql.DataSource} or {@code javax.sql.XADataSource}
         * when XA transactions are enabled), the matching Agroal connection provider implementation
         * is used.
         * <p>
         * Only used when the database kind of the datasource is {@code runtime}.
         */
        Optional<String> driver();
    }
}
