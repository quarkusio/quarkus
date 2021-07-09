package io.quarkus.datasource.deployment.devservices;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.IsDockerWorking;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesNativeConfigResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.LaunchMode;

public class DevServicesDatasourceProcessor {

    private static final Logger log = Logger.getLogger(DevServicesDatasourceProcessor.class);

    static volatile List<Closeable> databases;

    static volatile Map<String, String> cachedProperties;

    static volatile List<RunTimeConfigurationDefaultBuildItem> databaseConfig;

    static volatile boolean first = true;

    private final IsDockerWorking isDockerWorking = new IsDockerWorking(true);

    @BuildStep(onlyIfNot = IsNormal.class)
    DevServicesDatasourceResultBuildItem launchDatabases(CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers,
            List<DevServicesDatasourceProviderBuildItem> devDBProviders,
            DataSourcesBuildTimeConfig dataSourceBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
            List<DevServicesDatasourceConfigurationHandlerBuildItem> configurationHandlerBuildItems,
            BuildProducer<DevServicesNativeConfigResultBuildItem> devServicesResultBuildItemBuildProducer,
            BuildProducer<ServiceStartBuildItem> serviceStartBuildItemBuildProducer) {
        //figure out if we need to shut down and restart existing databases
        //if not and the DB's have already started we just return
        if (databases != null) {
            boolean restartRequired = false;
            if (!restartRequired) {
                for (Map.Entry<String, String> i : cachedProperties.entrySet()) {
                    if (!Objects.equals(i.getValue(),
                            ConfigProvider.getConfig().getOptionalValue(i.getKey(), String.class).orElse(null))) {
                        restartRequired = true;
                        break;
                    }
                }
            }
            if (!restartRequired) {
                for (RunTimeConfigurationDefaultBuildItem i : databaseConfig) {
                    runTimeConfigurationDefaultBuildItemBuildProducer.produce(i);
                }
                return null;
            }
            for (Closeable i : databases) {
                try {
                    i.close();
                } catch (Throwable e) {
                    log.error("Failed to stop database", e);
                }
            }
            databases = null;
            cachedProperties = null;
            databaseConfig = null;
        }
        DevServicesDatasourceResultBuildItem.DbResult defaultResult;
        Map<String, DevServicesDatasourceResultBuildItem.DbResult> namedResults = new HashMap<>();
        //now we need to figure out if we need to launch some databases
        //note that because we run in dev and test mode only we know the runtime
        //config at build time, as they both execute in the same JVM

        //to keep things simpler for now we are only going to support this for the default datasource
        //support for named datasources will come later

        Map<String, String> propertiesMap = new HashMap<>();
        List<Closeable> closeableList = new ArrayList<>();
        Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configHandlersByDbType = configurationHandlerBuildItems
                .stream()
                .collect(Collectors.toMap(DevServicesDatasourceConfigurationHandlerBuildItem::getDbKind,
                        Collections::singletonList,
                        (configurationHandlerBuildItems1, configurationHandlerBuildItems2) -> {
                            List<DevServicesDatasourceConfigurationHandlerBuildItem> ret = new ArrayList<>();
                            ret.addAll(configurationHandlerBuildItems1);
                            ret.addAll(configurationHandlerBuildItems2);
                            return ret;
                        }));
        Map<String, DevServicesDatasourceProvider> devDBProviderMap = devDBProviders.stream()
                .collect(Collectors.toMap(DevServicesDatasourceProviderBuildItem::getDatabase,
                        DevServicesDatasourceProviderBuildItem::getDevServicesProvider));

        defaultResult = startDevDb(null, curateOutcomeBuildItem, installedDrivers,
                !dataSourceBuildTimeConfig.namedDataSources.isEmpty(),
                devDBProviderMap,
                dataSourceBuildTimeConfig.defaultDataSource,
                configHandlersByDbType, propertiesMap, closeableList, launchMode.getLaunchMode());
        List<RunTimeConfigurationDefaultBuildItem> dbConfig = new ArrayList<>();
        if (defaultResult != null) {
            for (Map.Entry<String, String> i : defaultResult.getConfigProperties().entrySet()) {
                dbConfig.add(new RunTimeConfigurationDefaultBuildItem(i.getKey(), i.getValue()));
            }
        }
        for (Map.Entry<String, DataSourceBuildTimeConfig> entry : dataSourceBuildTimeConfig.namedDataSources.entrySet()) {
            DevServicesDatasourceResultBuildItem.DbResult result = startDevDb(entry.getKey(), curateOutcomeBuildItem,
                    installedDrivers, true,
                    devDBProviderMap, entry.getValue(), configHandlersByDbType, propertiesMap, closeableList,
                    launchMode.getLaunchMode());
            if (result != null) {
                namedResults.put(entry.getKey(), result);
                for (Map.Entry<String, String> i : result.getConfigProperties().entrySet()) {
                    dbConfig.add(new RunTimeConfigurationDefaultBuildItem(i.getKey(), i.getValue()));
                }
            }
        }
        for (RunTimeConfigurationDefaultBuildItem i : dbConfig) {
            runTimeConfigurationDefaultBuildItemBuildProducer
                    .produce(i);
        }
        databaseConfig = dbConfig;

        if (first) {
            first = false;
            Runnable closeTask = new Runnable() {
                @Override
                public void run() {
                    if (databases != null) {
                        for (Closeable i : databases) {
                            try {
                                i.close();
                            } catch (Throwable t) {
                                log.error("Failed to stop database", t);
                            }
                        }
                    }
                    first = true;
                    databases = null;
                    cachedProperties = null;
                }
            };
            QuarkusClassLoader cl = (QuarkusClassLoader) Thread.currentThread().getContextClassLoader();
            ((QuarkusClassLoader) cl.parent()).addCloseTask(closeTask);
            Thread closeHookThread = new Thread(closeTask, "Database shutdown thread");
            Runtime.getRuntime().addShutdownHook(closeHookThread);
            ((QuarkusClassLoader) cl.parent()).addCloseTask(new Runnable() {
                @Override
                public void run() {
                    Runtime.getRuntime().removeShutdownHook(closeHookThread);
                }
            });
        }
        databases = closeableList;
        cachedProperties = propertiesMap;

        if (defaultResult != null) {
            for (Map.Entry<String, String> entry : defaultResult.getConfigProperties().entrySet()) {
                devServicesResultBuildItemBuildProducer
                        .produce(new DevServicesNativeConfigResultBuildItem(entry.getKey(), entry.getValue()));
            }
        }
        for (DevServicesDatasourceResultBuildItem.DbResult i : namedResults.values()) {
            for (Map.Entry<String, String> entry : i.getConfigProperties().entrySet()) {
                devServicesResultBuildItemBuildProducer
                        .produce(new DevServicesNativeConfigResultBuildItem(entry.getKey(), entry.getValue()));
            }
        }
        return new DevServicesDatasourceResultBuildItem(defaultResult, namedResults);
    }

    private DevServicesDatasourceResultBuildItem.DbResult startDevDb(String dbName,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers,
            boolean hasNamedDatasources,
            Map<String, DevServicesDatasourceProvider> devDBProviders, DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configurationHandlerBuildItems,
            Map<String, String> propertiesMap, List<Closeable> closeableList,
            LaunchMode launchMode) {
        Optional<Boolean> enabled = dataSourceBuildTimeConfig.devservices.enabled;
        if (enabled.isPresent() && !enabled.get()) {
            //explicitly disabled
            log.debug("Not starting devservices for " + (dbName == null ? "default datasource" : dbName)
                    + " as it has been disabled in the config");
            return null;
        }

        Optional<String> defaultDbKind = DefaultDataSourceDbKindBuildItem.resolve(
                dataSourceBuildTimeConfig.dbKind,
                installedDrivers,
                dbName != null || enabled.orElse(!hasNamedDatasources),
                curateOutcomeBuildItem);

        if (!defaultDbKind.isPresent()) {
            //nothing we can do
            log.warn("Unable to determine a database type for " + (dbName == null ? "default datasource" : dbName));
            return null;
        }
        DevServicesDatasourceProvider devDbProvider = devDBProviders.get(defaultDbKind.get());
        List<DevServicesDatasourceConfigurationHandlerBuildItem> configHandlers = configurationHandlerBuildItems
                .get(defaultDbKind.get());
        if (devDbProvider == null || configHandlers == null) {
            log.warn("Unable to start devservices for " + (dbName == null ? "default datasource" : dbName)
                    + " as this datasource type (" + defaultDbKind.get() + ") does not support devservices");
            return null;
        }

        if (!enabled.isPresent()) {
            for (DevServicesDatasourceConfigurationHandlerBuildItem i : configHandlers) {
                if (i.getCheckConfiguredFunction().test(dbName)) {
                    //this database has explicit configuration
                    //we don't start the devservices
                    log.debug("Not starting devservices for " + (dbName == null ? "default datasource" : dbName)
                            + " as it has explicit configuration");
                    return null;
                }
            }
        }

        if (devDbProvider.isDockerRequired() && !isDockerWorking.getAsBoolean()) {
            String message = "Please configure the datasource URL for "
                    + (dbName == null ? "the default datasource" : " datasource '" + dbName + "'")
                    + " or ensure the Docker daemon is up and running.";
            if (launchMode == LaunchMode.TEST) {
                throw new IllegalStateException(message);
            } else {
                // in dev-mode we just want to warn users and allow them to recover
                log.warn(message);
                return null;
            }

        }

        //ok, so we know we need to start one

        String prefix = "quarkus.datasource.";
        if (dbName != null) {
            prefix = prefix + dbName + ".";
        }

        DevServicesDatasourceProvider.RunningDevServicesDatasource datasource = devDbProvider
                .startDatabase(ConfigProvider.getConfig().getOptionalValue(prefix + "username", String.class),
                        ConfigProvider.getConfig().getOptionalValue(prefix + "password", String.class),
                        Optional.ofNullable(dbName), dataSourceBuildTimeConfig.devservices.imageName,
                        dataSourceBuildTimeConfig.devservices.properties,
                        dataSourceBuildTimeConfig.devservices.port, launchMode);
        closeableList.add(datasource.getCloseTask());

        Map<String, String> devDebProperties = new HashMap<>();
        propertiesMap.put(prefix + "db-kind", dataSourceBuildTimeConfig.dbKind.orElse(null));
        for (DevServicesDatasourceConfigurationHandlerBuildItem devDbConfigurationHandlerBuildItem : configHandlers) {
            devDebProperties.putAll(devDbConfigurationHandlerBuildItem.getConfigProviderFunction()
                    .apply(dbName, datasource));
        }
        devDebProperties.put(prefix + "db-kind", defaultDbKind.get());
        if (datasource.getUsername() != null) {
            devDebProperties.put(prefix + "username", datasource.getUsername());
        }
        if (datasource.getPassword() != null) {
            devDebProperties.put(prefix + "password", datasource.getPassword());
        }
        return new DevServicesDatasourceResultBuildItem.DbResult(defaultDbKind.get(), devDebProperties);
    }
}
