package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Produces;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.DerbyTenSevenDialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.MySQL8Dialect;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.loader.BatchFetchStyle;
import org.hibernate.service.spi.ServiceContributor;
import org.hibernate.tool.hbm2ddl.MultipleLinesSqlCommandExtractor;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.agroal.deployment.DataSourceDriverBuildItem;
import io.quarkus.agroal.deployment.DataSourceInitializedBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.arc.deployment.ResourceAnnotationBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
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
import io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL95Dialect;
import io.quarkus.runtime.LaunchMode;

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

    /**
     * Hibernate ORM configuration
     */
    HibernateOrmConfig hibernateConfig;

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
            Optional<DataSourceDriverBuildItem> driverBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceUnitDescriptorProducer,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<JpaEntitiesBuildItem> domainObjectsProducer,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener,
            List<HibernateOrmIntegrationBuildItem> integrations,
            LaunchModeBuildItem launchMode) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.HIBERNATE_ORM));

        List<ParsedPersistenceXmlDescriptor> explicitDescriptors = loadOriginalXMLParsedDescriptors();

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

        // handle the implicit persistence unit
        List<ParsedPersistenceXmlDescriptor> allDescriptors = new ArrayList<>(explicitDescriptors.size() + 1);
        allDescriptors.addAll(explicitDescriptors);
        handleHibernateORMWithNoPersistenceXml(allDescriptors, resourceProducer, systemPropertyProducer, archiveRoot,
                driverBuildItem, applicationArchivesBuildItem, launchMode.getLaunchMode());

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

        // inspect service files for additional integrators
        Collection<Class<? extends Integrator>> integratorClasses = new LinkedHashSet<>();
        for (String integratorClassName : ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/org.hibernate.integrator.spi.Integrator")) {
            integratorClasses.add((Class<? extends Integrator>) recorderContext.classProxy(integratorClassName));
        }

        // inspect service files for service contributors
        Collection<Class<? extends ServiceContributor>> serviceContributorClasses = new LinkedHashSet<>();
        for (String serviceContributorClassName : ServiceUtil.classNamesNamedIn(getClass().getClassLoader(),
                "META-INF/services/org.hibernate.service.spi.ServiceContributor")) {
            serviceContributorClasses
                    .add((Class<? extends ServiceContributor>) recorderContext.classProxy(serviceContributorClassName));
        }

        beanContainerListener
                .produce(new BeanContainerListenerBuildItem(
                        recorder.initMetadata(allDescriptors, scanner, integratorClasses, serviceContributorClasses)));
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
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) throws UnsupportedEncodingException {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        resources.produce(new GeneratedResourceBuildItem("META-INF/services/io.quarkus.arc.ResourceReferenceProvider",
                JPAResourceReferenceProvider.class.getName().getBytes("UTF-8")));
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
            Optional<DataSourceInitializedBuildItem> dataSourceInitialized,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels,
            List<HibernateOrmIntegrationRuntimeConfiguredBuildItem> integrationsRuntimeConfigured) throws Exception {
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
            List<ParsedPersistenceXmlDescriptor> descriptors,
            BuildProducer<NativeImageResourceBuildItem> resourceProducer,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            ArchiveRootBuildItem root,
            Optional<DataSourceDriverBuildItem> driverBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            LaunchMode launchMode) {
        if (descriptors.isEmpty()) {
            //we have no persistence.xml so we will create a default one
            Optional<String> dialect = hibernateConfig.dialect;
            if (!dialect.isPresent()) {
                dialect = guessDialect(driverBuildItem.map(DataSourceDriverBuildItem::getDriver));
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
                if (hibernateConfig.statistics) {
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
                        String resourceAsString = root.getArchiveRoot().relativize(loadScriptPath).toString();
                        resourceProducer.produce(new NativeImageResourceBuildItem(resourceAsString));
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
                Map<String, String> cacheConfigEntries = HibernateConfigUtil
                        .getCacheConfigEntries(hibernateConfig);
                for (Entry<String, String> entry : cacheConfigEntries.entrySet()) {
                    desc.getProperties().setProperty(entry.getKey(), entry.getValue());
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

    private Optional<String> guessDialect(Optional<String> driver) {
        // For now select the latest dialect from the driver
        // later, we can keep doing that but also avoid DCE
        // of all the dialects we want in so that people can override them
        String resolvedDriver = driver.orElse("NODRIVER");
        if (resolvedDriver.contains("postgresql")) {
            return Optional.of(QuarkusPostgreSQL95Dialect.class.getName());
        }
        if (resolvedDriver.contains("org.h2.Driver")) {
            return Optional.of(QuarkusH2Dialect.class.getName());
        }
        if (resolvedDriver.contains("org.mariadb.jdbc.Driver")) {
            return Optional.of(MariaDB103Dialect.class.getName());
        }

        if (resolvedDriver.contains("com.mysql.cj.jdbc.Driver")) {
            return Optional.of(MySQL8Dialect.class.getName());
        }
        if (resolvedDriver.contains("org.apache.derby.jdbc.ClientDriver")) {
            return Optional.of((DerbyTenSevenDialect.class.getName()));
        }

        String error = driver.isPresent()
                ? "Hibernate extension could not guess the dialect from the driver '" + resolvedDriver
                        + "'. Add an explicit '" + HIBERNATE_ORM_CONFIG_PREFIX + "dialect' property."
                : "Hibernate extension cannot guess the dialect as no JDBC driver is specified by 'quarkus.datasource.driver'";
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
                additionalClasses.produce(new GeneratedClassBuildItem(true, className, enhanced != null ? enhanced : bytes));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read Model class", e);
            }
        }
    }

    private static List<ParsedPersistenceXmlDescriptor> loadOriginalXMLParsedDescriptors() {
        // Enforce the persistence.xml configuration to be interpreted literally without
        // allowing runtime overrides;
        // (check for the runtime provided properties to be empty as well)
        Map<Object, Object> configurationOverrides = Collections.emptyMap();
        return PersistenceXmlParser.locatePersistenceUnits(configurationOverrides);
    }
}
