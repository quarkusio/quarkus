package io.quarkus.datasource.deployment.devservices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DatasourceStartable;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceContainerConfig;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProvider;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceProviderBuildItem;
import io.quarkus.datasource.deployment.spi.DevServicesDatasourceResultBuildItem;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesComposeProjectBuildItem;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.DevServicesSharedNetworkBuildItem;
import io.quarkus.deployment.builditem.DockerStatusBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.console.ConsoleInstalledBuildItem;
import io.quarkus.deployment.console.StartupLogCompressor;
import io.quarkus.deployment.dev.devservices.DevServicesConfig;
import io.quarkus.deployment.logging.LoggingSetupBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = { IsDevServicesSupportedByLaunchMode.class, DevServicesConfig.Enabled.class })
public class DevServicesDatasourceProcessor {

    private static final Logger log = Logger.getLogger(DevServicesDatasourceProcessor.class);
    private static final int DOCKER_PS_ID_LENGTH = 12;

    @BuildStep
    DevServicesDatasourceResultBuildItem launchDatabases(
            Capabilities capabilities,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers,
            List<DevServicesDatasourceProviderBuildItem> devDBProviders,
            List<DevServicesSharedNetworkBuildItem> devServicesSharedNetworkBuildItem,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            LaunchModeBuildItem launchMode,
            List<DevServicesDatasourceConfigurationHandlerBuildItem> configurationHandlerBuildItems,
            BuildProducer<DevServicesResultBuildItem> devServicesResultBuildItemBuildProducer,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        Map<String, DevServicesDatasourceResultBuildItem.DbResult> results = new HashMap<>();
        //now we need to figure out if we need to launch some databases
        //note that because we run in dev and test mode only we know the runtime
        //config at build time, as they both execute in the same JVM

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
                .filter(d -> d.getDevServicesProvider() != null)
                .collect(Collectors.toMap(DevServicesDatasourceProviderBuildItem::getDatabase,
                        DevServicesDatasourceProviderBuildItem::getDevServicesProvider));

        Map<String, Object> newDatasourceConfigs = buildMapFromBuildConfig(dataSourcesBuildTimeConfig);

        for (Map.Entry<String, DataSourceBuildTimeConfig> entry : dataSourcesBuildTimeConfig.dataSources().entrySet()) {
            DevServicesResultBuildItem devService = startDevDb(entry.getKey(), capabilities, curateOutcomeBuildItem,
                    installedDrivers, dataSourcesBuildTimeConfig.hasNamedDataSources(),
                    devDBProviderMap, entry.getValue(), configHandlersByDbType,
                    dockerStatusBuildItem, composeProjectBuildItem,
                    launchMode.getLaunchMode(), consoleInstalledBuildItem, loggingSetupBuildItem,
                    devServicesConfig, useSharedNetwork, newDatasourceConfigs);
            if (devService != null) {
                devServicesResultBuildItemBuildProducer.produce(devService);
            }
        }

        return new DevServicesDatasourceResultBuildItem(results);
    }

    /**
     * Returns a map of properties that can trigger a datasource dev service restart if modified.
     * It builds this map from the datasource build time config.
     */
    private static Map<String, Object> buildMapFromBuildConfig(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        Map<String, Object> res = new HashMap<>();
        for (var datasource : dataSourcesBuildTimeConfig.dataSources().entrySet()) {
            String name = datasource.getKey();
            DataSourceBuildTimeConfig config = datasource.getValue();
            res.put(name + ".db-kind", config.dbKind());
            res.put(name + ".db-version", config.dbVersion());
            res.put(name + ".devservices.command", config.devservices().command());
            res.put(name + ".devservices.container-env", config.devservices().containerEnv());
            res.put(name + ".devservices.container-properties.", config.devservices().containerProperties());
            res.put(name + ".devservices.db-name", config.devservices().dbName());
            res.put(name + ".devservices.image-name", config.devservices().imageName());
            res.put(name + ".devservices.init-script-path", config.devservices().initScriptPath());
            res.put(name + ".devservices.init-privileged-script-path", config.devservices().initPrivilegedScriptPath());
            res.put(name + ".devservices.password", config.devservices().password());
            res.put(name + ".devservices.port", config.devservices().port());
            res.put(name + ".devservices.properties", config.devservices().properties());
            res.put(name + ".devservices.reuse", config.devservices().reuse());
            res.put(name + ".devservices.username", config.devservices().username());
            res.put(name + ".devservices.volumes", config.devservices().volumes());
            Optional<String> username = ConfigUtils.getFirstOptionalValue(
                    DataSourceUtil.dataSourcePropertyKeys(name, "username"), String.class);
            res.put(name + ".username", username);
            Optional<String> password = ConfigUtils.getFirstOptionalValue(
                    DataSourceUtil.dataSourcePropertyKeys(name, "password"), String.class);
            res.put(name + ".password", password);
        }
        return res;
    }

    private DevServicesResultBuildItem startDevDb(
            String dbName,
            Capabilities capabilities,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers,
            boolean hasNamedDatasources,
            Map<String, DevServicesDatasourceProvider> devDBProviders,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configurationHandlerBuildItems,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem, LaunchMode launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, DevServicesConfig devServicesConfig, boolean useSharedNetwork,
            Map<String, Object> configForWhichChangesShouldTriggerARestart) {

        String dataSourcePrettyName = getDataSourcePrettyName(dbName);

        if (!shouldStart(dbName, dataSourceBuildTimeConfig, useSharedNetwork, dataSourcePrettyName)) {
            return null;
        }

        Optional<String> maybeDefaultDbKind = getDefaultDbKind(dbName, curateOutcomeBuildItem, installedDrivers,
                hasNamedDatasources,
                dataSourceBuildTimeConfig);

        if (maybeDefaultDbKind.isEmpty()) {
            //nothing we can do
            log.warn("Unable to determine a database type for " + dataSourcePrettyName);
            return null;
        }

        String defaultDbKind = maybeDefaultDbKind.get();

        DevServicesDatasourceProvider devDbProvider = devDBProviders.get(defaultDbKind);
        List<DevServicesDatasourceConfigurationHandlerBuildItem> configHandlers = configurationHandlerBuildItems
                .get(defaultDbKind);

        if (!shouldStartBasedOnConfigHandler(dbName, devDBProviders, dataSourceBuildTimeConfig,
                configurationHandlerBuildItems, dockerStatusBuildItem, launchMode, devDbProvider, configHandlers, defaultDbKind,
                dataSourcePrettyName)) {
            return null;
        }

        //ok, so we know we need to start one
        StartupLogCompressor compressor = getCompressor(launchMode, consoleInstalledBuildItem, loggingSetupBuildItem,
                dataSourcePrettyName, defaultDbKind);

        try {
            DevServicesDatasourceContainerConfig containerConfig = getContainerConfig(dataSourceBuildTimeConfig);

            Map<String, Function<DatasourceStartable, String>> devDebProperties = new HashMap<>();
            for (DevServicesDatasourceConfigurationHandlerBuildItem devDbConfigurationHandlerBuildItem : configHandlers) {
                Map<String, Function<DatasourceStartable, String>> properties = devDbConfigurationHandlerBuildItem
                        .getDeferredConfigProviderFunction().apply(
                                dbName);
                processConfigMap(capabilities, properties, devDebProperties);
            }

            Optional<String> usernameFromConfig = ConfigUtils.getFirstOptionalValue(
                    DataSourceUtil.dataSourcePropertyKeys(dbName, "username"),
                    String.class);
            Optional<String> passwordFromConfig = ConfigUtils.getFirstOptionalValue(
                    DataSourceUtil.dataSourcePropertyKeys(dbName, "password"),
                    String.class);

            String feature = devDbProvider.getFeature();

            DevServicesResultBuildItem buildItem = devDbProvider
                    .findRunningComposeDatasource(launchMode, useSharedNetwork, containerConfig, composeProjectBuildItem)
                    .map(datasource -> DevServicesResultBuildItem.discovered().feature(feature).containerId(datasource.id())
                            .config(makeConfigMapForRunningDatasource(dbName, capabilities, configHandlers, datasource))
                            .build())
                    .orElseGet(() -> {
                        DatasourceStartable startable = devDbProvider
                                .createDatasourceStartable(
                                        usernameFromConfig,
                                        passwordFromConfig,
                                        dbName, containerConfig,
                                        launchMode, useSharedNetwork, devServicesConfig.timeout());

                        Map<String, String> credentials = new HashMap<>();
                        setDataSourceProperties(credentials, dbName, "username", startable.getUsername());
                        setDataSourceProperties(credentials, dbName, "password", startable.getPassword());

                        return DevServicesResultBuildItem.owned().feature(feature).startable(() -> startable)
                                .serviceName(dbName)
                                .serviceConfig(configForWhichChangesShouldTriggerARestart)
                                .config(credentials)
                                .configProvider(devDebProperties)
                                .postStartHook((s) -> {
                                    String id = s.runningDevServicesDatasource().id();
                                    logStart(id, dataSourcePrettyName, defaultDbKind);
                                })
                                .build();
                    });

            compressor.close();

            return buildItem;
        } catch (Throwable t) {
            compressor.closeAndDumpCaptured();
            throw new RuntimeException(t);
        }
    }

    private static void logStart(String id, String dataSourcePrettyName, String defaultDbKind) {
        if (id == null) {
            log.infof("Dev Services for %s (%s) started", dataSourcePrettyName, defaultDbKind);
        } else {
            log.infof("Dev Services for %s (%s) started - container ID is %s", dataSourcePrettyName,
                    defaultDbKind,
                    id.length() > DOCKER_PS_ID_LENGTH
                            ? id.substring(0,
                                    DOCKER_PS_ID_LENGTH)
                            : id);
        }
    }

    private static StartupLogCompressor getCompressor(LaunchMode launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem, LoggingSetupBuildItem loggingSetupBuildItem,
            String dataSourcePrettyName, String defaultDbKind) {
        return new StartupLogCompressor(
                (launchMode == LaunchMode.TEST ? "(test) " : "") + "Database for " + dataSourcePrettyName
                        + " (" + defaultDbKind + ") starting:",
                consoleInstalledBuildItem,
                loggingSetupBuildItem);
    }

    private static Optional<String> getDefaultDbKind(String dbName, CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<DefaultDataSourceDbKindBuildItem> installedDrivers, boolean hasNamedDatasources,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig) {
        Boolean enabled = dataSourceBuildTimeConfig.devservices().enabled().orElse(!hasNamedDatasources);

        return DefaultDataSourceDbKindBuildItem.resolve(
                dataSourceBuildTimeConfig.dbKind(),
                installedDrivers,
                (!DataSourceUtil.isDefault(dbName)) || enabled,
                curateOutcomeBuildItem);
    }

    private static String getDataSourcePrettyName(String dbName) {
        return DataSourceUtil.isDefault(dbName) ? "default datasource" : "datasource " + dbName;
    }

    private static void maybeWarnAboutMissingProvider(
            Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configurationHandlerBuildItems,
            Map<String, DevServicesDatasourceProvider> devDBProviderMap,
            String defaultDbKind, String dataSourcePrettyName) {
        boolean hasProvider = devDBProviderMap.containsKey(defaultDbKind);
        boolean hasConfigHandler = configurationHandlerBuildItems
                .containsKey(defaultDbKind);
        if (!hasProvider || !hasConfigHandler) {
            log.warn("Unable to start Dev Services for " + dataSourcePrettyName
                    + " as this datasource type (" + defaultDbKind + ") does not support Dev Services");
        }
    }

    private Map<String, String> makeConfigMapForRunningDatasource(String dbName, Capabilities capabilities,
            List<DevServicesDatasourceConfigurationHandlerBuildItem> configHandlers,
            DevServicesDatasourceProvider.RunningDevServicesDatasource datasource) {
        Map<String, String> devDebProperties = new HashMap<>();
        for (DevServicesDatasourceConfigurationHandlerBuildItem devDbConfigurationHandlerBuildItem : configHandlers) {
            Map<String, String> properties = devDbConfigurationHandlerBuildItem.getConfigProviderFunction().apply(dbName,
                    datasource);
            processConfigMap(capabilities, properties, devDebProperties);
        }
        if (datasource.username() != null) {
            setDataSourceProperties(devDebProperties, dbName, "username", datasource.username());
        }
        if (datasource.password() != null) {
            setDataSourceProperties(devDebProperties, dbName, "password", datasource.password());
        }
        return devDebProperties;
    }

    private static <T> void processConfigMap(Capabilities capabilities, Map<String, T> properties,
            Map<String, T> devDebProperties) {
        for (Map.Entry<String, T> entry : properties.entrySet()) {
            if (entry.getKey().contains(".jdbc.") && entry.getKey().endsWith(".url")) {
                if (capabilities.isCapabilityWithPrefixPresent(Capability.AGROAL)) {
                    devDebProperties.put(entry.getKey(), entry.getValue());
                }
            } else {
                devDebProperties.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static boolean shouldStartBasedOnConfigHandler(String dbName,
            Map<String, DevServicesDatasourceProvider> devDBProviderMap,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configurationHandlerBuildItems,
            DockerStatusBuildItem dockerStatusBuildItem, LaunchMode launchMode,
            DevServicesDatasourceProvider devDbProvider,
            List<DevServicesDatasourceConfigurationHandlerBuildItem> configHandlers, String defaultDbKind,
            String dataSourcePrettyName) {
        if (devDbProvider == null || configHandlers == null) {
            maybeWarnAboutMissingProvider(configurationHandlerBuildItems, devDBProviderMap,
                    defaultDbKind,
                    dataSourcePrettyName);
            return false;
        }

        if (dataSourceBuildTimeConfig.devservices().enabled().isEmpty()) {
            for (DevServicesDatasourceConfigurationHandlerBuildItem i : configHandlers) {
                if (i.getCheckConfiguredFunction().test(dbName)) {
                    //this database has explicit configuration
                    //we don't start the devservices
                    log.debug("Not starting Dev Services for " + dataSourcePrettyName
                            + " as it has explicit configuration");
                    return false;
                }
            }
        }

        if (devDbProvider.isDockerRequired() && !dockerStatusBuildItem.isContainerRuntimeAvailable()) {
            String message = "Please configure the datasource URL for " + dataSourcePrettyName
                    + " or ensure the Docker daemon is up and running.";
            if (launchMode == LaunchMode.TEST) {
                throw new IllegalStateException(message);
            } else {
                // in dev-mode we just want to warn users and allow them to recover
                log.warn(message);
                return false;
            }

        }
        return true;
    }

    private static boolean shouldStart(String dbName, DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            boolean useSharedNetwork, String dataSourcePrettyName) {
        if (!ConfigUtils.getFirstOptionalValue(
                DataSourceUtil.dataSourcePropertyKeys(dbName, "active"), Boolean.class)
                .orElse(true)) {
            log.debug("Not starting Dev Services for " + dataSourcePrettyName
                    + " as the datasource has been deactivated in the configuration");
            return false;
        }

        if (!(dataSourceBuildTimeConfig.devservices().enabled().orElse(true))) {
            log.debug("Not starting Dev Services for " + dataSourcePrettyName
                    + " as it has been disabled in the configuration");
            return false;
        }

        if (useSharedNetwork && dataSourceBuildTimeConfig.devservices().port().isPresent()) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Cannot set a port for the Dev Service of datasource '%s' using '%s', because it is using a shared network, which disables port mapping",
                    DataSourceUtil.dataSourcePropertyKey(dbName, "devservices.port"),
                    dataSourcePrettyName));
        }
        return true;
    }

    private static DevServicesDatasourceContainerConfig getContainerConfig(
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig) {
        return new DevServicesDatasourceContainerConfig(
                dataSourceBuildTimeConfig.devservices().imageName(),
                dataSourceBuildTimeConfig.devservices().containerEnv(),
                dataSourceBuildTimeConfig.devservices().containerProperties(),
                dataSourceBuildTimeConfig.devservices().properties(),
                dataSourceBuildTimeConfig.devservices().port(),
                dataSourceBuildTimeConfig.devservices().command(),
                dataSourceBuildTimeConfig.devservices().dbName(),
                dataSourceBuildTimeConfig.devservices().username(),
                dataSourceBuildTimeConfig.devservices().password(),
                dataSourceBuildTimeConfig.devservices().initScriptPath(),
                dataSourceBuildTimeConfig.devservices().initPrivilegedScriptPath(),
                dataSourceBuildTimeConfig.devservices().volumes(),
                dataSourceBuildTimeConfig.devservices().reuse(),
                dataSourceBuildTimeConfig.devservices().showLogs());
    }

    private void setDataSourceProperties(Map<String, String> propertiesMap, String dbName, String propertyKeyRadical,
            String value) {
        for (String key : DataSourceUtil.dataSourcePropertyKeys(dbName, propertyKeyRadical)) {
            propertiesMap.put(key, value);
        }
    }

}
