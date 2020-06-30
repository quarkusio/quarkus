package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Produces;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.SharedCacheMode;
import javax.persistence.metamodel.StaticMetamodel;
import javax.persistence.spi.PersistenceUnitTransactionType;
import javax.transaction.TransactionManager;

import org.hibernate.MultiTenancyStrategy;
import org.hibernate.annotations.Proxy;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.cfg.AvailableSettings;
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
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.logmanager.Level;

import io.quarkus.agroal.deployment.JdbcDataSourceBuildItem;
import io.quarkus.agroal.deployment.JdbcDataSourceSchemaReadyBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.ResourceAnnotationBuildItem;
import io.quarkus.arc.deployment.staticmethods.InterceptedStaticMethodsTransformersRegisteredBuildItem;
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.LogCategoryBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.deployment.util.ServiceUtil;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationBuildItem;
import io.quarkus.hibernate.orm.deployment.integration.HibernateOrmIntegrationRuntimeConfiguredBuildItem;
import io.quarkus.hibernate.orm.runtime.DefaultEntityManagerFactoryProducer;
import io.quarkus.hibernate.orm.runtime.DefaultEntityManagerProducer;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRecorder;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.JPAResourceReferenceProvider;
import io.quarkus.hibernate.orm.runtime.RequestScopedEntityManagerHolder;
import io.quarkus.hibernate.orm.runtime.TransactionEntityManagers;
import io.quarkus.hibernate.orm.runtime.boot.QuarkusPersistenceUnitDefinition;
import io.quarkus.hibernate.orm.runtime.boot.scan.QuarkusScanner;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.hibernate.orm.runtime.tenant.DataSourceTenantConnectionResolver;
import io.quarkus.runtime.LaunchMode;
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

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());
    private static final DotName STATIC_METAMODEL = DotName.createSimple(StaticMetamodel.class.getName());

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";

    /**
     * Hibernate ORM configuration
     */
    HibernateOrmConfig hibernateConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.HIBERNATE_ORM);
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
    List<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles(LaunchModeBuildItem launchMode) {
        List<HotDeploymentWatchedFileBuildItem> watchedFiles = new ArrayList<>();
        if (shouldIgnorePersistenceXmlResources()) {
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem("META-INF/persistence.xml"));
        }
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem(INTEGRATOR_SERVICE_FILE));

        getSqlLoadScript(launchMode.getLaunchMode()).ifPresent(script -> {
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem(script));
        });
        return watchedFiles;
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
            List<PersistenceXmlDescriptorBuildItem> actualXmlDescriptors) {

        //We won't generate an implied PU if there are explicitly configured PUs
        if (actualXmlDescriptors.isEmpty() == false) {
            //when we have any explicitly provided Persistence Unit, disable the automatically generated ones.
            return ImpliedBlockingPersistenceUnitTypeBuildItem.none();
        }

        //The default implied PU requires to bind to the default JDBC datasource, so check that we have one:
        Optional<JdbcDataSourceBuildItem> defaultJdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
                .filter(i -> i.isDefault())
                .findFirst();
        if (defaultJdbcDataSourceBuildItem.isPresent()) {
            return ImpliedBlockingPersistenceUnitTypeBuildItem
                    .generateImpliedPersistenceUnit(defaultJdbcDataSourceBuildItem.get());
        } else {
            return ImpliedBlockingPersistenceUnitTypeBuildItem.none();
        }
    }

    @BuildStep
    public void configurationDescriptorBuilding(
            ImpliedBlockingPersistenceUnitTypeBuildItem impliedPU,
            List<PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchModeBuildItem launchMode,
            JpaEntitiesBuildItem domainObjects,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorProducer) {

        if (!hasEntities(domainObjects, nonJpaModelBuildItems)) {
            // we can bail out early as there are no entities
            return;
        }

        // First produce the PUs having a persistence.xml: these are not reactive, as we don't allow using a persistence.xml for them.
        for (PersistenceXmlDescriptorBuildItem persistenceXmlDescriptorBuildItem : persistenceXmlDescriptors) {
            persistenceUnitDescriptorProducer
                    .produce(new PersistenceUnitDescriptorBuildItem(persistenceXmlDescriptorBuildItem.getDescriptor(),
                            getMultiTenancyStrategy(), false));
        }

        if (impliedPU.shouldGenerateImpliedBlockingPersistenceUnit()) {
            handleHibernateORMWithNoPersistenceXml(persistenceXmlDescriptors, resourceProducer, systemPropertyProducer,
                    impliedPU.getDatasourceBuildTimeConfiguration(), applicationArchivesBuildItem, launchMode.getLaunchMode(),
                    persistenceUnitDescriptorProducer);
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
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
        Set<String> entitiesToGenerateProxiesFor = new HashSet<>(domainObjects.getEntityClassNames());
        for (PersistenceUnitDescriptorBuildItem pud : persistenceUnitDescriptorBuildItems) {
            pud.addListedEntityClassNamesTo(entitiesToGenerateProxiesFor);
        }
        PreGeneratedProxies proxyDefinitions = generatedProxies(entitiesToGenerateProxiesFor, indexBuildItem.getIndex(),
                generatedClassBuildItemBuildProducer);
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

    private MultiTenancyStrategy getMultiTenancyStrategy() {
        final MultiTenancyStrategy multiTenancyStrategy = MultiTenancyStrategy
                .valueOf(hibernateConfig.multitenant.orElse(MultiTenancyStrategy.NONE.name()));
        if (multiTenancyStrategy == MultiTenancyStrategy.DISCRIMINATOR) {
            // See https://hibernate.atlassian.net/browse/HHH-6054
            throw new ConfigurationError("The Hibernate ORM multi tenancy strategy "
                    + MultiTenancyStrategy.DISCRIMINATOR + " is currently not supported");
        }
        return multiTenancyStrategy;
    }

    private PreGeneratedProxies generatedProxies(Set<String> entityClassNames, IndexView combinedIndex,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer) {
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
                DynamicType.Unloaded<?> proxyDef = proxyHelper.buildUnloadedProxy(mappedClass,
                        toArray(proxyInterfaces));

                for (Entry<TypeDescription, byte[]> i : proxyDef.getAllTypes().entrySet()) {
                    generatedClassBuildItemBuildProducer
                            .produce(new GeneratedClassBuildItem(true, i.getKey().getName(), i.getValue()));
                }
                preGeneratedProxies.getProxies().put(entity,
                        new PreGeneratedProxies.ProxyClassDetailsHolder(proxyDef.getTypeDescription().getName(),
                                proxyInterfaces.stream().map(Class::getName).collect(Collectors.toSet())));
            }
        }
        return preGeneratedProxies;
    }

    private static Class[] toArray(final Set<Class<?>> interfaces) {
        if (interfaces == null) {
            return ArrayHelper.EMPTY_CLASS_ARRAY;
        }
        return interfaces.toArray(new Class[interfaces.size()]);
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
            } else {
                getSqlLoadScript(launchMode.getLaunchMode()).ifPresent(script -> {
                    resources.produce(new NativeImageResourceBuildItem(script));
                });
            }
        }
    }

    @BuildStep
    void setupResourceInjection(BuildProducer<ResourceAnnotationBuildItem> resourceAnnotations,
            BuildProducer<GeneratedResourceBuildItem> resources,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        resources.produce(new GeneratedResourceBuildItem("META-INF/services/io.quarkus.arc.ResourceReferenceProvider",
                JPAResourceReferenceProvider.class.getName().getBytes(StandardCharsets.UTF_8)));
        resourceAnnotations.produce(new ResourceAnnotationBuildItem(PERSISTENCE_CONTEXT));
        resourceAnnotations.produce(new ResourceAnnotationBuildItem(PERSISTENCE_UNIT));
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans, Capabilities capabilities,
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
            unremovableClasses.add(TransactionEntityManagers.class);
        }
        unremovableClasses.add(RequestScopedEntityManagerHolder.class);
        if (getMultiTenancyStrategy() != MultiTenancyStrategy.NONE) {
            unremovableClasses.add(DataSourceTenantConnectionResolver.class);
        }

        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(unremovableClasses.toArray(new Class<?>[unremovableClasses.size()]))
                .build());

        if (descriptors.size() == 1) {
            // There is only one persistence unit - register CDI beans for EM and EMF if no
            // producers are defined
            if (isUserDefinedProducerMissing(combinedIndex.getIndex(), PERSISTENCE_UNIT)) {
                additionalBeans.produce(new AdditionalBeanBuildItem(DefaultEntityManagerFactoryProducer.class));
            }
            if (isUserDefinedProducerMissing(combinedIndex.getIndex(), PERSISTENCE_CONTEXT)) {
                additionalBeans.produce(new AdditionalBeanBuildItem(DefaultEntityManagerProducer.class));
            }
        }
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
    public void build(HibernateOrmRecorder recorder,
            Capabilities capabilities, BuildProducer<BeanContainerListenerBuildItem> buildProducer,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) throws Exception {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }
        MultiTenancyStrategy strategy = MultiTenancyStrategy
                .valueOf(hibernateConfig.multitenant.orElse(MultiTenancyStrategy.NONE.name()));
        buildProducer.produce(new BeanContainerListenerBuildItem(
                recorder.initializeJpa(capabilities.isPresent(Capability.TRANSACTIONS), strategy,
                        hibernateConfig.multitenantSchemaDatasource.orElse(null))));

        // Bootstrap all persistence units
        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : descriptors) {
            buildProducer.produce(new BeanContainerListenerBuildItem(
                    recorder.registerPersistenceUnit(persistenceUnitDescriptor.getPersistenceUnitName())));
        }
        buildProducer.produce(new BeanContainerListenerBuildItem(recorder.initDefaultPersistenceUnit()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void startPersistenceUnits(HibernateOrmRecorder recorder, BeanContainerBuildItem beanContainer,
            List<JdbcDataSourceBuildItem> dataSourcesConfigured,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationsRuntimeConfigured,
            List<JdbcDataSourceSchemaReadyBuildItem> schemaReadyBuildItem) throws Exception {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        recorder.startAllPersistenceUnits(beanContainer.getValue());
    }

    private Optional<String> getSqlLoadScript(LaunchMode launchMode) {
        // Explicit file or default Hibernate ORM file.
        if (hibernateConfig.sqlLoadScript.isPresent()) {
            if (NO_SQL_LOAD_SCRIPT_FILE.equalsIgnoreCase(hibernateConfig.sqlLoadScript.get())) {
                return Optional.empty();
            } else {
                return Optional.of(hibernateConfig.sqlLoadScript.get());
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

    private boolean isUserDefinedProducerMissing(IndexView index, DotName annotationName) {
        for (AnnotationInstance annotationInstance : index.getAnnotations(annotationName)) {
            if (annotationInstance.target().kind() == AnnotationTarget.Kind.METHOD) {
                if (annotationInstance.target().asMethod().hasAnnotation(PRODUCES)) {
                    return false;
                }
            } else if (annotationInstance.target().kind() == AnnotationTarget.Kind.FIELD) {
                for (AnnotationInstance i : annotationInstance.target().asField().annotations()) {
                    if (i.name().equals(PRODUCES)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void handleHibernateORMWithNoPersistenceXml(
            List<PersistenceXmlDescriptorBuildItem> descriptors,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            JdbcDataSourceBuildItem driverBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode, BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorProducer) {
        if (descriptors.isEmpty()) {
            //we have no persistence.xml so we will create a default one
            Optional<String> dialect = hibernateConfig.dialect;
            if (!dialect.isPresent()) {
                dialect = guessDialect(driverBuildItem.getDbKind());
            }
            dialect.ifPresent(s -> {
                // we found one
                ParsedPersistenceXmlDescriptor desc = new ParsedPersistenceXmlDescriptor(null); //todo URL
                desc.setName("default");
                desc.setTransactionType(PersistenceUnitTransactionType.JTA);
                desc.getProperties().setProperty(AvailableSettings.DIALECT, s);

                // The storage engine has to be set as a system property.
                if (hibernateConfig.dialectStorageEngine.isPresent()) {
                    systemProperty.produce(new SystemPropertyBuildItem(AvailableSettings.STORAGE_ENGINE,
                            hibernateConfig.dialectStorageEngine.get()));
                }
                // Physical Naming Strategy
                hibernateConfig.physicalNamingStrategy.ifPresent(
                        namingStrategy -> desc.getProperties()
                                .setProperty(AvailableSettings.PHYSICAL_NAMING_STRATEGY, namingStrategy));

                // Implicit Naming Strategy
                hibernateConfig.implicitNamingStrategy.ifPresent(
                        namingStrategy -> desc.getProperties()
                                .setProperty(AvailableSettings.IMPLICIT_NAMING_STRATEGY, namingStrategy));

                // Database
                desc.getProperties().setProperty(AvailableSettings.HBM2DDL_DATABASE_ACTION,
                        hibernateConfig.database.generation);

                if (hibernateConfig.database.generationHaltOnError) {
                    desc.getProperties().setProperty(AvailableSettings.HBM2DDL_HALT_ON_ERROR, "true");
                }

                //charset
                desc.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME,
                        hibernateConfig.database.charset.name());

                hibernateConfig.database.defaultCatalog.ifPresent(
                        catalog -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_CATALOG, catalog));

                hibernateConfig.database.defaultSchema.ifPresent(
                        schema -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_SCHEMA, schema));

                if (hibernateConfig.database.globallyQuotedIdentifiers) {
                    desc.getProperties().setProperty(AvailableSettings.GLOBALLY_QUOTED_IDENTIFIERS, "true");
                }

                // Query
                if (hibernateConfig.batchFetchSize > 0) {
                    desc.getProperties().setProperty(AvailableSettings.DEFAULT_BATCH_FETCH_SIZE,
                            Integer.toString(hibernateConfig.batchFetchSize));
                    desc.getProperties().setProperty(AvailableSettings.BATCH_FETCH_STYLE, BatchFetchStyle.PADDED.toString());
                }

                hibernateConfig.query.queryPlanCacheMaxSize.ifPresent(
                        maxSize -> desc.getProperties().setProperty(AvailableSettings.QUERY_PLAN_CACHE_MAX_SIZE, maxSize));

                hibernateConfig.query.defaultNullOrdering.ifPresent(
                        defaultNullOrdering -> desc.getProperties().setProperty(AvailableSettings.DEFAULT_NULL_ORDERING,
                                defaultNullOrdering));

                // JDBC
                hibernateConfig.jdbc.timezone.ifPresent(
                        timezone -> desc.getProperties().setProperty(AvailableSettings.JDBC_TIME_ZONE, timezone));

                hibernateConfig.jdbc.statementFetchSize.ifPresent(
                        fetchSize -> desc.getProperties().setProperty(AvailableSettings.STATEMENT_FETCH_SIZE,
                                fetchSize.toString()));

                hibernateConfig.jdbc.statementBatchSize.ifPresent(
                        fetchSize -> desc.getProperties().setProperty(AvailableSettings.STATEMENT_BATCH_SIZE,
                                fetchSize.toString()));

                // Logging
                if (hibernateConfig.log.sql) {
                    desc.getProperties().setProperty(AvailableSettings.SHOW_SQL, "true");
                    desc.getProperties().setProperty(AvailableSettings.FORMAT_SQL, "true");
                }

                if (hibernateConfig.log.jdbcWarnings.isPresent()) {
                    desc.getProperties().setProperty(AvailableSettings.LOG_JDBC_WARNINGS,
                            hibernateConfig.log.jdbcWarnings.get().toString());
                }

                // Statistics
                if (hibernateConfig.metricsEnabled
                        || (hibernateConfig.statistics.isPresent() && hibernateConfig.statistics.get())) {
                    desc.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
                }

                // sql-load-script
                Optional<String> importFile = getSqlLoadScript(launchMode);

                if (!importFile.isPresent()) {
                    // explicitly set a no file and ignore all other operations
                    desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, NO_SQL_LOAD_SCRIPT_FILE);
                } else {
                    Path loadScriptPath = applicationArchivesBuildItem.getRootArchive().getChildPath(importFile.get());

                    if (loadScriptPath != null && !Files.isDirectory(loadScriptPath)) {
                        // enlist resource if present
                        resourceProducer.produce(new NativeImageResourceBuildItem(importFile.get()));
                        desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, importFile.get());
                        desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES_SQL_EXTRACTOR,
                                MultipleLinesSqlCommandExtractor.class.getName());

                    } else if (hibernateConfig.sqlLoadScript.isPresent()) {
                        //raise exception if explicit file is not present (i.e. not the default)
                        throw new ConfigurationError(
                                "Unable to find file referenced in '" + HIBERNATE_ORM_CONFIG_PREFIX + "sql-load-script="
                                        + hibernateConfig.sqlLoadScript.get() + "'. Remove property or add file to your path.");
                    }
                }

                // Caching
                if (hibernateConfig.secondLevelCachingEnabled) {
                    Properties p = desc.getProperties();
                    //Only set these if the user isn't making an explicit choice:
                    p.putIfAbsent(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.TRUE);
                    p.putIfAbsent(USE_SECOND_LEVEL_CACHE, Boolean.TRUE);
                    p.putIfAbsent(USE_QUERY_CACHE, Boolean.TRUE);
                    p.putIfAbsent(JPA_SHARED_CACHE_MODE, SharedCacheMode.ENABLE_SELECTIVE);
                    Map<String, String> cacheConfigEntries = HibernateConfigUtil.getCacheConfigEntries(hibernateConfig);
                    for (Entry<String, String> entry : cacheConfigEntries.entrySet()) {
                        desc.getProperties().setProperty(entry.getKey(), entry.getValue());
                    }
                } else {
                    //Unless the global switch is explicitly set to off, in which case we disable all caching:
                    Properties p = desc.getProperties();
                    p.put(USE_DIRECT_REFERENCE_CACHE_ENTRIES, Boolean.FALSE);
                    p.put(USE_SECOND_LEVEL_CACHE, Boolean.FALSE);
                    p.put(USE_QUERY_CACHE, Boolean.FALSE);
                    p.put(JPA_SHARED_CACHE_MODE, SharedCacheMode.NONE);
                }

                persistenceUnitDescriptorProducer
                        .produce(new PersistenceUnitDescriptorBuildItem(desc, getMultiTenancyStrategy(), false));
            });
        } else {
            if (hibernateConfig.isAnyPropertySet()) {
                throw new ConfigurationError(
                        "Hibernate ORM configuration present in persistence.xml and Quarkus config file at the same time\n"
                                + "If you use persistence.xml remove all " + HIBERNATE_ORM_CONFIG_PREFIX
                                + "* properties from the Quarkus config file.");
            }
        }
    }

    @BuildStep
    public void produceLoggingCategories(BuildProducer<LogCategoryBuildItem> categories) {
        if (hibernateConfig.log.bindParam) {
            categories.produce(new LogCategoryBuildItem("org.hibernate.type.descriptor.sql.BasicBinder", Level.TRACE));
        }
    }

    @BuildStep(onlyIf = NativeBuild.class)
    public void test(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflective) {
        Collection<AnnotationInstance> annotationInstances = index.getIndex().getAnnotations(STATIC_METAMODEL);
        if (!annotationInstances.isEmpty()) {

            String[] metamodel = annotationInstances.stream()
                    .map(a -> a.target().asClass().name().toString())
                    .toArray(String[]::new);

            reflective.produce(new ReflectiveClassBuildItem(false, false, true, metamodel));
        }
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

    private void enhanceEntities(final JpaEntitiesBuildItem domainObjects,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : domainObjects.getAllModelClassNames()) {
            transformers.produce(new BytecodeTransformerBuildItem(true, i, hibernateEntityEnhancer));
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

}
