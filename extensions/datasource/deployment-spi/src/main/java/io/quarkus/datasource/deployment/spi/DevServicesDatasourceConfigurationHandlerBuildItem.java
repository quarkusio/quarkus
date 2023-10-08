package io.quarkus.datasource.deployment.spi;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.runtime.configuration.ConfigUtils;

/**
 * A handler that can map an automatic datasource to the relevant config properties.
 */
public final class DevServicesDatasourceConfigurationHandlerBuildItem extends MultiBuildItem {

    /**
     * The type of database this is for
     */
    private final String dbKind;
    /**
     * The function that provides the runtime config given a running DevServices database
     */
    private final BiFunction<String, DevServicesDatasourceProvider.RunningDevServicesDatasource, Map<String, String>> configProviderFunction;

    /**
     * Function that checks if a given datasource has been configured. If it has been configured generally the
     * DevServices will not be started.
     */
    private final Predicate<String> checkConfiguredFunction;

    public DevServicesDatasourceConfigurationHandlerBuildItem(String dbKind,
            BiFunction<String, DevServicesDatasourceProvider.RunningDevServicesDatasource, Map<String, String>> configProviderFunction,
            Predicate<String> checkConfiguredFunction) {
        this.dbKind = dbKind;
        this.configProviderFunction = configProviderFunction;
        this.checkConfiguredFunction = checkConfiguredFunction;
    }

    public BiFunction<String, DevServicesDatasourceProvider.RunningDevServicesDatasource, Map<String, String>> getConfigProviderFunction() {
        return configProviderFunction;
    }

    public String getDbKind() {
        return dbKind;
    }

    public Predicate<String> getCheckConfiguredFunction() {
        return checkConfiguredFunction;
    }

    public static DevServicesDatasourceConfigurationHandlerBuildItem jdbc(String dbKind) {
        return new DevServicesDatasourceConfigurationHandlerBuildItem(dbKind,
                new BiFunction<String, DevServicesDatasourceProvider.RunningDevServicesDatasource, Map<String, String>>() {

                    @Override
                    public Map<String, String> apply(String dsName,
                            DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevDb) {
                        String jdbcUrl = runningDevDb.jdbcUrl();
                        // we use datasourceURLPropNames to generate quoted and unquoted versions of the property key,
                        // because depending on whether a user configured other JDBC properties
                        // one of the URLs may be ignored
                        // see https://github.com/quarkusio/quarkus/issues/21387
                        return datasourceURLPropNames(dsName).stream()
                                .collect(Collectors.toMap(Function.identity(), ignored -> jdbcUrl));
                    }

                }, new Predicate<String>() {
                    @Override
                    public boolean test(String dsName) {
                        return ConfigUtils.isAnyPropertyPresent(datasourceURLPropNames(dsName));
                    }
                });
    }

    private static List<String> datasourceURLPropNames(String dsName) {
        return DataSourceUtil.dataSourcePropertyKeys(dsName, "jdbc.url");
    }

    public static DevServicesDatasourceConfigurationHandlerBuildItem reactive(String dbKind) {
        return new DevServicesDatasourceConfigurationHandlerBuildItem(dbKind,
                new BiFunction<String, DevServicesDatasourceProvider.RunningDevServicesDatasource, Map<String, String>>() {
                    @Override
                    public Map<String, String> apply(String dsName,
                            DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevDb) {
                        String reactiveUrl = runningDevDb.reactiveUrl();
                        // we use datasourceURLPropNames to generate quoted and unquoted versions of the property key,
                        // because depending on whether a user configured other reactive properties
                        // one of the URLs may be ignored
                        // see https://github.com/quarkusio/quarkus/issues/21387
                        return datasourceReactiveURLPropNames(dsName).stream()
                                .collect(Collectors.toMap(Function.identity(), ignored -> reactiveUrl));
                    }
                }, new Predicate<String>() {
                    @Override
                    public boolean test(String dsName) {
                        return ConfigUtils.isAnyPropertyPresent(datasourceReactiveURLPropNames(dsName));
                    }
                });
    }

    private static List<String> datasourceReactiveURLPropNames(String dsName) {
        return DataSourceUtil.dataSourcePropertyKeys(dsName, "reactive.url");
    }
}
