package io.quarkus.agroal.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.sql.Driver;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.DeploymentException;
import javax.sql.XADataSource;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.TransactionIntegration;
import io.quarkus.agroal.runtime.AbstractDataSourceProducer;
import io.quarkus.agroal.runtime.AgroalBuildTimeConfig;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.AgroalRuntimeConfig;
import io.quarkus.agroal.runtime.DataSourceBuildTimeConfig;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.HashUtil;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

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
    @BuildStep
    BeanContainerListenerBuildItem build(
            RecorderContext recorderContext,
            AgroalRecorder recorder,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<SubstrateResourceBuildItem> resource,
            BuildProducer<DataSourceDriverBuildItem> dataSourceDriver,
            SslNativeConfigBuildItem sslNativeConfig, BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport,
            BuildProducer<GeneratedBeanBuildItem> generatedBean) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.AGROAL));

        if (!agroalBuildTimeConfig.defaultDataSource.driver.isPresent() && agroalBuildTimeConfig.namedDataSources.isEmpty()) {
            log.warn("Agroal dependency is present but no driver has been defined for the default datasource");
            return null;
        }

        // For now, we can't push the security providers to Agroal so we need to include
        // the service file inside the image. Hopefully, we will get an entry point to
        // resolve them at build time and push them to Agroal soon.
        resource.produce(new SubstrateResourceBuildItem(
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

        createDataSourceProducerBean(generatedBean, dataSourceProducerClassName);

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
        if (!agroalBuildTimeConfig.defaultDataSource.driver.isPresent() && agroalBuildTimeConfig.namedDataSources.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        recorder.configureRuntimeProperties(agroalRuntimeConfig);

        dataSourceInitialized.produce(new DataSourceInitializedBuildItem());
    }

    @BuildStep
    UnremovableBeanBuildItem markBeansAsUnremovable() {
        return new UnremovableBeanBuildItem(beanInfo -> {
            Set<Type> types = beanInfo.getTypes();
            for (Type t : types) {
                if (UNREMOVABLE_BEANS.contains(t.name())) {
                    return true;
                }
            }

            return false;
        });
    }

    /**
     * Create a producer bean managing the lifecycle of the datasources.
     * <p>
     * Build time and runtime configuration are both injected into this bean.
     */
    private void createDataSourceProducerBean(BuildProducer<GeneratedBeanBuildItem> generatedBean,
            String dataSourceProducerClassName) {
        ClassOutput classOutput = new ClassOutput() {
            @Override
            public void write(String name, byte[] data) {
                generatedBean.produce(new GeneratedBeanBuildItem(name, data));
            }
        };

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

            defaultDataSourceMethodCreator.returnValue(
                    defaultDataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class),
                            defaultDataSourceMethodCreator.getThis(),
                            dataSourceName,
                            dataSourceBuildTimeConfig, dataSourceRuntimeConfig));
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

            namedDataSourceMethodCreator.returnValue(
                    namedDataSourceMethodCreator.invokeVirtualMethod(
                            MethodDescriptor.ofMethod(AbstractDataSourceProducer.class, "createDataSource",
                                    AgroalDataSource.class, String.class,
                                    DataSourceBuildTimeConfig.class, Optional.class),
                            namedDataSourceMethodCreator.getThis(),
                            namedDataSourceNameRH,
                            namedDataSourceBuildTimeConfig, namedDataSourceRuntimeConfig));
        }

        classCreator.close();
    }

    @BuildStep
    HealthBuildItem addHealthCheck(AgroalBuildTimeConfig agroalBuildTimeConfig) {
        return new HealthBuildItem("io.quarkus.agroal.runtime.health.DataSourceHealthCheck",
                agroalBuildTimeConfig.healthEnabled, "datasource");
    }
}
