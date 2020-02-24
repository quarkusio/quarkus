package io.quarkus.agroal.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.sql.XADataSource;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.Tag;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.metrics.AgroalCounter;
import io.quarkus.agroal.metrics.AgroalGauge;
import io.quarkus.agroal.runtime.AbstractDataSourceProducer;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourceJdbcRuntimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final Set<DotName> UNREMOVABLE_BEANS = new HashSet<>(Arrays.asList(
            DotName.createSimple(AbstractDataSourceProducer.class.getName()),
            DotName.createSimple(javax.sql.DataSource.class.getName())));

    @Record(STATIC_INIT)
    @BuildStep(loadsApplicationClasses = true)
    void build(
            RecorderContext recorderContext,
            AgroalRecorder recorder,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            BuildProducer<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedConfig,
            Capabilities capabilities) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.AGROAL));

        List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs = getAggregatedConfigBuildItems(
                dataSourcesBuildTimeConfig,
                dataSourcesJdbcBuildTimeConfig);

        if (aggregatedDataSourceBuildTimeConfigs.isEmpty()) {
            log.warn("The Agroal dependency is present but no JDBC datasources have been defined.");
            return;
        }

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            validateBuildTimeConfig(aggregatedDataSourceBuildTimeConfig);

            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false,
                            aggregatedDataSourceBuildTimeConfig.getJdbcConfig().driver.get()));

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
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.AGROAL));

        // Generate the DataSourceProducer bean
        String dataSourceProducerClassName = AbstractDataSourceProducer.class.getPackage().getName() + "."
                + "DataSourceProducer";

        createDataSourceProducerBean(generatedBean, dataSourceProducerClassName,
                aggregatedDataSourceBuildTimeConfigs,
                capabilities.isCapabilityPresent(Capabilities.METRICS));
    }

    private static void validateBuildTimeConfig(AggregatedDataSourceBuildTimeConfigBuildItem aggregatedConfig) {
        DataSourceJdbcBuildTimeConfig jdbcBuildTimeConfig = aggregatedConfig.getJdbcConfig();

        String fullDataSourceName = aggregatedConfig.isDefault() ? "default datasource"
                : "datasource named '" + aggregatedConfig.getName() + "'";

        // TODO: make the driver optional
        String driverName = jdbcBuildTimeConfig.driver.get();
        Class<?> driver;
        try {
            driver = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unable to load the datasource driver for the " + fullDataSourceName, e);
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
                            "Driver is an XA dataSource, but XA transactions have not been enabled on the default datasource; please either set 'quarkus.datasource.xa=true' or switch to a standard non-XA JDBC driver implementation");
                } else {
                    throw new ConfigurationException(
                            "Driver is an XA dataSource, but XA transactions have not been enabled on the datasource named '"
                                    + fullDataSourceName + "'; please either set 'quarkus.datasource." + fullDataSourceName
                                    + ".jdbc.xa=true' or switch to a standard non-XA JDBC driver implementation");
                }
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureDataSources(AgroalRecorder recorder,
            BuildProducer<JdbcDataSourceBuildItem> jdbcDataSource,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedBuildTimeConfigBuildItems,
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
            SslNativeConfigBuildItem sslNativeConfig) {
        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        recorder.configureDatasources(dataSourcesBuildTimeConfig, dataSourcesJdbcBuildTimeConfig, dataSourcesRuntimeConfig,
                dataSourcesJdbcRuntimeConfig, sslNativeConfig.isExplicitlyDisabled());

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedBuildTimeConfigBuildItem : aggregatedBuildTimeConfigBuildItems) {
            jdbcDataSource.produce(new JdbcDataSourceBuildItem(aggregatedBuildTimeConfigBuildItem.getName(),
                    aggregatedBuildTimeConfigBuildItem.getDataSourceConfig().kind.get(),
                    DataSourceUtil.isDefault(aggregatedBuildTimeConfigBuildItem.getName())));
        }
    }

    @BuildStep
    UnremovableBeanBuildItem markBeansAsUnremovable() {
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypesExclusion(UNREMOVABLE_BEANS));
    }

    private List<AggregatedDataSourceBuildTimeConfigBuildItem> getAggregatedConfigBuildItems(
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig) {
        List<AggregatedDataSourceBuildTimeConfigBuildItem> dataSources = new ArrayList<>();

        if (dataSourcesBuildTimeConfig.defaultDataSource.kind.isPresent()) {
            if (dataSourcesJdbcBuildTimeConfig.jdbc.enabled) {
                dataSources.add(new AggregatedDataSourceBuildTimeConfigBuildItem(DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                        dataSourcesBuildTimeConfig.defaultDataSource,
                        dataSourcesJdbcBuildTimeConfig.jdbc));
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
                    jdbcBuildTimeConfig));
        }

        return dataSources;
    }

    /**
     * Create a producer bean managing the lifecycle of the datasources.
     * <p>
     * Build time and runtime configuration are both injected into this bean.
     */
    private void createDataSourceProducerBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String dataSourceProducerClassName,
            List<AggregatedDataSourceBuildTimeConfigBuildItem> aggregatedDataSourceBuildTimeConfigs,
            boolean metricsCapabilityPresent) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(dataSourceProducerClassName)
                .superClass(AbstractDataSourceProducer.class)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        for (AggregatedDataSourceBuildTimeConfigBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedDataSourceBuildTimeConfigs) {
            String dataSourceName = aggregatedDataSourceBuildTimeConfig.getName();

            MethodCreator dataSourceMethodCreator = classCreator.getMethodCreator(
                    "createDataSource_" + HashUtil.sha1(dataSourceName),
                    AgroalDataSource.class);
            dataSourceMethodCreator.addAnnotation(ApplicationScoped.class);
            dataSourceMethodCreator.addAnnotation(Produces.class);
            if (aggregatedDataSourceBuildTimeConfig.isDefault()) {
                dataSourceMethodCreator.addAnnotation(Default.class);
            } else {
                dataSourceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                        new AnnotationValue[] { AnnotationValue.createStringValue("value", dataSourceName) }));
                dataSourceMethodCreator
                        .addAnnotation(AnnotationInstance.create(DotName.createSimple(DataSource.class.getName()), null,
                                new AnnotationValue[] { AnnotationValue.createStringValue("value", dataSourceName) }));
            }

            ResultHandle dataSourceNameRH = dataSourceMethodCreator.load(dataSourceName);
            ResultHandle dataSourceBuildTimeConfig = dataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDataSourceBuildTimeConfig",
                            DataSourceBuildTimeConfig.class, String.class),
                    dataSourceMethodCreator.getThis(), dataSourceNameRH);
            ResultHandle dataSourceJdbcBuildTimeConfig = dataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDataSourceJdbcBuildTimeConfig",
                            DataSourceJdbcBuildTimeConfig.class, String.class),
                    dataSourceMethodCreator.getThis(), dataSourceNameRH);
            ResultHandle dataSourceRuntimeConfig = dataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDataSourceRuntimeConfig",
                            DataSourceRuntimeConfig.class, String.class),
                    dataSourceMethodCreator.getThis(), dataSourceNameRH);
            ResultHandle dataSourceJdbcRuntimeConfig = dataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDataSourceJdbcRuntimeConfig",
                            DataSourceJdbcRuntimeConfig.class, String.class),
                    dataSourceMethodCreator.getThis(), dataSourceNameRH);
            ResultHandle mpMetricsEnabled = dataSourceMethodCreator.load(metricsCapabilityPresent);

            dataSourceMethodCreator.returnValue(
                    dataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class,
                                    String.class,
                                    DataSourceBuildTimeConfig.class,
                                    DataSourceJdbcBuildTimeConfig.class,
                                    DataSourceRuntimeConfig.class,
                                    DataSourceJdbcRuntimeConfig.class,
                                    boolean.class),
                            dataSourceMethodCreator.getThis(),
                            dataSourceNameRH,
                            dataSourceBuildTimeConfig, dataSourceJdbcBuildTimeConfig, dataSourceRuntimeConfig,
                            dataSourceJdbcRuntimeConfig, mpMetricsEnabled));
        }

        classCreator.close();
    }

    @BuildStep
    HealthBuildItem addHealthCheck(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.agroal.runtime.health.DataSourceHealthCheck",
                dataSourcesBuildTimeConfig.healthEnabled, "datasource");
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
