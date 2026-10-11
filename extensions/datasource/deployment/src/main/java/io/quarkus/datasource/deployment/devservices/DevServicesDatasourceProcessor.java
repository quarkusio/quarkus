package io.quarkus.datasource.deployment.devservices;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceFeatureRequirementBuildItem;
import io.quarkus.datasource.deployment.spi.DatabaseFeature;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
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
import io.quarkus.devservices.datasource.common.DatasourceStartable;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceConfigurationHandlerBuildItem;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceContainerConfig;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceProvider;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceProviderBuildItem;
import io.quarkus.devservices.datasource.common.DevServicesDatasourceResultBuildItem;
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
            List<DataSourceDefinedBuildItem> definedDatasources,
            LaunchModeBuildItem launchMode,
            List<DevServicesDatasourceConfigurationHandlerBuildItem> configurationHandlerBuildItems,
            List<DataSourceFeatureRequirementBuildItem> featureRequirements,
            BuildProducer<DevServicesResultBuildItem> devServicesResultBuildItemBuildProducer,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem,
            DevServicesConfig devServicesConfig) {

        boolean useSharedNetwork = DevServicesSharedNetworkBuildItem.isSharedNetworkRequired(devServicesConfig,
                devServicesSharedNetworkBuildItem);

        Map<String, DevServicesDatasourceResultBuildItem.DbResult> results = new HashMap<>();

        Map<String, Set<DatabaseFeature>> featuresByDatasource = featureRequirements
                .stream()
                .collect(Collectors.groupingBy(
                        DataSourceFeatureRequirementBuildItem::getDatasourceName,
                        Collectors.mapping(
                                DataSourceFeatureRequirementBuildItem::getFeature,
                                Collectors.toSet())));
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

        Set<String> definedDatasourceNames = definedDatasources.stream().map(DataSourceDefinedBuildItem::getName)
                .collect(Collectors.toSet());
        Map<String, String> dbKindByName = definedDatasources.stream()
                .collect(Collectors.toMap(DataSourceDefinedBuildItem::getName, DataSourceDefinedBuildItem::getDbKind));
        Map<String, Object> newDatasourceConfigs = buildMapFromBuildConfig(dataSourcesBuildTimeConfig,
                definedDatasourceNames);

        for (DataSourceDefinedBuildItem ds : definedDatasources) {
            String name = ds.getName();
            DataSourceBuildTimeConfig config = dataSourcesBuildTimeConfig.dataSources().get(name);
            DevServicesResultBuildItem devService;
            Optional<String> useFrom = config.devservices().useFrom();
            if (useFrom.isPresent()) {
                devService = mirrorDevDb(name, useFrom.get(), ds.getDbKind(), definedDatasourceNames, dbKindByName,
                        dataSourcesBuildTimeConfig, devDBProviderMap);
            } else {
                Set<DatabaseFeature> features = featuresByDatasource
                        .getOrDefault(name, Collections.emptySet());
                devService = startDevDb(name, capabilities,
                        ds,
                        devDBProviderMap, config, configHandlersByDbType,
                        dockerStatusBuildItem, composeProjectBuildItem,
                        launchMode.getLaunchMode(), consoleInstalledBuildItem, loggingSetupBuildItem,
                        devServicesConfig, useSharedNetwork, newDatasourceConfigs, features);
            }
            if (devService != null) {
                devServicesResultBuildItemBuildProducer.produce(devService);
            }
        }

        return new DevServicesDatasourceResultBuildItem(results);
    }

    /**
     * Returns a map of properties that can trigger a datasource dev service restart if modified.
     * Only includes datasources that are actually defined.
     */
    private static Map<String, Object> buildMapFromBuildConfig(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            Set<String> definedDatasourceNames) {
        Map<String, Object> res = new HashMap<>();
        for (String name : definedDatasourceNames) {
            DataSourceBuildTimeConfig config = dataSourcesBuildTimeConfig.dataSources().get(name);
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
            res.put(name + ".devservices.use-from", config.devservices().useFrom());
            res.put(name + ".devservices.shares-container", config.devservices().sharesContainer());
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
            DataSourceDefinedBuildItem definition,
            Map<String, DevServicesDatasourceProvider> devDBProviders,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            Map<String, List<DevServicesDatasourceConfigurationHandlerBuildItem>> configurationHandlerBuildItems,
            DockerStatusBuildItem dockerStatusBuildItem,
            DevServicesComposeProjectBuildItem composeProjectBuildItem, LaunchMode launchMode,
            Optional<ConsoleInstalledBuildItem> consoleInstalledBuildItem,
            LoggingSetupBuildItem loggingSetupBuildItem, DevServicesConfig devServicesConfig, boolean useSharedNetwork,
            Map<String, Object> configForWhichChangesShouldTriggerARestart,
            Set<DatabaseFeature> requiredFeatures) {

        String dataSourcePrettyName = getDataSourcePrettyName(dbName);

        if (!shouldStart(dbName, dataSourceBuildTimeConfig, useSharedNetwork, dataSourcePrettyName)) {
            return null;
        }

        String defaultDbKind = definition.getDbKind();

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
            DevServicesDatasourceContainerConfig containerConfig = getContainerConfig(dataSourceBuildTimeConfig,
                    dbName, requiredFeatures);

            Map<String, Function<DatasourceStartable, String>> deferredConfigProviders = new HashMap<>();
            for (DevServicesDatasourceConfigurationHandlerBuildItem devDbConfigurationHandlerBuildItem : configHandlers) {
                Map<String, Function<DatasourceStartable, String>> properties = devDbConfigurationHandlerBuildItem
                        .getDeferredConfigProviderFunction().apply(
                                dbName);
                processConfigMap(capabilities, properties, deferredConfigProviders);
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
                                .configProvider(s -> resolveDeferredConfig(s, deferredConfigProviders))
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

    /**
     * Instead of starting a container for {@code dbName}, waits for the Dev Services database of
     * {@code providerName} to start, then reuses its JDBC URL, reactive URL, username and password for
     * {@code dbName}. This allows a named datasource to share another one's Dev Services database.
     */
    private DevServicesResultBuildItem mirrorDevDb(String dbName, String providerName, String dbKind,
            Set<String> definedDatasourceNames, Map<String, String> dbKindByName,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig, Map<String, DevServicesDatasourceProvider> devDBProviders) {
        String dataSourcePrettyName = getDataSourcePrettyName(dbName);
        String resolvedProvider = DataSourceUtil.isDefault(providerName) ? DataSourceUtil.DEFAULT_DATASOURCE_NAME
                : providerName;
        String resolvedSelf = DataSourceUtil.isDefault(dbName) ? DataSourceUtil.DEFAULT_DATASOURCE_NAME : dbName;

        if (resolvedProvider.equals(resolvedSelf)) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Datasource '%s' cannot reference itself in '%s'. Set it to the name of another datasource.",
                    dataSourcePrettyName, DataSourceUtil.dataSourcePropertyKey(dbName, "devservices.use-from")));
        }
        if (!definedDatasourceNames.contains(resolvedProvider)) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Datasource '%s' references '%s' in '%s', but that datasource is not configured.",
                    dataSourcePrettyName, providerName, DataSourceUtil.dataSourcePropertyKey(dbName, "devservices.use-from")));
        }
        DataSourceBuildTimeConfig providerConfig = dataSourcesBuildTimeConfig.dataSources().get(resolvedProvider);
        if (!providerConfig.devservices().sharesContainer()) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Datasource '%s' references '%s' in '%s', but '%s' does not set '%s' to 'true'.",
                    dataSourcePrettyName, providerName,
                    DataSourceUtil.dataSourcePropertyKey(dbName, "devservices.use-from"),
                    getDataSourcePrettyName(resolvedProvider),
                    DataSourceUtil.dataSourcePropertyKey(resolvedProvider, "devservices.shares-container")));
        }
        if (providerConfig.devservices().useFrom().isPresent()) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Datasource '%s' references '%s' in '%s', but '%s' itself references another datasource "
                            + "in its own '%s'. Only one level of sharing is supported.",
                    dataSourcePrettyName, providerName,
                    DataSourceUtil.dataSourcePropertyKey(dbName, "devservices.use-from"),
                    getDataSourcePrettyName(resolvedProvider),
                    DataSourceUtil.dataSourcePropertyKey(resolvedProvider, "devservices.use-from")));
        }
        String providerDbKind = dbKindByName.get(resolvedProvider);
        if (!dbKind.equals(providerDbKind)) {
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Datasource '%s' (db-kind '%s') cannot reuse the Dev Services database of '%s' (db-kind '%s'); "
                            + "they must use the same db-kind.",
                    dataSourcePrettyName, dbKind, getDataSourcePrettyName(resolvedProvider), providerDbKind));
        }

        if (!shouldStart(dbName, dataSourcesBuildTimeConfig.dataSources().get(dbName), false, dataSourcePrettyName)) {
            return null;
        }

        DevServicesDatasourceProvider devDbProvider = devDBProviders.get(dbKind);
        String feature = devDbProvider != null ? devDbProvider.getFeature() : dbKind;

        SharedDatasourceStartable startable = new SharedDatasourceStartable();
        return DevServicesResultBuildItem.owned()
                .feature(feature)
                .serviceName(dbName)
                .serviceConfig(resolvedProvider)
                .startable(() -> startable)
                .dependsOnConfig(DataSourceUtil.dataSourcePropertyKey(resolvedProvider, "jdbc.url"),
                        SharedDatasourceStartable::setJdbcUrl, true)
                .dependsOnConfig(DataSourceUtil.dataSourcePropertyKey(resolvedProvider, "reactive.url"),
                        SharedDatasourceStartable::setReactiveUrl, true)
                .dependsOnConfig(DataSourceUtil.dataSourcePropertyKey(resolvedProvider, "username"),
                        SharedDatasourceStartable::setUsername, true)
                .dependsOnConfig(DataSourceUtil.dataSourcePropertyKey(resolvedProvider, "password"),
                        SharedDatasourceStartable::setPassword, true)
                .configProvider(s -> makeMirroredConfigMap(dbName, s))
                .postStartHook((s) -> log.infof("Dev Services for %s configured to reuse the Dev Services database of %s",
                        dataSourcePrettyName, getDataSourcePrettyName(resolvedProvider)))
                .build();
    }

    private Map<String, String> makeMirroredConfigMap(String dbName, SharedDatasourceStartable startable) {
        Map<String, String> config = new HashMap<>();
        if (startable.getJdbcUrl() != null) {
            setDataSourceProperties(config, dbName, "jdbc.url", startable.getJdbcUrl());
        }
        if (startable.getReactiveUrl() != null) {
            setDataSourceProperties(config, dbName, "reactive.url", startable.getReactiveUrl());
        }
        if (startable.getUsername() != null) {
            setDataSourceProperties(config, dbName, "username", startable.getUsername());
        }
        if (startable.getPassword() != null) {
            setDataSourceProperties(config, dbName, "password", startable.getPassword());
        }
        return config;
    }

    /**
     * A no-op {@link DatasourceStartable} standing in for a datasource that reuses another datasource's Dev
     * Services database instead of starting its own container.
     */
    private static final class SharedDatasourceStartable implements DatasourceStartable {

        private String jdbcUrl;
        private String reactiveUrl;
        private String username;
        private String password;

        void setJdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
        }

        void setReactiveUrl(String reactiveUrl) {
            this.reactiveUrl = reactiveUrl;
        }

        void setUsername(String username) {
            this.username = username;
        }

        void setPassword(String password) {
            this.password = password;
        }

        String getJdbcUrl() {
            return jdbcUrl;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getReactiveUrl() {
            return reactiveUrl;
        }

        @Override
        public String getEffectiveJdbcUrl() {
            return jdbcUrl;
        }

        @Override
        public void start() {
            // nothing to start, this datasource reuses another one's Dev Services database
        }

        @Override
        public void close() {
            // nothing to close, the shared datasource owns the actual container
        }

        @Override
        public String getConnectionInfo() {
            return jdbcUrl != null ? jdbcUrl : reactiveUrl;
        }

        @Override
        public String getContainerId() {
            return null;
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

    private static Map<String, String> resolveDeferredConfig(DatasourceStartable startable,
            Map<String, Function<DatasourceStartable, String>> deferredConfigProviders) {
        Map<String, String> resolved = new HashMap<>();
        for (Map.Entry<String, Function<DatasourceStartable, String>> entry : deferredConfigProviders.entrySet()) {
            resolved.put(entry.getKey(), entry.getValue().apply(startable));
        }
        return resolved;
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
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            String datasourceName,
            Set<DatabaseFeature> requiredFeatures) {
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
                dataSourceBuildTimeConfig.devservices().showLogs(),
                datasourceName,
                requiredFeatures);
    }

    private void setDataSourceProperties(Map<String, String> propertiesMap, String dbName, String propertyKeyRadical,
            String value) {
        for (String key : DataSourceUtil.dataSourcePropertyKeys(dbName, propertyKeyRadical)) {
            propertiesMap.put(key, value);
        }
    }

}
