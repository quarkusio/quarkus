package io.quarkus.agroal.deployment;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;
import javax.sql.XADataSource;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourceSupport;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.LegacyDataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.LegacyDataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.runtime.metrics.AgroalCounter;
import io.quarkus.agroal.runtime.metrics.AgroalGauge;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

@SuppressWarnings("deprecation")
class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final DotName DATA_SOURCE = DotName.createSimple(javax.sql.DataSource.class.getName());

    private static final String QUARKUS_DATASOURCE_CONFIG_PREFIX = "quarkus.datasource.";
    private static final String QUARKUS_DATASOURCE_DB_KIND_CONFIG_NAME = "db-kind";
    private static final String QUARKUS_DATASOURCE_DRIVER_CONFIG_NAME = "driver";

    @BuildStep
    void agroal(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<CapabilityBuildItem> capability) {
        feature.produce(new FeatureBuildItem(Feature.AGROAL));
        capability.produce(new CapabilityBuildItem(Capability.AGROAL));
    }

    @BuildStep
    void build(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedConfig) throws Exception {
        List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs = getAggregatedConfigBuildItems(
                dataSourcesBuildTimeConfig,
                dataSourcesJdbcBuildTimeConfig,
                legacyDataSourcesJdbcBuildTimeConfig,
                jdbcDriverBuildItems);

        if (aggregatedDataSourceBuildTimeConfigs.isEmpty()) {
            log.warn("The Agroal dependency is present but no JDBC datasources have been defined.");
            return;
        }

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            validateBuildTimeConfig(aggregatedDataSourceBuildTimeConfig);

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false,
                            aggregatedDataSourceBuildTimeConfig.getResolvedDriverClass()));

            aggregatedConfig.produce(aggregatedDataSourceBuildTimeConfig);
        }

        // For now, we can't push the security providers to Agroal so we need to include
        // the service file inside the image. Hopefully, we will get an entry point to
        // resolve them at build time and push them to Agroal soon.
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/" + io.agroal.api.security.AgroalSecurityProvider.class.getName()));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                io.agroal.api.security.AgroalDefaultSecurityProvider.class.getName(),
                io.agroal.api.security.AgroalKerberosSecurityProvider.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()));

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.AGROAL.getName()));
    }

    private static void validateBuildTimeConfig(AggregatedDataSourceBuildTimeConfigBuildItem aggregatedConfig) {
        DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = aggregatedConfig.getJdbcConfig();

        String fullDataSourceName = aggregatedConfig.isDefault() ? "default datasource"
                : "datasource named '" + aggregatedConfig.getName() + "'";

        String driverName = aggregatedConfig.getResolvedDriverClass();
        Class<?> driver;
        try {
            driver = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Unable to load the datasource driver " + driverName + " for the " + fullDataSourceName, e);
        }
        if (jdbcBuildTimeConfig.transactions == TransactionIntegration.XA) {
            if (!XADataSource.class.isAssignableFrom(driver)) {
                throw new ConfigurationException(
                        "Driver is not an XA dataSource, while XA has been enabled in the configuration of the "
                                + fullDataSourceName + ": either disable XA or switch the driver to an XADataSource");
            }
        } else {
            if (driver != null && !javax.sql.DataSource.class.isAssignableFrom(driver)
                    && !Driver.class.isAssignableFrom(driver)) {
                if (aggregatedConfig.isDefault()) {
                    throw new ConfigurationException(
                            "Driver " + driverName
                                    + " is an XA datasource, but XA transactions have not been enabled on the default datasource; please either set 'quarkus.datasource.jdbc.transactions=xa' or switch to a standard non-XA JDBC driver implementation");
                } else {
                    throw new ConfigurationException(
                            "Driver " + driverName
                                    + " is an XA datasource, but XA transactions have not been enabled on the datasource named '"
                                    + fullDataSourceName + "'; please either set 'quarkus.datasource." + fullDataSourceName
                                    + ".jdbc.transactions=xa' or switch to a standard non-XA JDBC driver implementation");
                }
            }
        }
    }

    private DataSourceSupport getDataSourceSupport(
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig, Capabilities capabilities) {
        Map<String, DataSourceSupport.Entry> dataSourceSupportEntries = new HashMap<>();
        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedBuildTimeConfigBuildItems) {
            String dataSourceName = aggregatedDataSourceBuildTimeConfig.getName();
            dataSourceSupportEntries.put(dataSourceName,
                    new DataSourceSupport.Entry(dataSourceName, aggregatedDataSourceBuildTimeConfig.getResolvedDbKind(),
                            aggregatedDataSourceBuildTimeConfig.getResolvedDriverClass(),
                            aggregatedDataSourceBuildTimeConfig.isLegacy(), aggregatedDataSourceBuildTimeConfig.isDefault()));
        }

        return new DataSourceSupport(sslNativeConfig.isExplicitlyDisabled(),
                capabilities.isPresent(Capability.METRICS), dataSourceSupportEntries);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void generateDataSourceSupportBean(AgroalRecorder recorder,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig,
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer) {
        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        // make a DataSourceProducer bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(DataSources.class).setUnremovable()
                .setDefaultScope(DotNames.SINGLETON).build());
        // add the @DataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(DataSource.class).build());

        // add implementations of AgroalPoolInterceptor
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(AgroalPoolInterceptor.class));

        // create the DataSourceSupport bean that DataSourceProducer uses as a dependency
        DataSourceSupport dataSourceSupport = getDataSourceSupport(aggregatedBuildTimeConfigBuildItems, sslNativeConfig,
                capabilities);
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(DataSourceSupport.class)
                .supplier(recorder.dataSourceSupportSupplier(dataSourceSupport))
                .unremovable()
                .done());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void generateDataSourceBeans(AgroalRecorder recorder,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig,
            Capabilities capabilities,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<JdbcDataSourceBuildItem> jdbcDataSource) {
        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        for (Map.Entry<String, DataSourceSupport.Entry> entry : getDataSourceSupport(aggregatedBuildTimeConfigBuildItems,
                sslNativeConfig,
                capabilities).entries.entrySet()) {

            String dataSourceName = entry.getKey();

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(AgroalDataSource.class)
                    .addType(DATA_SOURCE)
                    .scope(Singleton.class)
                    .setRuntimeInit()
                    .unremovable()
                    // pass the runtime config into the recorder to ensure that the DataSource related beans
                    // are created after runtime configuration has been setup
                    .supplier(recorder.agroalDataSourceSupplier(dataSourceName, dataSourcesRuntimeConfig));

            if (entry.getValue().isDefault) {
                configurator.addQualifier(Default.class);
            } else {
                // this definitely not ideal, but 'elytron-jdbc-security' uses it (although it could be easily changed)
                // which means that perhaps other extensions might depend on this as well...
                configurator.name(dataSourceName);

                configurator.addQualifier().annotation(DotNames.NAMED).addValue("value", dataSourceName).done();
                configurator.addQualifier().annotation(DataSource.class).addValue("value", dataSourceName).done();
            }

            syntheticBeanBuildItemBuildProducer.produce(configurator.done());

            jdbcDataSource.produce(new JdbcDataSourceBuildItem(dataSourceName,
                    entry.getValue().resolvedDbKind,
                    entry.getValue().isDefault));
        }
    }

    @BuildStep
    void configureDataSources(BuildProducer<JdbcDataSourceBuildItem> jdbcDataSource,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig) {
        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedBuildTimeConfigBuildItem : aggregatedBuildTimeConfigBuildItems) {
            jdbcDataSource.produce(new JdbcDataSourceBuildItem(aggregatedBuildTimeConfigBuildItem.getName(),
                    aggregatedBuildTimeConfigBuildItem.getResolvedDbKind(),
                    DataSourceUtil.isDefault(aggregatedBuildTimeConfigBuildItem.getName())));
        }
    }

    private List<AggregatedDataSourceBuildTimeConfigBuildItem> getAggregatedConfigBuildItems(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            LegacyDataSourcesJdbcBuildTimeConfig legacyDataSourcesJdbcBuildTimeConfig,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems) {
        List<AggregatedDataSourceBuildTimeConfigBuildItem> dataSources = new ArrayList<>();

        // New configuration
        if (dataSourcesBuildTimeConfig.defaultDataSource.dbKind.isPresent()) {
            if (dataSourcesJdbcBuildTimeConfig.jdbc.enabled) {
                dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                        dataSourcesBuildTimeConfig.defaultDataSource,
                        dataSourcesJdbcBuildTimeConfig.jdbc,
                        null,
                        dataSourcesBuildTimeConfig.defaultDataSource.dbKind.get(),
                        resolveDriver(null, dataSourcesBuildTimeConfig.defaultDataSource,
                                dataSourcesJdbcBuildTimeConfig.jdbc, jdbcDriverBuildItems),
                        false));
            }
        }
        for (Entry<String, DataSourceBuildTimeConfig> entry : dataSourcesBuildTimeConfig.namedDataSources.entrySet()) {
            DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig.namedDataSources
                    .containsKey(entry.getKey()) ? dataSourcesJdbcBuildTimeConfig.namedDataSources.get(entry.getKey()).jdbc
                            : new DataSourceJdbcBuildTimeConfig();
            if (!jdbcBuildTimeConfig.enabled) {
                continue;
            }
            dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(entry.getKey(),
                    entry.getValue(),
                    jdbcBuildTimeConfig,
                    null,
                    entry.getValue().dbKind.get(),
                    resolveDriver(entry.getKey(), entry.getValue(), jdbcBuildTimeConfig, jdbcDriverBuildItems),
                    false));
        }

        // Legacy configuration
        if (legacyDataSourcesJdbcBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            String resolvedDbKind = resolveLegacyKind(legacyDataSourcesJdbcBuildTimeConfig.defaultDataSource.driver.get());
            boolean alreadyConfigured = ensureNoConfigurationClash(dataSources, DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                    resolvedDbKind);

            if (!alreadyConfigured) {
                dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                        dataSourcesBuildTimeConfig.defaultDataSource,
                        dataSourcesJdbcBuildTimeConfig.jdbc,
                        legacyDataSourcesJdbcBuildTimeConfig.defaultDataSource,
                        resolvedDbKind,
                        legacyDataSourcesJdbcBuildTimeConfig.defaultDataSource.driver.get(),
                        true));
            }
        }
        for (Entry<String, LegacyDataSourceJdbcBuildTimeConfig> entry : legacyDataSourcesJdbcBuildTimeConfig.namedDataSources
                .entrySet()) {
            String datasourceName = entry.getKey();
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig = dataSourcesBuildTimeConfig.namedDataSources
                    .containsKey(datasourceName) ? dataSourcesBuildTimeConfig.namedDataSources.get(datasourceName)
                            : new DataSourceBuildTimeConfig();
            DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig.namedDataSources
                    .containsKey(datasourceName) ? dataSourcesJdbcBuildTimeConfig.namedDataSources.get(datasourceName).jdbc
                            : new DataSourceJdbcBuildTimeConfig();

            String resolvedDbKind = resolveLegacyKind(entry.getValue().driver.get());
            boolean alreadyConfigured = ensureNoConfigurationClash(dataSources, datasourceName, resolvedDbKind);

            if (!alreadyConfigured) {
                dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(datasourceName,
                        dataSourceBuildTimeConfig,
                        jdbcBuildTimeConfig,
                        entry.getValue(),
                        resolvedDbKind,
                        entry.getValue().driver.get(),
                        true));
            }
        }

        return dataSources;
    }

    private boolean ensureNoConfigurationClash(List<AggregatedDataSourceBuildTimeConfigBuildItem> dataSources,
            String datasourceName, String resolvedDbKind) {

        boolean alreadyConfigured = false;
        for (AggregatedDataSourceBuildTimeConfigBuildItem alreadyConfiguredDataSource : dataSources) {
            if (alreadyConfiguredDataSource.getName().equals(datasourceName)) {
                if (!alreadyConfiguredDataSource.getResolvedDbKind().equals(resolvedDbKind)) {
                    throw new RuntimeException("Incompatible values detected between "
                            + quotedDataSourcePropertyName(datasourceName, QUARKUS_DATASOURCE_DB_KIND_CONFIG_NAME) + " and "
                            + quotedDataSourcePropertyName(datasourceName, QUARKUS_DATASOURCE_DRIVER_CONFIG_NAME)
                            + ". Consider removing the latter.");
                }
                alreadyConfigured = true;
            }
        }
        if (alreadyConfigured) {
            log.warn("Configuring " + quotedDataSourcePropertyName(datasourceName, QUARKUS_DATASOURCE_DRIVER_CONFIG_NAME)
                    + " is redundant when "
                    + quotedDataSourcePropertyName(datasourceName, QUARKUS_DATASOURCE_DB_KIND_CONFIG_NAME)
                    + " is also configured");
        }

        return alreadyConfigured;
    }

    private String quotedDataSourcePropertyName(String datasourceName, String propertyName) {
        return "\"" + dataSourcePropertyName(datasourceName, propertyName) + "\"";
    }

    private String dataSourcePropertyName(String datasourceName, String propertyName) {
        if (DataSourceUtil.DEFAULT_DATASOURCE_NAME.equals(datasourceName)) {
            return QUARKUS_DATASOURCE_CONFIG_PREFIX + propertyName;
        }
        return QUARKUS_DATASOURCE_CONFIG_PREFIX + datasourceName + "." + propertyName;
    }

    private String resolveDriver(String dataSourceName, DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig, List<JdbcDriverBuildItem> jdbcDriverBuildItems) {
        if (dataSourceJdbcBuildTimeConfig.driver.isPresent()) {
            return dataSourceJdbcBuildTimeConfig.driver.get();
        }

        Optional<JdbcDriverBuildItem> matchingJdbcDriver = jdbcDriverBuildItems.stream()
                .filter(i -> dataSourceBuildTimeConfig.dbKind.get().equals(i.getDbKind()))
                .findFirst();

        if (matchingJdbcDriver.isPresent()) {
            if (io.quarkus.agroal.runtime.TransactionIntegration.XA == dataSourceJdbcBuildTimeConfig.transactions) {
                if (matchingJdbcDriver.get().getDriverXAClass().isPresent()) {
                    return matchingJdbcDriver.get().getDriverXAClass().get();
                }
            } else {
                return matchingJdbcDriver.get().getDriverClass();
            }
        }

        throw new ConfigurationException("Unable to determine the driver for " + (dataSourceName == null ? "default datasource"
                : "datasource named " + dataSourceName));
    }

    private String resolveLegacyKind(String driver) {
        switch (driver) {
            case "org.apache.derby.jdbc.ClientDriver":
            case "org.apache.derby.jdbc.ClientXADataSource":
                return DatabaseKind.DERBY;
            case "org.h2.Driver":
            case "org.h2.jdbcx.JdbcDataSource":
                return DatabaseKind.H2;
            case "org.mariadb.jdbc.Driver":
            case "org.mariadb.jdbc.MySQLDataSource":
                return DatabaseKind.MARIADB;
            case "com.microsoft.sqlserver.jdbc.SQLServerDriver":
            case "com.microsoft.sqlserver.jdbc.SQLServerXADataSource":
                return DatabaseKind.MSSQL;
            case "com.mysql.cj.jdbc.Driver":
            case "com.mysql.cj.jdbc.MysqlXADataSource":
                return DatabaseKind.MYSQL;
            case "org.postgresql.Driver":
            case "org.postgresql.xa.PGXADataSource":
                return DatabaseKind.POSTGRESQL;
        }

        return "other-legacy";
    }

    @BuildStep
    HealthBuildItem addHealthCheck(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.agroal.runtime.health.DataSourceHealthCheck",
                dataSourcesBuildTimeConfig.healthEnabled);
    }

    @BuildStep
    void registerMetrics(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs,
            BuildProducer<MetricBuildItem> metrics) {
        Metadata activeCountMetadata = Metadata.builder()
                .withName("agroal.active.count")
                .withDescription("Number of active connections. These connections are in use and not available to be acquired.")
                .withType(MetricType.GAUGE)
                .build();
        Metadata availableCountMetadata = Metadata.builder()
                .withName("agroal.available.count")
                .withDescription("Number of idle connections in the pool, available to be acquired.")
                .withType(MetricType.GAUGE)
                .build();
        Metadata maxUsedCountMetadata = Metadata.builder()
                .withName("agroal.max.used.count")
                .withDescription("Maximum number of connections active simultaneously.")
                .withType(MetricType.GAUGE)
                .build();
        Metadata awaitingCountMetadata = Metadata.builder()
                .withName("agroal.awaiting.count")
                .withDescription("Approximate number of threads blocked, waiting to acquire a connection.")
                .withType(MetricType.GAUGE)
                .build();
        Metadata blockingTimeAverageMetadata = Metadata.builder()
                .withName("agroal.blocking.time.average")
                .withDescription("Average time an application waited to acquire a connection.")
                .withUnit(MetricUnits.MILLISECONDS)
                .withType(MetricType.GAUGE)
                .build();
        Metadata blockingTimeMaxMetadata = Metadata.builder()
                .withName("agroal.blocking.time.max")
                .withDescription("Maximum time an application waited to acquire a connection.")
                .withUnit(MetricUnits.MILLISECONDS)
                .withType(MetricType.GAUGE)
                .build();
        Metadata blockingTimeTotalMetadata = Metadata.builder()
                .withName("agroal.blocking.time.total")
                .withDescription("Total time applications waited to acquire a connection.")
                .withUnit(MetricUnits.MILLISECONDS)
                .withType(MetricType.GAUGE)
                .build();
        Metadata creationTimeAverageMetadata = Metadata.builder()
                .withName("agroal.creation.time.average")
                .withDescription("Average time for a connection to be created.")
                .withUnit(MetricUnits.MILLISECONDS)
                .withType(MetricType.GAUGE)
                .build();
        Metadata creationTimeMaxMetadata = Metadata.builder()
                .withName("agroal.creation.time.max")
                .withDescription("Maximum time for a connection to be created.")
                .withUnit(MetricUnits.MILLISECONDS)
                .withType(MetricType.GAUGE)
                .build();
        Metadata creationTimeTotalMetadata = Metadata.builder()
                .withName("agroal.creation.time.total")
                .withDescription("Total time waiting for connections to be created.")
                .withUnit(MetricUnits.MILLISECONDS)
                .withType(MetricType.GAUGE)
                .build();
        Metadata acquireCountMetadata = Metadata.builder()
                .withName("agroal.acquire.count")
                .withDescription("Number of times an acquire operation succeeded.")
                .withType(MetricType.COUNTER)
                .build();
        Metadata creationCountMetadata = Metadata.builder()
                .withName("agroal.creation.count")
                .withDescription("Number of created connections.")
                .withType(MetricType.COUNTER)
                .build();
        Metadata leakDetectionCountMetadata = Metadata.builder()
                .withName("agroal.leak.detection.count")
                .withDescription("Number of times a leak was detected. A single connection can be detected multiple times.")
                .withType(MetricType.COUNTER)
                .build();
        Metadata destroyCountMetadata = Metadata.builder()
                .withName("agroal.destroy.count")
                .withDescription("Number of destroyed connections.")
                .withType(MetricType.COUNTER)
                .build();
        Metadata flushCountMetadata = Metadata.builder()
                .withName("agroal.flush.count")
                .withDescription("Number of connections removed from the pool, not counting invalid / idle.")
                .withType(MetricType.COUNTER)
                .build();
        Metadata invalidCountMetadata = Metadata.builder()
                .withName("agroal.invalid.count")
                .withDescription("Number of connections removed from the pool for being invalid.")
                .withType(MetricType.COUNTER)
                .build();
        Metadata reapCountMetadata = Metadata.builder()
                .withName("agroal.reap.count")
                .withDescription("Number of connections removed from the pool for being idle.")
                .withType(MetricType.COUNTER)
                .build();

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            String dataSourceName = aggregatedDataSourceBuildTimeConfig.getName();
            // expose metrics for this datasource if metrics are enabled both globally and for this data source
            // (they are enabled for each data source by default if they are also enabled globally)
            boolean metricsEnabledForThisDatasource = dataSourcesBuildTimeConfig.metricsEnabled &&
                    aggregatedDataSourceBuildTimeConfig.getJdbcConfig().enableMetrics.orElse(true);
            Tag tag = new Tag("datasource", DataSourceUtil.isDefault(dataSourceName) ? "default" : dataSourceName);
            String configRootName = "datasource";
            metrics.produce(new MetricBuildItem(activeCountMetadata,
                    new AgroalGauge(dataSourceName, "activeCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(maxUsedCountMetadata,
                    new AgroalGauge(dataSourceName, "maxUsedCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(awaitingCountMetadata,
                    new AgroalGauge(dataSourceName, "awaitingCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(availableCountMetadata,
                    new AgroalGauge(dataSourceName, "availableCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(blockingTimeAverageMetadata,
                    new AgroalGauge(dataSourceName, "blockingTimeAverage"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(blockingTimeMaxMetadata,
                    new AgroalGauge(dataSourceName, "blockingTimeMax"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(blockingTimeTotalMetadata,
                    new AgroalGauge(dataSourceName, "blockingTimeTotal"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(creationTimeAverageMetadata,
                    new AgroalGauge(dataSourceName, "creationTimeAverage"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(creationTimeMaxMetadata,
                    new AgroalGauge(dataSourceName, "creationTimeMax"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(creationTimeTotalMetadata,
                    new AgroalGauge(dataSourceName, "creationTimeTotal"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(acquireCountMetadata,
                    new AgroalCounter(dataSourceName, "acquireCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(creationCountMetadata,
                    new AgroalCounter(dataSourceName, "creationCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(leakDetectionCountMetadata,
                    new AgroalCounter(dataSourceName, "leakDetectionCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(destroyCountMetadata,
                    new AgroalCounter(dataSourceName, "destroyCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(flushCountMetadata,
                    new AgroalCounter(dataSourceName, "flushCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(invalidCountMetadata,
                    new AgroalCounter(dataSourceName, "invalidCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
            metrics.produce(new MetricBuildItem(reapCountMetadata,
                    new AgroalCounter(dataSourceName, "reapCount"),
                    metricsEnabledForThisDatasource,
                    configRootName,
                    tag));
        }
    }
}
