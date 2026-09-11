package io.quarkus.agroal.deployment.component;

import static io.quarkus.agroal.deployment.AgroalDataSourceBuildUtil.AGROAL_INJECTABLE_TYPES;
import static io.quarkus.agroal.deployment.AgroalDataSourceBuildUtil.DATASOURCE_QUALIFIER;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sql.XADataSource;

import org.jboss.jandex.AnnotationValue;
import org.jboss.logging.Logger;

import io.quarkus.agroal.deployment.JdbcDataSourceDefinitionBuildItem;
import io.quarkus.agroal.runtime.DataSourceJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig;
import io.quarkus.agroal.runtime.TransactionIntegration;
import io.quarkus.agroal.spi.JdbcDriverBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryFinishedBuildItem;
import io.quarkus.arc.deployment.BeanDiscoveryInjectionPointsBuildItem;
import io.quarkus.arc.deployment.InjectionPointScanningUtil;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.deployment.component.DataSourceProcessorSupport;
import io.quarkus.datasource.deployment.spi.DataSourceDbKindResolverBuildItem;
import io.quarkus.datasource.deployment.spi.DefaultDataSourceDbVersionBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceDefinitionBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceLookupBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestBuildItem;
import io.quarkus.datasource.deployment.spi.component.DataSourceRequestHandlerBuildItem;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import io.quarkus.runtime.util.Reason;

/**
 * Handles the lifecycle of {@link ProgrammingParadigm#BLOCKING JDBC} datasources:
 * declaring the JDBC datasource {@link DataSourceLookupBuildItem lookup},
 * collecting requests from configuration, and producing definitions.
 * <p>
 * A <b>request</b> ({@link DataSourceRequestBuildItem}) declares that a datasource is needed,
 * carrying a name, a {@link ProgrammingParadigm}, and a {@link Reason} explaining why.
 * A <b>definition</b> ({@link DataSourceDefinitionBuildItem}) is produced by checking each
 * request against the lookup: if the lookup deems the datasource unavailable, a
 * {@code ConfigurationException} is thrown with the full reason chain; otherwise a definition
 * is produced for downstream consumption (connection pool creation, dev services, etc.).
 *
 * @see io.quarkus.datasource.deployment.component.DataSourceLookupProcessor
 * @see io.quarkus.reactive.datasource.deployment.component.DataSourceDefinitionReactiveProcessor
 */
class DataSourceDefinitionBlockingProcessor {

    private static final Logger log = Logger.getLogger(DataSourceDefinitionBlockingProcessor.class);

    @BuildStep
    DataSourceRequestHandlerBuildItem defineJdbcDataSourceRequestHandler(
            DataSourcesJdbcBuildTimeConfig jdbcConfig,
            DataSourceDbKindResolverBuildItem dbKindResolverBuildItem) {
        var dbKindResolver = dbKindResolverBuildItem.get();
        return new DataSourceRequestHandlerBuildItem(ProgrammingParadigm.BLOCKING,
                (dataSourceName, paradigm) -> {
                    if (paradigm != ProgrammingParadigm.BLOCKING) {
                        return List.of();
                    }
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
                        unavailableReasons.add(dbKindResolver.unavailableReason(dataSourceName, paradigm));
                    }
                    return unavailableReasons;
                });
    }

    @BuildStep
    void collectImplicitJdbcDataSourceRequests(
            DataSourcesBuildTimeConfig config,
            DataSourcesJdbcBuildTimeConfig jdbcConfig,
            DataSourceLookupBuildItem lookupBuildItem,
            BuildProducer<DataSourceRequestBuildItem> dataSourceRequests) {
        Predicate<String> enabled = name -> jdbcConfig.dataSources().get(name).jdbc().enabled();
        DataSourceProcessorSupport.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.BLOCKING, config.dataSources().keySet(), "*", enabled,
                dataSourceRequests);
        DataSourceProcessorSupport.collectImplicitDataSourceRequestsFromConfiguration(
                ProgrammingParadigm.BLOCKING, jdbcConfig.dataSources().keySet(), "jdbc.*", enabled,
                dataSourceRequests);

    }

    @BuildStep
    void collectInjectionJdbcDataSourceRequests(
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
    void defineJdbcDataSources(
            DataSourcesBuildTimeConfig config,
            DataSourcesJdbcBuildTimeConfig jdbcConfig,
            DataSourceDbKindResolverBuildItem dbKindResolutionBuildItem,
            DataSourceLookupBuildItem lookupBuildItem,
            List<DataSourceRequestBuildItem> dataSourceReferences,
            List<DefaultDataSourceDbVersionBuildItem> defaultDbVersions,
            Capabilities capabilities,
            List<JdbcDriverBuildItem> jdbcDriverBuildItems,
            BuildProducer<JdbcDataSourceDefinitionBuildItem> dataSourceDefinitions,
            BuildProducer<DataSourceDefinitionBuildItem> definedDataSources,
            BuildProducer<ValidationPhaseBuildItem.ValidationErrorBuildItem> validationErrors,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<NativeImageResourceBuildItem> resource,
            BuildProducer<ServiceProviderBuildItem> service,
            BuildProducer<ExtensionSslNativeSupportBuildItem> sslNativeSupport) {
        Set<String> defined = DataSourceProcessorSupport.defineDataSources(
                ProgrammingParadigm.BLOCKING, config,
                lookupBuildItem,
                dataSourceReferences,
                validationErrors);

        if (defined.isEmpty()) {
            log.warn("The Agroal dependency is present but no JDBC datasources have been defined.");
            return;
        }

        boolean otelJdbcInstrumentationActive = false;
        for (String dataSourceName : defined) {
            String dbKind = dbKindResolutionBuildItem.get().getOptional(dataSourceName)
                    // Should not throw since DataSourceProcessorSupport.defineDataSources skips datasources with no db-kind.
                    .orElseThrow();

            definedDataSources.produce(new DataSourceDefinitionBuildItem(dataSourceName, dbKind, ProgrammingParadigm.BLOCKING));

            var dataSourceJdbcConfig = jdbcConfig.dataSources().get(dataSourceName).jdbc();
            var definition = new JdbcDataSourceDefinitionBuildItem(dataSourceName,
                    config.dataSources().get(dataSourceName),
                    dataSourceJdbcConfig,
                    dbKind,
                    resolveDriver(dataSourceName, dbKind, dataSourceJdbcConfig, jdbcDriverBuildItems),
                    config.dataSources().get(dataSourceName).dbVersion()
                            .or(() -> DefaultDataSourceDbVersionBuildItem.resolveDefaultDbVersion(dbKind,
                                    defaultDbVersions, config.dbVersionDefaults())));
            validateBuildTimeConfig(definition);
            dataSourceDefinitions.produce(definition);

            if (definition.getJdbcConfig().telemetry()) {
                otelJdbcInstrumentationActive = true;
            }

            reflectiveClass
                    .produce(ReflectiveClassBuildItem.builder(definition.getResolvedDriverClass())
                            .methods().build());
        }

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

    private static void validateBuildTimeConfig(JdbcDataSourceDefinitionBuildItem aggregatedConfig) {
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
}
