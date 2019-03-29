/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.hibernate.orm.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.inject.Produces;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.spi.PersistenceUnitTransactionType;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.hibernate.loader.BatchFetchStyle;
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
import io.quarkus.deployment.QuarkusConfig;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.ArchiveRootBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentConfigFileBuildItem;
import io.quarkus.deployment.builditem.SystemPropertyBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.configuration.ConfigurationError;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.deployment.util.IoUtil;
import io.quarkus.hibernate.orm.runtime.DefaultEntityManagerFactoryProducer;
import io.quarkus.hibernate.orm.runtime.DefaultEntityManagerProducer;
import io.quarkus.hibernate.orm.runtime.HibernateOrmTemplate;
import io.quarkus.hibernate.orm.runtime.JPAConfig;
import io.quarkus.hibernate.orm.runtime.JPAResourceReferenceProvider;
import io.quarkus.hibernate.orm.runtime.RequestScopedEntityManagerHolder;
import io.quarkus.hibernate.orm.runtime.TransactionEntityManagers;
import io.quarkus.hibernate.orm.runtime.boot.scan.QuarkusScanner;

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

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    /**
     * Hibernate ORM configuration
     */
    HibernateOrmConfig hibernateConfig;

    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/persistence.xml");
    }

    @BuildStep
    void doParseAndRegisterSubstrateResources(BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceProducer,
            BuildProducer<SubstrateResourceBuildItem> resourceProducer,
            BuildProducer<HotDeploymentConfigFileBuildItem> hotDeploymentProducer,
            BuildProducer<SystemPropertyBuildItem> systemPropertyProducer,
            ArchiveRootBuildItem root,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            Optional<DataSourceDriverBuildItem> driverBuildItem) throws IOException {
        List<ParsedPersistenceXmlDescriptor> descriptors = loadOriginalXMLParsedDescriptors();
        handleHibernateORMWithNoPersistenceXml(descriptors, resourceProducer, hotDeploymentProducer, systemPropertyProducer,
                root, driverBuildItem, applicationArchivesBuildItem);
        for (ParsedPersistenceXmlDescriptor i : descriptors) {
            persistenceProducer.produce(new PersistenceUnitDescriptorBuildItem(i));
        }
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(RecorderContext recorder, HibernateOrmTemplate template,
            List<PersistenceUnitDescriptorBuildItem> descItems,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<JpaEntitiesBuildItem> domainObjectsProducer,
            BuildProducer<BeanContainerListenerBuildItem> beanContainerListener) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.HIBERNATE_ORM));

        List<ParsedPersistenceXmlDescriptor> descriptors = descItems.stream()
                .map(PersistenceUnitDescriptorBuildItem::getDescriptor).collect(Collectors.toList());

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
        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, descriptors, compositeIndex, nonJpaModelClasses);
        final JpaEntitiesBuildItem domainObjects = scavenger.discoverModelAndRegisterForReflection();

        // remember how to run the enhancers later
        domainObjectsProducer.produce(domainObjects);

        if (!hasEntities(domainObjects, nonJpaModelBuildItems)) {
            // we can bail out early
            return;
        }

        for (String className : domainObjects.getClassNames()) {
            template.addEntity(className);
        }
        template.enlistPersistenceUnit();
        template.callHibernateFeatureInit();

        //set up the scanner, as this scanning has already been done we need to just tell it about the classes we
        //have discovered. This scanner is bytecode serializable and is passed directly into the template
        QuarkusScanner scanner = new QuarkusScanner();
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String i : domainObjects.getClassNames()) {
            QuarkusScanner.ClassDescriptorImpl desc = new QuarkusScanner.ClassDescriptorImpl(i,
                    ClassDescriptor.Categorization.MODEL);
            classDescriptors.add(desc);
        }
        scanner.setClassDescriptors(classDescriptors);

        //now we serialize the XML and class list to bytecode, to remove the need to re-parse the XML on JVM startup
        recorder.registerNonDefaultConstructor(ParsedPersistenceXmlDescriptor.class.getDeclaredConstructor(URL.class),
                (i) -> Collections.singletonList(i.getPersistenceUnitRootUrl()));
        beanContainerListener.produce(new BeanContainerListenerBuildItem(template.initMetadata(descriptors, scanner)));
    }

    @BuildStep
    void handleNativeImageImportSql(BuildProducer<SubstrateResourceBuildItem> resources,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        for (PersistenceUnitDescriptorBuildItem i : descriptors) {
            //add resources
            if (i.getDescriptor().getProperties().containsKey("javax.persistence.sql-load-script-source")) {
                resources.produce(new SubstrateResourceBuildItem(
                        (String) i.getDescriptor().getProperties().get("javax.persistence.sql-load-script-source")));
            } else {
                resources.produce(new SubstrateResourceBuildItem("import.sql"));
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
                JPAResourceReferenceProvider.class.getName().getBytes()));
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
    public void build(HibernateOrmTemplate template,
            Capabilities capabilities, BuildProducer<BeanContainerListenerBuildItem> buildProducer,
            List<PersistenceUnitDescriptorBuildItem> descriptors,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) throws Exception {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        buildProducer.produce(new BeanContainerListenerBuildItem(
                template.initializeJpa(capabilities.isCapabilityPresent(Capabilities.TRANSACTIONS))));
        // Bootstrap all persistence units
        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : descriptors) {
            buildProducer.produce(new BeanContainerListenerBuildItem(
                    template.registerPersistenceUnit(persistenceUnitDescriptor.getDescriptor().getName())));
        }
        buildProducer.produce(new BeanContainerListenerBuildItem(template.initDefaultPersistenceUnit()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void startPersistenceUnits(HibernateOrmTemplate template, BeanContainerBuildItem beanContainer,
            Optional<DataSourceInitializedBuildItem> dataSourceInitialized,
            JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) throws Exception {
        if (!hasEntities(jpaEntities, nonJpaModels)) {
            return;
        }

        template.startAllPersistenceUnits(beanContainer.getValue());
    }

    private boolean hasEntities(JpaEntitiesBuildItem jpaEntities, List<NonJpaModelBuildItem> nonJpaModels) {
        return !jpaEntities.getClassNames().isEmpty() || !nonJpaModels.isEmpty();
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
            BuildProducer<SubstrateResourceBuildItem> resourceProducer,
            BuildProducer<HotDeploymentConfigFileBuildItem> hotDeploymentProducer,
            BuildProducer<SystemPropertyBuildItem> systemProperty,
            ArchiveRootBuildItem root,
            Optional<DataSourceDriverBuildItem> driverBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem) {
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
                hibernateConfig.database.generation.ifPresent(
                        action -> desc.getProperties().setProperty(AvailableSettings.HBM2DDL_DATABASE_ACTION, action));

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
                // explicit file or default one
                String file = hibernateConfig.sqlLoadScript.orElse("import.sql"); //default Hibernate ORM file imported

                Optional<Path> loadScriptPath = Optional
                        .ofNullable(applicationArchivesBuildItem.getRootArchive().getChildPath(file));
                // enlist resource if present
                loadScriptPath
                        .filter(path -> !Files.isDirectory(path))
                        .ifPresent(path -> {
                            String resourceAsString = root.getPath().relativize(loadScriptPath.get()).toString();
                            resourceProducer.produce(new SubstrateResourceBuildItem(resourceAsString));
                            hotDeploymentProducer.produce(new HotDeploymentConfigFileBuildItem(resourceAsString));
                            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_IMPORT_FILES, file);
                        });

                //raise exception if explicit file is not present (i.e. not the default)
                hibernateConfig.sqlLoadScript
                        .filter(o -> !loadScriptPath.filter(path -> !Files.isDirectory(path)).isPresent())
                        .ifPresent(
                                c -> {
                                    throw new ConfigurationError(
                                            "Unable to find file referenced in '" + HIBERNATE_ORM_CONFIG_PREFIX
                                                    + "sql-load-script="
                                                    + c + "'. Remove property or add file to your path.");
                                });

                // Caching
                // FIXME: this should use a Map as soon as Map support is complete
                String prefix = HIBERNATE_ORM_CONFIG_PREFIX + "cache.";
                for (String propName : ConfigProvider.getConfig().getPropertyNames()) {
                    if (propName.startsWith(prefix)) {
                        String value = QuarkusConfig.getString(propName, null, false);
                        String hibernateKey = propName.replace(HIBERNATE_ORM_CONFIG_PREFIX, "hibernate.")
                                .replace("\"", "");
                        desc.getProperties().setProperty(hibernateKey, value);
                    }
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
            return Optional.of(PostgreSQL95Dialect.class.getName());
        }
        if (resolvedDriver.contains("org.h2.Driver")) {
            return Optional.of(H2Dialect.class.getName());
        }
        if (resolvedDriver.contains("org.mariadb.jdbc.Driver")) {
            return Optional.of(MariaDB103Dialect.class.getName());
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
        for (String i : domainObjects.getClassNames()) {
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
