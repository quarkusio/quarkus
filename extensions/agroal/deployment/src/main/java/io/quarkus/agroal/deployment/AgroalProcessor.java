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
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbKindBuildItem;
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
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

@SuppressWarnings("deprecation")
class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final DotName DATA_SOURCE = DotName.createSimple(javax.sql.DataSource.class.getName());

    @BuildStep
    void agroal(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.AGROAL));
    }

    @BuildStep
    void build(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            List<DefaultDataSourceDbKindBuildItem> defaultDbKinds,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {
        if (dataSourcesBuildTimeConfig.driver.isPresent() || dataSourcesBuildTimeConfig.url.isPresent()) {
            throw new ConfigurationException(
                    "quarkus.datasource.url and quarkus.datasource.driver have been deprecated in Quarkus 1.3 and removed in 1.9. "
                            + "Please use the new datasource configuration as explained in https://quarkus.io/guides/datasource.");
        }

        List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs = getAggregatedConfigBuildItems(
                dataSourcesBuildTimeConfig,
                dataSourcesJdbcBuildTimeConfig, curateOutcomeBuildItem,
                jdbcDriverBuildItems, defaultDbKinds);

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
                    new DataSourceSupport.Entry(dataSourceName, aggregatedDataSourceBuildTimeConfig.getDbKind(),
                            aggregatedDataSourceBuildTimeConfig.getResolvedDriverClass(),
                            aggregatedDataSourceBuildTimeConfig.isDefault()));
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

    private List<AggregatedDataSourceBuildTimeConfigBuildItem> getAggregatedConfigBuildItems(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems,
            List<DefaultDataSourceDbKindBuildItem> defaultDbKinds) {
        List<AggregatedDataSourceBuildTimeConfigBuildItem> dataSources = new ArrayList<>();

        Optional<String> effectiveDbKind = DefaultDataSourceDbKindBuildItem
                .resolve(dataSourcesBuildTimeConfig.defaultDataSource.dbKind, defaultDbKinds,
                        dataSourcesBuildTimeConfig.defaultDataSource.devservices.enabled.orElse(
                                dataSourcesBuildTimeConfig.defaultDataSource.devservices.enabledDeprecated
                                        .orElse(dataSourcesBuildTimeConfig.namedDataSources.isEmpty())),
                        curateOutcomeBuildItem);

        if (effectiveDbKind.isPresent()) {
            if (dataSourcesJdbcBuildTimeConfig.jdbc.enabled) {
                dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                        dataSourcesBuildTimeConfig.defaultDataSource,
                        dataSourcesJdbcBuildTimeConfig.jdbc,
                        effectiveDbKind.get(),
                        resolveDriver(DataSourceUtil.DEFAULT_DATASOURCE_NAME, effectiveDbKind.get(),
                                dataSourcesJdbcBuildTimeConfig.jdbc, jdbcDriverBuildItems)));
            }
        }
        for (Entry<String, DataSourceBuildTimeConfig> entry : dataSourcesBuildTimeConfig.namedDataSources.entrySet()) {
            DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig.namedDataSources
                    .containsKey(entry.getKey()) ? dataSourcesJdbcBuildTimeConfig.namedDataSources.get(entry.getKey()).jdbc
                            : new DataSourceJdbcBuildTimeConfig();
            if (!jdbcBuildTimeConfig.enabled) {
                continue;
            }
            Optional<String> dbKind = DefaultDataSourceDbKindBuildItem
                    .resolve(entry.getValue().dbKind, defaultDbKinds,
                            true,
                            curateOutcomeBuildItem);
            if (!dbKind.isPresent()) {
                continue;
            }
            dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(entry.getKey(),
                    entry.getValue(),
                    jdbcBuildTimeConfig,
                    dbKind.get(),
                    resolveDriver(entry.getKey(), dbKind.get(), jdbcBuildTimeConfig, jdbcDriverBuildItems)));
        }

        return dataSources;
    }

    private String resolveDriver(String dataSourceName, String dbKind,
            DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig, List<JdbcDriverBuildItem> jdbcDriverBuildItems) {
        if (dataSourceJdbcBuildTimeConfig.driver.isPresent()) {
            return dataSourceJdbcBuildTimeConfig.driver.get();
        }

        Optional<JdbcDriverBuildItem> matchingJdbcDriver = jdbcDriverBuildItems.stream()
                .filter(i -> dbKind.equals(i.getDbKind()))
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

        throw new ConfigurationException("Unable to find a JDBC driver corresponding to the database kind '"
                + dbKind + "' for the "
                + (DataSourceUtil.isDefault(dataSourceName) ? "default datasource"
                        : "datasource '" + dataSourceName + "'")
                + ". Either provide a suitable JDBC driver extension, define the driver manually, or disable the JDBC datasource by adding "
                + (DataSourceUtil.isDefault(dataSourceName) ? "'quarkus.datasource.jdbc=false'"
                        : "'quarkus.datasource." + dataSourceName + ".jdbc=false'")
                + " to your configuration if you don't need it.");
    }

    @BuildStep
    HealthBuildItem addHealthCheck(Capabilities capabilities, DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem("io.quarkus.agroal.runtime.health.DataSourceHealthCheck",
                    dataSourcesBuildTimeConfig.healthEnabled);
        } else {
            return null;
        }
    }
}
