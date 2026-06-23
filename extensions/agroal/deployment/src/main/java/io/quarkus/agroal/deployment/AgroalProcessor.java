package io.quarkus.agroal.deployment;

import static io.quarkus.agroal.deployment.AgroalDataSourceBuildUtil.qualifiers;
import static io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem.isOtelSdkEnabled;
import static io.quarkus.deployment.Capability.OPENTELEMETRY_TRACER;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import javax.sql.XADataSource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Singleton;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.TransactionSynchronizationRegistry;

import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassType;
import org.jboss.jandex.DotName;
import org.jboss.logging.Logger;
import org.jboss.tm.XAResourceRecoveryRegistry;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalPoolInterceptor;
import io.quarkus.agroal.DataSource;
import io.quarkus.agroal.runtime.AgroalDataSourceSupport;
import io.quarkus.agroal.runtime.AgroalOpenTelemetryWrapper;
import io.quarkus.agroal.runtime.AgroalRecorder;
import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig;
import io.quarkus.agroal.runtime.JdbcDriver;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.agroal.spi.JdbcPropertyBuildItem;
import io.quarkus.arc.BeanDestroyer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryInjectionPointsBuildItem;
import io.quarkus.arc.deployment.InjectionPointScanningUtil;
import io.quarkus.arc.deployment.OpenTelemetrySdkBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.DataSourceProcessorUtil;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceDefinedBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestBuildItem;
import io.quarkus.datasource.deployment.spi.DataSourceRequestHandlerBuildItem;
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
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.SslNativeConfigBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBundleBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.narayana.jta.deployment.NarayanaInitBuildItem;
import io.quarkus.narayana.jta.runtime.TransactionManagerConfiguration;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;
import io.quarkus.smallrye.health.deployment.spi.HealthBuildItem;

@SuppressWarnings("deprecation")
class AgroalProcessor {

    private static final Logger log = Logger.getLogger(AgroalProcessor.class);

    private static final DotName DATA_SOURCE = DotName.createSimple(javax.sql.DataSource.class.getName());
    private static final DotName AGROAL_DATA_SOURCE = DotName.createSimple(AgroalDataSource.class.getName());

    @BuildStep
    void agroal(BuildProducer<FeatureBuildItem> feature,
            Capabilities capabilities,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        feature.produce(new FeatureBuildItem(Feature.AGROAL));
        additionalBeans.produce(new AdditionalBeanBuildItem(JdbcDriver.class));
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClass(DataSource.class).build());
    }

    @BuildStep
    DataSourceRequestHandlerBuildItem defineJdbcDataSourceRequestHandler(
            DataSourcesJdbcBuildTimeConfig jdbcConfig,
            DataSourceDbKindResolverBuildItem dbKindResolverBuildItem) {
        var dbKindResolver = dbKindResolverBuildItem.get();
        return new DataSourceRequestHandlerBuildItem(ProgrammingParadigm.BLOCKING,
                dataSourceName -> {
                    var unavailableReasons = new ArrayList<Reason>();
                    if (!jdbcConfig.dataSources().get(dataSourceName).jdbc().enabled()) {
                        unavailableReasons.add(new Reason(String.format(Locale.ROOT, """
                                JDBC datasource '%s' was disabled explicitly by setting '%s' to 'false'. \
                                Refer to https://quarkus.io/guides/datasource for guidance.
                                """,
                                dataSourceName,
                                DataSourceUtil.dataSourcePropertyKey(dataSourceName, "jdbc"))));
                    }
                    if (dbKindResolver.getOptional(dataSourceName).isEmpty()) {
                        unavailableReasons.add(dbKindResolver.unavailableReason(dataSourceName, ProgrammingParadigm.BLOCKING));
                    }
                    return unavailableReasons;
                });
    }

    @BuildStep
    void collectImplicitJdbcDataSourceRequests(
            DataSourcesBuildTimeConfig config,
            DataSourcesJdbcBuildTimeConfig jdbcConfig,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Predicate<String> enabled = name -> jdbcConfig.dataSources().get(name).jdbc().enabled();
        DataSourceProcessorUtil.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.BLOCKING, config, config.dataSources().keySet(), enabled,
                "*", dataSourceRequests);
        DataSourceProcessorUtil.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.BLOCKING, config, jdbcConfig.dataSources().keySet(), enabled,
                "jdbc.*", dataSourceRequests);

    }

    private static final DotName DATASOURCE_QUALIFIER = DotName.createSimple(DataSource.class);
    static final Set<DotName> AGROAL_INJECTABLE_TYPES = Set.of(DATA_SOURCE, AGROAL_DATA_SOURCE);

    @BuildStep
    void collectJdbcDataSourceRequestsFromInjection(
            BeanDiscoveryFinishedBuildItem beanDiscovery,
            BeanDiscoveryInjectionPointsBuildItem injectionPointIndex,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        InjectionPointScanningUtil.collectUnsatisfiedInjectionPoints(
                beanDiscovery, injectionPointIndex,
                AGROAL_INJECTABLE_TYPES,
                List.of(DATASOURCE_QUALIFIER, DotNames.NAMED),
                DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                qualifier -> {
                    AnnotationValue value = qualifier.value();
                    return (value != null && !value.asString().isEmpty()) ? value.asString()
                            : DataSourceUtil.DEFAULT_DATASOURCE_NAME;
                },
                (name, reason) -> dataSourceRequests
                        .produce(new DataSourceRequestBuildItem(name, ProgrammingParadigm.BLOCKING, reason)));
    }

    @BuildStep
    public void defineJdbcDataSources(
            DataSourceDbKindResolverBuildItem dbKindResolutionBuildItem,
            DataSourceLookupBuildItem lookupBuildItem,
            List<DataSourceRequestBuildItem> dataSourceReferences,
            BuildProducer<DataSourceDefinitionBuildItem> dataSourceDefinitons,
            BuildProducer<DataSourceDefinedBuildItem> definedDataSources,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors) {
    }

    @BuildStep
    void build(
            DataSourcesBuildTimeConfig config,
            DataSourcesJdbcBuildTimeConfig jdbcConfig,
            DataSourceDbKindResolverBuildItem dbKindResolutionBuildItem,
            DataSourceLookupBuildItem lookupBuildItem,
            List<DataSourceRequestBuildItem> dataSourceReferences,
            Capabilities capabilities,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems,
            BuildProducer<DataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<DataSourceDefinedBuildItem> definedDataSources,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> service,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        Set<String> defined = DataSourceProcessorUtil.defineDataSources(
                ProgrammingParadigm.BLOCKING, config,
                lookupBuildItem,
                dataSourceReferences,
                validationErrors);

        if (defined.isEmpty()) {
            log.warn("The Datasource Reactive dependency is present but no Reactive datasources have been defined.");
            return;
        }

        boolean otelJdbcInstrumentationActive = false;
        for (String dataSourceName : defined) {
            String dbKind = dbKindResolutionBuildItem.get().getOptional(dataSourceName)
                    // Should not throw since DataSourceProcessorUtil.defineDataSources skips datasources with no db-kind.
                    .orElseThrow();

            definedDataSources.produce(new DataSourceDefinedBuildItem(dataSourceName, ProgrammingParadigm.BLOCKING, dbKind));

            var dataSourceJdbcConfig = jdbcConfig.dataSources().get(dataSourceName).jdbc();
            var definition = new DataSourceDefinitionBuildItem(dataSourceName,
                    config.dataSources().get(dataSourceName),
                    dataSourceJdbcConfig,
                    dbKind,
                    resolveDriver(dataSourceName, dbKind, dataSourceJdbcConfig, jdbcDriverBuildItems));
            validateBuildTimeConfig(definition);
            dataSourceDefinitions.produce(definition);

            if (definition.getJdbcConfig().telemetry()) {
                otelJdbcInstrumentationActive = true;
            }

            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(definition.getResolvedDriverClass())
                            .methods().build());
        }

        // OTel bean registration moved to registerOtelBeans() to avoid cycle with BeanDiscoveryFinishedBuildItem

        // For now, we can't push the security providers to Agroal so we need to include
        // the service file inside the image. Hopefully, we will get an entry point to
        // resolve them at build time and push them to Agroal soon.
        resource.produce(new NativeImageResourceBuildItem(
                "META-INF/services/" + io.agroal.api.security.AgroalSecurityProvider.class.getName()));

        // accessed through io.quarkus.agroal.runtime.DataSources.loadDriversInTCCL
        service.produce(ServiceProviderBuildItem.allProvidersFromClassPath(Driver.class.getName()));

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

    private static void validateBuildTimeConfig(DataSourceDefinitionBuildItem aggregatedConfig) {
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

    private AgroalDataSourceSupport getDataSourceSupport(
            List<DataSourceDefinitionBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig, Capabilities capabilities) {
        Map<String, AgroalDataSourceSupport.Entry> dataSourceSupportEntries = new HashMap<>();
        for (DataSourceDefinitionBuildItem aggregatedDataSourceBuildTimeConfig : aggregatedBuildTimeConfigBuildItems) {
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
            DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            List<DataSourceDefinitionBuildItem> aggregatedBuildTimeConfigBuildItems,
            SslNativeConfigBuildItem sslNativeConfig,
            Capabilities capabilities,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        if (aggregatedBuildTimeConfigBuildItems.isEmpty()) {
            // No datasource has been configured so bail out
            return;
        }

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

        // DataSources and AgroalOpenTelemetryWrapper are registered as SyntheticBeanBuildItems,
        // not as AdditionalBeanBuildItems, because this step depends (transitively) on
        // BeanDiscoveryFinishedBuildItem and AdditionalBeanBuildItem feeds into Arc's
        // bean discovery, which would create a cycle.
        syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(DataSources.class)
                .addType(DataSources.class)
                .scope(Singleton.class)
                .unremovable()
                .setRuntimeInit()
                .addInjectionPoint(ClassType.create(DotName.createSimple(AgroalDataSourceSupport.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(DataSourcesRuntimeConfig.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(DataSourcesJdbcRuntimeConfig.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionManagerConfiguration.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionManager.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(XAResourceRecoveryRegistry.class)))
                .addInjectionPoint(ClassType.create(DotName.createSimple(TransactionSynchronizationRegistry.class)))
                .createWith(recorder.dataSourcesSupplier(dataSourcesBuildTimeConfig, dataSourcesJdbcBuildTimeConfig))
                .done());
        if (capabilities.isPresent(OPENTELEMETRY_TRACER)) {
            syntheticBeanBuildItemBuildProducer.produce(SyntheticBeanBuildItem.configure(AgroalOpenTelemetryWrapper.class)
                    .scope(Singleton.class)
                    .unremovable()
                    .setRuntimeInit()
                    .supplier(recorder.openTelemetryWrapperSupplier())
                    .done());
        }
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    @Consume(NarayanaInitBuildItem.class)
    void generateDataSourceBeans(AgroalRecorder recorder,
            List<DataSourceDefinitionBuildItem> dataSourceDefinitions,
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

        for (DataSourceDefinitionBuildItem aggregatedBuildTimeConfigBuildItem : dataSourceDefinitions) {

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
                    aggregatedBuildTimeConfigBuildItem.getDataSourceConfig().dbVersion(),
                    aggregatedBuildTimeConfigBuildItem.getJdbcConfig().transactions() != TransactionIntegration.DISABLED,
                    aggregatedBuildTimeConfigBuildItem.getJdbcConfig().transactions() == TransactionIntegration.XA,
                    aggregatedBuildTimeConfigBuildItem.isDefault()));
        }
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
