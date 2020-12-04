package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Singleton;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.SharedCacheMode;
import javax.persistence.ValidationMode;
import javax.persistence.metamodel.StaticMetamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.Proxy;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.beanvalidation.BeanValidationIntegrator;
import org.hibernate.dialect.DB297Dialect;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.HibernateProxy;
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
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem.ExtendedBeanConfigurator;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem.BeanTypeExclusion;
import io.quarkus.arc.deployment.staticmethods.InterceptedStaticMethodsTransformersRegisteredBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LiveReloadBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.hibernate.orm.PersistenceUnit;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.JPAConfigSupport;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.hibernate.orm.runtime.RequestScopedSessionHolder;
import io.quarkus.hibernate.orm.runtime.TransactionSessions;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.scan.QuarkusScanner;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantConnectionResolver;
import io.quarkus.hibernate.orm.runtime.tenant.TenantResolver;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.configuration.ConfigurationException;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;

/**
 * Simulacrum of JPA bootstrap.
 * <p>
 * This does not address the proper integration with Hibernate
 * Rather prepare the path to providing the right metadata
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero <sanne@hibernate.org>
 */
public final class HibernateOrmProcessor {

    public static final String HIBERNATE_ORM_CONFIG_PREFIX = "quarkus.hibernate-orm.";
    public static final String NO_SQL_LOAD_SCRIPT_FILE = "no-file";

    private static final Logger LOG = Logger.getLogger(HibernateOrmProcessor.class);

    private static final DotName TENANT_CONNECTION_RESOLVER = DotName.createSimple(TenantConnectionResolver.class.getName());
    private static final DotName TENANT_RESOLVER = DotName.createSimple(TenantResolver.class.getName());

    private static final DotName STATIC_METAMODEL = DotName.createSimple(StaticMetamodel.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PERSISTENCE_UNIT_REPEATABLE_CONTAINER = DotName
            .createSimple(PersistenceUnit.List.class.getName());
    private static final DotName JPA_ENTITY = DotName.createSimple(Entity.class.getName());
    private static final DotName MAPPED_SUPERCLASS = DotName.createSimple(MappedSuperclass.class.getName());

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    private static final String PROXY_CACHE = HibernateOrmProcessor.class.getName() + ".proxyCache";

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.HIBERNATE_ORM);
    }

    @BuildStep
    void checkTransactionsSupport(Capabilities capabilities) {
        // JTA is necessary for blocking Hibernate ORM but not necessarily for Hibernate Reactive
        if (capabilities.isMissing(Capability.TRANSACTIONS)
                && capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
            throw new ConfigurationException("The Hibernate ORM extension is only functional in a JTA environment.");
        }
    }

    @BuildStep
    void includeArchivesHostingEntityPackagesInIndex(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<AdditionalApplicationArchiveMarkerBuildItem> additionalApplicationArchiveMarkers) {
        if (hibernateOrmConfig.defaultPersistenceUnit.packages.isPresent()) {
            for (String pakkage : hibernateOrmConfig.defaultPersistenceUnit.packages.get()) {
                additionalApplicationArchiveMarkers
                        .produce(new AdditionalApplicationArchiveMarkerBuildItem(pakkage.replace('.', '/')));
            }
        }
        for (HibernateOrmConfigPersistenceUnit persistenceUnit : hibernateOrmConfig.persistenceUnits.values()) {
            if (persistenceUnit.packages.isPresent()) {
                for (String pakkage : persistenceUnit.packages.get()) {
                    additionalApplicationArchiveMarkers
                            .produce(new AdditionalApplicationArchiveMarkerBuildItem(pakkage.replace('.', '/')));
                }
            }
        }
    }

    @BuildStep
    AdditionalIndexedClassesBuildItem addPersistenceUnitAnnotationToIndex() {
        return new AdditionalIndexedClassesBuildItem(PersistenceUnit.class.getName());
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
            JpaEntitiesBuildItem jpaEntities,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors) {

        if (!hasEntities(jpaEntities, nonJpaModelBuildItems)) {
            // we can bail out early as there are no entities
            return;
        }

        final boolean enversIsPresent = capabilities.isPresent(Capability.HIBERNATE_ENVERS);

        // First produce the PUs having a persistence.xml: these are not reactive, as we don't allow using a persistence.xml for them.
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            persistenceUnitDescriptors
                    .produce(new PersistenceUnitDescriptorBuildItem(persistenceXmlDescriptorBuildItem.getDescriptor(),
                            DataSourceUtil.DEFAULT_DATASOURCE_NAME,
                            getMultiTenancyStrategy(Optional.ofNullable(persistenceXmlDescriptorBuildItem.getDescriptor()
                                    .getProperties().getProperty(AvailableSettings.MULTI_TENANT))),
                            null,
                            false,
                            true,
                            enversIsPresent));
        }

        if (impliedPU.shouldGenerateImpliedBlockingPersistenceUnit()) {
            handleHibernateORMWithNoPersistenceXml(hibernateOrmConfig, index, persistenceXmlDescriptors,
                    jdbcDataSources, applicationArchivesBuildItem, launchMode.getLaunchMode(), jpaEntities, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors);
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
        CompositeIndex compositeIndex = CompositeIndex.create(index.getIndex(), indexer.complete());
        return new JpaModelIndexBuildItem(compositeIndex);
    }

    @BuildStep
    public void defineJpaEntities(
            JpaModelIndexBuildItem indexBuildItem,
            BuildProducer<JpaEntitiesBuildItem> domainObjectsProducer,
            List<IgnorableNonIndexedClasses> ignorableNonIndexedClassesBuildItems,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors) throws Exception {

        Set<String> nonJpaModelClasses = nonJpaModelBuildItems.stream()
                .map(NonJpaModelBuildItem::getClassName)
                .collect(Collectors.toSet());

        Set<String> ignorableNonIndexedClasses = Collections.emptySet();
        if (!ignorableNonIndexedClassesBuildItems.isEmpty()) {
            ignorableNonIndexedClasses = new HashSet<>();
            for (IgnorableNonIndexedClasses buildItem : ignorableNonIndexedClassesBuildItems) {
                ignorableNonIndexedClasses.addAll(buildItem.getClasses());
            }
        }

        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, persistenceXmlDescriptors,
                indexBuildItem.getIndex(),
                nonJpaModelClasses, ignorableNonIndexedClasses);
        final JpaEntitiesBuildItem domainObjects = scavenger.discoverModelAndRegisterForReflection();
        domainObjectsProducer.produce(domainObjects);
    }

    @BuildStep
    public ProxyDefinitionsBuildItem pregenProxies(
            JpaEntitiesBuildItem domainObjects,
            JpaModelIndexBuildItem indexBuildItem,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            LiveReloadBuildItem liveReloadBuildItem) {
        Set<String> entitiesToGenerateProxiesFor = new HashSet<>(domainObjects.getEntityClassNames());
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            entitiesToGenerateProxiesFor.addAll(pud.getManagedClassNames());
        }
        PreGeneratedProxies proxyDefinitions = generatedProxies(entitiesToGenerateProxiesFor, indexBuildItem.getIndex(),
                generatedClassBuildItemBuildProducer, liveReloadBuildItem);
        return new ProxyDefinitionsBuildItem(proxyDefinitions);
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext, HibernateOrmRecorder recorder,
            Capabilities capabilities,
            JpaEntitiesBuildItem domainObjects,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            List<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorBuildItems,
            List<HibernateOrmIntegrationBuildItem> integrations, //Used to make sure ORM integrations are performed before this item
            ProxyDefinitionsBuildItem proxyDefinitions,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener) throws Exception {

        feature.produce(new FeatureBuildItem(Feature.HIBERNATE_ORM));

        final boolean enableORM = hasEntities(domainObjects, nonJpaModelBuildItems);
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

        recorder.enlistPersistenceUnit(domainObjects.getEntityClassNames());

        final QuarkusScanner scanner = buildQuarkusScanner(domainObjects);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // inspect service files for additional integrators
        Collection<Class<? extends Integrator>> integratorClasses = new LinkedHashSet<>();
        for (String integratorClassName : ServiceUtil.classNamesNamedIn(classLoader, INTEGRATOR_SERVICE_FILE)) {
            integratorClasses.add((Class<? extends Integrator>) recorderContext.classProxy(integratorClassName));
        }

        List<QuarkusPersistenceUnitDefinition> finalStagePUDescriptors = new ArrayList<>();
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            finalStagePUDescriptors.add(pud.asOutputPersistenceUnitDefinition());
        }

        //Make it possible to record the QuarkusPersistenceUnitDefinition as bytecode:
        recorderContext.registerSubstitution(QuarkusPersistenceUnitDefinition.class,
                QuarkusPersistenceUnitDefinition.Serialized.class,
                QuarkusPersistenceUnitDefinition.Substitution.class);

        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initMetadata(finalStagePUDescriptors, scanner, integratorClasses,
                                proxyDefinitions.getProxies())));
    }

    @BuildStep
    void handleNativeImageImportSql(BuildProducer<NativeImageResourceBuildItem> resources,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels,
            LaunchModeBuildItem launchMode) {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
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
            BuildProducer<AdditionalBeanBuildItem> additionalBeans, Capabilities capabilities,
            CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        List<Class<?>> unremovableClasses = new ArrayList<>();
        unremovableClasses.add(JPAConfig.class);
        if (capabilities.isPresent(Capability.TRANSACTIONS)) {
            unremovableClasses.add(TransactionManager.class);
            unremovableClasses.add(TransactionSessions.class);
        }
        unremovableClasses.add(RequestScopedSessionHolder.class);

        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(unremovableClasses.toArray(new Class<?>[unremovableClasses.size()]))
                .build());
    }

    @Consume(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    @BuildStep
    public HibernateEnhancersRegisteredBuildItem enhancerDomainObjects(JpaEntitiesBuildItem domainObjects,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        // Modify the bytecode of all entities to enable lazy-loading, dirty checking, etc..
        enhanceEntities(domainObjects, transformers, additionalJpaModelBuildItems, additionalClasses);
        // this allows others to register their enhancers after Hibernate, so they run before ours
        return new HibernateEnhancersRegisteredBuildItem();
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(HibernateOrmRecorder recorder, HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            BuildProducer<BeanContainerListenerBuildItem> buildProducer,
            BuildProducer<SyntheticBeanBuildItem> syntheticBeans,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) throws Exception {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        Set<String> persistenceUnitNames = new HashSet<>();

        Map<String, Set<String>> entityPersistenceUnitMapping = new HashMap<>();
        for (PersistenceUnitDescriptorBuildItem descriptor : descriptors) {
            persistenceUnitNames.add(descriptor.getPersistenceUnitName());

            for (String entityClass : descriptor.getManagedClassNames()) {
                entityPersistenceUnitMapping.putIfAbsent(entityClass, new HashSet<>());
                entityPersistenceUnitMapping.get(entityClass).add(descriptor.getPersistenceUnitName());
            }
        }

        jpaModelPersistenceUnitMapping.produce(new JpaModelPersistenceUnitMappingBuildItem(entityPersistenceUnitMapping));

        syntheticBeans.produce(SyntheticBeanBuildItem.configure(JPAConfigSupport.class)
                .scope(Singleton.class)
                .unremovable()
                .supplier(recorder.jpaConfigSupportSupplier(
                        new JPAConfigSupport(persistenceUnitNames, entityPersistenceUnitMapping)))
                .done());
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public PersistenceProviderSetUpBuildItem setupPersistenceProvider(HibernateOrmRecorder recorder,
            Capabilities capabilities,
            HibernateOrmRuntimeConfig hibernateOrmRuntimeConfig) {
        if (capabilities.isMissing(Capability.HIBERNATE_REACTIVE)) {
            recorder.setupPersistenceProvider(hibernateOrmRuntimeConfig);
        }

        return new PersistenceProviderSetUpBuildItem();
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public ServiceStartBuildItem startPersistenceUnits(HibernateOrmRecorder recorder, BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> dataSourcesConfigured,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationsRuntimeConfigured,
            List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem,
            List<PersistenceProviderSetUpBuildItem> persistenceProviderSetUp) throws Exception {
        if (hasEntities(jpaEntities, nonJpaModels)) {
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
            unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanTypeExclusion(TENANT_CONNECTION_RESOLVER)));
            unremovableBeans.produce(new UnremovableBeanBuildItem(new BeanTypeExclusion(TENANT_RESOLVER)));
        }
    }

    @BuildStep
    public void produceLoggingCategories(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<LogCategoryBuildItem> categories) {
        if (hibernateOrmConfig.log.bindParam || hibernateOrmConfig.log.bindParameters) {
            categories.produce(new LogCategoryBuildItem("org.hibernate.type.descriptor.sql.BasicBinder", Level.TRACE));
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    public void registerStaticMetamodelClassesForReflection(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflective) {
        Collection<AnnotationInstance> annotationInstances = index.getIndex().getAnnotations(STATIC_METAMODEL);
        if (!annotationInstances.isEmpty()) {

            String[] metamodel = annotationInstances.stream()
                    .map(a -> a.target().asClass().name().toString())
                    .toArray(String[]::new);

            reflective.produce(new ReflectiveClassBuildItem(false, false, true, metamodel));
        }
    }

    private static Optional<String> getSqlLoadScript(Optional<String> sqlLoadScript, LaunchMode launchMode) {
        // Explicit file or default Hibernate ORM file.
        if (sqlLoadScript.isPresent()) {
            if (NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(sqlLoadScript.get())) {
                return Optional.empty();
            } else {
                return Optional.of(sqlLoadScript.get());
            }
        } else if (launchMode == LaunchMode.NORMAL) {
            return Optional.empty();
        } else {
            return Optional.of("import.sql");
        }
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getEntityClassNames().isEmpty() || !nonJpaModels.isEmpty();
    }

    private void handleHibernateORMWithNoPersistenceXml(
            HibernateOrmConfig hibernateOrmConfig,
            CombinedIndexBuildItem index,
            List<PersistenceXmlDescriptorBuildItem> descriptors,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            JpaEntitiesBuildItem jpaEntities,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors) {
        if (!descriptors.isEmpty()) {
            if (hibernateOrmConfig.isAnyPropertySet() || !hibernateOrmConfig.persistenceUnits.isEmpty()) {
                throw new ConfigurationError(
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

        Map<String, Set<String>> modelClassesPerPersistencesUnits = getModelClassesPerPersistenceUnits(hibernateOrmConfig,
                jpaEntities, index.getIndex(), enableDefaultPersistenceUnit);
        Set<String> modelClassesForDefaultPersistenceUnit = modelClassesPerPersistencesUnits
                .getOrDefault(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, Collections.emptySet());

        Set<String> storageEngineCollector = new HashSet<>();

        if (enableDefaultPersistenceUnit) {
            producePersistenceUnitDescriptorFromConfig(
                    hibernateOrmConfig, PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    hibernateOrmConfig.defaultPersistenceUnit,
                    modelClassesForDefaultPersistenceUnit,
                    jdbcDataSources, applicationArchivesBuildItem, launchMode, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    storageEngineCollector);
        } else if (!modelClassesForDefaultPersistenceUnit.isEmpty()
                && (!hibernateOrmConfig.defaultPersistenceUnit.datasource.isPresent()
                        || DataSourceUtil.isDefault(hibernateOrmConfig.defaultPersistenceUnit.datasource.get()))
                && !defaultJdbcDataSource.isPresent()) {
            LOG.warn(
                    "Model classes are defined for the default persistence unit but no default datasource found: the default EntityManagerFactory will not be created.");
        }

        for (Entry<String, HibernateOrmConfigPersistenceUnit> persistenceUnitEntry : hibernateOrmConfig.persistenceUnits
                .entrySet()) {
            producePersistenceUnitDescriptorFromConfig(
                    hibernateOrmConfig, persistenceUnitEntry.getKey(), persistenceUnitEntry.getValue(),
                    modelClassesPerPersistencesUnits.getOrDefault(persistenceUnitEntry.getKey(), Collections.emptySet()),
                    jdbcDataSources, applicationArchivesBuildItem, launchMode, capabilities,
                    systemProperties, nativeImageResources, hotDeploymentWatchedFiles, persistenceUnitDescriptors,
                    storageEngineCollector);
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
            Set<String> modelClasses,
            List<JdbcDataSourceBuildItem> jdbcDataSources,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode,
            Capabilities capabilities,
            BuildProducer<SystemPropertyBuildItem> systemProperties,
            BuildProducer<NativeImageResourceBuildItem> nativeImageResources,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptors,
            Set<String> storageEngineCollector) {
        // Find the associated datasource
        JdbcDataSourceBuildItem jdbcDataSource;
        String dataSource;
        if (persistenceUnitConfig.datasource.isPresent()) {
            jdbcDataSource = jdbcDataSources.stream()
                    .filter(i -> persistenceUnitConfig.datasource.get().equals(i.getName()))
                    .findFirst()
                    .orElseThrow(() -> new ConfigurationException(
                            String.format("The datasource '%1$s' is not configured but the persistence unit '%2$s' uses it.",
                                    persistenceUnitConfig.datasource.get(), persistenceUnitName)));
            dataSource = persistenceUnitConfig.datasource.get();
        } else {
            if (!PersistenceUnitUtil.isDefaultPersistenceUnit(persistenceUnitName)) {
                // if it's not the default persistence unit, we mandate a datasource to prevent common errors
                throw new ConfigurationException(
                        String.format("Datasource must be defined for persistence unit '%s'.", persistenceUnitName));
            }

            jdbcDataSource = jdbcDataSources.stream()
                    .filter(i -> i.isDefault())
                    .findFirst()
                    .orElseThrow(() -> new ConfigurationException(
                            String.format("The default datasource is not configured but the persistence unit '%s' uses it.",
                                    persistenceUnitName)));
            dataSource = DataSourceUtil.DEFAULT_DATASOURCE_NAME;
        }

        Optional<String> dialect = persistenceUnitConfig.dialect.dialect;
        if (!dialect.isPresent()) {
            dialect = guessDialect(jdbcDataSource.getDbKind());
        }

        if (!dialect.isPresent()) {
            return;
        }

        // we found one
        ParsedPersistenceXmlDescriptor descriptor = new ParsedPersistenceXmlDescriptor(null); //todo URL
        descriptor.setName(persistenceUnitName);

        descriptor.setExcludeUnlistedClasses(true);
        if (modelClasses.isEmpty()) {
            LOG.warnf("Could not find any entities affected to the persistence unit '%s'.", persistenceUnitName);
        } else {
            descriptor.addClasses(new ArrayList<>(modelClasses));
        }

        descriptor.setTransactionType(PersistenceUnitTransactionType.JTA);
        descriptor.getProperties().setProperty(AvailableSettings.DIALECT, dialect.get());

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

        //charset
        descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME,
                persistenceUnitConfig.database.charset.name());

        persistenceUnitConfig.database.defaultCatalog.ifPresent(
                catalog -> descriptor.getProperties().setProperty(AvailableSettings.DEFAULT_CATALOG, catalog));

        persistenceUnitConfig.database.defaultSchema.ifPresent(
                schema -> descriptor.getProperties().setProperty(AvailableSettings.DEFAULT_SCHEMA, schema));

        if (persistenceUnitConfig.database.globallyQuotedIdentifiers) {
            descriptor.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
        }

        // Query
        if (persistenceUnitConfig.fetch.batchSize > 0) {
            setBatchFetchSize(descriptor, persistenceUnitConfig.fetch.batchSize);
        } else if (persistenceUnitConfig.batchFetchSize > 0) {
            setBatchFetchSize(descriptor, persistenceUnitConfig.batchFetchSize);
        }

        if (persistenceUnitConfig.fetch.maxDepth.isPresent()) {
            setMaxFetchDepth(descriptor, persistenceUnitConfig.fetch.maxDepth);
        } else if (persistenceUnitConfig.maxFetchDepth.isPresent()) {
            setMaxFetchDepth(descriptor, persistenceUnitConfig.maxFetchDepth);
        }

        descriptor.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, Integer.toString(
                persistenceUnitConfig.query.queryPlanCacheMaxSize));

        descriptor.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                persistenceUnitConfig.query.defaultNullOrdering.name().toLowerCase());

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
        }

        // sql-load-script
        Optional<String> importFile = getSqlLoadScript(persistenceUnitConfig.sqlLoadScript, launchMode);

        if (importFile.isPresent()) {
            Path loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile.get());

            if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                // enlist resource if present
                nativeImageResources.produce(new NativeImageResourceBuildItem(importFile.get()));
                descriptor.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, importFile.get());
            } else if (persistenceUnitConfig.sqlLoadScript.isPresent()) {
                //raise exception if explicit file is not present (i.e. not the default)
                throw new ConfigurationError(
                        "Unable to find file referenced in '" + HIBERNATE_ORM_CONFIG_PREFIX + "sql-load-script="
                                + persistenceUnitConfig.sqlLoadScript.get() + "'. Remove property or add file to your path.");
            }
            if (launchMode == LaunchMode.DEVELOPMENT) {
                // in dev mode we want to make sure that we watch for changes to file even if it doesn't currently exist
                // as a user could still add it after performing the initial configuration
                hotDeploymentWatchedFiles.produce(new HotDeploymentWatchedFileBuildItem(importFile.get()));
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
        if (isMySQLOrMariaDB(dialect.get()) && persistenceUnitConfig.dialect.storageEngine.isPresent()) {
            storageEngineCollector.add(persistenceUnitConfig.dialect.storageEngine.get());
        }

        final boolean isEnversPresent = capabilities.isPresent(Capability.HIBERNATE_ENVERS);

        persistenceUnitDescriptors.produce(
                new PersistenceUnitDescriptorBuildItem(descriptor, dataSource,
                        getMultiTenancyStrategy(persistenceUnitConfig.multitenant),
                        persistenceUnitConfig.multitenantSchemaDatasource.orElse(null),
                        false, false, isEnversPresent));
    }

    public static Optional<String> guessDialect(String resolvedDbKind) {
        // For now select the latest dialect from the driver
        // later, we can keep doing that but also avoid DCE
        // of all the dialects we want in so that people can override them
        if (DatabaseKind.isDB2(resolvedDbKind)) {
            return Optional.of(DB297Dialect.class.getName());
        }
        if (DatabaseKind.isPostgreSQL(resolvedDbKind)) {
            return Optional.of(QuarkusPostgreSQL10Dialect.class.getName());
        }
        if (DatabaseKind.isH2(resolvedDbKind)) {
            return Optional.of(QuarkusH2Dialect.class.getName());
        }
        if (DatabaseKind.isMariaDB(resolvedDbKind)) {
            return Optional.of(MariaDB103Dialect.class.getName());
        }
        if (DatabaseKind.isMySQL(resolvedDbKind)) {
            return Optional.of(MySQL8Dialect.class.getName());
        }
        if (DatabaseKind.isDerby(resolvedDbKind)) {
            return Optional.of(DerbyTenSevenDialect.class.getName());
        }
        if (DatabaseKind.isMsSQL(resolvedDbKind)) {
            return Optional.of(SQLServer2012Dialect.class.getName());
        }

        String error = "Hibernate extension could not guess the dialect from the database kind '" + resolvedDbKind
                + "'. Add an explicit '" + HIBERNATE_ORM_CONFIG_PREFIX + "dialect' property.";
        throw new ConfigurationError(error);
    }

    private static void setMaxFetchDepth(ParsedPersistenceXmlDescriptor descriptor, OptionalInt maxFetchDepth) {
        descriptor.getProperties().setProperty(AvailableSettings.MAX_FETCH_DEPTH, String.valueOf(maxFetchDepth.getAsInt()));
    }

    private static void setBatchFetchSize(ParsedPersistenceXmlDescriptor descriptor, int batchFetchSize) {
        descriptor.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE,
                Integer.toString(batchFetchSize));
        descriptor.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
    }

    private void enhanceEntities(final JpaEntitiesBuildItem domainObjects,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : domainObjects.getAllModelClassNames()) {
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

    private static Map<String, Set<String>> getModelClassesPerPersistenceUnits(HibernateOrmConfig hibernateOrmConfig,
            JpaEntitiesBuildItem jpaEntities, IndexView index, boolean enableDefaultPersistenceUnit) {
        if (hibernateOrmConfig.persistenceUnits.isEmpty()) {
            // no named persistence units, all the entities will be associated with the default one
            // so we don't need to split them
            return Collections.singletonMap(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                    jpaEntities.getAllModelClassNames());
        }

        Map<String, Set<String>> modelClassesPerPersistenceUnits = new HashMap<>();

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
                        .orElseThrow(() -> new ConfigurationException(String.format(
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
        } else {
            throw new ConfigurationException(
                    "Multiple persistence units are defined but the entities are not mapped to them. You should either use the .packages Quarkus configuration property or package-level @PersistenceUnit annotations.");
        }

        Set<String> modelClassesWithPersistenceUnitAnnotations = new TreeSet<>();

        for (String modelClassName : jpaEntities.getAllModelClassNames()) {
            ClassInfo modelClassInfo = index.getClassByName(DotName.createSimple(modelClassName));
            Set<String> relatedModelClassNames = getRelatedModelClassNames(index, jpaEntities.getAllModelClassNames(),
                    modelClassInfo);

            if (modelClassInfo != null && (modelClassInfo.classAnnotation(PERSISTENCE_UNIT) != null
                    || modelClassInfo.classAnnotation(PERSISTENCE_UNIT_REPEATABLE_CONTAINER) != null)) {
                modelClassesWithPersistenceUnitAnnotations.add(modelClassInfo.name().toString());
            }

            for (Entry<String, Set<String>> packageRuleEntry : packageRules.entrySet()) {
                if (modelClassName.startsWith(packageRuleEntry.getKey())) {
                    for (String persistenceUnitName : packageRuleEntry.getValue()) {
                        modelClassesPerPersistenceUnits.putIfAbsent(persistenceUnitName, new HashSet<>());
                        modelClassesPerPersistenceUnits.get(persistenceUnitName).add(modelClassName);

                        // also add the hierarchy to the persistence unit
                        // we would need to add all the underlying model to it but adding the hierarchy
                        // is necessary for Panache as we need to add PanacheEntity to the PU
                        for (String relatedModelClassName : relatedModelClassNames) {
                            modelClassesPerPersistenceUnits.get(persistenceUnitName).add(relatedModelClassName);
                        }
                    }
                }
            }
        }

        if (!modelClassesWithPersistenceUnitAnnotations.isEmpty()) {
            throw new IllegalStateException(String.format(
                    "@PersistenceUnit annotations are not supported at the class level on model classes:\n\t- %s\nUse the `.packages` configuration property or package-level annotations instead.",
                    String.join("\n\t- ", modelClassesWithPersistenceUnitAnnotations)));
        }

        Set<String> affectedModelClasses = modelClassesPerPersistenceUnits.values().stream().flatMap(Set::stream)
                .collect(Collectors.toSet());
        Set<String> unaffectedModelClasses = jpaEntities.getAllModelClassNames().stream()
                .filter(c -> !affectedModelClasses.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        if (!unaffectedModelClasses.isEmpty()) {
            LOG.warnf("Could not find a suitable persistence unit for model classes:\n\t- %s",
                    String.join("\n\t- ", unaffectedModelClasses));
        }

        return modelClassesPerPersistenceUnits;
    }

    private static Set<String> getRelatedModelClassNames(IndexView index, Set<String> knownModelClassNames,
            ClassInfo modelClassInfo) {
        if (modelClassInfo == null) {
            return Collections.emptySet();
        }

        Set<String> relatedModelClassNames = new HashSet<>();

        // for now we only deal with entities and mapped super classes
        if (modelClassInfo.classAnnotation(JPA_ENTITY) == null &&
                modelClassInfo.classAnnotation(MAPPED_SUPERCLASS) == null) {
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
        if (hibernateOrmConfig.defaultPersistenceUnit.packages.isPresent()) {
            return true;
        }

        for (HibernateOrmConfigPersistenceUnit persistenceUnitConfig : hibernateOrmConfig.persistenceUnits.values()) {
            if (persistenceUnitConfig.packages.isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static Collection<AnnotationInstance> getPackageLevelPersistenceUnitAnnotations(IndexView index) {
        Collection<AnnotationInstance> persistenceUnitAnnotations = index.getAnnotationsWithRepeatable(PERSISTENCE_UNIT, index);
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
     * @param domainObjects the previously discovered domain objects
     * @return a new QuarkusScanner with all domainObjects registered
     */
    public static QuarkusScanner buildQuarkusScanner(JpaEntitiesBuildItem domainObjects) {
        QuarkusScanner scanner = new QuarkusScanner();
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String i : domainObjects.getAllModelClassNames()) {
            QuarkusScanner.ClassDescriptorImpl desc = new QuarkusScanner.ClassDescriptorImpl(i,
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
            throw new ConfigurationError("The Hibernate ORM multitenancy strategy "
                    + MultiTenancyStrategy.DISCRIMINATOR + " is currently not supported");
        }
        return multiTenancyStrategy;
    }

    private PreGeneratedProxies generatedProxies(Set<String> entityClassNames, IndexView combinedIndex,
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
        for (AnnotationInstance i : combinedIndex.getAnnotations(DotName.createSimple(Proxy.class.getName()))) {
            AnnotationValue proxyClass = i.value("proxyClass");
            if (proxyClass == null) {
                continue;
            }
            proxyAnnotations.put(i.target().asClass().name().toString(), proxyClass.asClass().name().toString());
        }
        try (ProxyBuildingHelper proxyHelper = new ProxyBuildingHelper(Thread.currentThread().getContextClassLoader())) {
            for (String entity : entityClassNames) {
                CachedProxy result;
                if (proxyCache.cache.containsKey(entity) && !isModified(entity, changedClasses, combinedIndex)) {
                    result = proxyCache.cache.get(entity);
                } else {
                    Set<Class<?>> proxyInterfaces = new HashSet<>();
                    proxyInterfaces.add(HibernateProxy.class); //always added
                    Class<?> mappedClass = proxyHelper.uninitializedClass(entity);
                    String proxy = proxyAnnotations.get(entity);
                    if (proxy != null) {
                        proxyInterfaces.add(proxyHelper.uninitializedClass(proxy));
                    } else if (!proxyHelper.isProxiable(mappedClass)) {
                        //if there is no @Proxy we need to make sure the actual class is proxiable
                        continue;
                    }
                    for (ClassInfo subclass : combinedIndex.getAllKnownSubclasses(DotName.createSimple(entity))) {
                        String subclassName = subclass.name().toString();
                        if (!entityClassNames.contains(subclassName)) {
                            //not an entity
                            continue;
                        }
                        proxy = proxyAnnotations.get(subclassName);
                        if (proxy != null) {
                            proxyInterfaces.add(proxyHelper.uninitializedClass(proxy));
                        }
                    }
                    DynamicType.Unloaded<?> unloaded = proxyHelper.buildUnloadedProxy(mappedClass,
                            toArray(proxyInterfaces));
                    result = new CachedProxy(unloaded,
                            proxyInterfaces.stream().map(Class::getName).collect(Collectors.toSet()));
                    proxyCache.cache.put(entity, result);
                }
                for (Entry<TypeDescription, byte[]> i : result.proxyDef.getAllTypes().entrySet()) {
                    generatedClassBuildItemBuildProducer
                            .produce(new GeneratedClassBuildItem(true, i.getKey().getName(), i.getValue()));
                }
                preGeneratedProxies.getProxies().put(entity,
                        new PreGeneratedProxies.ProxyClassDetailsHolder(result.proxyDef.getTypeDescription().getName(),
                                result.interfaces));
            }
        }
        return preGeneratedProxies;
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
