package io.quarkus.hibernate.orm.deployment.persistenceunit;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport.configureProperties;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport.configureSqlLoadScript;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorSupport.setDialectAndStorageEngine;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.isHibernateValidatorPresent;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;

import javax.xml.namespace.QName;

import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.xml.bind.JAXBElement;

import org.hibernate.cfg.MappingSettings;
import org.hibernate.integrator.spi.Integrator;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.dev.spi.DevModeType;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaPersistenceUnitModel;
import io.quarkus.hibernate.orm.deployment.PersistenceProviderSetUpBuildItem;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.component.PersistenceUnitDefinitionBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitDefinedBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.SqlLoadScriptDefaultBuildItem;
import io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil;
import io.quarkus.hibernate.orm.dev.HibernateOrmDevIntegrator;
import io.quarkus.hibernate.orm.runtime.HibernateOrmPersistenceUnitProviderHelper;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.boot.xml.JAXBElementSubstitution;
import io.quarkus.hibernate.orm.runtime.boot.xml.QNameSubstitution;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.orm.runtime.schema.SchemaManagementIntegrator;
import io.quarkus.hibernate.validator.spi.BeanValidationTraversableResolverBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.runtime.util.ProgrammingParadigm;
import tools.jackson.databind.JacksonModule;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
final class PersistenceUnitProcessor {

    private static final Logger LOG = Logger.getLogger(PersistenceUnitProcessor.class);

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    private static final String JACKSON_3_JSON_FORMAT_MAPPER = "org.hibernate.type.format.jackson.Jackson3JsonFormatMapper";

    // --- Metadata registration ---

    @BuildStep
    void registerHibernateOrmMetadataForCoreDialects(
            BuildProducer<DatabaseKindDialectBuildItem> producer) {
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.DB2, "DB2",
                Set.of("org.hibernate.dialect.DB2Dialect")));
        // H2: Using our own default version is extra important for H2
        // See https://github.com/quarkusio/quarkus/issues/1886
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.H2, "H2",
                Set.of("org.hibernate.dialect.H2Dialect")));
        // MariaDB: Use version from build-parent/pom.xml
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.MARIADB, "MariaDB",
                Set.of("org.hibernate.dialect.MariaDBDialect"), DialectVersions.Defaults.MARIADB));
        // MSSQL: Use version from build-parent/pom.xml
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.MSSQL, "Microsoft SQL Server",
                Set.of("org.hibernate.dialect.SQLServerDialect"), DialectVersions.Defaults.MSSQL));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.MYSQL, "MySQL",
                Set.of("org.hibernate.dialect.MySQLDialect")));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.ORACLE, "Oracle",
                Set.of("org.hibernate.dialect.OracleDialect")));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.POSTGRESQL, "PostgreSQL",
                Set.of("org.hibernate.dialect.PostgreSQLDialect")));
    }

    // --- Validation ---

    @BuildStep
    void checkTransactionsSupport(Capabilities capabilities, BuildProducer<ValidationErrorBuildItem> validationErrors) {
        // JTA is necessary for blocking Hibernate ORM but not necessarily for Hibernate Reactive
        if (capabilities.isMissing(Capability.TRANSACTIONS)
                && capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
            validationErrors.produce(new ValidationErrorBuildItem(
                    new ConfigurationException("The Hibernate ORM extension is only functional in a JTA environment.")));
        }
    }

    @BuildStep
    void checkFormatMapperConfig(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<ValidationErrorBuildItem> validationError) {
        try {
            hibernateOrmConfig.mapping().format().global().action();
        } catch (ConfigurationException e) {
            validationError.produce(new ValidationErrorBuildItem(e));
        }
    }

    // --- PU definition and descriptor building ---

    @BuildStep
    void aggregateDefinedPersistenceUnits(
            List<PersistenceUnitDefinitionBuildItem> puDefinitions,
            BuildProducer<PersistenceUnitDefinedBuildItem> definedPersistenceUnits) {
        Map<String, Set<ProgrammingParadigm>> paradigmsByName = new LinkedHashMap<>();
        Map<String, Optional<String>> dataSourceByName = new LinkedHashMap<>();
        for (PersistenceUnitDefinitionBuildItem item : puDefinitions) {
            dataSourceByName.putIfAbsent(item.getPersistenceUnitName(), item.getDataSourceName());
            paradigmsByName.computeIfAbsent(item.getPersistenceUnitName(), k -> EnumSet.noneOf(ProgrammingParadigm.class))
                    .add(item.getParadigm());
        }
        for (var entry : paradigmsByName.entrySet()) {
            definedPersistenceUnits.produce(new PersistenceUnitDefinedBuildItem(
                    entry.getKey(), dataSourceByName.get(entry.getKey()), entry.getValue()));
        }
    }

    @BuildStep
    public void buildBlockingPersistenceUnitsFromConfig(
            HibernateOrmConfig hibernateOrmConfig,
            List<PersistenceUnitDefinitionBuildItem> persistenceUnitDefinitions,
            JpaModelPerPersistenceUnitBuildItem jpaModel,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            Capabilities capabilities,
            List<SqlLoadScriptDefaultBuildItem> additionalSqlLoadScriptDefaults,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {
        for (PersistenceUnitDefinitionBuildItem puDefinition : persistenceUnitDefinitions) {
            if (puDefinition.getParadigm() != ProgrammingParadigm.BLOCKING) {
                continue;
            }
            var model = jpaModel.getModelPerPersistenceUnit().get(puDefinition.getPersistenceUnitName());
            if (model == null) {
                model = new JpaPersistenceUnitModel();
            }
            buildBlockingPersistenceUnitFromConfig(
                    hibernateOrmConfig, puDefinition, model,
                    jdbcDataSources, applicationArchivesBuildItem, launchMode.getLaunchMode(), capabilities,
                    additionalSqlLoadScriptDefaults,
                    nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    reflectiveMethods, dbKindMetadataBuildItems);
        }
    }

    // --- Static init and runtime bootstrap ---

    @SuppressWarnings({ "unchecked", "deprecation" })
    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext, HibernateOrmRecorder recorder,
            Capabilities capabilities,
            JpaModelBuildItem jpaModel,
            HibernateOrmConfig hibernateOrmConfig,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            List<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationBuildItems,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            BuildProducer<BeanValidationTraversableResolverBuildItem> beanValidationTraversableResolver,
            LaunchModeBuildItem launchMode) throws Exception {
        validateHibernatePropertiesNotUsed();

        final boolean enableORM = !persistenceUnitDescriptorBuildItems.isEmpty();
        final boolean hibernateReactivePresent = capabilities.isPresent(Capability.HIBERNATE_REACTIVE);
        //The Hibernate Reactive extension is able to handle registration of PersistenceProviders for both reactive and
        //traditional blocking Hibernate, by depending on this module and delegating to this code.
        //So when the Hibernate Reactive extension is present, trust that it will register its own PersistenceProvider
        //which will be responsible to decide which type of ORM to bootstrap.
        //But if the extension is not present, we need to register our own PersistenceProvider - even if the ORM is not enabled!
        if (!hibernateReactivePresent) {
            recorder.callHibernateFeatureInit(enableORM);
        }

        if (!enableORM) {
            // we can bail out early
            return;
        }

        recorder.enlistPersistenceUnit(jpaModel.getEntityClassNames());

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // inspect service files for additional integrators
        Collection<Class<? extends Integrator>> integratorClasses = new LinkedHashSet<>();
        for (String integratorClassName : ServiceUtil.classNamesNamedIn(classLoader, INTEGRATOR_SERVICE_FILE)) {
            integratorClasses.add((Class<? extends Integrator>) recorderContext.classProxy(integratorClassName));
        }
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            if (launchMode.getDevModeType().orElse(null) != DevModeType.REMOTE_SERVER_SIDE) {
                integratorClasses.add(HibernateOrmDevIntegrator.class);
            }
            integratorClasses.add(SchemaManagementIntegrator.class);
        }

        Map<String, List<HibernateOrmIntegrationStaticDescriptor>> integrationStaticDescriptors = HibernateOrmIntegrationStaticConfiguredBuildItem
                .collectDescriptors(integrationBuildItems);

        List<QuarkusPersistenceUnitDefinition> finalStagePUDescriptors = new ArrayList<>();
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            finalStagePUDescriptors.add(
                    pud.asOutputPersistenceUnitDefinition(integrationStaticDescriptors
                            .getOrDefault(pud.getPersistenceUnitName(), Collections.emptyList())));
        }

        if (hasXmlMappings(persistenceUnitDescriptorBuildItems)) {
            //Make it possible to record JAXBElement as bytecode:
            recorderContext.registerSubstitution(JAXBElement.class,
                    JAXBElementSubstitution.Serialized.class,
                    JAXBElementSubstitution.class);
            recorderContext.registerSubstitution(QName.class,
                    QNameSubstitution.Serialized.class,
                    QNameSubstitution.class);
        }

        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initMetadata(finalStagePUDescriptors, integratorClasses)));
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR) && hibernateOrmConfig.enabled()) {
            beanValidationTraversableResolver
                    .produce(new BeanValidationTraversableResolverBuildItem(recorder.attributeLoadedPredicate()));
        }
    }

    @SuppressWarnings("deprecation")
    @BuildStep
    @Consume(RecorderBeanInitializedBuildItem.class)
    @Record(RUNTIME_INIT)
    public PersistenceProviderSetUpBuildItem setupPersistenceProvider(
            HibernateOrmRecorder recorder,
            Capabilities capabilities,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationBuildItems) {
        if (capabilities.isPresent(Capability.AGROAL)) {
            recorder.setupPersistenceProvider(
                    HibernateOrmIntegrationRuntimeConfiguredBuildItem.collectDescriptors(integrationBuildItems));
        }

        return new PersistenceProviderSetUpBuildItem();
    }

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Consume(JdbcDataSourceBuildItem.class)
    @Consume(JdbcDataSourceSchemaReadyBuildItem.class)
    @Consume(PersistenceProviderSetUpBuildItem.class)
    @Record(RUNTIME_INIT)
    // Producing ServiceStartBuildItem ensures this will get called before any CDI bean gets initialized
    public ServiceStartBuildItem startPersistenceUnits(HibernateOrmRecorder recorder, BeanContainerBuildItem beanContainer,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            ShutdownContextBuildItem shutdownContextBuildItem) {
        if (!persistenceUnitDescriptors.isEmpty()) {
            recorder.startAllPersistenceUnits(beanContainer.getValue(), shutdownContextBuildItem);
        }

        return new ServiceStartBuildItem("Hibernate ORM");

    }

    @Record(RUNTIME_INIT)
    @Consume(ServiceStartBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    void warnOfSchemaProblems(HibernateOrmRecorder recorder,
            HibernateOrmConfig config,
            List<PersistenceUnitDefinedBuildItem> definedPersistenceUnits) {
        for (PersistenceUnitDefinedBuildItem pu : definedPersistenceUnits) {
            if (config.persistenceUnits().get(pu.getPersistenceUnitName()).validateInDevMode()) {
                recorder.doValidation(pu.getPersistenceUnitName());
            }
        }
    }

    // --- Native image / reflection / logging ---

    @BuildStep
    void handleNativeImageImportSql(BuildProducer<NativeImageResourceBuildItem> resources,
            List<PersistenceUnitDescriptorBuildItem> descriptors) {
        for (PersistenceUnitDescriptorBuildItem i : descriptors) {
            //add resources
            String resourceName = i.getExplicitSqlImportScriptResourceName();
            if (resourceName != null) {
                resources.produce(new NativeImageResourceBuildItem(resourceName));
            }
        }
    }

    @BuildStep
    void registerJCacheForReflection(
            HibernateOrmConfig config,
            List<PersistenceUnitDefinedBuildItem> definedPersistenceUnits,
            BuildProducer<ReflectiveClassBuildItem> reflective) {

        // Only register JCache classes if at least one persistence unit has caching enabled
        boolean cachingEnabled = definedPersistenceUnits.stream()
                .anyMatch(pu -> config.persistenceUnits().get(pu.getPersistenceUnitName()).secondLevelCachingEnabled());

        if (cachingEnabled) {
            // TODO can we avoid this reflection?
            reflective.produce(ReflectiveClassBuildItem.builder(
                    "com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider")
                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                    .methods().fields().build());

            // Register custom cache weigher classes for reflection (native image support)
            for (var puConfig : config.persistenceUnits().values()) {
                for (var cacheEntry : puConfig.cache().entrySet()) {
                    cacheEntry.getValue().memory().weigherClass().ifPresent(weigherClass -> reflective
                            .produce(ReflectiveClassBuildItem.builder(weigherClass)
                                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                                    .build()));
                }
            }
        }
    }

    @BuildStep
    public void allowJacksonModuleDiscovery(Capabilities capabilities,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnits,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        if (capabilities.isMissing(Capability.JACKSON) || persistenceUnits.isEmpty()) {
            // We won't be using Hibernate's default FormatMapper relying on Jackson for sure
            return;
        }
        // Hibernate's default FormatMapper relying on Jackson requires
        // service loading to discover modules in the classpath.
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(JacksonModule.class.getName()));

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(JACKSON_3_JSON_FORMAT_MAPPER).fields(false).methods(false)
                .constructors().reason("Hibernate instantiates the class reflectively").build());
    }

    @BuildStep
    public void produceLoggingCategories(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<LogCategoryBuildItem> categories) {
        if (hibernateOrmConfig.log().bindParameters()) {
            categories.produce(new LogCategoryBuildItem("org.hibernate.orm.jdbc.bind", Level.TRACE, true));
        }
    }

    // --- Private helpers ---

    private static void buildBlockingPersistenceUnitFromConfig(
            HibernateOrmConfig hibernateOrmConfig,
            PersistenceUnitDefinitionBuildItem puDefinition,
            JpaPersistenceUnitModel model,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            Capabilities capabilities,
            List<SqlLoadScriptDefaultBuildItem> additionalSqlLoadScriptDefaults,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {
        String persistenceUnitName = puDefinition.getPersistenceUnitName();
        HibernateOrmConfigPersistenceUnit persistenceUnitConfig = puDefinition.getConfig();
        Optional<PersistenceUnitDefinitionBuildItem.AdditionalConfig> additionalPuConfig = puDefinition.getAdditionalConfig();
        Optional<String> dataSourceName = puDefinition.getDataSourceName();
        Optional<JdbcDataSourceBuildItem> jdbcDataSource = dataSourceName
                .map(name -> HibernateProcessorUtil.findDataSourceWithName(name,
                        jdbcDataSources,
                        JdbcDataSourceBuildItem::getName));

        Properties descriptorProperties = new Properties();
        additionalPuConfig.ifPresent(c -> descriptorProperties.putAll(c.properties()));

        // Previously we were pushing both class names and package names
        // to getManagedClassNames(), which was a misnomer: it could actually
        // return both class names and package names.
        // ORM 7's ScanningCoordinator would sort them out at runtime.
        // See for proof:
        // - how org.hibernate.boot.archive.scan.internal.ScanResultCollector.isListedOrDetectable
        //   was used for packages too, even though it relied (indirectly) on getManagedClassNames().
        // - the comment at org/hibernate/boot/model/process/internal/ScanningCoordinator.java:246:
        //   "IMPL NOTE : "explicitlyListedClassNames" can contain class or package names..."
        // ORM 8.0 removed ScanningCoordinator (scanning is now the container's responsibility),
        // so we now pass class names and package names separately.
        QuarkusPersistenceUnitDescriptor descriptor = new QuarkusPersistenceUnitDescriptor(
                persistenceUnitName,
                new HibernateOrmPersistenceUnitProviderHelper(),
                PersistenceUnitTransactionType.JTA,
                new ArrayList<>(model.allModelClassNames()),
                new ArrayList<>(model.modelPackageNames()),
                new Properties(),
                false);
        Set<String> entityClassNames = new HashSet<>(descriptor.getManagedClassNames());
        entityClassNames.retainAll(model.entityClassNames());

        MultiTenancyStrategy multiTenancyStrategy = HibernateProcessorUtil
                .getMultiTenancyStrategy(persistenceUnitConfig.multitenant());

        Optional<String> explicitDialect = additionalPuConfig
                .flatMap(PersistenceUnitDefinitionBuildItem.AdditionalConfig::explicitDialect)
                .or(() -> persistenceUnitConfig.dialect().dialect());
        Optional<DatabaseKind.SupportedDatabaseKind> supportedDatabaseKind = collectDialectConfig(persistenceUnitName,
                persistenceUnitConfig,
                dbKindMetadataBuildItems, jdbcDataSource, multiTenancyStrategy,
                explicitDialect,
                reflectiveMethods, descriptor.getProperties()::setProperty);

        configureProperties(descriptor, persistenceUnitConfig, hibernateOrmConfig, false);

        if (capabilities.isPresent(Capability.JACKSON)) {
            descriptor.getProperties().setProperty(MappingSettings.JSON_FORMAT_MAPPER,
                    JACKSON_3_JSON_FORMAT_MAPPER);
        }

        if (additionalPuConfig.isEmpty()) {
            configureSqlLoadScript(persistenceUnitName, persistenceUnitConfig, applicationArchivesBuildItem, launchMode,
                    additionalSqlLoadScriptDefaults,
                    nativeImageResources, hotDeploymentWatchedFiles, descriptor);
        }

        persistenceUnitDescriptors.produce(
                new PersistenceUnitDescriptorBuildItem(descriptor,
                        new RecordedConfig(
                                dataSourceName,
                                jdbcDataSource.map(JdbcDataSourceBuildItem::getDbKind),
                                supportedDatabaseKind.map(DatabaseKind.SupportedDatabaseKind::getMainName),
                                jdbcDataSource.flatMap(JdbcDataSourceBuildItem::getDbVersion),
                                jdbcDataSource.map(JdbcDataSourceBuildItem::isDbVersionUserSpecified).orElse(false),
                                explicitDialect,
                                entityClassNames,
                                multiTenancyStrategy,
                                hibernateOrmConfig.database().ormCompatibilityVersion(),
                                persistenceUnitConfig.unsupportedProperties()),
                        model.xmlMappings(),
                        false,
                        isHibernateValidatorPresent(capabilities)));
    }

    private static Optional<DatabaseKind.SupportedDatabaseKind> collectDialectConfig(String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems,
            Optional<JdbcDataSourceBuildItem> jdbcDataSource,
            MultiTenancyStrategy multiTenancyStrategy,
            Optional<String> dialect,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BiConsumer<String, String> puPropertiesCollector) {
        final HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig = persistenceUnitConfig
                .dialect();

        Optional<String> dbKind = jdbcDataSource.map(JdbcDataSourceBuildItem::getDbKind);
        Optional<String> dbVersion = jdbcDataSource.flatMap(JdbcDataSourceBuildItem::getDbVersion);
        if (multiTenancyStrategy != MultiTenancyStrategy.DATABASE && jdbcDataSource.isEmpty()) {
            String dsConfigProperty = HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "datasource");
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Datasource must be defined for persistence unit '%s'. Setting the datasource for the persistence unit can be done via the '%s' property. "
                            + " Refer to https://quarkus.io/guides/datasource for guidance.",
                    persistenceUnitName, dsConfigProperty),
                    new HashSet<>(Arrays.asList("quarkus.datasource.db-kind", "quarkus.datasource.username",
                            "quarkus.datasource.password", "quarkus.datasource.jdbc.url")));
        }

        Optional<DatabaseKind.SupportedDatabaseKind> supportedDatabaseKind = setDialectAndStorageEngine(
                persistenceUnitName,
                dbKind,
                dialect,
                dbVersion,
                dialectConfig,
                dbKindMetadataBuildItems,
                puPropertiesCollector);

        if ((dbKind.isPresent() && DatabaseKind.isPostgreSQL(dbKind.get())
                || (dialect.isPresent() && dialect.get().toLowerCase(Locale.ROOT).contains("postgres")))) {
            // Workaround for https://hibernate.atlassian.net/browse/HHH-19063
            reflectiveMethods.produce(new ReflectiveMethodBuildItem(
                    "Accessed in org.hibernate.engine.jdbc.env.internal.DefaultSchemaNameResolver.determineAppropriateResolverDelegate",
                    true, "org.postgresql.jdbc.PgConnection", "getSchema"));
        }

        return supportedDatabaseKind;
    }

    private void validateHibernatePropertiesNotUsed() {
        try {
            final Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(
                    "hibernate.properties");
            if (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                throw new IllegalStateException(
                        "The Hibernate ORM configuration in Quarkus does not support sourcing configuration properties from resources named `hibernate.properties`,"
                                + " and this is now expressly prohibited as such a file could lead to unpredictable semantics. Please remove it from `"
                                + url.toExternalForm() + '`');
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean hasXmlMappings(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems) {
        for (PersistenceUnitDescriptorBuildItem descriptor : persistenceUnitDescriptorBuildItems) {
            if (descriptor.hasXmlMappings()) {
                return true;
            }
        }
        return false;
    }

}
