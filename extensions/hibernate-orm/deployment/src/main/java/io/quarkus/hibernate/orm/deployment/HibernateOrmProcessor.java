package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static io.quarkus.hibernate.orm.deployment.HibernateConfigUtil.firstPresent;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.OptionalInt;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.persistence.AttributeConverter;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.boot.archive.scan.spi.PackageDescriptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.id.SequenceMismatchStrategy;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.boot.internal.EntityManagerFactoryBuilderImpl;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.loader.BatchFetchStyle;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logging.Logger;
import org.jboss.logmanager.Level;

import io.quarkus.agroal.spi.JdbcDataSourceBuildItem;
import io.quarkus.agroal.spi.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.agroal.spi.JdbcInitialSQLGeneratorBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
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
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.IsNormal;
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
import io.quarkus.deployment.builditem.DevServicesAdditionalConfigBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.TransformedClassesBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.dev.console.DevConsoleManager;
import io.quarkus.devconsole.spi.DevConsoleRuntimeTemplateInfoBuildItem;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationStaticConfiguredBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.DatabaseKindDialectBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.scan.QuarkusScanner;
import io.quarkus.hibernate.orm.runtime.boot.xml.JAXBElementSubstitution;
import io.quarkus.hibernate.orm.runtime.boot.xml.QNameSubstitution;
import io.quarkus.hibernate.orm.runtime.boot.xml.RecordableXmlMapping;
import io.quarkus.hibernate.orm.runtime.cdi.QuarkusArcBeanContainer;
import io.quarkus.hibernate.orm.runtime.devconsole.HibernateOrmDevConsoleCreateDDLSupplier;
import io.quarkus.hibernate.orm.runtime.devconsole.HibernateOrmDevConsoleIntegrator;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticDescriptor;
import io.quarkus.hibernate.orm.runtime.migration.MultiTenancyStrategy;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.schema.SchemaManagementIntegrator;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.panache.common.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.panache.common.deployment.HibernateModelClassCandidatesForFieldAccessBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigUtils;
import io.quarkus.runtime.configuration.ConfigurationException;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.pool.TypePool;

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
    public static final String NO_SQL_LOAD_SCRIPT_FILE = "no-file";

    private static final Logger LOG = Logger.getLogger(HibernateOrmProcessor.class);

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    @BuildStep
    void registerHibernateOrmMetadataForCoreDialects(
            BuildProducer<DatabaseKindDialectBuildItem> producer) {
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.DB2,
                "org.hibernate.dialect.DB297Dialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.DERBY,
                "org.hibernate.dialect.DerbyTenSevenDialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.H2,
                "io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.MARIADB,
                "org.hibernate.dialect.MariaDB106Dialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.MSSQL,
                "org.hibernate.dialect.SQLServer2016Dialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.MYSQL,
                "org.hibernate.dialect.MySQL8Dialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.ORACLE,
                "org.hibernate.dialect.Oracle12cDialect"));
        producer.produce(new DatabaseKindDialectBuildItem(DatabaseKind.POSTGRESQL,
                "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect"));
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
        for (HibernateOrmConfigPersistenceUnit persistenceUnit : hibernateOrmConfig.getAllPersistenceUnitConfigsAsMap()
                .values()) {
            if (persistenceUnit.packages.isPresent()) {
                for (String pakkage : persistenceUnit.packages.get()) {
                    additionalApplicationArchiveMarkers
                            .produce(new AdditionalApplicationArchiveMarkerBuildItem(pakkage.replace('.', '/')));
                }
            }
        }
    }

    @BuildStep(onlyIf = IsDevelopment.class)
    void handleMoveSql(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfoProducer,
            BuildProducer<JdbcInitialSQLGeneratorBuildItem> initialSQLGeneratorBuildItemBuildProducer,
            HibernateOrmConfig config, CurateOutcomeBuildItem curateOutcomeBuildItem) {

        DevConsoleRuntimeTemplateInfoBuildItem devConsoleRuntimeTemplateInfoBuildItem = new DevConsoleRuntimeTemplateInfoBuildItem(
                "create-ddl." + PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                new HibernateOrmDevConsoleCreateDDLSupplier(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME), this.getClass(),
                curateOutcomeBuildItem);
        runtimeInfoProducer.produce(devConsoleRuntimeTemplateInfoBuildItem);
        for (Entry<String, HibernateOrmConfigPersistenceUnit> entry : config.getAllPersistenceUnitConfigsAsMap().entrySet()) {
            handleGenerateSqlForPu(runtimeInfoProducer, initialSQLGeneratorBuildItemBuildProducer, entry.getKey(),
                    entry.getValue().datasource.orElse(DataSourceUtil.DEFAULT_DATASOURCE_NAME), curateOutcomeBuildItem);
        }
    }

    private void handleGenerateSqlForPu(BuildProducer<DevConsoleRuntimeTemplateInfoBuildItem> runtimeInfoProducer,
            BuildProducer<JdbcInitialSQLGeneratorBuildItem> initialSQLGeneratorBuildItemBuildProducer, String puName,
            String dsName, CurateOutcomeBuildItem curateOutcomeBuildItem) {
        DevConsoleRuntimeTemplateInfoBuildItem devConsoleRuntimeTemplateInfoBuildItem = new DevConsoleRuntimeTemplateInfoBuildItem(
                "create-ddl." + puName, new HibernateOrmDevConsoleCreateDDLSupplier(puName), this.getClass(),
                curateOutcomeBuildItem);
        runtimeInfoProducer.produce(devConsoleRuntimeTemplateInfoBuildItem);
        initialSQLGeneratorBuildItemBuildProducer.produce(new JdbcInitialSQLGeneratorBuildItem(dsName, new Supplier<String>() {
            @Override
            public String get() {
                return DevConsoleManager.getTemplateInfo()
                        .get(devConsoleRuntimeTemplateInfoBuildItem.getGroupId() + "."
                                + devConsoleRuntimeTemplateInfoBuildItem.getArtifactId())
                        .get(devConsoleRuntimeTemplateInfoBuildItem.getName()).toString();
            }
        }));
    }

    @Record(RUNTIME_INIT)
    @Consume(ServiceStartBuildItem.class)
    @BuildStep(onlyIf = IsDevelopment.class)
    void warnOfSchemaProblems(HibernateOrmConfig config, HibernateOrmRecorder recorder) {
        for (var e : config.getAllPersistenceUnitConfigsAsMap().entrySet()) {
            if (e.getValue().validateInDevMode) {
                recorder.doValidation(e.getKey());
            }
        }
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    void devServicesAutoGenerateByDefault(List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItems,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            HibernateOrmConfig config,
            BuildProducer<DevServicesAdditionalConfigBuildItem> devServicesAdditionalConfigProducer) {
        Set<String> managedSources = schemaReadyBuildItems.stream().map(JdbcDataSourceSchemaReadyBuildItem::getDatasourceNames)
                .collect(HashSet::new, Collection::addAll, Collection::addAll);

        for (Entry<String, HibernateOrmConfigPersistenceUnit> entry : config.getAllPersistenceUnitConfigsAsMap().entrySet()) {
            Optional<String> dataSourceName = entry.getValue().datasource;
            List<String> propertyKeysIndicatingDataSourceConfigured = DataSourceUtil
                    .dataSourcePropertyKeys(dataSourceName.orElse(null), "username");

            if (!managedSources.contains(dataSourceName.orElse(DataSourceUtil.DEFAULT_DATASOURCE_NAME))) {
                String databaseGenerationPropertyKey = HibernateOrmRuntimeConfig.puPropertyKey(entry.getKey(),
                        "database.generation");
                if (!ConfigUtils.isAnyPropertyPresent(propertyKeysIndicatingDataSourceConfigured)
                        && !ConfigUtils.isPropertyPresent(databaseGenerationPropertyKey)) {
                    devServicesAdditionalConfigProducer
                            .produce(new DevServicesAdditionalConfigBuildItem(devServicesConfig -> {
                                // Only force DB generation if the datasource is configured through dev services
                                if (propertyKeysIndicatingDataSourceConfigured.stream()
                                        .anyMatch(devServicesConfig::containsKey)) {
                                    String forcedValue = "drop-and-create";
                                    LOG.infof("Setting %s=%s to initialize Dev Services managed database",
                                            databaseGenerationPropertyKey, forcedValue);
                                    return Map.of(databaseGenerationPropertyKey, forcedValue);
                                } else {
                                    return Map.of();
                                }
                            }));
                }
            }
        }
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem addPersistenceUnitAnnotationToIndex() {
        return new AdditionalIndexedClassesBuildItem(ClassNames.QUARKUS_PERSISTENCE_UNIT.toString());
    }

    // We do our own enhancement during the compilation phase, so disable any
    // automatic entity enhancement by Hibernate ORM
    // This has to happen before Hibernate ORM classes are initialized: see
    // org.hibernate.cfg.Environment#BYTECODE_PROVIDER_INSTANCE
    @BuildStep
    public SystemPropertyBuildItem enforceDisableRuntimeEnhancer() {
        return new SystemPropertyBuildItem(AvailableSettings.BYTECODE_PROVIDER,
                org.hibernate.cfg.Environment.BYTECODE_PROVIDER_NAME_NONE);
    }

    @BuildStep
    public void enrollBeanValidationTypeSafeActivatorForReflection(Capabilities capabilities,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true,
                    "org.hibernate.cfg.beanvalidation.TypeSafeActivator"));
            reflectiveClasses.produce(new ReflectiveClassBuildItem(false, false, false,
                    BeanValidationIntegrator.BV_CHECK_CLASS));
        }
    }

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles(LaunchModeBuildItem launchMode) {
        List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>();
        if (shouldIgnorePersistenceXmlResources()) {
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem("META-INF/persistence.xml"));
        }
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem(INTEGRATOR_SERVICE_FILE));

        // SQL load scripts are handled when assembling the Quarkus-configured persistence units

        return watchedFiles;
    }

    //Integration point: allow other extensions to define additional PersistenceXmlDescriptorBuildItem
    @BuildStep
    public void parsePersistenceXmlDescriptors(
            BuildProducer<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptorBuildItemBuildProducer) {
        if (!shouldIgnorePersistenceXmlResources()) {
            List<ParsedPersistenceXmlDescriptor> explicitDescriptors = QuarkusPersistenceXmlParser.locatePersistenceUnits();
            for (ParsedPersistenceXmlDescriptor desc : explicitDescriptors) {
                persistenceXmlDescriptorBuildItemBuildProducer.produce(new PersistenceXmlDescriptorBuildItem(desc));
            }
        }
    }

    //Integration point: allow other extensions to watch for ImpliedBlockingPersistenceUnitTypeBuildItem
    @BuildStep
    public ImpliedBlockingPersistenceUnitTypeBuildItem defineTypeOfImpliedPU(
            List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem, //This is from Agroal SPI: safe to use even for Hibernate Reactive
            List<PersistenceXmlDescriptorBuildItem> actualXmlDescriptors,
            Capabilities capabilities) {

        //We won't generate an implied PU if there are explicitly configured PUs
        if (actualXmlDescriptors.isEmpty() == false) {
            //when we have any explicitly provided Persistence Unit, disable the automatically generated ones.
            return ImpliedBlockingPersistenceUnitTypeBuildItem.none();
        }

        // If we have some blocking datasources defined, we can have an implied PU
        if (jdbcDataSourcesBuildItem.size() == 0 && capabilities.isPresent(Capability.HIBERNATE_REACTIVE)) {
            // if we don't have any blocking datasources and Hibernate Reactive is present,
            // we don't want a blocking persistence unit
            return ImpliedBlockingPersistenceUnitTypeBuildItem.none();
        } else {
            // even if we don't have any JDBC datasource, we trigger the implied blocking persistence unit
            // to properly trigger error conditions and error messages to guide the user
            return ImpliedBlockingPersistenceUnitTypeBuildItem.generateImpliedPersistenceUnit();
        }
    }

    @BuildStep
    public void configurationDescriptorBuilding(
            HibernateOrmConfig hibernateOrmConfig,
            CombinedIndexBuildItem index,
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedPU,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaModelBuildItem jpaModel,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {

        if (!hasEntities(jpaModel)) {
            // we can bail out early as there are no entities
            LOG.warn("Hibernate ORM is disabled because no JPA entities were found");
            return;
        }

        // First produce the PUs having a persistence.xml: these are not reactive, as we don't allow using a persistence.xml for them.
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            ParsedPersistenceXmlDescriptor xmlDescriptor = persistenceXmlDescriptorBuildItem.getDescriptor();
            persistenceUnitDescriptors
                    .produce(new PersistenceUnitDescriptorBuildItem(xmlDescriptor,
                            xmlDescriptor.getName(),
                            Optional.of(DataSourceUtil.DEFAULT_DATASOURCE_NAME),
                            getMultiTenancyStrategy(Optional.ofNullable(persistenceXmlDescriptorBuildItem.getDescriptor()
                                    .getProperties().getProperty(AvailableSettings.MULTI_TENANT))),
                            null,
                            jpaModel.getXmlMappings(persistenceXmlDescriptorBuildItem.getDescriptor().getName()),
                            Collections.emptyMap(),
                            false,
                            true));
        }

        if (impliedPU.shouldGenerateImpliedBlockingPersistenceUnit()) {
            handleHibernateORMWithNoPersistenceXml(hibernateOrmConfig, index, persistenceXmlDescriptors,
                    jdbcDataSources, applicationArchivesBuildItem, launchMode.getLaunchMode(), jpaModel, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    dbKindMetadataBuildItems);
        }
    }

    @BuildStep
    public JpaModelIndexBuildItem jpaEntitiesIndexer(
            CombinedIndexBuildItem index,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems) {
        // build a composite index with additional jpa model classes
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        for (AdditionalJpaModelBuildItem jpaModel : additionalJpaModelBuildItems) {
            IndexingUtil.indexClass(jpaModel.getClassName(), indexer, index.getIndex(), additionalIndex,
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
            ParsedPersistenceXmlDescriptor descriptor = persistenceXmlDescriptor.getDescriptor();
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    descriptor.getName(), descriptor.getPersistenceUnitRootUrl(), descriptor.getManagedClassNames(),
                    descriptor.getMappingFileNames()));
        }
    }

    @BuildStep
    public void contributeQuarkusConfigToJpaModel(
            BuildProducer<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions,
            HibernateOrmConfig hibernateOrmConfig) {
        for (Entry<String, HibernateOrmConfigPersistenceUnit> entry : hibernateOrmConfig.getAllPersistenceUnitConfigsAsMap()
                .entrySet()) {
            String name = entry.getKey();
            HibernateOrmConfigPersistenceUnit config = entry.getValue();
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    name, null, Collections.emptySet(),
                    config.mappingFiles.orElse(Collections.emptySet())));
        }
    }

    @BuildStep
    public void defineJpaEntities(
            JpaModelIndexBuildItem indexBuildItem,
            BuildProducer<JpaModelBuildItem> domainObjectsProducer,
            List<IgnorableNonIndexedClasses> ignorableNonIndexedClassesBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions) {

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
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem) {
        Set<String> managedClassAndPackageNames = new HashSet<>(jpaModel.getEntityClassNames());
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            // Note: getManagedClassNames() can also return *package* names
            // See the source code of Hibernate ORM for proof:
            // org.hibernate.boot.archive.scan.internal.ScanResultCollector.isListedOrDetectable
            // is used for packages too, and it relies (indirectly) on getManagedClassNames().
            managedClassAndPackageNames.addAll(pud.getManagedClassNames());
        }
        PreGeneratedProxies proxyDefinitions = generatedProxies(managedClassAndPackageNames,
                indexBuildItem.getIndex(), transformedClassesBuildItem,
                generatedClassBuildItemBuildProducer, liveReloadBuildItem);

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
            for (DotName name : HibernateOrmAnnotations.JPA_MAPPING_ANNOTATIONS) {
                annotationClassNames.add(name.toString());
            }
            for (DotName name : HibernateOrmAnnotations.HIBERNATE_MAPPING_ANNOTATIONS) {
                annotationClassNames.add(name.toString());
            }
            reflective.produce(new ReflectiveClassBuildItem(true, true, true, annotationClassNames.toArray(new String[0])));
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
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            List<HibernateOrmIntegrationStaticConfiguredBuildItem> integrationBuildItems,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            LaunchModeBuildItem launchMode) throws Exception {
        validateHibernatePropertiesNotUsed();

        final boolean enableORM = hasEntities(jpaModel);
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
            integratorClasses.add(HibernateOrmDevConsoleIntegrator.class);
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
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaModelBuildItem jpaModel,
            LaunchModeBuildItem launchMode) {
        if (!hasEntities(jpaModel)) {
            return;
        }
        for (PersistenceUnitDescriptorBuildItem i : descriptors) {
            //add resources
            String resourceName = i.getExplicitSqlImportScriptResourceName();
            if (resourceName != null) {
                resources.produce(new NativeImageResourceBuildItem(resourceName));
            }
        }
    }

    @BuildStep
    void registerBeans(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans,
            Capabilities capabilities,
            CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaModelBuildItem jpaModel) {
        if (!hasEntities(jpaModel)) {
            return;
        }

        List<Class<?>> unremovableClasses = new ArrayList<>();
        unremovableClasses.add(JPAConfig.class);
        if (capabilities.isPresent(Capability.TRANSACTIONS)) {
            unremovableClasses.add(TransactionManager.class);
            unremovableClasses.add(TransactionSessions.class);
        }
        unremovableClasses.add(RequestScopedSessionHolder.class);
        unremovableClasses.add(QuarkusArcBeanContainer.class);

        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(unremovableClasses.toArray(new Class<?>[unremovableClasses.size()]))
                .build());

        // Some user-injectable beans are retrieved programmatically and shouldn't be removed
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(AttributeConverter.class));
        unremovableBeans.produce(UnremovableBeanBuildItem.beanTypes(jpaModel.getPotentialCdiBeanClassNames()));
    }

    @Consume(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    @BuildStep
    public HibernateEnhancersRegisteredBuildItem enhancerDomainObjects(JpaModelBuildItem jpaModel,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        // Modify the bytecode of all entities to enable lazy-loading, dirty checking, etc..
        enhanceEntities(jpaModel, transformers, additionalJpaModelBuildItems, additionalClasses);
        // this allows others to register their enhancers after Hibernate, so they run before ours
        return new HibernateEnhancersRegisteredBuildItem();
    }

    @BuildStep
    public HibernateModelClassCandidatesForFieldAccessBuildItem candidatesForFieldAccess(JpaModelBuildItem jpaModel) {
        // Ask Panache to replace direct access to public fields with calls to accessors for all model classes.
        return new HibernateModelClassCandidatesForFieldAccessBuildItem(jpaModel.getAllModelClassNames());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void build(HibernateOrmRecorder recorder, HibernateOrmConfig hibernateOrmConfig,
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            BuildProducer<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaModelBuildItem jpaModel) throws Exception {
        if (!hasEntities(jpaModel)) {
            return;
        }

        Map<String, Set<String>> entityPersistenceUnitMapping = new HashMap<>();
        for (PersistenceUnitDescriptorBuildItem descriptor : descriptors) {
            for (String entityClass : descriptor.getManagedClassNames()) {
                entityPersistenceUnitMapping.putIfAbsent(entityClass, new HashSet<>());
                entityPersistenceUnitMapping.get(entityClass).add(descriptor.getPersistenceUnitName());
            }
        }

        jpaModelPersistenceUnitMapping.produce(new JpaModelPersistenceUnitMappingBuildItem(entityPersistenceUnitMapping));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public PersistenceProviderSetUpBuildItem setupPersistenceProvider(HibernateOrmRecorder recorder,
            Capabilities capabilities, HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationBuildItems,
            BuildProducer<RecorderBeanInitializedBuildItem> orderEnforcer) {
        if (capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
            recorder.setupPersistenceProvider(hibernateOrmRuntimeConfig,
                    HibernateOrmIntegrationRuntimeConfiguredBuildItem.collectDescriptors(integrationBuildItems));
        }

        return new PersistenceProviderSetUpBuildItem();
    }

    @BuildStep
    @Consume(SyntheticBeansRuntimeInitBuildItem.class)
    @Record(RUNTIME_INIT)
    public ServiceStartBuildItem startPersistenceUnits(HibernateOrmRecorder recorder, BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> dataSourcesConfigured,
            JpaModelBuildItem jpaModel,
            List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem,
            List<PersistenceProviderSetUpBuildItem> persistenceProviderSetUp) throws Exception {
        if (hasEntities(jpaModel)) {
            recorder.startAllPersistenceUnits(beanContainer.getValue());
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
            if (persistenceUnitDescriptor.getMultiTenancyStrategy() == MultiTenancyStrategy.NONE) {
                continue;
            }

            multitenancyEnabled = true;

            ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem.configure(DataSourceTenantConnectionResolver.class)
                    .scope(ApplicationScoped.class)
                    .types(TenantConnectionResolver.class)
                    .setRuntimeInit()
                    .defaultBean()
                    .unremovable()
                    .supplier(recorder.dataSourceTenantConnectionResolver(persistenceUnitDescriptor.getPersistenceUnitName(),
                            persistenceUnitDescriptor.getDataSource(), persistenceUnitDescriptor.getMultiTenancyStrategy(),
                            persistenceUnitDescriptor.getMultiTenancySchemaDataSource()));

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

        if (multitenancyEnabled) {
            unremovableBeans
                    .produce(new UnremovableBeanBuildItem(new BeanTypeExclusion(ClassNames.TENANT_CONNECTION_RESOLVER)));
            unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanTypeExclusion(ClassNames.TENANT_RESOLVER)));
        }
    }

    @BuildStep
    public void produceLoggingCategories(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<LogCategoryBuildItem> categories) {
        if (hibernateOrmConfig.log.bindParam || hibernateOrmConfig.log.bindParameters) {
            categories.produce(new LogCategoryBuildItem("org.hibernate.type.descriptor.sql.BasicBinder", Level.TRACE, true));
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

            reflective.produce(new ReflectiveClassBuildItem(false, false, true, metamodel));
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
        HibernateOrmAnnotations.ANNOTATED_WITH_INJECT_SERVICE.stream()
                .map(DotName::toString)
                .forEach(classes::add);

        // Integrators relying on @InjectService.
        index.getIndex().getAnnotations(ClassNames.INJECT_SERVICE).stream()
                .map(a -> a.target().asMethod().declaringClass().name().toString())
                .forEach(classes::add);

        if (!classes.isEmpty()) {
            reflective.produce(new ReflectiveClassBuildItem(false, true, false, classes.toArray(new String[0])));
        }
    }

    private static List<String> getSqlLoadScript(Optional<List<String>> sqlLoadScript, LaunchMode launchMode) {
        // Explicit file or default Hibernate ORM file.
        if (sqlLoadScript.isPresent()) {
            return sqlLoadScript.get().stream()
                    .filter(s -> !NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(s))
                    .collect(Collectors.toList());
        } else if (launchMode == LaunchMode.NORMAL) {
            return Collections.emptyList();
        } else {
            return List.of("import.sql");
        }
    }

    private boolean hasEntities(JpaModelBuildItem jpaModel) {
        return !jpaModel.getEntityClassNames().isEmpty();
    }

    private void handleHibernateORMWithNoPersistenceXml(
            HibernateOrmConfig hibernateOrmConfig,
            CombinedIndexBuildItem index,
            List<PersistenceXmlDescriptorBuildItem> descriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            JpaModelBuildItem jpaModel,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {
        if (!descriptors.isEmpty()) {
            if (hibernateOrmConfig.isAnyPropertySet() || !hibernateOrmConfig.persistenceUnits.isEmpty()) {
                throw new ConfigurationException(
                        "Hibernate ORM configuration present in persistence.xml and Quarkus config file at the same time\n"
                                + "If you use persistence.xml remove all " + HIBERNATE_ORM_CONFIG_PREFIX
                                + "* properties from the Quarkus config file.");
            } else {
                return;
            }
        }

        Optional<JdbcDataSourceBuildItem> defaultJdbcDataSource = jdbcDataSources.stream()
                .filter(i -> i.isDefault())
                .findFirst();
        boolean enableDefaultPersistenceUnit = (defaultJdbcDataSource.isPresent()
                && hibernateOrmConfig.persistenceUnits.isEmpty())
                || hibernateOrmConfig.defaultPersistenceUnit.isAnyPropertySet();

        Map<String, Set<String>> modelClassesAndPackagesPerPersistencesUnits = getModelClassesAndPackagesPerPersistenceUnits(
                hibernateOrmConfig, jpaModel, index.getIndex(), enableDefaultPersistenceUnit);
        Set<String> modelClassesAndPackagesForDefaultPersistenceUnit = modelClassesAndPackagesPerPersistencesUnits
                .getOrDefault(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, Collections.emptySet());

        Set<String> storageEngineCollector = new HashSet<>();

        if (enableDefaultPersistenceUnit) {
            producePersistenceUnitDescriptorFromConfig(
                    hibernateOrmConfig, PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    hibernateOrmConfig.defaultPersistenceUnit,
                    modelClassesAndPackagesForDefaultPersistenceUnit,
                    jpaModel.getXmlMappings(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME),
                    jdbcDataSources, applicationArchivesBuildItem, launchMode, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    storageEngineCollector, dbKindMetadataBuildItems);
        } else if (!modelClassesAndPackagesForDefaultPersistenceUnit.isEmpty()
                && (!hibernateOrmConfig.defaultPersistenceUnit.datasource.isPresent()
                        || DataSourceUtil.isDefault(hibernateOrmConfig.defaultPersistenceUnit.datasource.get()))
                && !defaultJdbcDataSource.isPresent()) {
            throw new ConfigurationException(
                    "Model classes are defined for the default persistence unit, but no default datasource was found."
                            + " The default EntityManagerFactory will not be created."
                            + " To solve this, configure the default datasource."
                            + " Refer to https://quarkus.io/guides/datasource for guidance.",
                    new HashSet<>(Arrays.asList("quarkus.datasource.db-kind", "quarkus.datasource.username",
                            "quarkus.datasource.password", "quarkus.datasource.jdbc.url")));
        }

        for (Entry<String, HibernateOrmConfigPersistenceUnit> persistenceUnitEntry : hibernateOrmConfig.persistenceUnits
                .entrySet()) {
            producePersistenceUnitDescriptorFromConfig(
                    hibernateOrmConfig, persistenceUnitEntry.getKey(), persistenceUnitEntry.getValue(),
                    modelClassesAndPackagesPerPersistencesUnits.getOrDefault(persistenceUnitEntry.getKey(),
                            Collections.emptySet()),
                    jpaModel.getXmlMappings(persistenceUnitEntry.getKey()),
                    jdbcDataSources, applicationArchivesBuildItem, launchMode, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    storageEngineCollector, dbKindMetadataBuildItems);
        }

        if (storageEngineCollector.size() > 1) {
            throw new ConfigurationException(
                    "The dialect storage engine is a global configuration property: it must be consistent across all persistence units.");
        }
    }

    private static void producePersistenceUnitDescriptorFromConfig(
            HibernateOrmConfig hibernateOrmConfig,
            String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig,
            Set<String> modelClassesAndPackages,
            List<RecordableXmlMapping> xmlMappings,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            Set<String> storageEngineCollector,
            List<DatabaseKindDialectBuildItem> dbKindMetadataBuildItems) {
        Optional<JdbcDataSourceBuildItem> jdbcDataSource = findJdbcDataSource(persistenceUnitName, persistenceUnitConfig,
                jdbcDataSources);

        Optional<String> explicitDialect = persistenceUnitConfig.dialect.dialect;
        String dialect;
        MultiTenancyStrategy multiTenancyStrategy = getMultiTenancyStrategy(persistenceUnitConfig.multitenant);
        if (multiTenancyStrategy == MultiTenancyStrategy.DATABASE) {
            // The datasource is optional for the DATABASE multi-tenancy strategy,
            // since the datasource will be resolved separately for each tenant.
            if (explicitDialect.isPresent()) {
                dialect = explicitDialect.get();
            } else if (jdbcDataSource.isPresent()) {
                dialect = Dialects.guessDialect(persistenceUnitName, jdbcDataSource.get().getDbKind(),
                        dbKindMetadataBuildItems);
            } else {
                throw new ConfigurationException(String.format(Locale.ROOT,
                        "The Hibernate ORM extension could not infer the dialect for persistence unit '%s'."
                                + " When using database multi-tenancy, you must either configure a datasource for that persistence unit"
                                + " (refer to https://quarkus.io/guides/datasource for guidance),"
                                + " or set the dialect explicitly through property '"
                                + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "dialect") + "'.",
                        persistenceUnitName));
            }

        } else {
            if (!jdbcDataSource.isPresent()) {
                throw new ConfigurationException(String.format(Locale.ROOT,
                        "Datasource must be defined for persistence unit '%s'."
                                + " Refer to https://quarkus.io/guides/datasource for guidance.",
                        persistenceUnitName),
                        new HashSet<>(Arrays.asList("quarkus.datasource.db-kind", "quarkus.datasource.username",
                                "quarkus.datasource.password", "quarkus.datasource.jdbc.url")));
            }
            if (explicitDialect.isPresent()) {
                dialect = explicitDialect.get();
            } else {
                dialect = Dialects.guessDialect(persistenceUnitName, jdbcDataSource.get().getDbKind(),
                        dbKindMetadataBuildItems);
            }
        }

        // we found one
        ParsedPersistenceXmlDescriptor descriptor = new ParsedPersistenceXmlDescriptor(null); //todo URL
        descriptor.setName(persistenceUnitName);

        descriptor.setExcludeUnlistedClasses(true);
        if (modelClassesAndPackages.isEmpty()) {
            LOG.warnf("Could not find any entities affected to the persistence unit '%s'.", persistenceUnitName);
        } else {
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
            descriptor.addClasses(new ArrayList<>(modelClassesAndPackages));
        }

        descriptor.setTransactionType(PersistenceUnitTransactionType.JTA);
        descriptor.getProperties().setProperty(AvailableSettings.DIALECT, dialect);

        // The storage engine has to be set as a system property.
        if (persistenceUnitConfig.dialect.storageEngine.isPresent()) {
            systemProperties.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE,
                    persistenceUnitConfig.dialect.storageEngine.get()));
        }
        // Physical Naming Strategy
        persistenceUnitConfig.physicalNamingStrategy.ifPresent(
                namingStrategy -> descriptor.getProperties()
                        .setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, namingStrategy));

        // Implicit Naming Strategy
        persistenceUnitConfig.implicitNamingStrategy.ifPresent(
                namingStrategy -> descriptor.getProperties()
                        .setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, namingStrategy));

        // Metadata builder contributor
        persistenceUnitConfig.metadataBuilderContributor.ifPresent(
                className -> descriptor.getProperties()
                        .setProperty(EntityManagerFactoryBuilderImpl.METADATA_BUILDER_CONTRIBUTOR, className));

        //charset
        descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME,
                persistenceUnitConfig.database.charset.name());

        if (persistenceUnitConfig.database.globallyQuotedIdentifiers) {
            descriptor.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
        }

        // Query
        int batchSize = firstPresent(persistenceUnitConfig.fetch.batchSize, persistenceUnitConfig.batchFetchSize)
                .orElse(16);
        if (batchSize > 0) {
            descriptor.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE,
                    Integer.toString(batchSize));
            descriptor.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
        }

        if (persistenceUnitConfig.fetch.maxDepth.isPresent()) {
            setMaxFetchDepth(descriptor, persistenceUnitConfig.fetch.maxDepth);
        } else if (persistenceUnitConfig.maxFetchDepth.isPresent()) {
            setMaxFetchDepth(descriptor, persistenceUnitConfig.maxFetchDepth);
        }

        descriptor.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, Integer.toString(
                persistenceUnitConfig.query.queryPlanCacheMaxSize));

        descriptor.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                persistenceUnitConfig.query.defaultNullOrdering.name().toLowerCase(Locale.ROOT));

        // Disable sequence validations: they are reportedly slow, and people already get the same validation from normal schema validation
        descriptor.getProperties().put(AvailableSettings.SEQUENCE_INCREMENT_SIZE_MISMATCH_STRATEGY,
                SequenceMismatchStrategy.NONE);

        // JDBC
        persistenceUnitConfig.jdbc.timezone.ifPresent(
                timezone -> descriptor.getProperties().setProperty(AvailableSettings.JDBC_TIME_ZONE, timezone));

        persistenceUnitConfig.jdbc.statementFetchSize.ifPresent(
                fetchSize -> descriptor.getProperties().setProperty(AvailableSettings.STATEMENT_FETCH_SIZE,
                        String.valueOf(fetchSize)));

        persistenceUnitConfig.jdbc.statementBatchSize.ifPresent(
                fetchSize -> descriptor.getProperties().setProperty(AvailableSettings.STATEMENT_BATCH_SIZE,
                        String.valueOf(fetchSize)));

        // Statistics
        if (hibernateOrmConfig.metricsEnabled
                || (hibernateOrmConfig.statistics.isPresent() && hibernateOrmConfig.statistics.get())) {
            descriptor.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
            //When statistics are enabled, the default in Hibernate ORM is to also log them after each
            // session; turn that off by default as it's very noisy:
            descriptor.getProperties().setProperty(AvailableSettings.LOG_SESSION_METRICS,
                    String.valueOf(hibernateOrmConfig.logSessionMetrics.orElse(false)));
        }

        // sql-load-scripts
        List<String> importFiles = getSqlLoadScript(persistenceUnitConfig.sqlLoadScript, launchMode);

        if (!importFiles.isEmpty()) {
            for (String importFile : importFiles) {
                Path loadScriptPath;
                try {
                    loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile);
                } catch (RuntimeException e) {
                    throw new ConfigurationException(
                            "Unable to interpret path referenced in '"
                                    + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "sql-load-script") + "="
                                    + String.join(",", persistenceUnitConfig.sqlLoadScript.get())
                                    + "': " + e.getMessage());
                }

                if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                    // enlist resource if present
                    nativeImageResources.produce(new NativeImageResourceBuildItem(importFile));
                } else if (persistenceUnitConfig.sqlLoadScript.isPresent()) {
                    //raise exception if explicit file is not present (i.e. not the default)
                    throw new ConfigurationException(
                            "Unable to find file referenced in '"
                                    + HibernateOrmRuntimeConfig.puPropertyKey(persistenceUnitName, "sql-load-script") + "="
                                    + String.join(",", persistenceUnitConfig.sqlLoadScript.get())
                                    + "'. Remove property or add file to your path.");
                }
                // in dev mode we want to make sure that we watch for changes to file even if it doesn't currently exist
                // as a user could still add it after performing the initial configuration
                hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(importFile));
            }

            // only set the found import files if configured
            if (persistenceUnitConfig.sqlLoadScript.isPresent()) {
                descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, String.join(",", importFiles));
            }
        } else {
            //Disable implicit loading of the default import script (import.sql)
            descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, "");
        }

        // Caching
        if (persistenceUnitConfig.secondLevelCachingEnabled) {
            Properties p = descriptor.getProperties();
            //Only set these if the user isn't making an explicit choice:
            p.putIfAbsent(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
            p.putIfAbsent(USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
            p.putIfAbsent(USE_QUERY_CACHE, Boolean.TRUE);
            p.putIfAbsent(JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
            Map<String, String> cacheConfigEntries = HibernateConfigUtil.getCacheConfigEntries(persistenceUnitConfig);
            for (Entry<String, String> entry : cacheConfigEntries.entrySet()) {
                descriptor.getProperties().setProperty(entry.getKey(), entry.getValue());
            }
        } else {
            //Unless the global switch is explicitly set to off, in which case we disable all caching:
            Properties p = descriptor.getProperties();
            p.put(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.FALSE);
            p.put(USE_SECOND_LEVEL_CACHE, Boolean.FALSE);
            p.put(USE_QUERY_CACHE, Boolean.FALSE);
            p.put(JPA_SHARED_CACHE_MODE, SharedCacheMode.NONE);
        }

        // Hibernate Validator integration: we force the callback mode to have bootstrap errors reported rather than validation ignored
        // if there is any issue when bootstrapping Hibernate Validator.
        if (capabilities.isPresent(Capability.HIBERNATE_VALIDATOR)) {
            descriptor.getProperties().setProperty(AvailableSettings.JPA_VALIDATION_MODE, ValidationMode.CALLBACK.name());
        }

        // Collect the storage engines if MySQL or MariaDB
        if (isMySQLOrMariaDB(dialect) && persistenceUnitConfig.dialect.storageEngine.isPresent()) {
            storageEngineCollector.add(persistenceUnitConfig.dialect.storageEngine.get());
        }

        // Discriminator Column
        descriptor.getProperties().setProperty(AvailableSettings.IGNORE_EXPLICIT_DISCRIMINATOR_COLUMNS_FOR_JOINED_SUBCLASS,
                String.valueOf(persistenceUnitConfig.discriminator.ignoreExplicitForJoined));

        persistenceUnitDescriptors.produce(
                new PersistenceUnitDescriptorBuildItem(descriptor, descriptor.getName(),
                        jdbcDataSource.map(JdbcDataSourceBuildItem::getName),
                        multiTenancyStrategy,
                        persistenceUnitConfig.multitenantSchemaDatasource.orElse(null),
                        xmlMappings,
                        persistenceUnitConfig.unsupportedProperties,
                        false, false));
    }

    private static Optional<JdbcDataSourceBuildItem> findJdbcDataSource(String persistenceUnitName,
            HibernateOrmConfigPersistenceUnit persistenceUnitConfig, List<JdbcDataSourceBuildItem> jdbcDataSources) {
        if (persistenceUnitConfig.datasource.isPresent()) {
            return Optional.of(jdbcDataSources.stream()
                    .filter(i -> persistenceUnitConfig.datasource.get().equals(i.getName()))
                    .findFirst()
                    .orElseThrow(() -> new ConfigurationException(String.format(Locale.ROOT,
                            "The datasource '%1$s' is not configured but the persistence unit '%2$s' uses it."
                                    + " To solve this, configure datasource '%1$s'."
                                    + " Refer to https://quarkus.io/guides/datasource for guidance.",
                            persistenceUnitConfig.datasource.get(), persistenceUnitName))));
        } else if (PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
            return jdbcDataSources.stream()
                    .filter(i -> i.isDefault())
                    .findFirst();
        } else {
            // if it's not the default persistence unit, we mandate an explicit datasource to prevent common errors
            return Optional.empty();
        }
    }

    private static void setMaxFetchDepth(ParsedPersistenceXmlDescriptor descriptor, OptionalInt maxFetchDepth) {
        descriptor.getProperties().setProperty(AvailableSettings.MAX_FETCH_DEPTH, String.valueOf(maxFetchDepth.getAsInt()));
    }

    private void enhanceEntities(final JpaModelBuildItem jpaModel,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : jpaModel.getAllModelClassNames()) {
            transformers.produce(new BytecodeTransformerBuildItem(true, i, hibernateEntityEnhancer, true));
        }
        for (AdditionalJpaModelBuildItem additionalJpaModel : additionalJpaModelBuildItems) {
            String className = additionalJpaModel.getClassName();
            try {
                byte[] bytes = IoUtil.readClassAsBytes(HibernateOrmProcessor.class.getClassLoader(), className);
                byte[] enhanced = hibernateEntityEnhancer.enhance(className, bytes);
                additionalClasses.produce(new GeneratedClassBuildItem(false, className, enhanced != null ? enhanced : bytes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Model class", e);
            }
        }
    }

    public static Map<String, Set<String>> getModelClassesAndPackagesPerPersistenceUnits(HibernateOrmConfig hibernateOrmConfig,
            JpaModelBuildItem jpaModel, IndexView index, boolean enableDefaultPersistenceUnit) {
        Map<String, Set<String>> modelClassesAndPackagesPerPersistenceUnits = new HashMap<>();

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
                if (!hibernateOrmConfig.defaultPersistenceUnit.packages.isPresent()) {
                    throw new ConfigurationException("Packages must be configured for the default persistence unit.");
                }

                for (String packageName : hibernateOrmConfig.defaultPersistenceUnit.packages.get()) {
                    packageRules.computeIfAbsent(normalizePackage(packageName), p -> new HashSet<>())
                            .add(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
                }
            }

            // handle the named persistence units
            for (Entry<String, HibernateOrmConfigPersistenceUnit> candidatePersistenceUnitEntry : hibernateOrmConfig.persistenceUnits
                    .entrySet()) {
                String candidatePersistenceUnitName = candidatePersistenceUnitEntry.getKey();

                Set<String> candidatePersistenceUnitPackages = candidatePersistenceUnitEntry.getValue().packages
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
        } else if (!hibernateOrmConfig.persistenceUnits.isEmpty()) {
            throw new ConfigurationException(
                    "Multiple persistence units are defined but the entities are not mapped to them. You should either use the .packages Quarkus configuration property or package-level @PersistenceUnit annotations.");
        } else {
            // No .packages configuration, no package-level persistence unit annotations,
            // and no named persistence units: all the entities will be associated with the default one
            // so we don't need to split them
            Set<String> allModelClassesAndPackages = new HashSet<>(jpaModel.getAllModelClassNames());
            allModelClassesAndPackages.addAll(jpaModel.getAllModelPackageNames());
            return Collections.singletonMap(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, allModelClassesAndPackages);
        }

        Set<String> modelClassesWithPersistenceUnitAnnotations = new TreeSet<>();

        for (String modelClassName : jpaModel.getAllModelClassNames()) {
            ClassInfo modelClassInfo = index.getClassByName(DotName.createSimple(modelClassName));
            Set<String> relatedModelClassNames = getRelatedModelClassNames(index, jpaModel.getAllModelClassNames(),
                    modelClassInfo);

            if (modelClassInfo != null && (modelClassInfo.classAnnotation(ClassNames.QUARKUS_PERSISTENCE_UNIT) != null
                    || modelClassInfo.classAnnotation(ClassNames.QUARKUS_PERSISTENCE_UNIT_REPEATABLE_CONTAINER) != null)) {
                modelClassesWithPersistenceUnitAnnotations.add(modelClassInfo.name().toString());
            }

            for (Entry<String, Set<String>> packageRuleEntry : packageRules.entrySet()) {
                if (modelClassName.startsWith(packageRuleEntry.getKey())) {
                    for (String persistenceUnitName : packageRuleEntry.getValue()) {
                        modelClassesAndPackagesPerPersistenceUnits.putIfAbsent(persistenceUnitName, new HashSet<>());
                        modelClassesAndPackagesPerPersistenceUnits.get(persistenceUnitName).add(modelClassName);

                        // also add the hierarchy to the persistence unit
                        // we would need to add all the underlying model to it but adding the hierarchy
                        // is necessary for Panache as we need to add PanacheEntity to the PU
                        for (String relatedModelClassName : relatedModelClassNames) {
                            modelClassesAndPackagesPerPersistenceUnits.get(persistenceUnitName).add(relatedModelClassName);
                        }
                    }
                }
            }
        }

        if (!modelClassesWithPersistenceUnitAnnotations.isEmpty()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "@PersistenceUnit annotations are not supported at the class level on model classes:\n\t- %s\nUse the `.packages` configuration property or package-level annotations instead.",
                    String.join("\n\t- ", modelClassesWithPersistenceUnitAnnotations)));
        }

        Set<String> affectedModelClasses = modelClassesAndPackagesPerPersistenceUnits.values().stream().flatMap(Set::stream)
                .collect(Collectors.toSet());
        Set<String> unaffectedModelClasses = jpaModel.getAllModelClassNames().stream()
                .filter(c -> !affectedModelClasses.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unaffectedModelClasses.isEmpty()) {
            LOG.warnf("Could not find a suitable persistence unit for model classes:\n\t- %s",
                    String.join("\n\t- ", unaffectedModelClasses));
        }

        for (String modelPackageName : jpaModel.getAllModelPackageNames()) {
            Set<String> persistenceUnitNames = packageRules.get(modelPackageName);
            if (persistenceUnitNames == null) {
                continue;
            }
            for (String persistenceUnitName : persistenceUnitNames) {
                modelClassesAndPackagesPerPersistenceUnits.putIfAbsent(persistenceUnitName, new HashSet<>());
                modelClassesAndPackagesPerPersistenceUnits.get(persistenceUnitName).add(modelPackageName);
            }
        }

        return modelClassesAndPackagesPerPersistenceUnits;
    }

    private static Set<String> getRelatedModelClassNames(IndexView index, Set<String> knownModelClassNames,
            ClassInfo modelClassInfo) {
        if (modelClassInfo == null) {
            return Collections.emptySet();
        }

        Set<String> relatedModelClassNames = new HashSet<>();

        // for now we only deal with entities and mapped super classes
        if (modelClassInfo.classAnnotation(ClassNames.JPA_ENTITY) == null &&
                modelClassInfo.classAnnotation(ClassNames.MAPPED_SUPERCLASS) == null) {
            return Collections.emptySet();
        }

        modelClassInfo = index.getClassByName(modelClassInfo.superName());

        while (modelClassInfo != null && !modelClassInfo.name().equals(DotNames.OBJECT)) {
            String modelSuperClassName = modelClassInfo.name().toString();
            if (knownModelClassNames.contains(modelSuperClassName)) {
                relatedModelClassNames.add(modelSuperClassName);
            }
            modelClassInfo = index.getClassByName(modelClassInfo.superName());
        }

        return relatedModelClassNames;
    }

    private static String normalizePackage(String pakkage) {
        if (pakkage.endsWith(".")) {
            return pakkage;
        }
        return pakkage + ".";
    }

    private static boolean hasPackagesInQuarkusConfig(HibernateOrmConfig hibernateOrmConfig) {
        for (HibernateOrmConfigPersistenceUnit persistenceUnitConfig : hibernateOrmConfig.getAllPersistenceUnitConfigsAsMap()
                .values()) {
            if (persistenceUnitConfig.packages.isPresent()) {
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
     * Undocumented feature: we allow setting the System property
     * "SKIP_PARSE_PERSISTENCE_XML" to fully ignore any persistence.xml
     * resource.
     *
     * @return true if we're expected to ignore them
     */
    private boolean shouldIgnorePersistenceXmlResources() {
        return Boolean.getBoolean("SKIP_PARSE_PERSISTENCE_XML");
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
        if (multiTenancyStrategy == MultiTenancyStrategy.DISCRIMINATOR) {
            // See https://hibernate.atlassian.net/browse/HHH-6054
            throw new ConfigurationException("The Hibernate ORM multitenancy strategy "
                    + MultiTenancyStrategy.DISCRIMINATOR + " is currently not supported");
        }
        return multiTenancyStrategy;
    }

    private PreGeneratedProxies generatedProxies(Set<String> managedClassAndPackageNames, IndexView combinedIndex,
            TransformedClassesBuildItem transformedClassesBuildItem,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem) {
        ProxyCache proxyCache = liveReloadBuildItem.getContextObject(ProxyCache.class);
        if (proxyCache == null) {
            proxyCache = new ProxyCache();
            liveReloadBuildItem.setContextObject(ProxyCache.class, proxyCache);
        }
        Set<String> changedClasses = Collections.emptySet();
        if (liveReloadBuildItem.getChangeInformation() != null) {
            changedClasses = liveReloadBuildItem.getChangeInformation().getChangedClasses();
        } else {
            //we don't have class change info, invalidate the cache
            proxyCache.cache.clear();
        }
        //create a map of entity to proxy type
        PreGeneratedProxies preGeneratedProxies = new PreGeneratedProxies();
        Map<String, String> proxyAnnotations = new HashMap<>();
        for (AnnotationInstance i : combinedIndex.getAnnotations(ClassNames.PROXY)) {
            AnnotationValue proxyClass = i.value("proxyClass");
            if (proxyClass == null) {
                continue;
            }
            proxyAnnotations.put(i.target().asClass().name().toString(), proxyClass.asClass().name().toString());
        }
        TypePool transformedClassesTypePool = createTransformedClassesTypePool(transformedClassesBuildItem,
                managedClassAndPackageNames);
        try (ProxyBuildingHelper proxyHelper = new ProxyBuildingHelper(transformedClassesTypePool)) {
            for (String managedClassOrPackageName : managedClassAndPackageNames) {
                CachedProxy result;
                if (proxyCache.cache.containsKey(managedClassOrPackageName)
                        && !isModified(managedClassOrPackageName, changedClasses, combinedIndex)) {
                    result = proxyCache.cache.get(managedClassOrPackageName);
                } else {
                    Set<String> proxyInterfaceNames = new TreeSet<>();
                    proxyInterfaceNames.add(ClassNames.HIBERNATE_PROXY.toString()); //always added
                    String proxy = proxyAnnotations.get(managedClassOrPackageName);
                    if (proxy == null) {
                        if (!proxyHelper.isProxiable(managedClassOrPackageName)) {
                            //if there is no @Proxy we need to make sure the actual class is proxiable
                            continue;
                        }
                    } else {
                        proxyInterfaceNames.add(proxy);
                    }
                    final String mappedClass = managedClassOrPackageName;
                    for (ClassInfo subclass : combinedIndex
                            .getAllKnownSubclasses(DotName.createSimple(managedClassOrPackageName))) {
                        String subclassName = subclass.name().toString();
                        if (!managedClassAndPackageNames.contains(subclassName)) {
                            //not an entity
                            continue;
                        }
                        proxy = proxyAnnotations.get(subclassName);
                        if (proxy != null) {
                            proxyInterfaceNames.add(proxy);
                        }
                    }
                    DynamicType.Unloaded<?> unloaded = proxyHelper.buildUnloadedProxy(mappedClass, proxyInterfaceNames);
                    result = new CachedProxy(unloaded, proxyInterfaceNames);
                    proxyCache.cache.put(managedClassOrPackageName, result);
                }
                for (Entry<TypeDescription, byte[]> i : result.proxyDef.getAllTypes().entrySet()) {
                    generatedClassBuildItemBuildProducer
                            .produce(new GeneratedClassBuildItem(true, i.getKey().getName(), i.getValue()));
                }
                preGeneratedProxies.getProxies().put(managedClassOrPackageName,
                        new PreGeneratedProxies.ProxyClassDetailsHolder(result.proxyDef.getTypeDescription().getName(),
                                result.interfaces));
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
        return TypePool.Default.of(new ClassFileLocator.Compound(
                new ClassFileLocator.Simple(transformedClasses),
                ClassFileLocator.ForClassLoader.of(Thread.currentThread().getContextClassLoader())));
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
        if (superName != null) {
            return isModified(superName.toString(), changedClasses, index);
        }
        return false;
    }

    private static Class[] toArray(final Set<Class<?>> interfaces) {
        if (interfaces == null) {
            return ArrayHelper.EMPTY_CLASS_ARRAY;
        }
        return interfaces.toArray(new Class[interfaces.size()]);
    }

    private static boolean isMySQLOrMariaDB(String dialect) {
        String lowercaseDialect = dialect.toLowerCase(Locale.ROOT);
        return lowercaseDialect.contains("mysql") || lowercaseDialect.contains("mariadb");
    }

    private static final class ProxyCache {

        Map<String, CachedProxy> cache = new HashMap<>();
    }

    static final class CachedProxy {
        final DynamicType.Unloaded<?> proxyDef;
        final Set<String> interfaces;

        CachedProxy(DynamicType.Unloaded<?> proxyDef, Set<String> interfaces) {
            this.proxyDef = proxyDef;
            this.interfaces = interfaces;
        }
    }
}
