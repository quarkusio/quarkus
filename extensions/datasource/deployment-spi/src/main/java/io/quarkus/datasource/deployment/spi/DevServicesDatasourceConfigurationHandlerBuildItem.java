package io.quarkus.datasource.deployment.spi;

import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import io.quarkus.builder.item.MultiBuildItem;
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
                        if (dsName == null) {
                            return Collections.singletonMap("quarkus.datasource.jdbc.url", runningDevDb.getUrl());
                        } else {
                            return Collections.singletonMap("quarkus.datasource.\"" + dsName + "\".jdbc.url",
                                    runningDevDb.getUrl());
                        }
                    }
                }, new Predicate<String>() {
                    @Override
                    public boolean test(String dsName) {
                        if (dsName == null) {
                            return ConfigUtils.isPropertyPresent("quarkus.datasource.jdbc.url");
                        } else {
                            return ConfigUtils.isPropertyPresent("quarkus.datasource.\"" + dsName + "\".jdbc.url") ||
                                    ConfigUtils.isPropertyPresent("quarkus.datasource." + dsName + ".jdbc.url");
                        }
                    }
                });
    }

    public static DevServicesDatasourceConfigurationHandlerBuildItem reactive(String dbKind) {
        return new DevServicesDatasourceConfigurationHandlerBuildItem(dbKind,
                new BiFunction<String, DevServicesDatasourceProvider.RunningDevServicesDatasource, Map<String, String>>() {
                    @Override
                    public Map<String, String> apply(String dsName,
                            DevServicesDatasourceProvider.RunningDevServicesDatasource runningDevDb) {
                        if (dsName == null) {
                            return Collections.singletonMap("quarkus.datasource.reactive.url",
                                    runningDevDb.getUrl().replaceFirst("jdbc:", "vertx-reactive:"));
                        } else {
                            return Collections.singletonMap("quarkus.datasource.\"" + dsName + "\".reactive.url",
                                    runningDevDb.getUrl().replaceFirst("jdbc:", "vertx-reactive:"));
                        }
                    }
                }, new Predicate<String>() {
                    @Override
                    public boolean test(String dsName) {
                        if (dsName == null) {
                            return ConfigUtils.isPropertyPresent("quarkus.datasource.reactive.url");
                        } else {
                            return ConfigUtils.isPropertyPresent("quarkus.datasource.\"" + dsName + "\".reactive.url") ||
                                    ConfigUtils.isPropertyPresent("quarkus.datasource." + dsName + ".reactive.url");
                        }
                    }
                });
    }
}
