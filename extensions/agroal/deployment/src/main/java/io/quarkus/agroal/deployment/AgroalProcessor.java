package io.quarkus.agroal.deployment;

import static io.quarkus.agroal.deployment.AgroalDataSourceBuildUtil.qualifiers;
import static io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem.isOtelSdkEnabled;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;

import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceSupport;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcPropertyBuildItem;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.narayana.jta.deployment.NarayanaInitBuildItem;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

@SuppressWarnings("deprecation")
class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final DotName DATA_SOURCE = DotName.createSimple(javax.sql.DataSource.class.getName());
    private static final DotName AGROAL_DATA_SOURCE = DotName.createSimple(AgroalDataSource.class.getName());

    @BuildStep
    void agroal(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(Feature.AGROAL));
    }

    private AgroalDataSourceSupport getDataSourceSupport(
            List<JdbcDataSourceDefinitionBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig, Capabilities capabilities) {
        Map<String, AgroalDataSourceSupport.Entry> dataSourceSupportEntries = new HashMap<>();
        for (JdbcDataSourceDefinitionBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedBuildTimeConfigBuildItems) {
            String dataSourceName = aggregatedDataSourceBuildTimeConfig.getName();
            dataSourceSupportEntries.put(dataSourceName,
                    new AgroalDataSourceSupport.Entry(dataSourceName, aggregatedDataSourceBuildTimeConfig.getDbKind(),
                            aggregatedDataSourceBuildTimeConfig.getDataSourceConfig().dbVersion(),
                            aggregatedDataSourceBuildTimeConfig.getResolvedDriverClass(),
                            aggregatedDataSourceBuildTimeConfig.isDefault()));
        }

        return new AgroalDataSourceSupport(sslNativeConfig.isExplicitlyDisabled(),
                capabilities.isPresent(Capability.METRICS), dataSourceSupportEntries);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void generateDataSourceSupportBean(AgroalRecorder recorder,
            List<JdbcDataSourceDefinitionBuildItem> aggregatedBuildTimeConfigBuildItems,
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

        // make AgroalPoolInterceptor beans unremovable, users still have to make them beans
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(AgroalPoolInterceptor.class));

        // create the AgroalDataSourceSupport bean that DataSources/DataSourceHealthCheck use as a dependency
        AgroalDataSourceSupport agroalDataSourceSupport = getDataSourceSupport(aggregatedBuildTimeConfigBuildItems,
                sslNativeConfig,
                capabilities);
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(AgroalDataSourceSupport.class)
                .supplier(recorder.dataSourceSupportSupplier(agroalDataSourceSupport))
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .done());
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(NarayanaInitBuildItem.class)
    void generateDataSourceBeans(AgroalRecorder recorder,
            List<JdbcDataSourceDefinitionBuildItem> dataSourceDefinitions,
            SslNativeConfigBuildItem sslNativeConfig,
            Capabilities capabilities,
            Optional<OpenTelemetrySdkBuildItem> openTelemetrySdkBuildItem,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<JdbcDataSourceBuildItem> jdbcDataSource,
            List<JdbcPropertyBuildItem> jdbcPropertyBuildItems) {
        if (dataSourceDefinitions.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

        for (JdbcDataSourceDefinitionBuildItem aggregatedBuildTimeConfigBuildItem : dataSourceDefinitions) {

            String dataSourceName = aggregatedBuildTimeConfigBuildItem.getName();

            // Filter the JDBC properties for the current datasource
            Map<String, String> jdbcProperties = jdbcPropertyBuildItems.stream()
                    .filter(p -> dataSourceName.equals(p.dataSourceName()))
                    .collect(Collectors.toMap(JdbcPropertyBuildItem::propertyName, JdbcPropertyBuildItem::propertyValue));

            SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                    .configure(AgroalDataSource.class)
                    .addType(DATA_SOURCE)
                    .addType(AGROAL_DATA_SOURCE)
                    .scope(ApplicationScoped.class)
                    .qualifiers(qualifiers(dataSourceName))
                    .setRuntimeInit()
                    .unremovable()
                    .addInjectionPoint(ClassType.create(DotName.createSimple(DataSources.class)))
                    .startup()
                    .checkActive(recorder.agroalDataSourceCheckActiveSupplier(dataSourceName))
                    .createWith(recorder.agroalDataSourceSupplier(dataSourceName, isOtelSdkEnabled(openTelemetrySdkBuildItem),
                            jdbcProperties))
                    .destroyer(BeanDestroyer.AutoCloseableDestroyer.class);

            if (!DataSourceUtil.isDefault(dataSourceName)) {
                // this definitely not ideal, but 'elytron-jdbc-security' uses it (although it could be easily changed)
                // which means that perhaps other extensions might depend on this as well...
                configurator.name(dataSourceName);
            }

            syntheticBeanBuildItemBuildProducer.produce(configurator.done());

            jdbcDataSource.produce(new JdbcDataSourceBuildItem(dataSourceName,
                    aggregatedBuildTimeConfigBuildItem.getDbKind(),
                    aggregatedBuildTimeConfigBuildItem.getDbVersion(),
                    aggregatedBuildTimeConfigBuildItem.getDataSourceConfig().dbVersion().isPresent(),
                    aggregatedBuildTimeConfigBuildItem.getJdbcConfig().transactions() != TransactionIntegration.DISABLED,
                    aggregatedBuildTimeConfigBuildItem.getJdbcConfig().transactions() == TransactionIntegration.XA,
                    aggregatedBuildTimeConfigBuildItem.isDefault()));
        }
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

    @BuildStep
    void registerRowSetSupport(
            BuildProducer<NativeImageResourceBundleBuildItem> resourceBundleProducer,
            BuildProducer<NativeImageResourceBuildItem> nativeResourceProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClassProducer) {
        resourceBundleProducer.produce(new NativeImageResourceBundleBuildItem("com.sun.rowset.RowSetResourceBundle"));
        nativeResourceProducer.produce(new NativeImageResourceBuildItem("javax/sql/rowset/rowset.properties"));
        reflectiveClassProducer.produce(ReflectiveClassBuildItem.builder(
                "com.sun.rowset.providers.RIOptimisticProvider",
                "com.sun.rowset.providers.RIXMLProvider").build());
    }

    @BuildStep
    void reduceLogging(BuildProducer<LogCategoryBuildItem> logCategories) {
        logCategories.produce(new LogCategoryBuildItem("io.agroal.pool", Level.WARNING));
    }
}
