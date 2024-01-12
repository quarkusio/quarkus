package io.quarkus.agroal.deployment;

import static io.quarkus.deployment.Capability.OPENTELEMETRY_TRACER;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.XADataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.AgroalDataSourcesInitializer;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourceSupport;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.agroal.spi.OpenTelemetryInitBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.bootstrap.classloading.QuarkusClassLoader;
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
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.narayana.jta.deployment.NarayanaInitBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

@SuppressWarnings("deprecation")
class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final String OPEN_TELEMETRY_DRIVER = "io.opentelemetry.instrumentation.jdbc.OpenTelemetryDriver";
    private static final DotName DATA_SOURCE = DotName.createSimple(javax.sql.DataSource.class.getName());
    private static final DotName AGROAL_DATA_SOURCE = DotName.createSimple(AgroalDataSource.class.getName());

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
            Capabilities capabilities,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            CurateOutcomeBuildItem curateOutcomeBuildItem) throws Exception {
        if (dataSourcesBuildTimeConfig.driver().isPresent() || dataSourcesBuildTimeConfig.url().isPresent()) {
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

        boolean otelJdbcInstrumentationActive = false;
        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            validateBuildTimeConfig(aggregatedDataSourceBuildTimeConfig);

            if (aggregatedDataSourceBuildTimeConfig.getJdbcConfig().tracing()) {
                reflectiveClass
                        .produce(ReflectiveClassBuildItem.builder(DataSources.TRACING_DRIVER_CLASSNAME).methods()
                                .build());
            }

            if (aggregatedDataSourceBuildTimeConfig.getJdbcConfig().telemetry()) {
                otelJdbcInstrumentationActive = true;
            }

            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(aggregatedDataSourceBuildTimeConfig.getResolvedDriverClass())
                            .methods().build());

            aggregatedConfig.produce(aggregatedDataSourceBuildTimeConfig);
        }

        if (otelJdbcInstrumentationActive && capabilities.isPresent(OPENTELEMETRY_TRACER)) {
            // at least one datasource is using OpenTelemetry JDBC instrumentation,
            // therefore we register the OpenTelemetry data source wrapper bean
            additionalBeans.produce(new AdditionalBeanBuildItem.Builder()
                    .addBeanClass("io.quarkus.agroal.runtime.AgroalOpenTelemetryWrapper")
                    .setDefaultScope(DotNames.SINGLETON).build());
        }

        // For now, we can't push the security providers to Agroal so we need to include
        // the service file inside the image. Hopefully, we will get an entry point to
        // resolve them at build time and push them to Agroal soon.
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/" + io.agroal.api.security.AgroalSecurityProvider.class.getName()));

        reflectiveClass.produce(ReflectiveClassBuildItem.builder(io.agroal.pool.ConnectionHandler[].class.getName(),
                io.agroal.pool.ConnectionHandler.class.getName(),
                io.agroal.api.security.AgroalDefaultSecurityProvider.class.getName(),
                io.agroal.api.security.AgroalKerberosSecurityProvider.class.getName(),
                java.sql.Statement[].class.getName(),
                java.sql.Statement.class.getName(),
                java.sql.ResultSet.class.getName(),
                java.sql.ResultSet[].class.getName()).build());

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(Feature.AGROAL.getName()));
    }

    private static void validateBuildTimeConfig(AggregatedDataSourceBuildTimeConfigBuildItem aggregatedConfig) {
        DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = aggregatedConfig.getJdbcConfig();

        String fullDataSourceName = aggregatedConfig.isDefault() ? "default datasource"
                : "datasource named '" + aggregatedConfig.getName() + "'";

        if (jdbcBuildTimeConfig.tracing()) {
            if (!QuarkusClassLoader.isClassPresentAtRuntime(DataSources.TRACING_DRIVER_CLASSNAME)) {
                throw new ConfigurationException(
                        "Unable to load the tracing driver " + DataSources.TRACING_DRIVER_CLASSNAME + " for the "
                                + fullDataSourceName);
            }
        }

        String driverName = aggregatedConfig.getResolvedDriverClass();
        Class<?> driver;
        try {
            driver = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(
                    "Unable to load the datasource driver " + driverName + " for the " + fullDataSourceName, e);
        }
        if (jdbcBuildTimeConfig.transactions() == TransactionIntegration.XA) {
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
                            aggregatedDataSourceBuildTimeConfig.getDataSourceConfig().dbVersion(),
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
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JdbcDriver.class));

        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        // make a DataSources bean
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(DataSources.class).setUnremovable()
                .setDefaultScope(DotNames.SINGLETON).build());
        // add the @DataSource class otherwise it won't be registered as a qualifier
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(DataSource.class).build());
        // make sure datasources are initialized at startup
        additionalBeans.produce(new AdditionalBeanBuildItem(AgroalDataSourcesInitializer.class));

        // make AgroalPoolInterceptor beans unremovable, users still have to make them beans
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(AgroalPoolInterceptor.class));

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
    @Consume(OpenTelemetryInitBuildItem.class)
    @Consume(NarayanaInitBuildItem.class)
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

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedBuildTimeConfigBuildItem : aggregatedBuildTimeConfigBuildItems) {

            String dataSourceName = aggregatedBuildTimeConfigBuildItem.getName();

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(AgroalDataSource.class)
                    .addType(DATA_SOURCE)
                    .addType(AGROAL_DATA_SOURCE)
                    .scope(ApplicationScoped.class)
                    .setRuntimeInit()
                    .unremovable()
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DataSources.class)))
                    // pass the runtime config into the recorder to ensure that the DataSource related beans
                    // are created after runtime configuration has been set up
                    .createWith(recorder.agroalDataSourceSupplier(dataSourceName, dataSourcesRuntimeConfig));

            if (aggregatedBuildTimeConfigBuildItem.isDefault()) {
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
                    aggregatedBuildTimeConfigBuildItem.getDbKind(),
                    aggregatedBuildTimeConfigBuildItem.getDataSourceConfig().dbVersion(),
                    aggregatedBuildTimeConfigBuildItem.getJdbcConfig().transactions() != TransactionIntegration.DISABLED,
                    aggregatedBuildTimeConfigBuildItem.isDefault()));
        }
    }

    private List<AggregatedDataSourceBuildTimeConfigBuildItem> getAggregatedConfigBuildItems(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems,
            List<DefaultDataSourceDbKindBuildItem> defaultDbKinds) {
        List<AggregatedDataSourceBuildTimeConfigBuildItem> dataSources = new ArrayList<>();

        for (Entry<String, DataSourceBuildTimeConfig> entry : dataSourcesBuildTimeConfig.dataSources().entrySet()) {
            DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig
                    .dataSources().get(entry.getKey()).jdbc();
            if (!jdbcBuildTimeConfig.enabled()) {
                continue;
            }

            boolean enableImplicitResolution = DataSourceUtil.isDefault(entry.getKey())
                    ? entry.getValue().devservices().enabled().orElse(!dataSourcesBuildTimeConfig.hasNamedDataSources())
                    : true;

            Optional<String> effectiveDbKind = DefaultDataSourceDbKindBuildItem
                    .resolve(entry.getValue().dbKind(), defaultDbKinds,
                            enableImplicitResolution,
                            curateOutcomeBuildItem);

            if (!effectiveDbKind.isPresent()) {
                continue;
            }

            dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(entry.getKey(),
                    entry.getValue(),
                    jdbcBuildTimeConfig,
                    effectiveDbKind.get(),
                    resolveDriver(entry.getKey(), effectiveDbKind.get(), jdbcBuildTimeConfig, jdbcDriverBuildItems)));
        }

        return dataSources;
    }

    private String resolveDriver(String dataSourceName, String dbKind,
            DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig, List<JdbcDriverBuildItem> jdbcDriverBuildItems) {
        if (dataSourceJdbcBuildTimeConfig.driver().isPresent()) {
            return dataSourceJdbcBuildTimeConfig.driver().get();
        }

        Optional<JdbcDriverBuildItem> matchingJdbcDriver = jdbcDriverBuildItems.stream()
                .filter(i -> dbKind.equals(i.getDbKind()))
                .findFirst();

        if (matchingJdbcDriver.isPresent()) {
            if (io.quarkus.agroal.runtime.TransactionIntegration.XA == dataSourceJdbcBuildTimeConfig.transactions()) {
                if (matchingJdbcDriver.get().getDriverXAClass().isPresent()) {
                    return matchingJdbcDriver.get().getDriverXAClass().get();
                }
            } else {
                return matchingJdbcDriver.get().getDriverClass();
            }
        }

        throw new ConfigurationException(String.format(
                "Unable to find a JDBC driver corresponding to the database kind '%s' for the %s (available: '%s'). "
                        + "Check if it's a typo, otherwise provide a suitable JDBC driver extension, define the driver manually,"
                        + " or disable the JDBC datasource by adding '%s=false' to your configuration if you don't need it.",
                dbKind, DataSourceUtil.isDefault(dataSourceName) ? "default datasource" : "datasource '" + dataSourceName + "'",
                jdbcDriverBuildItems.stream().map(JdbcDriverBuildItem::getDbKind).collect(Collectors.joining("','")),
                DataSourceUtil.dataSourcePropertyKey(dataSourceName, "jdbc")));
    }

    @BuildStep
    HealthBuildItem addHealthCheck(Capabilities capabilities, DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        if (capabilities.isPresent(Capability.SMALLRYE_HEALTH)) {
            return new HealthBuildItem("io.quarkus.agroal.runtime.health.DataSourceHealthCheck",
                    dataSourcesBuildTimeConfig.healthEnabled());
        } else {
            return null;
        }
    }

    /**
     * TODO: remove the step when https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/8080 is closed
     */
    @BuildStep
    void adaptOpenTelemetryJdbcInstrumentationForNative(BuildProducer<RemovedResourceBuildItem> producer,
            Capabilities capabilities) {
        // remove 'JdbcSingletons' as it initialize OpenTelemetry at build time
        // 'OpenTelemetryDriver' is removed as it is directly using 'JdbcSingletons'
        // we also need to check for the driver presence at classpath as it is possible that both OpenTelemetry
        // and Agroal extensions are used, but dependency 'opentelemetry-jdbc' is not present
        if (capabilities.isPresent(OPENTELEMETRY_TRACER) && QuarkusClassLoader.isClassPresentAtRuntime(OPEN_TELEMETRY_DRIVER)) {
            producer.produce(
                    new RemovedResourceBuildItem(ArtifactKey.fromString("io.opentelemetry.instrumentation:opentelemetry-jdbc"),
                            Set.of("META-INF/services/java.sql.Driver")));
            producer.produce(
                    new RemovedResourceBuildItem(ArtifactKey.fromString("io.opentelemetry.instrumentation:opentelemetry-jdbc"),
                            Set.of("io/opentelemetry/instrumentation/jdbc/OpenTelemetryDriver")));
            producer.produce(
                    new RemovedResourceBuildItem(ArtifactKey.fromString("io.opentelemetry.instrumentation:opentelemetry-jdbc"),
                            Set.of("io/opentelemetry/instrumentation.jdbc/internal/JdbcSingletons")));
        }
    }
}
