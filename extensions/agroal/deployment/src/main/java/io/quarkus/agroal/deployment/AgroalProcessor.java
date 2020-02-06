package io.quarkus.agroal.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.sql.Driver;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;
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
import io.quarkus.agroal.runtime.AgroalBuildTimeConfig;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.AgroalRuntimeConfig;
import io.quarkus.agroal.runtime.DataSourceBuildTimeConfig;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
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
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;

class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final Set<DotName> UNREMOVABLE_BEANS = new HashSet<>(Arrays.asList(
            DotName.createSimple(AbstractDataSourceProducer.class.getName()),
            DotName.createSimple(javax.sql.DataSource.class.getName())));

    /**
     * The Agroal build time configuration.
     */
    AgroalBuildTimeConfig agroalBuildTimeConfig;

    @SuppressWarnings("unchecked")
    @Record(STATIC_INIT)
    @BuildStep(loadsApplicationClasses = true)
    BeanContainerListenerBuildItem build(
            RecorderContext recorderContext,
            AgroalRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<DataSourceDriverBuildItem> dataSourceDriver,
            SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<GeneratedBeanBuildItem> generatedBean,
            Capabilities capabilities) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.AGROAL));

        if (!agroalBuildTimeConfig.defaultDataSource.driver.isPresent() && agroalBuildTimeConfig.namedDataSources.isEmpty()) {
            log.warn("Agroal dependency is present but no driver has been defined for the default datasource");
            return null;
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

        validateBuildTimeConfig(null, agroalBuildTimeConfig.defaultDataSource);
        agroalBuildTimeConfig.namedDataSources.forEach(AgroalProcessor::validateBuildTimeConfig);

        // Add reflection for the drivers
        if (agroalBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            reflectiveClass
                    .produce(new ReflectiveClassBuildItem(true, false, agroalBuildTimeConfig.defaultDataSource.driver.get()));

            // TODO: this will need to change to support multiple datasources but it can wait
            dataSourceDriver.produce(new DataSourceDriverBuildItem(agroalBuildTimeConfig.defaultDataSource.driver.get()));
        }
        for (Entry<String, DataSourceBuildTimeConfig> namedDataSourceEntry : agroalBuildTimeConfig.namedDataSources
                .entrySet()) {
            if (namedDataSourceEntry.getValue().driver.isPresent()) {
                reflectiveClass
                        .produce(new ReflectiveClassBuildItem(true, false, namedDataSourceEntry.getValue().driver.get()));
            }
        }

        // Enable SSL support by default
        sslNativeSupport.produce(new ExtensionSslNativeSupportBuildItem(FeatureBuildItem.AGROAL));

        // Generate the DataSourceProducer bean
        String dataSourceProducerClassName = AbstractDataSourceProducer.class.getPackage().getName() + "."
                + "DataSourceProducer";

        createDataSourceProducerBean(generatedBean, dataSourceProducerClassName,
                capabilities.isCapabilityPresent(Capabilities.METRICS));

        return new BeanContainerListenerBuildItem(recorder.addDataSource(
                (Class<? extends AbstractDataSourceProducer>) recorderContext.classProxy(dataSourceProducerClassName),
                agroalBuildTimeConfig,
                sslNativeConfig.isExplicitlyDisabled()));
    }

    private static void validateBuildTimeConfig(final String datasourceName, final DataSourceBuildTimeConfig ds) {
        if (!ds.driver.isPresent()) {
            // When the driver is not defined on the default datasource, we need to be more lenient as the datasource
            // component might not be enabled at all so we only throw an exception for named datasources
            if (datasourceName != null) {
                throw new DeploymentException("Named datasource '" + datasourceName + "' doesn't have a driver defined.");
            }
            return;
        }

        String driverName = ds.driver.get();
        Class<?> driver;
        try {
            driver = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            if (datasourceName == null) {
                throw new DeploymentException("Unable to load the datasource driver for the default datasource", e);
            } else {
                throw new DeploymentException(
                        "Unable to load the datasource driver for datasource named '" + datasourceName + "'",
                        e);
            }
        }
        if (ds.transactions == TransactionIntegration.XA) {
            if (!XADataSource.class.isAssignableFrom(driver)) {
                if (datasourceName == null) {
                    throw new DeploymentException(
                            "Driver is not an XA dataSource, while XA has been enabled in the configuration of the default datasource: either disable XA or switch the driver to an XADataSource");
                } else {
                    throw new DeploymentException(
                            "Driver is not an XA dataSource, while XA has been enabled in the configuration of the datasource named '"
                                    + datasourceName + "': either disable XA or switch the driver to an XADataSource");
                }
            }
        } else {
            if (driver != null && !javax.sql.DataSource.class.isAssignableFrom(driver)
                    && !Driver.class.isAssignableFrom(driver)) {
                if (datasourceName == null) {
                    throw new DeploymentException(
                            "Driver is an XA dataSource, but XA transactions have not been enabled on the default datasource; please either set 'quarkus.datasource.xa=true' or switch to a standard non-XA JDBC driver implementation");
                } else {
                    throw new DeploymentException(
                            "Driver is an XA dataSource, but XA transactions have not been enabled on the datasource named '"
                                    + datasourceName + "'; please either set 'quarkus.datasource." + datasourceName
                                    + ".xa=true' or switch to a standard non-XA JDBC driver implementation");
                }
            }
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    void configureRuntimeProperties(AgroalRecorder recorder,
            BuildProducer<DataSourceInitializedBuildItem> dataSourceInitialized,
            AgroalRuntimeConfig agroalRuntimeConfig) {
        Optional<String> defaultDataSourceDriver = agroalBuildTimeConfig.defaultDataSource.driver;
        if (!defaultDataSourceDriver.isPresent() && agroalBuildTimeConfig.namedDataSources.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        recorder.configureRuntimeProperties(agroalRuntimeConfig);

        Set<String> dataSourceNames = agroalBuildTimeConfig.namedDataSources.keySet();
        DataSourceInitializedBuildItem buildItem;
        if (defaultDataSourceDriver.isPresent()) {
            buildItem = DataSourceInitializedBuildItem.ofDefaultDataSourceAnd(dataSourceNames);
        } else {
            buildItem = DataSourceInitializedBuildItem.ofDataSources(dataSourceNames);
        }
        dataSourceInitialized.produce(buildItem);
    }

    @BuildStep
    UnremovableBeanBuildItem markBeansAsUnremovable() {
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypesExclusion(UNREMOVABLE_BEANS));
    }

    /**
     * Create a producer bean managing the lifecycle of the datasources.
     * <p>
     * Build time and runtime configuration are both injected into this bean.
     */
    private void createDataSourceProducerBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String dataSourceProducerClassName, boolean metricsCapabilityPresent) {
        ClassOutput classOutput = new GeneratedBeanGizmoAdaptor(generatedBean);

        ClassCreator classCreator = ClassCreator.builder().classOutput(classOutput)
                .className(dataSourceProducerClassName)
                .superClass(AbstractDataSourceProducer.class)
                .build();
        classCreator.addAnnotation(ApplicationScoped.class);

        if (agroalBuildTimeConfig.defaultDataSource.driver.isPresent()) {
            MethodCreator defaultDataSourceMethodCreator = classCreator.getMethodCreator("createDefaultDataSource",
                    AgroalDataSource.class);
            defaultDataSourceMethodCreator.addAnnotation(ApplicationScoped.class);
            defaultDataSourceMethodCreator.addAnnotation(Produces.class);
            defaultDataSourceMethodCreator.addAnnotation(Default.class);

            ResultHandle dataSourceName = defaultDataSourceMethodCreator.load(AgroalRecorder.DEFAULT_DATASOURCE_NAME);
            ResultHandle dataSourceBuildTimeConfig = defaultDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDefaultBuildTimeConfig",
                            DataSourceBuildTimeConfig.class),
                    defaultDataSourceMethodCreator.getThis());
            ResultHandle dataSourceRuntimeConfig = defaultDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getDefaultRuntimeConfig", Optional.class),
                    defaultDataSourceMethodCreator.getThis());
            ResultHandle mpMetricsEnabled = defaultDataSourceMethodCreator.load(metricsCapabilityPresent);

            defaultDataSourceMethodCreator.returnValue(
                    defaultDataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class, boolean.class),
                            defaultDataSourceMethodCreator.getThis(),
                            dataSourceName,
                            dataSourceBuildTimeConfig, dataSourceRuntimeConfig, mpMetricsEnabled));
        }

        for (Entry<String, DataSourceBuildTimeConfig> namedDataSourceEntry : agroalBuildTimeConfig.namedDataSources
                .entrySet()) {
            String namedDataSourceName = namedDataSourceEntry.getKey();

            if (!namedDataSourceEntry.getValue().driver.isPresent()) {
                log.warn("No driver defined for named datasource " + namedDataSourceName + ". Ignoring.");
                continue;
            }

            MethodCreator namedDataSourceMethodCreator = classCreator.getMethodCreator(
                    "createNamedDataSource_" + HashUtil.sha1(namedDataSourceName),
                    AgroalDataSource.class);
            namedDataSourceMethodCreator.addAnnotation(ApplicationScoped.class);
            namedDataSourceMethodCreator.addAnnotation(Produces.class);
            namedDataSourceMethodCreator.addAnnotation(AnnotationInstance.create(DotNames.NAMED, null,
                    new AnnotationValue[] { AnnotationValue.createStringValue("value", namedDataSourceName) }));
            namedDataSourceMethodCreator
                    .addAnnotation(AnnotationInstance.create(DotName.createSimple(DataSource.class.getName()), null,
                            new AnnotationValue[] { AnnotationValue.createStringValue("value", namedDataSourceName) }));

            ResultHandle namedDataSourceNameRH = namedDataSourceMethodCreator.load(namedDataSourceName);
            ResultHandle namedDataSourceBuildTimeConfig = namedDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getBuildTimeConfig",
                            DataSourceBuildTimeConfig.class, String.class),
                    namedDataSourceMethodCreator.getThis(), namedDataSourceNameRH);
            ResultHandle namedDataSourceRuntimeConfig = namedDataSourceMethodCreator.invokeVirtualMethod(
                    MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "getRuntimeConfig", Optional.class,
                            String.class),
                    namedDataSourceMethodCreator.getThis(), namedDataSourceNameRH);
            ResultHandle mpMetricsEnabled = namedDataSourceMethodCreator.load(metricsCapabilityPresent);

            namedDataSourceMethodCreator.returnValue(
                    namedDataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class, boolean.class),
                            namedDataSourceMethodCreator.getThis(),
                            namedDataSourceNameRH,
                            namedDataSourceBuildTimeConfig, namedDataSourceRuntimeConfig, mpMetricsEnabled));
        }

        classCreator.close();
    }

    @BuildStep
    HealthBuildItem addHealthCheck(AgroalBuildTimeConfig agroalBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.agroal.runtime.health.DataSourceHealthCheck",
                agroalBuildTimeConfig.healthEnabled, "datasource");
    }

    @BuildStep
    void registerMetrics(AgroalBuildTimeConfig agroalBuildTimeConfig,
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

        HashMap<String, DataSourceBuildTimeConfig> datasources = new HashMap<>(agroalBuildTimeConfig.namedDataSources);
        if (agroalBuildTimeConfig.defaultDataSource != null) {
            datasources.put(null, agroalBuildTimeConfig.defaultDataSource);
        }

        for (Entry<String, DataSourceBuildTimeConfig> dataSourceEntry : datasources.entrySet()) {
            String dataSourceName = dataSourceEntry.getKey();
            // expose metrics for this datasource if metrics are enabled both globally and for this data source
            // (they are enabled for each data source by default if they are also enabled globally)
            boolean metricsEnabledForThisDatasource = agroalBuildTimeConfig.metricsEnabled &&
                    dataSourceEntry.getValue().enableMetrics.orElse(true);
            Tag tag = new Tag("datasource", dataSourceName != null ? dataSourceName : "default");
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
