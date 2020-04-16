package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;
import static org.hibernate.cfg.AvailableSettings.JPA_SHARED_CACHE_MODE;
import static org.hibernate.cfg.AvailableSettings.USE_DIRECT_REFERENCE_CACHE_ENTRIES;
import static org.hibernate.cfg.AvailableSettings.USE_QUERY_CACHE;
import static org.hibernate.cfg.AvailableSettings.USE_SECOND_LEVEL_CACHE;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URL;
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
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.MetricType;
import org.hibernate.annotations.Proxy;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.bytecode.internal.bytebuddy.BytecodeProviderImpl;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.dialect.SQLServer2012Dialect;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;
import org.hibernate.service.spi.ServiceContributor;
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
import io.quarkus.datasource.common.runtime.DatabaseKind;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
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
import io.quarkus.hibernate.orm.runtime.boot.scan.QuarkusScanner;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect;
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect;
import io.quarkus.hibernate.orm.runtime.metrics.HibernateCounter;
import io.quarkus.hibernate.orm.runtime.proxies.PreGeneratedProxies;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.smallrye.metrics.deployment.spi.MetricBuildItem;
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

    private static final String HIBERNATE_ORM_CONFIG_PREFIX = "quarkus.hibernate-orm.";
    private static final String NO_SQL_LOAD_SCRIPT_FILE = "no-file";

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    private static final String INTEGRATOR_SERVICE_FILE = "META-INF/services/org.hibernate.integrator.spi.Integrator";
    private static final String SERVICE_CONTRIBUTOR_SERVICE_FILE = "META-INF/services/org.hibernate.service.spi.ServiceContributor";

    /**
     * Hibernate ORM configuration
     */
    HibernateOrmConfig hibernateConfig;

    @BuildStep
    CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capabilities.HIBERNATE_ORM);
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
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem("META-INF/persistence.xml"));
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem(INTEGRATOR_SERVICE_FILE));
        watchedFiles.add(new HotDeploymentWatchedFileBuildItem(SERVICE_CONTRIBUTOR_SERVICE_FILE));

        getSqlLoadScript(launchMode.getLaunchMode()).ifPresent(script -> {
            watchedFiles.add(new HotDeploymentWatchedFileBuildItem(script));
        });
        return watchedFiles;
    }

    @SuppressWarnings("unchecked")
    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorderContext, HibernateOrmRecorder recorder,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            List<IgnorableNonIndexedClasses> ignorableNonIndexedClassesBuildItems,
            CombinedIndexBuildItem index,
            ArchiveRootBuildItem archiveRoot,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            List<JdbcDataSourceBuildItem> jdbcDataSourcesBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<JpaEntitiesBuildItem> domainObjectsProducer,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            BuildProducer<GeneratedClassBuildItem> generatedClassBuildItemBuildProducer,
            List<HibernateOrmIntegrationBuildItem> integrations,
            LaunchModeBuildItem launchMode) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.HIBERNATE_ORM));

        List<ParsedPersistenceXmlDescriptor> explicitDescriptors = QuarkusPersistenceXmlParser.locatePersistenceUnits();

        // build a composite index with additional jpa model classes
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        for (AdditionalJpaModelBuildItem jpaModel : additionalJpaModelBuildItems) {
            IndexingUtil.indexClass(jpaModel.getClassName(), indexer, index.getIndex(), additionalIndex,
                    HibernateOrmProcessor.class.getClassLoader());
        }
        CompositeIndex compositeIndex = CompositeIndex.create(index.getIndex(), indexer.complete());

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

        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, explicitDescriptors, compositeIndex,
                nonJpaModelClasses, ignorableNonIndexedClasses);
        final JpaEntitiesBuildItem domainObjects = scavenger.discoverModelAndRegisterForReflection();

        // remember how to run the enhancers later
        domainObjectsProducer.produce(domainObjects);

        final boolean enableORM = hasEntities(domainObjects, nonJpaModelBuildItems);
        recorder.callHibernateFeatureInit(enableORM);

        if (!enableORM) {
            // we can bail out early
            return;
        }

        // we only support the default datasource for now
        Optional<JdbcDataSourceBuildItem> defaultJdbcDataSourceBuildItem = jdbcDataSourcesBuildItem.stream()
                .filter(i -> i.isDefault())
                .findFirst();

        // handle the implicit persistence unit
        List<ParsedPersistenceXmlDescriptor> allDescriptors = new ArrayList<>(explicitDescriptors.size() + 1);
        allDescriptors.addAll(explicitDescriptors);
        handleHibernateORMWithNoPersistenceXml(allDescriptors, resourceProducer, systemPropertyProducer, archiveRoot,
                defaultJdbcDataSourceBuildItem, applicationArchivesBuildItem, launchMode.getLaunchMode());

        for (ParsedPersistenceXmlDescriptor descriptor : allDescriptors) {
            persistenceUnitDescriptorProducer.produce(new PersistenceUnitDescriptorBuildItem(descriptor));
        }

        for (String className : domainObjects.getEntityClassNames()) {
            recorder.addEntity(className);
        }
        recorder.enlistPersistenceUnit();

        //set up the scanner, as this scanning has already been done we need to just tell it about the classes we
        //have discovered. This scanner is bytecode serializable and is passed directly into the recorder
        QuarkusScanner scanner = new QuarkusScanner();
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String i : domainObjects.getAllModelClassNames()) {
            QuarkusScanner.ClassDescriptorImpl desc = new QuarkusScanner.ClassDescriptorImpl(i,
                    ClassDescriptor.Categorization.MODEL);
            classDescriptors.add(desc);
        }
        scanner.setClassDescriptors(classDescriptors);

        //now we serialize the XML and class list to bytecode, to remove the need to re-parse the XML on JVM startup
        recorderContext.registerNonDefaultConstructor(ParsedPersistenceXmlDescriptor.class.getDeclaredConstructor(URL.class),
                (i) -> Collections.singletonList(i.getPersistenceUnitRootUrl()));

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        // inspect service files for additional integrators
        Collection<Class<? extends Integrator>> integratorClasses = new LinkedHashSet<>();
        for (String integratorClassName : ServiceUtil.classNamesNamedIn(classLoader, INTEGRATOR_SERVICE_FILE)) {
            integratorClasses.add((Class<? extends Integrator>) recorderContext.classProxy(integratorClassName));
        }
        // inspect service files for service contributors
        Collection<Class<? extends ServiceContributor>> serviceContributorClasses = new LinkedHashSet<>();
        for (String serviceContributorClassName : ServiceUtil.classNamesNamedIn(classLoader,
                SERVICE_CONTRIBUTOR_SERVICE_FILE)) {
            serviceContributorClasses
                    .add((Class<? extends ServiceContributor>) recorderContext.classProxy(serviceContributorClassName));
        }

        Set<String> entitiesToGenerateProxiesFor = new HashSet<>(domainObjects.getEntityClassNames());
        for (ParsedPersistenceXmlDescriptor unit : allDescriptors) {
            entitiesToGenerateProxiesFor.addAll(unit.getManagedClassNames());
        }
        PreGeneratedProxies proxyDefinitions = generatedProxies(entitiesToGenerateProxiesFor, compositeIndex,
                generatedClassBuildItemBuildProducer);
        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initMetadata(allDescriptors, scanner, integratorClasses, serviceContributorClasses,
                                proxyDefinitions)));
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
        try {

            final BytecodeProviderImpl bytecodeProvider = new BytecodeProviderImpl();
            final ByteBuddyProxyHelper byteBuddyProxyHelper = bytecodeProvider.getByteBuddyProxyHelper();

            for (String entity : entityClassNames) {
                Set<Class<?>> proxyInterfaces = new HashSet<>();
                proxyInterfaces.add(HibernateProxy.class); //always added
                Class<?> mappedClass = Class.forName(entity, false, Thread.currentThread().getContextClassLoader());
                String proxy = proxyAnnotations.get(entity);
                if (proxy != null) {
                    proxyInterfaces.add(Class.forName(proxy, false, Thread.currentThread().getContextClassLoader()));
                } else if (!isProxiable(mappedClass)) {
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
                        proxyInterfaces.add(Class.forName(proxy, false, Thread.currentThread().getContextClassLoader()));
                    }
                }
                DynamicType.Unloaded<?> proxyDef = byteBuddyProxyHelper.buildUnloadedProxy(mappedClass,
                        toArray(proxyInterfaces));

                for (Entry<TypeDescription, byte[]> i : proxyDef.getAllTypes().entrySet()) {
                    generatedClassBuildItemBuildProducer
                            .produce(new GeneratedClassBuildItem(true, i.getKey().getName(), i.getValue()));
                }
                preGeneratedProxies.getProxies().put(entity,
                        new PreGeneratedProxies.ProxyClassDetailsHolder(proxyDef.getTypeDescription().getName(),
                                proxyInterfaces.stream().map(Class::getName).collect(Collectors.toSet())));
            }

        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return preGeneratedProxies;
    }

    private boolean isProxiable(Class<?> mappedClass) {
        if (Modifier.isFinal(mappedClass.getModifiers())) {
            return false;
        }
        try {
            mappedClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            return false;
        }
        return true;
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
            if (i.getDescriptor().getProperties().containsKey("javax.persistence.sql-load-script-source")) {
                resources.produce(new NativeImageResourceBuildItem(
                        (String) i.getDescriptor().getProperties().get("javax.persistence.sql-load-script-source")));
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
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans, CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        additionalBeans.produce(AdditionalBeanBuildItem.builder().setUnremovable()
                .addBeanClasses(JPAConfig.class, TransactionEntityManagers.class,
                        RequestScopedEntityManagerHolder.class)
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

        buildProducer.produce(new BeanContainerListenerBuildItem(
                recorder.initializeJpa(capabilities.isCapabilityPresent(Capabilities.TRANSACTIONS))));
        // Bootstrap all persistence units
        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : descriptors) {
            buildProducer.produce(new BeanContainerListenerBuildItem(
                    recorder.registerPersistenceUnit(persistenceUnitDescriptor.getDescriptor().getName())));
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

    @BuildStep
    public void metrics(HibernateOrmConfig config,
            BuildProducer<MetricBuildItem> metrics) {
        // TODO: When multiple PUs are supported, create metrics for each PU. For now we only assume the "default" PU.
        boolean metricsEnabled = config.metricsEnabled && config.statistics.orElse(true);
        metrics.produce(createMetricBuildItem("hibernate-orm.sessions.open",
                "Global number of sessions opened",
                "sessionsOpened",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.sessions.closed",
                "Global number of sessions closed",
                "sessionsClosed",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.sessions.closed",
                "Global number of sessions closed",
                "sessionsClosed",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.transactions",
                "The number of transactions we know to have completed",
                "transactionCount",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.transactions.successful",
                "The number of transactions we know to have been successful",
                "successfulTransactions",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.optimistic.lock.failures",
                "The number of Hibernate StaleObjectStateExceptions or JPA OptimisticLockExceptions that occurred.",
                "optimisticLockFailures",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.flushes",
                "Global number of flush operations executed (either manual or automatic).",
                "flushes",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.connections.obtained",
                "Get the global number of connections asked by the sessions " +
                        "(the actual number of connections used may be much smaller depending " +
                        "whether you use a connection pool or not)",
                "connectionsObtained",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.statements.prepared",
                "The number of prepared statements that were acquired",
                "statementsPrepared",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.statements.closed",
                "The number of prepared statements that were released",
                "statementsClosed",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.second-level-cache.puts",
                "Global number of cacheable entities/collections put in the cache",
                "secondLevelCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.second-level-cache.hits",
                "Global number of cacheable entities/collections successfully retrieved from the cache",
                "secondLevelCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.second-level-cache.misses",
                "Global number of cacheable entities/collections not found in the cache and loaded from the database.",
                "secondLevelCacheMisses",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.loaded",
                "Global number of entity loads",
                "entitiesLoaded",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.updated",
                "Global number of entity updates",
                "entitiesUpdated",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.inserted",
                "Global number of entity inserts",
                "entitiesInserted",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.deleted",
                "Global number of entity deletes",
                "entitiesDeleted",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.entities.fetched",
                "Global number of entity fetches",
                "entitiesFetched",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.loaded",
                "Global number of collections loaded",
                "collectionsLoaded",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.updated",
                "Global number of collections updated",
                "collectionsUpdated",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.removed",
                "Global number of collections removed",
                "collectionsRemoved",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.recreated",
                "Global number of collections recreated",
                "collectionsRecreated",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.collections.fetched",
                "Global number of collections fetched",
                "collectionsFetched",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.queries.executions",
                "Global number of natural id queries executed against the database",
                "naturalIdQueriesExecutedToDatabase",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.cache.hits",
                "Global number of cached natural id lookups successfully retrieved from cache",
                "naturalIdCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.cache.puts",
                "Global number of cacheable natural id lookups put in cache",
                "naturalIdCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.natural-id.cache.misses",
                "Global number of cached natural id lookups *not* found in cache",
                "naturalIdCacheMisses",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.queries.executed",
                "Global number of executed queries",
                "queriesExecutedToDatabase",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.query-cache.puts",
                "Global number of cacheable queries put in cache",
                "queryCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.query-cache.hits",
                "Global number of cached queries successfully retrieved from cache",
                "queryCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.query-cache.misses",
                "Global number of cached queries *not* found in cache",
                "queryCacheMisses",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.timestamps-cache.puts",
                "Global number of timestamps put in cache",
                "updateTimestampsCachePuts",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.timestamps-cache.hits",
                "Global number of timestamps successfully retrieved from cache",
                "updateTimestampsCacheHits",
                metricsEnabled));
        metrics.produce(createMetricBuildItem("hibernate-orm.timestamps-cache.misses",
                "Global number of timestamp requests that were not found in the cache",
                "updateTimestampsCacheMisses",
                metricsEnabled));
    }

    private MetricBuildItem createMetricBuildItem(String metricName, String description, String metric,
            boolean metricsEnabled) {
        return new MetricBuildItem(Metadata.builder()
                .withName(metricName)
                .withDescription(description)
                .withType(MetricType.COUNTER)
                .build(),
                new HibernateCounter("default", metric),
                metricsEnabled,
                "hibernate-orm");
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
            List<ParsedPersistenceXmlDescriptor> descriptors,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            ArchiveRootBuildItem root,
            Optional<JdbcDataSourceBuildItem> driverBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode) {
        if (descriptors.isEmpty()) {
            //we have no persistence.xml so we will create a default one
            Optional<String> dialect = hibernateConfig.dialect;
            if (!dialect.isPresent()) {
                dialect = guessDialect(driverBuildItem.map(JdbcDataSourceBuildItem::getDbKind));
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

                hibernateConfig.database.charset.ifPresent(
                        charset -> desc.getProperties().setProperty(AvailableSettings.HBM2DDL_CHARSET_NAME, charset));

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

                descriptors.add(desc);
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

    private Optional<String> guessDialect(Optional<String> dbKind) {
        // For now select the latest dialect from the driver
        // later, we can keep doing that but also avoid DCE
        // of all the dialects we want in so that people can override them
        String resolvedDbKind = dbKind.orElse("NO_DATABASE_KIND");
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
            return Optional.of((DerbyTenSevenDialect.class.getName()));
        }
        if (DatabaseKind.isMsSQL(resolvedDbKind)) {
            return Optional.of((SQLServer2012Dialect.class.getName()));
        }

        String error = dbKind.isPresent()
                ? "Hibernate extension could not guess the dialect from the database kind '" + resolvedDbKind
                        + "'. Add an explicit '" + HIBERNATE_ORM_CONFIG_PREFIX + "dialect' property."
                : "Hibernate extension cannot guess the dialect as no database kind is specified by 'quarkus.datasource.db-kind'";
        throw new ConfigurationError(error);
    }

    private void enhanceEntities(final JpaEntitiesBuildItem domainObjects,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            BuildProducer<GeneratedClassBuildItem> additionalClasses) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : domainObjects.getAllModelClassNames()) {
            transformers.produce(new BytecodeTransformerBuildItem(i, hibernateEntityEnhancer));
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
