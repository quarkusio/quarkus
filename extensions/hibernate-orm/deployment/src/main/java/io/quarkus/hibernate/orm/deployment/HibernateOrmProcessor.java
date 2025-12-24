package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureProperties;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.configureSqlLoadScript;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.isHibernateValidatorPresent;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.jsonFormatterCustomizationCheck;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.jsonMapperKind;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.setDialectAndStorageEngine;
import static io.quarkus.hibernate.orm.deployment.util.HibernateProcessorUtil.xmlMapperKind;
import static io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofClassAnnotation;
import static io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem.ofMethodAnnotation;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Default;
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.xml.bind.JAXBElement;

import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;
import org.hibernate.annotations.processing.SQL;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.boot.beanvalidation.BeanValidationIntegrator;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;

import com.fasterxml.jackson.databind.Module;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.RecorderBeanInitializedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.SyntheticBeansRuntimeInitBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanTypeExclusion;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.deployment.staticmethods.InterceptedStaticMethodsTransformersRegisteredBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.BuildException;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeRecorderConstantDefinitionBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.QuarkusClassFileLocator;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.dev.HibernateOrmDevIntegrator;
import io.quarkus.hibernate.orm.runtime.HibernateOrmPersistenceUnitProviderHelper;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDescriptor;
import io.quarkus.hibernate.orm.runtime.boot.scan.QuarkusScanner;
import io.quarkus.hibernate.orm.runtime.boot.xml.JAXBElementSubstitution;
import io.quarkus.hibernate.orm.runtime.boot.xml.QNameSubstitution;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;
import io.quarkus.hibernate.orm.runtime.customized.FormatMapperKind;
import io.quarkus.hibernate.orm.runtime.customized.JsonFormatterCustomizationCheck;
import io.quarkus.hibernate.orm.runtime.graal.RegisterServicesForReflectionFeature;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.recording.RecordedConfig;
import io.quarkus.hibernate.orm.runtime.schema.SchemaManagementIntegrator;
import io.quarkus.hibernate.orm.runtime.service.FlatClassLoaderService;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.hibernate.validator.spi.BeanValidationTraversableResolverBuildItem;
import io.quarkus.panache.hibernate.common.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.panache.hibernate.common.deployment.HibernateModelClassCandidatesForFieldAccessBuildItem;
import io.quarkus.reactive.datasource.spi.ReactiveDataSourceBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.security.spi.SecuredInterfaceAnnotationBuildItem;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;
import net.bytebuddy.pool.TypePool.CacheProvider;
import net.bytebuddy.pool.TypePool.Default.ReaderMode;

/**
 * Simulacrum of JPA bootstrap.
 * <p>
 * This does not address the proper integration with Hibernate
 * Rather prepare the path to providing the right metadata
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
@BuildSteps(onlyIf = HibernateOrmEnabled.class)
public final class HibernateOrmProcessor {

    public static final String HIBERNATE_ORM_CONFIG_PREFIX = "quarkus.hibernate-orm.";

    /**
     * Collection of Hibernate annotations for which, if detected on interface, Hibernate processor generates repository.
     */
    public static final Set<Class<?>> HIBERNATE_REPOSITORY_ANNOTATIONS = Set.of(Find.class, HQL.class, SQL.class);

    private static final Logger LOG = Logger.getLogger(HibernateOrmProcessor.class);

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    private static final String JAKARTA_DATA_REPOSITORY_ANNOTATION = "jakarta.data.repository.Repository";

    @BuildStep
    NativeImageFeatureBuildItem registerServicesForReflection(BuildProducer<ServiceProviderBuildItem> services) {
        for (DotName serviceProvider : ClassNames.SERVICE_PROVIDERS) {
            services.produce(ServiceProviderBuildItem.allProvidersFromClassPath(serviceProvider.toString()));
        }

        return new NativeImageFeatureBuildItem(RegisterServicesForReflectionFeature.class);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void registerStrategyForReflection(
            BuildProducer<ReflectiveClassBuildItem> reflective) {

        // Hibernate ORM uses reflection at runtime two create these two strategies,
        // So we need this to support native-image
        // These strategies are only used in offline mode https://github.com/quarkusio/quarkus/pull/48130 so far
        // When Hibernate will support temporary table creation inside the `hbm2ddl` tool
        // https://hibernate.atlassian.net/browse/HHH-15525 the strategies won't be needed anymore and this can be removed
        reflective.produce(ReflectiveClassBuildItem.builder(
                LocalTemporaryTableInsertStrategy.class,
                LocalTemporaryTableMutationStrategy.class)
                .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                .methods().fields().build());
    }

    @BuildStep
    void registerHibernateOrmMetadataForCoreDialects(
            BuildProducer<DatabaseKindDialectBuildItem> producer) {
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.DB2, "DB2",
                Set.of("org.hibernate.dialect.DB2Dialect")));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.H2, "H2",
                Set.of("org.hibernate.dialect.H2Dialect"),
                // Using our own default version is extra important for H2
                // See https://github.com/quarkusio/quarkus/issues/1886
                DialectVersions.Defaults.H2));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.MARIADB, "MariaDB",
                Set.of("org.hibernate.dialect.MariaDBDialect"),
                DialectVersions.Defaults.MARIADB));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.MSSQL, "Microsoft SQL Server",
                Set.of("org.hibernate.dialect.SQLServerDialect"),
                DialectVersions.Defaults.MSSQL));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.MYSQL, "MySQL",
                Set.of("org.hibernate.dialect.MySQLDialect")));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.ORACLE, "Oracle",
                Set.of("org.hibernate.dialect.OracleDialect")));
        producer.produce(DatabaseKindDialectBuildItem.forCoreDialect(DatabaseKind.POSTGRESQL, "PostgreSQL",
                Set.of("org.hibernate.dialect.PostgreSQLDialect")));
    }

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
    void includeArchivesHostingEntityPackagesInIndex(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> additionalApplicationArchiveMarkers) {
        for (HibernateOrmConfigPersistenceUnit persistenceUnit : hibernateOrmConfig.persistenceUnits()
                .values()) {
            if (persistenceUnit.packages().isPresent()) {
                for (String pakkage : persistenceUnit.packages().get()) {
                    additionalApplicationArchiveMarkers
                            .produce(new AdditionalApplicationArchiveMarkerBuildItem(pakkage.replace('.', '/')));
                }
            }
        }
    }

    @Record(RUNTIME_INIT)
    @Consume(ServiceStartBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    void warnOfSchemaProblems(HibernateOrmRecorder recorder, HibernateOrmConfig hibernateOrmBuildTimeConfig) {
        for (var e : hibernateOrmBuildTimeConfig.persistenceUnits().entrySet()) {
            if (e.getValue().validateInDevMode()) {
                recorder.doValidation(e.getKey());
            }
        }
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem addPersistenceUnitAnnotationToIndex() {
        return new AdditionalIndexedClassesBuildItem(ClassNames.QUARKUS_PERSISTENCE_UNIT.toString());
    }

    @BuildStep
    public void enrollBeanValidationTypeSafeActivatorForReflection(Capabilities capabilities,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            // BeanValidationIntegrator is only added if this capability is present, see FastBootMetadataBuilder

            // Accessed in org.hibernate.boot.beanvalidation.BeanValidationIntegrator.loadTypeSafeActivatorClass
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder("org.hibernate.boot.beanvalidation.TypeSafeActivator")
                    .methods().fields().build());
            // Accessed in org.hibernate.boot.beanvalidation.BeanValidationIntegrator.isBeanValidationApiAvailable
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(BeanValidationIntegrator.JAKARTA_BV_CHECK_CLASS)
                    .constructors(false).build());
        }
    }

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles(HibernateOrmConfig config) {
        List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>();
        if (!shouldIgnorePersistenceXmlResources(config)) {
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem("META-INF/persistence.xml"));
        }
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem(INTEGRATOR_SERVICE_FILE));

        // SQL load scripts are handled when assembling the Quarkus-configured persistence units

        return watchedFiles;
    }

    //Integration point: allow other extensions to define additional PersistenceXmlDescriptorBuildItem
    @BuildStep
    public void parsePersistenceXmlDescriptors(HibernateOrmConfig config,
            BuildProducer<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptorBuildItemBuildProducer) {
        if (!shouldIgnorePersistenceXmlResources(config)) {
            var parser = PersistenceXmlParser.create(Map.of(), null, FlatClassLoaderService.INSTANCE);
            var urls = parser.getClassLoaderService().locateResources("META-INF/persistence.xml");
            if (urls.isEmpty()) {
                return;
            }
            for (var desc : parser.parse(urls).values()) {
                persistenceXmlDescriptorBuildItemBuildProducer.produce(new PersistenceXmlDescriptorBuildItem(desc));
            }
        }
    }

    //Integration point: allow other extensions to watch for ImpliedBlockingPersistenceUnitTypeBuildItem
    @BuildStep
    public ImpliedBlockingPersistenceUnitTypeBuildItem defineTypeOfImpliedPU(
            List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem, //This is from Agroal SPI: safe to use even for Hibernate Reactive
            Capabilities capabilities) {
        if (capabilities.isPresent(Capability.HIBERNATE_REACTIVE) && jdbcDataSourcesBuildItem.isEmpty()) {
            // if we don't have any blocking datasources and Hibernate Reactive is present,
            // we don't want a blocking persistence unit
            return ImpliedBlockingPersistenceUnitTypeBuildItem.none();
        } else {
            return ImpliedBlockingPersistenceUnitTypeBuildItem.generateImpliedPersistenceUnit();
        }
    }

    @BuildStep
    public void allowJacksonModuleDiscovery(Capabilities capabilities,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnits,
            BuildProducer<ServiceProviderBuildItem> serviceProviders) {
        if (capabilities.isMissing(Capability.JACKSON) || persistenceUnits.isEmpty()) {
            // We won't be using Hibernate's default FormatMapper relying on Jackson for sure
            return;
        }
        // Hibernate's default FormatMapper relying on Jackson requires
        // service loading to discover modules in the classpath.
        serviceProviders.produce(ServiceProviderBuildItem.allProvidersFromClassPath(Module.class.getName()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void configurationDescriptorBuilding(
            HibernateOrmRecorder recorder,
            HibernateOrmConfig hibernateOrmConfig,
            CombinedIndexBuildItem index,
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedPU,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            JpaModelBuildItem jpaModel,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {
        // First produce the PUs having a persistence.xml: these are not reactive, as we don't allow using a persistence.xml for them.
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            PersistenceUnitDescriptor xmlDescriptor = persistenceXmlDescriptorBuildItem.getDescriptor();
            String puName = xmlDescriptor.getName();
            Optional<JdbcDataSourceBuildItem> jdbcDataSource = jdbcDataSources.stream()
                    .filter(i -> i.isDefault())
                    .findFirst();
            collectDialectConfigForPersistenceXml(puName, xmlDescriptor);
            Optional<FormatMapperKind> jsonMapper = jsonMapperKind(capabilities,
                    hibernateOrmConfig.mapping().format().global());
            Optional<FormatMapperKind> xmlMapper = xmlMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
            jsonMapper.flatMap(FormatMapperKind::requiredBeanType)
                    .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
            xmlMapper.flatMap(FormatMapperKind::requiredBeanType)
                    .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
            JsonFormatterCustomizationCheck jsonFormatterCustomizationCheck = jsonFormatterCustomizationCheck(
                    capabilities, jsonMapper);
            persistenceUnitDescriptors
                    .produce(new PersistenceUnitDescriptorBuildItem(
                            QuarkusPersistenceUnitDescriptor.validateAndReadFrom(xmlDescriptor),
                            new RecordedConfig(
                                    Optional.of(DataSourceUtil.DEFAULT_DATASOURCE_NAME),
                                    jdbcDataSource.map(JdbcDataSourceBuildItem::getDbKind),
                                    Optional.empty(),
                                    jdbcDataSource.flatMap(JdbcDataSourceBuildItem::getDbVersion),
                                    Optional.ofNullable(xmlDescriptor.getProperties().getProperty(AvailableSettings.DIALECT)),
                                    Set.of(), // Not relevant for persistence.xml, because such a PU never gets deactivated.
                                    getMultiTenancyStrategy(
                                            Optional.ofNullable(persistenceXmlDescriptorBuildItem.getDescriptor()
                                                    .getProperties().getProperty("hibernate.multiTenancy"))), //FIXME this property is meaningless in Hibernate ORM 6
                                    hibernateOrmConfig.database().ormCompatibilityVersion(),
                                    hibernateOrmConfig.mapping().format().global(),
                                    jsonFormatterCustomizationCheck,
                                    Collections.emptyMap()),
                            null,
                            jpaModel.getXmlMappings(persistenceXmlDescriptorBuildItem.getDescriptor().getName()),
                            true, isHibernateValidatorPresent(capabilities), jsonMapper, xmlMapper));
        }

        if (impliedPU.shouldGenerateImpliedBlockingPersistenceUnit()) {
            handleHibernateORMWithNoPersistenceXml(hibernateOrmConfig, index, persistenceXmlDescriptors,
                    jdbcDataSources, reactiveDataSources, applicationArchivesBuildItem, launchMode.getLaunchMode(),
                    additionalJpaModelBuildItems,
                    jpaModel, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    reflectiveMethods, unremovableBeans, dbKindMetadataBuildItems);
        }
    }

    @BuildStep
    @SuppressWarnings("deprecation")
    public JpaModelIndexBuildItem jpaEntitiesIndexer(
            CombinedIndexBuildItem index,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems) {
        Set<String> additionalClassNames = new HashSet<>();
        for (AdditionalJpaModelBuildItem jpaModel : additionalJpaModelBuildItems) {
            additionalClassNames.add(jpaModel.getClassName());
        }
        for (io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem jpaModel : deprecatedAdditionalJpaModelBuildItems) {
            additionalClassNames.add(jpaModel.getClassName());
        }
        // build a composite index with additional jpa model classes
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        for (String className : additionalClassNames) {
            IndexingUtil.indexClass(className, indexer, index.getIndex(), additionalIndex,
                    HibernateOrmProcessor.class.getClassLoader());
        }
        CompositeIndex compositeIndex = CompositeIndex.create(index.getComputingIndex(), indexer.complete());
        return new JpaModelIndexBuildItem(compositeIndex);
    }

    @BuildStep
    public void contributePersistenceXmlToJpaModel(
            BuildProducer<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors) {
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptor : persistenceXmlDescriptors) {
            PersistenceUnitDescriptor descriptor = persistenceXmlDescriptor.getDescriptor();
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    descriptor.getName(), descriptor.getPersistenceUnitRootUrl(), descriptor.getManagedClassNames(),
                    descriptor.getMappingFileNames()));
        }
    }

    @BuildStep
    public void contributeQuarkusConfigToJpaModel(
            BuildProducer<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions,
            HibernateOrmConfig hibernateOrmConfig) {
        for (Entry<String, HibernateOrmConfigPersistenceUnit> entry : hibernateOrmConfig.persistenceUnits()
                .entrySet()) {
            String name = entry.getKey();
            HibernateOrmConfigPersistenceUnit config = entry.getValue();
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    name, null, Collections.emptySet(),
                    config.mappingFiles().orElse(Collections.emptySet())));
        }
    }

    @BuildStep
    public void defineJpaEntities(
            JpaModelIndexBuildItem indexBuildItem,
            BuildProducer<JpaModelBuildItem> domainObjectsProducer,
            List<IgnorableNonIndexedClasses> ignorableNonIndexedClassesBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions) throws BuildException {

        Set<String> ignorableNonIndexedClasses = Collections.emptySet();
        if (!ignorableNonIndexedClassesBuildItems.isEmpty()) {
            ignorableNonIndexedClasses = new HashSet<>();
            for (IgnorableNonIndexedClasses buildItem : ignorableNonIndexedClassesBuildItems) {
                ignorableNonIndexedClasses.addAll(buildItem.getClasses());
            }
        }

        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, hotDeploymentWatchedFiles,
                jpaModelPuContributions, indexBuildItem.getIndex(), ignorableNonIndexedClasses);
        final JpaModelBuildItem domainObjects = scavenger.discoverModelAndRegisterForReflection();
        domainObjectsProducer.produce(domainObjects);
    }

    @BuildStep
    public BytecodeRecorderConstantDefinitionBuildItem pregenProxies(
            JpaModelBuildItem jpaModel,
            JpaModelIndexBuildItem indexBuildItem,
            TransformedClassesBuildItem transformedClassesBuildItem,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem,
            ExecutorService buildExecutor) throws ExecutionException, InterruptedException {
        Set<String> managedClassAndPackageNames = new HashSet<>(jpaModel.getEntityClassNames());
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            // Note: getManagedClassNames() can also return *package* names
            // See the source code of Hibernate ORM for proof:
            // org.hibernate.boot.archive.scan.internal.ScanResultCollector.isListedOrDetectable
            // is used for packages too, and it relies (indirectly) on getManagedClassNames().
            managedClassAndPackageNames.addAll(pud.getManagedClassNames());
        }

        for (AdditionalJpaModelBuildItem additionalJpaModelBuildItem : additionalJpaModelBuildItems) {
            managedClassAndPackageNames.add(additionalJpaModelBuildItem.getClassName());
        }

        PreGeneratedProxies proxyDefinitions = generateProxies(managedClassAndPackageNames,
                indexBuildItem.getIndex(), transformedClassesBuildItem,
                generatedClassBuildItemBuildProducer, liveReloadBuildItem, buildExecutor);

        // Make proxies available through a constant;
        // this is a hack to avoid introducing circular dependencies between build steps.
        //
        // If we just passed the proxy definitions to #build as a normal build item,
        // we would have the following dependencies:
        //
        // #pregenProxies => ProxyDefinitionsBuildItem => #build => BeanContainerListenerBuildItem
        // => Arc container init => BeanContainerBuildItem
        // => some RestEasy Reactive Method => BytecodeTransformerBuildItem
        // => build step that transforms bytecode => TransformedClassesBuildItem
        // => #pregenProxies
        //
        // Since the dependency from #preGenProxies to #build is only a static init thing
        // (#build needs to pass the proxy definitions to the recorder),
        // we get rid of the circular dependency by defining a constant
        // to pass the proxy definitions to the recorder.
        // That way, the dependency is only between #pregenProxies
        // and the build step that generates the bytecode of bytecode recorders.
        return new BytecodeRecorderConstantDefinitionBuildItem(PreGeneratedProxies.class, proxyDefinitions);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void preGenAnnotationProxies(List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflective,
            BuildProducer<NativeImageProxyDefinitionBuildItem> proxyDefinitions) {
        if (hasXmlMappings(persistenceUnitDescriptorBuildItems)) {
            // XML mapping may need to create annotation proxies, which requires reflection
            // and pre-generation of the proxy classes.
            // This probably could be optimized,
            // but there are plans to make deep changes to XML mapping in ORM (to rely on Jandex directly),
            // so let's not waste our time on optimizations that won't be relevant in a few months.
            List<String> annotationClassNames = new ArrayList<>();
            for (DotName name : ClassNames.JPA_MAPPING_ANNOTATIONS) {
                annotationClassNames.add(name.toString());
            }
            for (DotName name : ClassNames.HIBERNATE_MAPPING_ANNOTATIONS) {
                annotationClassNames.add(name.toString());
            }
            reflective.produce(ReflectiveClassBuildItem.builder(annotationClassNames.toArray(new String[0]))
                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                    .methods().fields().build());
            for (String annotationClassName : annotationClassNames) {
                proxyDefinitions.produce(new NativeImageProxyDefinitionBuildItem(annotationClassName));
            }
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

    @SuppressWarnings("unchecked")
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

        final QuarkusScanner scanner = buildQuarkusScanner(jpaModel);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // inspect service files for additional integrators
        Collection<Class<? extends Integrator>> integratorClasses = new LinkedHashSet<>();
        for (String integratorClassName : ServiceUtil.classNamesNamedIn(classLoader, INTEGRATOR_SERVICE_FILE)) {
            integratorClasses.add((Class<? extends Integrator>) recorderContext.classProxy(integratorClassName));
        }
        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            integratorClasses.add(HibernateOrmDevIntegrator.class);
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
                        recorder.initMetadata(finalStagePUDescriptors, scanner, integratorClasses)));
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR) && hibernateOrmConfig.enabled()) {
            beanValidationTraversableResolver
                    .produce(new BeanValidationTraversableResolverBuildItem(recorder.attributeLoadedPredicate()));
        }
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

    @Consume(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    @BuildStep
    @SuppressWarnings("deprecation")
    public HibernateEnhancersRegisteredBuildItem enhancerDomainObjects(JpaModelBuildItem jpaModel,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        // Modify the bytecode of all entities to enable lazy-loading, dirty checking, etc..
        enhanceEntities(jpaModel, transformers, additionalJpaModelBuildItems,
                deprecatedAdditionalJpaModelBuildItems, additionalClasses);
        // this allows others to register their enhancers after Hibernate, so they run before ours
        return new HibernateEnhancersRegisteredBuildItem();
    }

    @BuildStep
    public HibernateModelClassCandidatesForFieldAccessBuildItem candidatesForFieldAccess(JpaModelBuildItem jpaModel) {
        // Ask Panache to replace direct access to public fields with calls to accessors for all model classes.
        return new HibernateModelClassCandidatesForFieldAccessBuildItem(jpaModel.getManagedClassNames());
    }

    @BuildStep
    public void build(BuildProducer<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<PersistenceUnitDescriptorBuildItem> descriptors) throws Exception {
        if (descriptors.isEmpty()) {
            return;
        }

        Map<String, Set<String>> entityPersistenceUnitMapping = new HashMap<>();
        boolean incomplete = false;
        for (PersistenceUnitDescriptorBuildItem descriptor : descriptors) {
            if (descriptor.isFromPersistenceXml()) {
                // In this case some entities might be detected by Hibernate on boot,
                // so we need to let Panache know that this mapping may be incomplete.
                incomplete = true;
            }
            for (String entityClass : descriptor.getManagedClassNames()) {
                entityPersistenceUnitMapping.putIfAbsent(entityClass, new HashSet<>());
                entityPersistenceUnitMapping.get(entityClass).add(descriptor.getPersistenceUnitName());
            }
        }

        jpaModelPersistenceUnitMapping
                .produce(new JpaModelPersistenceUnitMappingBuildItem(entityPersistenceUnitMapping, incomplete));
    }

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

    @BuildStep
    @Record(RUNTIME_INIT)
    public void multitenancy(HibernateOrmRecorder recorder,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {
        boolean multitenancyEnabled = false;

        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : persistenceUnitDescriptors) {
            String persistenceUnitConfigName = persistenceUnitDescriptor.getPersistenceUnitName();
            var multitenancyStrategy = persistenceUnitDescriptor.getConfig().getMultiTenancyStrategy();
            switch (multitenancyStrategy) {
                case NONE -> {
                }
                case DISCRIMINATOR -> multitenancyEnabled = true;
                case DATABASE, SCHEMA -> {
                    multitenancyEnabled = true;

                    String multiTenancySchemaDataSource = persistenceUnitDescriptor.getMultiTenancySchemaDataSource();
                    Optional<String> datasource;
                    if (multitenancyStrategy == MultiTenancyStrategy.SCHEMA && multiTenancySchemaDataSource != null) {
                        LOG.warnf("Configuration property '%1$s' is deprecated. Use '%2$s' instead.",
                                HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitConfigName,
                                        "multitenant-schema-datasource"),
                                HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitConfigName, "datasource"));
                        datasource = Optional.of(multiTenancySchemaDataSource);
                    } else {
                        datasource = persistenceUnitDescriptor.getConfig().getDataSource();
                    }

                    ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem
                            .configure(DataSourceTenantConnectionResolver.class)
                            .scope(ApplicationScoped.class)
                            .types(TenantConnectionResolver.class)
                            .setRuntimeInit()
                            .defaultBean()
                            .unremovable()
                            .supplier(recorder.dataSourceTenantConnectionResolver(
                                    persistenceUnitDescriptor.getPersistenceUnitName(),
                                    datasource,
                                    persistenceUnitDescriptor.getConfig().getMultiTenancyStrategy()));

                    if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitDescriptor.getPersistenceUnitName())) {
                        configurator.addQualifier(Default.class);
                    } else {
                        configurator.addQualifier().annotation(DotNames.NAMED)
                                .addValue("value", persistenceUnitDescriptor.getPersistenceUnitName()).done();
                        configurator.addQualifier().annotation(PersistenceUnit.class)
                                .addValue("value", persistenceUnitDescriptor.getPersistenceUnitName()).done();
                    }

                    syntheticBeans.produce(configurator.done());
                }
            }
        }

        if (multitenancyEnabled) {
            unremovableBeans
                    .produce(new UnremovableBeanBuildItem(new BeanTypeExclusion(ClassNames.TENANT_CONNECTION_RESOLVER)));
            unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanTypeExclusion(ClassNames.TENANT_RESOLVER)));
        }
    }

    @BuildStep
    public void produceLoggingCategories(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<LogCategoryBuildItem> categories) {
        if (hibernateOrmConfig.log().bindParam() || hibernateOrmConfig.log().bindParameters()) {
            categories.produce(new LogCategoryBuildItem("org.hibernate.orm.jdbc.bind", Level.TRACE, true));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerStaticMetamodelClassesForReflection(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflective) {
        Collection<AnnotationInstance> annotationInstances = index.getIndex().getAnnotations(ClassNames.STATIC_METAMODEL);
        if (!annotationInstances.isEmpty()) {

            String[] metamodel = annotationInstances.stream()
                    .map(a -> a.target().asClass().name().toString())
                    .toArray(String[]::new);

            reflective.produce(ReflectiveClassBuildItem.builder(metamodel)
                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                    .constructors(false).fields().build());
        }
    }

    /*
     * Enable reflection for methods annotated with @InjectService,
     * such as org.hibernate.engine.jdbc.cursor.internal.StandardRefCursorSupport.injectJdbcServices.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerInjectServiceMethodsForReflection(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflective) {
        Set<String> classes = new HashSet<>();

        // Built-in service classes; can't rely on Jandex as Hibernate ORM is not indexed by default.
        ClassNames.ANNOTATED_WITH_INJECT_SERVICE.stream()
                .map(DotName::toString)
                .forEach(classes::add);

        // Integrators relying on @InjectService.
        index.getIndex().getAnnotations(ClassNames.INJECT_SERVICE).stream()
                .map(a -> a.target().asMethod().declaringClass().name().toString())
                .forEach(classes::add);

        if (!classes.isEmpty()) {
            reflective.produce(ReflectiveClassBuildItem.builder(classes.toArray(new String[0]))
                    .reason(ClassNames.HIBERNATE_ORM_PROCESSOR.toString())
                    .constructors(false).methods().build());
        }
    }

    @BuildStep
    void registerJakartaDataRepositorySecurityAnnotations(Capabilities capabilities,
            BuildProducer<SecuredInterfaceAnnotationBuildItem> securedInterfaceAnnotationProducer) {
        if (capabilities.isPresent(Capability.SECURITY)) {
            securedInterfaceAnnotationProducer.produce(ofClassAnnotation(JAKARTA_DATA_REPOSITORY_ANNOTATION));
            HIBERNATE_REPOSITORY_ANNOTATIONS
                    .forEach(annotation -> securedInterfaceAnnotationProducer.produce(ofMethodAnnotation(annotation)));
        }
    }

    private void handleHibernateORMWithNoPersistenceXml(
            HibernateOrmConfig hibernateOrmConfig,
            CombinedIndexBuildItem index,
            List<PersistenceXmlDescriptorBuildItem> descriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            JpaModelBuildItem jpaModel,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {
        if (!descriptors.isEmpty()) {
            if (hibernateOrmConfig.isAnyNonPersistenceXmlPropertySet()) {
                throw new ConfigurationException(
                        "A legacy persistence.xml file is present in the classpath, but Hibernate ORM is also configured through the Quarkus config file.\n"
                                + "Legacy persistence.xml files and Quarkus configuration cannot be used at the same time.\n"
                                + "To ignore persistence.xml files, set the configuration property"
                                + " 'quarkus.hibernate-orm.persistence-xml.ignore' to 'true'.\n"
                                + "To use persistence.xml files, remove all '" + HIBERNATE_ORM_CONFIG_PREFIX
                                + "*' properties from the Quarkus config file.");
            } else {
                // It's theoretically possible to use the Quarkus Hibernate ORM extension
                // without setting any build-time configuration property,
                // so the condition above might not catch all attempts to use persistence.xml and Quarkus-configured PUs
                // at the same time.
                // At that point, the only thing we can do is log something,
                // so that hopefully people in that situation will notice that their Quarkus configuration is being ignored.
                LOG.infof(
                        "A legacy persistence.xml file is present in the classpath. This file will be used to configure JPA/Hibernate ORM persistence units,"
                                + " and any configuration of the Hibernate ORM extension will be ignored."
                                + " To ignore persistence.xml files instead, set the configuration property"
                                + " 'quarkus.hibernate-orm.persistence-xml.ignore' to 'true'.");
                return;
            }
        }

        if (!hibernateOrmConfig.blocking()) {
            LOG.infof(
                    "Hibernate ORM was disabled explicitly by quarkus.hibernate-orm.blocking=false");
            return;
        }

        Optional<JdbcDataSourceBuildItem> defaultJdbcDataSource = jdbcDataSources.stream()
                .filter(i -> i.isDefault())
                .findFirst();
        boolean enableDefaultPersistenceUnit = (defaultJdbcDataSource.isPresent()
                && hibernateOrmConfig.namedPersistenceUnits().isEmpty())
                || hibernateOrmConfig.defaultPersistenceUnit().isAnyPropertySet();

        var modelPerPersistencesUnit = getModelPerPersistenceUnit(
                hibernateOrmConfig, additionalJpaModelBuildItems, jpaModel, index.getIndex(), enableDefaultPersistenceUnit);
        var modelForDefaultPersistenceUnit = modelPerPersistencesUnit.get(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
        if (modelForDefaultPersistenceUnit == null) {
            modelForDefaultPersistenceUnit = new JpaPersistenceUnitModel();
        }

        if (enableDefaultPersistenceUnit) {
            producePersistenceUnitDescriptorFromConfig(
                    hibernateOrmConfig, jpaModel, PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    hibernateOrmConfig.defaultPersistenceUnit(),
                    modelForDefaultPersistenceUnit.allModelClassAndPackageNames(),
                    jpaModel.getXmlMappings(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME),
                    jdbcDataSources, reactiveDataSources, applicationArchivesBuildItem, launchMode, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    reflectiveMethods, unremovableBeans, dbKindMetadataBuildItems);
        } else if (!modelForDefaultPersistenceUnit.entityClassNames().isEmpty()
                && (!hibernateOrmConfig.defaultPersistenceUnit().datasource().isPresent()
                        || DataSourceUtil.isDefault(hibernateOrmConfig.defaultPersistenceUnit().datasource().get()))
                && !defaultJdbcDataSource.isPresent()) {
            // We're not enable the default PU, meaning there is no explicit configuration for it,
            // and we couldn't find a default datasource.
            // But there are entities assigned to it, meaning these entities can never work properly.
            // This looks like a mistake, so we'll error out.
            String persistenceUnitName = PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME;
            String dataSourceName = DataSourceUtil.DEFAULT_DATASOURCE_NAME;
            var cause = DataSourceUtil.dataSourceNotConfigured(dataSourceName);
            throw new ConfigurationException(String.format(Locale.ROOT,
                    "Persistence unit '%s' defines entities %s, but its datasource '%s' cannot be found: %s"
                            + " Alternatively, disable Hibernate ORM by setting '%s=false', and the entities will be ignored.",
                    persistenceUnitName, modelForDefaultPersistenceUnit.entityClassNames(),
                    dataSourceName,
                    cause.getMessage(),
                    HibernateOrmRuntimeConfig.extensionPropertyKey("enabled")),
                    cause);
        }

        for (Entry<String, HibernateOrmConfigPersistenceUnit> persistenceUnitEntry : hibernateOrmConfig.namedPersistenceUnits()
                .entrySet()) {
            var persistenceUnitName = persistenceUnitEntry.getKey();
            var model = modelPerPersistencesUnit.get(persistenceUnitEntry.getKey());
            producePersistenceUnitDescriptorFromConfig(
                    hibernateOrmConfig, jpaModel, persistenceUnitName, persistenceUnitEntry.getValue(),
                    model == null ? Collections.emptySet() : model.allModelClassAndPackageNames(),
                    jpaModel.getXmlMappings(persistenceUnitName),
                    jdbcDataSources, reactiveDataSources, applicationArchivesBuildItem, launchMode, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    reflectiveMethods, unremovableBeans, dbKindMetadataBuildItems);
        }
    }

    private static void producePersistenceUnitDescriptorFromConfig(
            HibernateOrmConfig hibernateOrmConfig, JpaModelBuildItem jpaModel,
            String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            Set<String> modelClassesAndPackages,
            List<RecordableXmlMapping> xmlMappings,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            List<ReactiveDataSourceBuildItem> reactiveDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {

        Optional<JdbcDataSourceBuildItem> jdbcDataSource = HibernateDataSourceUtil.findDataSourceWithNameDefault(
                persistenceUnitName,
                jdbcDataSources,
                JdbcDataSourceBuildItem::getName,
                JdbcDataSourceBuildItem::isDefault, persistenceUnitConfig.datasource());

        Optional<ReactiveDataSourceBuildItem> reactiveDataSource = HibernateDataSourceUtil.findDataSourceWithNameDefault(
                persistenceUnitName,
                reactiveDataSources,
                ReactiveDataSourceBuildItem::getName,
                ReactiveDataSourceBuildItem::isDefault, persistenceUnitConfig.datasource());

        if (jdbcDataSource.isEmpty() && reactiveDataSource.isPresent()) {
            LOG.debugf("The datasource '%s' is only reactive, do not create this PU '%s' as blocking",
                    persistenceUnitConfig.datasource().orElse(DEFAULT_PERSISTENCE_UNIT_NAME), persistenceUnitName);
            return;
        }

        boolean explicitDataSource = persistenceUnitConfig.datasource().isPresent();
        if (jdbcDataSource.isEmpty() && explicitDataSource) {
            String dataSourceName = persistenceUnitConfig.datasource().get();
            throw PersistenceUnitUtil.unableToFindDataSource(persistenceUnitName, dataSourceName,
                    DataSourceUtil.dataSourceNotConfigured(dataSourceName));
        }

        Optional<String> dataSourceName = jdbcDataSource.map(JdbcDataSourceBuildItem::getName);

        QuarkusPersistenceUnitDescriptor descriptor = new QuarkusPersistenceUnitDescriptor(
                persistenceUnitName,
                new HibernateOrmPersistenceUnitProviderHelper(),
                PersistenceUnitTransactionType.JTA,
                // That's right, we're pushing both class names and package names
                // to a method called "addClasses".
                // It's a misnomer: while the method populates the set that backs getManagedClasses(),
                // that method is also poorly named because it can actually return both class names
                // and package names.
                // See for proof:
                // - how org.hibernate.boot.archive.scan.internal.ScanResultCollector.isListedOrDetectable
                //   is used for packages too, even though it relies (indirectly) on getManagedClassNames().
                // - the comment at org/hibernate/boot/model/process/internal/ScanningCoordinator.java:246:
                //   "IMPL NOTE : "explicitlyListedClassNames" can contain class or package names..."
                new ArrayList<>(modelClassesAndPackages),
                new Properties(),
                false);
        Set<String> entityClassNames = new HashSet<>(descriptor.getManagedClassNames());
        entityClassNames.retainAll(jpaModel.getEntityClassNames());

        MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy(persistenceUnitConfig.multitenant());

        Optional<DatabaseKind.SupportedDatabaseKind> supportedDatabaseKind = collectDialectConfig(persistenceUnitName,
                persistenceUnitConfig,
                dbKindMetadataBuildItems, jdbcDataSource, multiTenancyStrategy,
                systemProperties, reflectiveMethods, descriptor.getProperties()::setProperty);

        configureProperties(descriptor, persistenceUnitConfig, hibernateOrmConfig, false);

        configureSqlLoadScript(persistenceUnitName, persistenceUnitConfig, applicationArchivesBuildItem, launchMode,
                nativeImageResources, hotDeploymentWatchedFiles, descriptor);

        Optional<FormatMapperKind> jsonMapper = jsonMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
        Optional<FormatMapperKind> xmlMapper = xmlMapperKind(capabilities, hibernateOrmConfig.mapping().format().global());
        jsonMapper.flatMap(FormatMapperKind::requiredBeanType)
                .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
        xmlMapper.flatMap(FormatMapperKind::requiredBeanType)
                .ifPresent(type -> unremovableBeans.produce(UnremovableBeanBuildItem.beanClassNames(type)));
        JsonFormatterCustomizationCheck jsonFormatterCustomizationCheck = jsonFormatterCustomizationCheck(
                capabilities, jsonMapper);
        persistenceUnitDescriptors.produce(
                new PersistenceUnitDescriptorBuildItem(descriptor,
                        new RecordedConfig(
                                dataSourceName,
                                jdbcDataSource.map(JdbcDataSourceBuildItem::getDbKind),
                                supportedDatabaseKind.map(DatabaseKind.SupportedDatabaseKind::getMainName),
                                jdbcDataSource.flatMap(JdbcDataSourceBuildItem::getDbVersion),
                                persistenceUnitConfig.dialect().dialect(),
                                entityClassNames,
                                multiTenancyStrategy,
                                hibernateOrmConfig.database().ormCompatibilityVersion(),
                                hibernateOrmConfig.mapping().format().global(),
                                jsonFormatterCustomizationCheck,
                                persistenceUnitConfig.unsupportedProperties()),
                        persistenceUnitConfig.multitenantSchemaDatasource().orElse(null),
                        xmlMappings,
                        false,
                        isHibernateValidatorPresent(capabilities), jsonMapper, xmlMapper));
    }

    private static Optional<DatabaseKind.SupportedDatabaseKind> collectDialectConfig(String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems,
            Optional<JdbcDataSourceBuildItem> jdbcDataSource,
            MultiTenancyStrategy multiTenancyStrategy,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<ReflectiveMethodBuildItem> reflectiveMethods,
            BiConsumer<String, String> puPropertiesCollector) {
        final HibernateOrmConfigPersistenceUnit.HibernateOrmConfigPersistenceUnitDialect dialectConfig = persistenceUnitConfig
                .dialect();

        Optional<String> dialect = dialectConfig.dialect();
        Optional<String> dbKind = jdbcDataSource.map(JdbcDataSourceBuildItem::getDbKind);
        Optional<String> explicitDbMinVersion = jdbcDataSource.flatMap(JdbcDataSourceBuildItem::getDbVersion);
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
                explicitDbMinVersion,
                dialectConfig,
                dbKindMetadataBuildItems,
                systemProperties,
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

    private static void collectDialectConfigForPersistenceXml(String persistenceUnitName,
            PersistenceUnitDescriptor puDescriptor) {
        Properties properties = puDescriptor.getProperties();
        String dialect = puDescriptor.getProperties().getProperty(AvailableSettings.DIALECT);
        // Legacy behavior: we used to do this through a custom DialectSelector,
        // but we might as well do it at build time.
        // TODO should we do this for other dialects as well?
        //   Similar (but different) issue: https://github.com/quarkusio/quarkus/issues/31588
        if (("H2".equals(dialect) || "org.hibernate.dialect.H2Dialect".equals(dialect))
                && !properties.containsKey(AvailableSettings.JAKARTA_HBM2DDL_DB_MAJOR_VERSION)
                && !properties.containsKey(AvailableSettings.JAKARTA_HBM2DDL_DB_MINOR_VERSION)
                && !properties.containsKey(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION)) {
            Logger.getLogger(HibernateOrmProcessor.class)
                    .infof("Persistence unit '%1$s': Enforcing Quarkus defaults for dialect 'org.hibernate.dialect.H2Dialect'"
                            + " by automatically setting '%2$s=%3$s'.",
                            persistenceUnitName, AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, DialectVersions.Defaults.H2);
            properties.setProperty(AvailableSettings.JAKARTA_HBM2DDL_DB_VERSION, DialectVersions.Defaults.H2);
        }
    }

    @SuppressWarnings("deprecation")
    private void enhanceEntities(final JpaModelBuildItem jpaModel,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : jpaModel.getManagedClassNames()) {

            transformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(i)
                    .setVisitorFunction(hibernateEntityEnhancer)
                    .setCacheable(true).build());
        }
        Set<String> additionalClassNames = new HashSet<>();
        for (AdditionalJpaModelBuildItem additionalJpaModel : additionalJpaModelBuildItems) {
            additionalClassNames.add(additionalJpaModel.getClassName());
        }
        for (io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem additionalJpaModel : deprecatedAdditionalJpaModelBuildItems) {
            additionalClassNames.add(additionalJpaModel.getClassName());
        }
        for (String className : additionalClassNames) {
            try {
                byte[] bytes = IoUtil.readClassAsBytes(HibernateOrmProcessor.class.getClassLoader(), className);
                byte[] enhanced = hibernateEntityEnhancer.enhance(className, bytes);
                additionalClasses.produce(new GeneratedClassBuildItem(false, className, enhanced != null ? enhanced : bytes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Model class", e);
            }
        }
    }

    public static Map<String, JpaPersistenceUnitModel> getModelPerPersistenceUnit(HibernateOrmConfig hibernateOrmConfig,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems, JpaModelBuildItem jpaModel,
            IndexView index, boolean enableDefaultPersistenceUnit) {
        Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit = new HashMap<>();

        boolean hasPackagesInQuarkusConfig = hasPackagesInQuarkusConfig(hibernateOrmConfig);
        Collection<AnnotationInstance> packageLevelPersistenceUnitAnnotations = getPackageLevelPersistenceUnitAnnotations(
                index);

        Map<String, Set<String>> packageRules = new HashMap<>();

        if (hasPackagesInQuarkusConfig) {
            // Config based packages have priorities over annotations.
            // As long as there is one defined, annotations are ignored.
            if (!packageLevelPersistenceUnitAnnotations.isEmpty()) {
                LOG.warn(
                        "Mixing Quarkus configuration and @PersistenceUnit annotations to define the persistence units is not supported. Ignoring the annotations.");
            }

            // handle the default persistence unit
            if (enableDefaultPersistenceUnit) {
                if (!hibernateOrmConfig.defaultPersistenceUnit().packages().isPresent()) {
                    throw new ConfigurationException("Packages must be configured for the default persistence unit.");
                }

                for (String packageName : hibernateOrmConfig.defaultPersistenceUnit().packages().get()) {
                    packageRules.computeIfAbsent(normalizePackage(packageName), p -> new HashSet<>())
                            .add(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
                }
            }

            // handle the named persistence units
            for (Entry<String, HibernateOrmConfigPersistenceUnit> candidatePersistenceUnitEntry : hibernateOrmConfig
                    .namedPersistenceUnits()
                    .entrySet()) {
                String candidatePersistenceUnitName = candidatePersistenceUnitEntry.getKey();

                Set<String> candidatePersistenceUnitPackages = candidatePersistenceUnitEntry.getValue().packages()
                        .orElseThrow(() -> new ConfigurationException(String.format(Locale.ROOT,
                                "Packages must be configured for persistence unit '%s'.", candidatePersistenceUnitName)));

                for (String packageName : candidatePersistenceUnitPackages) {
                    packageRules.computeIfAbsent(normalizePackage(packageName), p -> new HashSet<>())
                            .add(candidatePersistenceUnitName);
                }
            }
        } else if (!packageLevelPersistenceUnitAnnotations.isEmpty()) {
            for (AnnotationInstance packageLevelPersistenceUnitAnnotation : packageLevelPersistenceUnitAnnotations) {
                String className = packageLevelPersistenceUnitAnnotation.target().asClass().name().toString();
                String packageName;
                if (className == null || className.isEmpty() || className.indexOf('.') == -1) {
                    packageName = "";
                } else {
                    packageName = normalizePackage(className.substring(0, className.lastIndexOf('.')));
                }

                String persistenceUnitName = packageLevelPersistenceUnitAnnotation.value().asString();
                if (persistenceUnitName != null && !persistenceUnitName.isEmpty()) {
                    packageRules.computeIfAbsent(packageName, p -> new HashSet<>())
                            .add(persistenceUnitName);
                }
            }
        } else if (!hibernateOrmConfig.namedPersistenceUnits().isEmpty()) {
            throw new ConfigurationException(
                    "Multiple persistence units are defined but the entities are not mapped to them. You should either use the .packages Quarkus configuration property or package-level @PersistenceUnit annotations.");
        } else {
            // No .packages configuration, no package-level persistence unit annotations,
            // and no named persistence units: all the entities will be associated with the default one
            // so we don't need to split them
            var model = new JpaPersistenceUnitModel();
            model.entityClassNames().addAll(jpaModel.getEntityClassNames());
            model.allModelClassAndPackageNames().addAll(jpaModel.getAllModelClassNames());
            model.allModelClassAndPackageNames().addAll(jpaModel.getAllModelPackageNames());
            return Map.of(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, model);
        }

        Set<String> modelClassesWithPersistenceUnitAnnotations = new TreeSet<>();

        for (String modelClassName : jpaModel.getAllModelClassNames()) {
            ClassInfo modelClassInfo = index.getClassByName(DotName.createSimple(modelClassName));
            Set<String> relatedModelClassNames = getRelatedModelClassNames(index, jpaModel.getAllModelClassNames(),
                    modelClassInfo);

            if (modelClassInfo != null && (modelClassInfo.declaredAnnotation(ClassNames.QUARKUS_PERSISTENCE_UNIT) != null
                    || modelClassInfo.declaredAnnotation(ClassNames.QUARKUS_PERSISTENCE_UNIT_REPEATABLE_CONTAINER) != null)) {
                modelClassesWithPersistenceUnitAnnotations.add(modelClassInfo.name().toString());
            }

            for (Entry<String, Set<String>> packageRuleEntry : packageRules.entrySet()) {
                if (modelClassName.startsWith(packageRuleEntry.getKey())) {
                    for (String persistenceUnitName : packageRuleEntry.getValue()) {
                        var model = modelPerPersistenceUnit.computeIfAbsent(persistenceUnitName,
                                ignored -> new JpaPersistenceUnitModel());

                        if (jpaModel.getEntityClassNames().contains(modelClassName)) {
                            model.entityClassNames().add(modelClassName);
                        }
                        model.allModelClassAndPackageNames().add(modelClassName);

                        // also add the hierarchy to the persistence unit
                        // we would need to add all the underlying model to it but adding the hierarchy
                        // is necessary for Panache as we need to add PanacheEntity to the PU
                        model.allModelClassAndPackageNames().addAll(relatedModelClassNames);
                    }
                }
            }
        }

        Set<String> assignedModelClasses = new HashSet<>();
        for (AdditionalJpaModelBuildItem additionalJpaModel : additionalJpaModelBuildItems) {
            var className = additionalJpaModel.getClassName();
            var persistenceUnits = additionalJpaModel.getPersistenceUnits();
            if (persistenceUnits == null) {
                // Legacy behavior -- remove when the deprecated one-argument constructor of AdditionalJpaModelBuildItem gets removed.
                continue;
            }
            assignedModelClasses.add(className); // Even if persistenceUnits is empty, the class is still assigned (to nothing)
            for (String persistenceUnitName : persistenceUnits) {
                var model = modelPerPersistenceUnit.computeIfAbsent(persistenceUnitName,
                        ignored -> new JpaPersistenceUnitModel());

                if (jpaModel.getEntityClassNames().contains(className)) {
                    model.entityClassNames().add(className);
                }
                model.allModelClassAndPackageNames().add(className);
            }
        }

        if (!modelClassesWithPersistenceUnitAnnotations.isEmpty()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "@PersistenceUnit annotations are not supported at the class level on model classes:\n\t- %s\nUse the `.packages` configuration property or package-level annotations instead.",
                    String.join("\n\t- ", modelClassesWithPersistenceUnitAnnotations)));
        }

        assignedModelClasses.addAll(modelPerPersistenceUnit.values().stream()
                .map(JpaPersistenceUnitModel::allModelClassAndPackageNames).flatMap(Set::stream).toList());
        Set<String> unaffectedModelClasses = jpaModel.getAllModelClassNames().stream()
                .filter(c -> !assignedModelClasses.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unaffectedModelClasses.isEmpty()) {
            LOG.warnf("Could not find a suitable persistence unit for model classes:\n\t- %s",
                    String.join("\n\t- ", unaffectedModelClasses));
        }

        for (String modelPackageName : jpaModel.getAllModelPackageNames()) {
            // Package rules keys are "normalized" package names, so we want to normalize the package on lookup:
            Set<String> persistenceUnitNames = packageRules.get(normalizePackage(modelPackageName));
            if (persistenceUnitNames == null) {
                continue;
            }
            for (String persistenceUnitName : persistenceUnitNames) {
                var model = modelPerPersistenceUnit.computeIfAbsent(persistenceUnitName,
                        ignored -> new JpaPersistenceUnitModel());
                model.allModelClassAndPackageNames().add(modelPackageName);
            }
        }

        return modelPerPersistenceUnit;
    }

    private static Set<String> getRelatedModelClassNames(IndexView index, Set<String> knownModelClassNames,
            ClassInfo modelClassInfo) {
        if (modelClassInfo == null) {
            return Collections.emptySet();
        }

        Set<String> relatedModelClassNames = new HashSet<>();

        // for now we only deal with entities and mapped super classes
        if (modelClassInfo.declaredAnnotation(ClassNames.JPA_ENTITY) == null &&
                modelClassInfo.declaredAnnotation(ClassNames.MAPPED_SUPERCLASS) == null) {
            return Collections.emptySet();
        }

        addRelatedModelClassNamesRecursively(index, knownModelClassNames, relatedModelClassNames, modelClassInfo);

        return relatedModelClassNames;
    }

    private static void addRelatedModelClassNamesRecursively(IndexView index, Set<String> knownModelClassNames,
            Set<String> relatedModelClassNames, ClassInfo modelClassInfo) {
        if (modelClassInfo == null || modelClassInfo.name().equals(DotNames.OBJECT)) {
            return;
        }

        String modelClassName = modelClassInfo.name().toString();
        if (knownModelClassNames.contains(modelClassName)) {
            relatedModelClassNames.add(modelClassName);
        }

        addRelatedModelClassNamesRecursively(index, knownModelClassNames, relatedModelClassNames,
                index.getClassByName(modelClassInfo.superName()));

        for (DotName interfaceName : modelClassInfo.interfaceNames()) {
            addRelatedModelClassNamesRecursively(index, knownModelClassNames, relatedModelClassNames,
                    index.getClassByName(interfaceName));
        }
    }

    private static String normalizePackage(String pakkage) {
        if (pakkage.endsWith(".")) {
            return pakkage;
        }
        return pakkage + ".";
    }

    private static boolean hasPackagesInQuarkusConfig(HibernateOrmConfig hibernateOrmConfig) {
        for (HibernateOrmConfigPersistenceUnit persistenceUnitConfig : hibernateOrmConfig.persistenceUnits()
                .values()) {
            if (persistenceUnitConfig.packages().isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static Collection<AnnotationInstance> getPackageLevelPersistenceUnitAnnotations(IndexView index) {
        Collection<AnnotationInstance> persistenceUnitAnnotations = index
                .getAnnotationsWithRepeatable(ClassNames.QUARKUS_PERSISTENCE_UNIT, index);
        Collection<AnnotationInstance> packageLevelPersistenceUnitAnnotations = new ArrayList<>();

        for (AnnotationInstance persistenceUnitAnnotation : persistenceUnitAnnotations) {
            if (persistenceUnitAnnotation.target().kind() != Kind.CLASS) {
                continue;
            }

            if (!"package-info".equals(persistenceUnitAnnotation.target().asClass().simpleName())) {
                continue;
            }
            packageLevelPersistenceUnitAnnotations.add(persistenceUnitAnnotation);
        }

        return packageLevelPersistenceUnitAnnotations;
    }

    /**
     * Checks whether we should ignore {@code persistence.xml} files.
     * <p>
     * The main way to ignore {@code persistence.xml} files is to set the configuration property
     * {@code quarkus.hibernate-orm.persistence-xml.ignore}.
     * <p>
     * But there is also an undocumented feature: we allow setting the System property
     * "SKIP_PARSE_PERSISTENCE_XML" to ignore any {@code persistence.xml} resource.
     *
     * @return true if we're expected to ignore them
     */
    private boolean shouldIgnorePersistenceXmlResources(HibernateOrmConfig config) {
        return config.persistenceXml().ignore() || Boolean.getBoolean("SKIP_PARSE_PERSISTENCE_XML");
    }

    /**
     * Set up the scanner, as this scanning has already been done we need to just tell it about the classes we
     * have discovered. This scanner is bytecode serializable and is passed directly into the recorder
     *
     * @param jpaModel the previously discovered JPA model (domain objects, ...)
     * @return a new QuarkusScanner with all domainObjects registered
     */
    public static QuarkusScanner buildQuarkusScanner(JpaModelBuildItem jpaModel) {
        QuarkusScanner scanner = new QuarkusScanner();
        Set<PackageDescriptor> packageDescriptors = new HashSet<>();
        for (String packageName : jpaModel.getAllModelPackageNames()) {
            QuarkusScanner.PackageDescriptorImpl desc = new QuarkusScanner.PackageDescriptorImpl(packageName);
            packageDescriptors.add(desc);
        }
        scanner.setPackageDescriptors(packageDescriptors);
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String className : jpaModel.getEntityClassNames()) {
            QuarkusScanner.ClassDescriptorImpl desc = new QuarkusScanner.ClassDescriptorImpl(className,
                    ClassDescriptor.Categorization.MODEL);
            classDescriptors.add(desc);
        }
        scanner.setClassDescriptors(classDescriptors);
        return scanner;
    }

    private static MultiTenancyStrategy getMultiTenancyStrategy(Optional<String> multitenancyStrategy) {
        final MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy
                .valueOf(multitenancyStrategy.orElse(MultiTenancyStrategy.NONE.name())
                        .toUpperCase(Locale.ROOT));
        return multiTenancyStrategy;
    }

    private PreGeneratedProxies generateProxies(Set<String> managedClassAndPackageNames, IndexView combinedIndex,
            TransformedClassesBuildItem transformedClassesBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem,
            ExecutorService buildExecutor) throws ExecutionException, InterruptedException {
        ProxyCache proxyCache = liveReloadBuildItem.getContextObject(ProxyCache.class);
        if (proxyCache == null) {
            proxyCache = new ProxyCache();
            liveReloadBuildItem.setContextObject(ProxyCache.class, proxyCache);
        }
        Set<String> changedClasses = Set.of();
        if (liveReloadBuildItem.getChangeInformation() != null) {
            changedClasses = liveReloadBuildItem.getChangeInformation().getChangedClasses();
        } else {
            //we don't have class change info, invalidate the cache
            proxyCache.cache.clear();
        }
        //create a map of entity to proxy type
        TypePool transformedClassesTypePool = createTransformedClassesTypePool(transformedClassesBuildItem,
                managedClassAndPackageNames);

        PreGeneratedProxies preGeneratedProxies = new PreGeneratedProxies();

        try (ProxyBuildingHelper proxyHelper = new ProxyBuildingHelper(transformedClassesTypePool)) {
            final ConcurrentLinkedDeque<Future<CachedProxy>> generatedProxyQueue = new ConcurrentLinkedDeque<>();
            Set<String> proxyInterfaceNames = Set.of(ClassNames.HIBERNATE_PROXY.toString());

            for (String managedClassOrPackageName : managedClassAndPackageNames) {
                if (proxyCache.cache.containsKey(managedClassOrPackageName)
                        && !isModified(managedClassOrPackageName, changedClasses, combinedIndex)) {
                    CachedProxy proxy = proxyCache.cache.get(managedClassOrPackageName);
                    generatedProxyQueue.add(CompletableFuture.completedFuture(proxy));
                } else {
                    if (!proxyHelper.isProxiable(combinedIndex.getClassByName(managedClassOrPackageName))) {
                        // we need to make sure we have a class and not a package and that it is proxiable
                        continue;
                    }
                    // we now are sure we have a proper class and not a package, let's avoid the confusion
                    String managedClass = managedClassOrPackageName;
                    generatedProxyQueue.add(buildExecutor.submit(() -> {
                        DynamicType.Unloaded<?> unloaded = proxyHelper.buildUnloadedProxy(managedClass);
                        return new CachedProxy(managedClass, unloaded);
                    }));
                }
            }

            for (Future<CachedProxy> proxyFuture : generatedProxyQueue) {
                CachedProxy proxy = proxyFuture.get();
                proxyCache.cache.put(proxy.managedClassName, proxy);
                for (Entry<TypeDescription, byte[]> i : proxy.proxyDef.getAllTypes().entrySet()) {
                    generatedClassBuildItemBuildProducer
                            .produce(new GeneratedClassBuildItem(true, i.getKey().getName(), i.getValue()));
                }
                preGeneratedProxies.getProxies().put(proxy.managedClassName, new PreGeneratedProxies.ProxyClassDetailsHolder(
                        proxy.proxyDef.getTypeDescription().getName()));
            }
        }

        return preGeneratedProxies;
    }

    // Creates a TypePool that is aware of class transformations applied to entity classes,
    // so that ByteBuddy can take these transformations into account.
    // This is especially important when getters/setters are added to entity classes,
    // because we want those methods to be overridden in proxies to trigger proxy initialization.
    private TypePool createTransformedClassesTypePool(TransformedClassesBuildItem transformedClassesBuildItem,
            Set<String> entityClasses) {
        Map<String, byte[]> transformedClasses = new HashMap<>();
        for (Set<TransformedClassesBuildItem.TransformedClass> transformedClassSet : transformedClassesBuildItem
                .getTransformedClassesByJar().values()) {
            for (TransformedClassesBuildItem.TransformedClass transformedClass : transformedClassSet) {
                String className = transformedClass.getClassName();
                if (entityClasses.contains(className)) {
                    transformedClasses.put(className, transformedClass.getData());
                }
            }
        }

        ClassFileLocator classFileLocator = new ClassFileLocator.Compound(
                new ClassFileLocator.Simple(transformedClasses),
                QuarkusClassFileLocator.INSTANCE);

        // we can reuse the core TypePool but we may not reuse the full enhancer TypePool
        // or PublicFieldWithProxyAndLazyLoadingAndInheritanceTest will fail
        return new TypePool.Default(new CacheProvider.Simple(), classFileLocator, ReaderMode.FAST,
                HibernateEntityEnhancer.CORE_TYPE_POOL);
    }

    private boolean isModified(String entity, Set<String> changedClasses, IndexView index) {
        if (changedClasses.contains(entity)) {
            return true;
        }
        ClassInfo clazz = index.getClassByName(DotName.createSimple(entity));
        if (clazz == null) {
            //if it is not in the index, then it has not been modified
            return false;
        }
        for (DotName i : clazz.interfaceNames()) {
            if (isModified(i.toString(), changedClasses, index)) {
                return true;
            }
        }
        DotName superName = clazz.superName();
        if (superName != null && !DotName.OBJECT_NAME.equals(superName)) {
            return isModified(superName.toString(), changedClasses, index);
        }
        return false;
    }

    private static final class ProxyCache {

        Map<String, CachedProxy> cache = new HashMap<>();
    }

    static final class CachedProxy {
        final String managedClassName;
        final DynamicType.Unloaded<?> proxyDef;

        CachedProxy(String managedClassName, DynamicType.Unloaded<?> proxyDef) {
            this.managedClassName = managedClassName;
            this.proxyDef = proxyDef;
        }
    }
}
