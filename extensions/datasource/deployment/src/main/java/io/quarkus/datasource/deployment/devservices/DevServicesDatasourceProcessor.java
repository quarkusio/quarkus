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

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.CuratedApplicationShutdownBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem.RunningDevService;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.GlobalDevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;

@BuildSteps(onlyIfNot = IsNormal.class, onlyIf = GlobalDevServicesConfig.Enabled.class)
public class DevServicesDatasourceProcessor {

    private static final Logger log = Logger.getLogger(DevServicesDatasourceProcessor.class);
    private static final int DOCKER_PS_ID_LENGTH = 12;

    static volatile List<RunningDevService> databases;

    static volatile Map<String, String> cachedProperties;

    static volatile boolean first = true;

    @BuildStep
    DevServicesDatasourceResultBuildItem launchDatabases(CurateOutcomeBuildItem curateOutcomeBuildItem,
            DockerStatusBuildItem dockerStatusBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers,
            List<DevServicesDatasourceProviderBuildItem> devDBProviders,
            DataSourcesBuildTimeConfig dataSourceBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            List<DevServicesDatasourceConfigurationHandlerBuildItem> configurationHandlerBuildItems,
            BuildProducer<DevServicesResultBuildItem> devServicesResultBuildItemBuildProducer,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            CuratedApplicationShutdownBuildItem closeBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            GlobalDevServicesConfig globalDevServicesConfig) {
        //figure out if we need to shut down and restart existing databases
        //if not and the DB's have already started we just return
        if (databases != null) {
            boolean restartRequired = false;
            if (!restartRequired) {
                for (Map.Entry<String, String> entry : cachedProperties.entrySet()) {
                    if (!Objects.equals(entry.getValue(),
                            trim(ConfigProvider.getConfig().getOptionalValue(entry.getKey(), String.class).orElse(null)))) {
                        restartRequired = true;
                        break;
                    }
                }
            }
            if (!restartRequired) {
                //devservices properties may have been added
                for (var name : ConfigProvider.getConfig().getPropertyNames()) {
                    if (name.startsWith("quarkus.datasource.") && name.contains(".devservices.")
                            && !cachedProperties.containsKey(name)) {
                        restartRequired = true;
                        break;
                    }
                }
            }
            if (!restartRequired) {
                for (RunningDevService database : databases) {
                    devServicesResultBuildItemBuildProducer.produce(database.toBuildItem());
                }
                // keep the previous behaviour of producing DevServicesDatasourceResultBuildItem only when the devservices first starts.
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
        }
        DevServicesDatasourceResultBuildItem.DbResult defaultResult;
        Map<String, DevServicesDatasourceResultBuildItem.DbResult> namedResults = new HashMap<>();
        //now we need to figure out if we need to launch some databases
        //note that because we run in dev and test mode only we know the runtime
        //config at build time, as they both execute in the same JVM

        //to keep things simpler for now we are only going to support this for the default datasource
        //support for named datasources will come later

        Map<String, String> propertiesMap = new HashMap<>();
        List<RunningDevService> runningDevServices = new ArrayList<>();
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
        RunningDevService defaultDevService = startDevDb(null, curateOutcomeBuildItem, installedDrivers,
                !dataSourceBuildTimeConfig.namedDataSources.isEmpty(),
                devDBProviderMap,
                dataSourceBuildTimeConfig.defaultDataSource,
                configHandlersByDbType, propertiesMap,
                dockerStatusBuildItem, launchMode.getLaunchMode(), consoleInstalledBuildItem,
                loggingSetupBuildItem, globalDevServicesConfig);
        if (defaultDevService != null) {
            runningDevServices.add(defaultDevService);
        }
        defaultResult = toDbResult(defaultDevService);
        for (Map.Entry<String, DataSourceBuildTimeConfig> entry : dataSourceBuildTimeConfig.namedDataSources.entrySet()) {
            RunningDevService namedDevService = startDevDb(entry.getKey(), curateOutcomeBuildItem,
                    installedDrivers, true,
                    devDBProviderMap, entry.getValue(), configHandlersByDbType, propertiesMap,
                    dockerStatusBuildItem,
                    launchMode.getLaunchMode(), consoleInstalledBuildItem, loggingSetupBuildItem, globalDevServicesConfig);
            if (namedDevService != null) {
                runningDevServices.add(namedDevService);
                namedResults.put(entry.getKey(), toDbResult(namedDevService));
            }
        }

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
            closeBuildItem.addCloseTask(closeTask, true);
        }
        databases = runningDevServices;
        cachedProperties = propertiesMap;
        for (RunningDevService database : databases) {
            devServicesResultBuildItemBuildProducer.produce(database.toBuildItem());
        }
        return new DevServicesDatasourceResultBuildItem(defaultResult, namedResults);
    }

    private String trim(String optional) {
        if (optional == null) {
            return null;
        }
        return optional.trim();
    }

    private RunningDevService startDevDb(String dbName,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers,
            boolean hasNamedDatasources,
            Map<String, DevServicesDatasourceProvider> devDBProviders, DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configurationHandlerBuildItems,
            Map<String, String> propertiesMap,
            DockerStatusBuildItem dockerStatusBuildItem,
            LaunchMode launchMode, Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, GlobalDevServicesConfig globalDevServicesConfig) {
        boolean explicitlyDisabled = !(dataSourceBuildTimeConfig.devservices.enabled.orElse(true));
        if (explicitlyDisabled) {
            //explicitly disabled
            log.debug("Not starting Dev Services for " + (dbName == null ? "default datasource" : dbName)
                    + " as it has been disabled in the configuration");
            return null;
        }

        Boolean enabled = dataSourceBuildTimeConfig.devservices.enabled.orElse(!hasNamedDatasources);

        Optional<String> defaultDbKind = DefaultDataSourceDbKindBuildItem.resolve(
                dataSourceBuildTimeConfig.dbKind,
                installedDrivers,
                dbName != null || enabled,
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
            log.warn("Unable to start Dev Services for " + (dbName == null ? "default datasource" : dbName)
                    + " as this datasource type (" + defaultDbKind.get() + ") does not support Dev Services");
            return null;
        }

        if (dataSourceBuildTimeConfig.devservices.enabled.isEmpty()) {
            for (DevServicesDatasourceConfigurationHandlerBuildItem i : configHandlers) {
                if (i.getCheckConfiguredFunction().test(dbName)) {
                    //this database has explicit configuration
                    //we don't start the devservices
                    log.debug("Not starting Dev Services for " + (dbName == null ? "default datasource" : dbName)
                            + " as it has explicit configuration");
                    return null;
                }
            }
        }

        String prettyName = dbName == null ? "the default datasource" : " datasource '" + dbName + "'";
        if (devDbProvider.isDockerRequired() && !dockerStatusBuildItem.isDockerAvailable()) {
            String message = "Please configure the datasource URL for "
                    + prettyName
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

        StartupLogCompressor compressor = new StartupLogCompressor(
                (launchMode == LaunchMode.TEST ? "(test) " : "") + "Database for " + prettyName
                        + " (" + defaultDbKind.get() + ") starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);
        try {
            DevServicesDatasourceContainerConfig containerConfig = new DevServicesDatasourceContainerConfig(
                    dataSourceBuildTimeConfig.devservices.imageName,
                    dataSourceBuildTimeConfig.devservices.containerProperties,
                    dataSourceBuildTimeConfig.devservices.properties,
                    dataSourceBuildTimeConfig.devservices.port,
                    dataSourceBuildTimeConfig.devservices.command,
                    dataSourceBuildTimeConfig.devservices.dbName,
                    dataSourceBuildTimeConfig.devservices.username,
                    dataSourceBuildTimeConfig.devservices.password);

            DevServicesDatasourceProvider.RunningDevServicesDatasource datasource = devDbProvider
                    .startDatabase(
                            ConfigUtils.getFirstOptionalValue(DataSourceUtil.dataSourcePropertyKeys(dbName, "username"),
                                    String.class),
                            ConfigUtils.getFirstOptionalValue(DataSourceUtil.dataSourcePropertyKeys(dbName, "password"),
                                    String.class),
                            Optional.ofNullable(dbName), containerConfig,
                            launchMode, globalDevServicesConfig.timeout);

            for (String key : DataSourceUtil.dataSourcePropertyKeys(dbName, "db-kind")) {
                propertiesMap.put(key, dataSourceBuildTimeConfig.dbKind.orElse(null));
            }
            String devServicesPrefix = "devservices.";
            if (dataSourceBuildTimeConfig.devservices.command.isPresent()) {
                setDataSourceProperties(propertiesMap, dbName, devServicesPrefix + "command",
                        dataSourceBuildTimeConfig.devservices.command.get());
            }
            if (dataSourceBuildTimeConfig.devservices.imageName.isPresent()) {
                setDataSourceProperties(propertiesMap, dbName, devServicesPrefix + "image-name",
                        dataSourceBuildTimeConfig.devservices.imageName.get());
            }
            if (dataSourceBuildTimeConfig.devservices.port.isPresent()) {
                setDataSourceProperties(propertiesMap, dbName, devServicesPrefix + "port",
                        Integer.toString(dataSourceBuildTimeConfig.devservices.port.getAsInt()));
            }
            if (!dataSourceBuildTimeConfig.devservices.properties.isEmpty()) {
                for (var e : dataSourceBuildTimeConfig.devservices.properties.entrySet()) {
                    setDataSourceProperties(propertiesMap, dbName, devServicesPrefix + "properties." + e.getKey(),
                            e.getValue());
                }
            }

            Map<String, String> devDebProperties = new HashMap<>();
            for (DevServicesDatasourceConfigurationHandlerBuildItem devDbConfigurationHandlerBuildItem : configHandlers) {
                devDebProperties.putAll(devDbConfigurationHandlerBuildItem.getConfigProviderFunction()
                        .apply(dbName, datasource));
            }
            setDataSourceProperties(devDebProperties, dbName, "db-kind", defaultDbKind.get());
            if (datasource.getUsername() != null) {
                setDataSourceProperties(devDebProperties, dbName, "username", datasource.getUsername());
            }
            if (datasource.getPassword() != null) {
                setDataSourceProperties(devDebProperties, dbName, "password", datasource.getPassword());
            }
            compressor.close();
            if (datasource.getId() == null) {
                log.infof("Dev Services for %s (%s) started", prettyName, defaultDbKind.get());
            } else {
                log.infof("Dev Services for %s (%s) started - container ID is %s", prettyName, defaultDbKind.get(),
                        datasource.getId().length() > DOCKER_PS_ID_LENGTH ? datasource.getId().substring(0,
                                DOCKER_PS_ID_LENGTH) : datasource.getId());
            }

            List<String> devservicesPrefixes = DataSourceUtil.dataSourcePropertyKeys(dbName, "devservices.");
            for (var name : ConfigProvider.getConfig().getPropertyNames()) {
                for (String prefix : devservicesPrefixes) {
                    if (name.startsWith(prefix)) {
                        devDebProperties.put(name, ConfigProvider.getConfig().getValue(name, String.class));
                    }
                }
            }
            return new RunningDevService(defaultDbKind.get(), datasource.getId(), datasource.getCloseTask(), devDebProperties);
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }

    private void setDataSourceProperties(Map<String, String> propertiesMap, String dbName, String propertyKeyRadical,
            String value) {
        for (String key : DataSourceUtil.dataSourcePropertyKeys(dbName, propertyKeyRadical)) {
            propertiesMap.put(key, value);
        }
    }

    private DevServicesDatasourceResultBuildItem.DbResult toDbResult(RunningDevService devService) {
        if (devService == null) {
            return null;
        }
        return new DevServicesDatasourceResultBuildItem.DbResult(devService.getName(), devService.getConfig());
    }
}
