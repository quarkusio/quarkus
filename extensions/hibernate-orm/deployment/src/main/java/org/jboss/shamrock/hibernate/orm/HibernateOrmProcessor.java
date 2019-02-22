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

package org.jboss.shamrock.hibernate.orm;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.shamrock.deployment.annotations.ExecutionTime.STATIC_INIT;

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
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;
import org.jboss.shamrock.agroal.DataSourceDriverBuildItem;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.arc.deployment.ResourceAnnotationBuildItem;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.ShamrockConfig;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.ApplicationIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.ArchiveRootBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedClassBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.configuration.ConfigurationError;
import org.jboss.shamrock.deployment.index.IndexingUtil;
import org.jboss.shamrock.deployment.logging.LogCleanupFilterBuildItem;
import org.jboss.shamrock.deployment.recording.RecorderContext;
import org.jboss.shamrock.deployment.util.IoUtil;
import org.jboss.shamrock.hibernate.orm.runtime.DefaultEntityManagerFactoryProducer;
import org.jboss.shamrock.hibernate.orm.runtime.DefaultEntityManagerProducer;
import org.jboss.shamrock.hibernate.orm.runtime.HibernateOrmTemplate;
import org.jboss.shamrock.hibernate.orm.runtime.JPAConfig;
import org.jboss.shamrock.hibernate.orm.runtime.JPAResourceReferenceProvider;
import org.jboss.shamrock.hibernate.orm.runtime.RequestScopedEntityManagerHolder;
import org.jboss.shamrock.hibernate.orm.runtime.TransactionEntityManagers;
import org.jboss.shamrock.hibernate.orm.runtime.boot.scan.ShamrockScanner;
import org.jboss.shamrock.runtime.logging.LogCleanupFilter;

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

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    /**
     * Hibernate ORM configuration
     */
    HibernateConfig hibernate;

    @BuildStep
    void setupLogFilters(BuildProducer<LogCleanupFilterBuildItem> filters) {
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.Version", "HHH000412"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.cfg.Environment", "HHH000206"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.bytecode.enhance.spi.Enhancer", "Enhancing [%s] as"));
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.bytecode.enhance.internal.bytebuddy.BiDirectionalAssociationHandler", "Could not find"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.internal.util.LogHelper", "HHH000204"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.annotations.common.Version", "HCANN000001"));
        filters.produce(
                new LogCleanupFilterBuildItem("org.hibernate.engine.jdbc.env.internal.LobCreatorBuilderImpl", "HHH000422"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.dialect.Dialect", "HHH000400"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.type.BasicTypeRegistry", "HHH000270"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.orm.beans", "HHH10005002"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tuple.PojoInstantiator", "HHH000182"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tuple.entity.EntityMetamodel", "HHH000157"));
        filters.produce(new LogCleanupFilterBuildItem(
                "org.hibernate.engine.transaction.jta.platform.internal.JtaPlatformInitiator", "HHH000490"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.tool.schema.internal.SchemaCreatorImpl", "HHH000476"));
        filters.produce(
                new LogCleanupFilterBuildItem("org.hibernate.hql.internal.QueryTranslatorFactoryInitiator", "HHH000397"));
        filters.produce(new LogCleanupFilterBuildItem("org.hibernate.jpa.boot.internal.PersistenceXmlParser", "HHH000318"));
    }

    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/persistence.xml");
    }

    @BuildStep
    void doParseAndRegisterSubstrateResources(BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceProducer,
            BuildProducer<SubstrateResourceBuildItem> resourceProducer,
            BuildProducer<HotDeploymentConfigFileBuildItem> hotDeploymentProducer,
            ArchiveRootBuildItem root,
            ApplicationArchivesBuildItem applicationArchivesBuildItem,
            Optional<DataSourceDriverBuildItem> driverBuildItem) throws IOException {
        List<ParsedPersistenceXmlDescriptor> descriptors = loadOriginalXMLParsedDescriptors();
        handleHibernateORMWithNoPersistenceXml(descriptors, resourceProducer, hotDeploymentProducer, root, driverBuildItem,
                applicationArchivesBuildItem);
        for (ParsedPersistenceXmlDescriptor i : descriptors) {
            persistenceProducer.produce(new PersistenceUnitDescriptorBuildItem(i));
        }
    }

    @BuildStep
    void handleNativeImageImportSql(BuildProducer<SubstrateResourceBuildItem> resources,
            List<PersistenceUnitDescriptorBuildItem> descriptors) {
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
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans, CombinedIndexBuildItem combinedIndex,
            List<PersistenceUnitDescriptorBuildItem> descriptors) {
        additionalBeans.produce(new AdditionalBeanBuildItem(false, JPAConfig.class, TransactionEntityManagers.class,
                RequestScopedEntityManagerHolder.class));

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
    void setupResourceInjection(BuildProducer<ResourceAnnotationBuildItem> resourceAnnotations, Capabilities capabilities,
            BuildProducer<GeneratedResourceBuildItem> resources) {
        resources.produce(new GeneratedResourceBuildItem("META-INF/services/org.jboss.protean.arc.ResourceReferenceProvider",
                JPAResourceReferenceProvider.class.getName().getBytes()));
        resourceAnnotations.produce(new ResourceAnnotationBuildItem(PERSISTENCE_CONTEXT));
        resourceAnnotations.produce(new ResourceAnnotationBuildItem(PERSISTENCE_UNIT));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public BeanContainerListenerBuildItem build(RecorderContext recorder, HibernateOrmTemplate template,
            List<PersistenceUnitDescriptorBuildItem> descItems,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<NonJpaModelBuildItem> nonJpaModelBuildItems,
            CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<JpaEntitiesBuildItems> domainObjectsProducer) throws Exception {

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
        final JpaEntitiesBuildItems domainObjects = scavenger.discoverModelAndRegisterForReflection();

        for (String className : domainObjects.getClassNames()) {
            template.addEntity(className);
        }
        template.enlistPersistenceUnit();
        template.callHibernateFeatureInit();

        // remember how to run the enhancers later
        domainObjectsProducer.produce(domainObjects);

        //set up the scanner, as this scanning has already been done we need to just tell it about the classes we
        //have discovered. This scanner is bytecode serializable and is passed directly into the template
        ShamrockScanner scanner = new ShamrockScanner();
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String i : domainObjects.getClassNames()) {
            ShamrockScanner.ClassDescriptorImpl desc = new ShamrockScanner.ClassDescriptorImpl(i,
                    ClassDescriptor.Categorization.MODEL);
            classDescriptors.add(desc);
        }
        scanner.setClassDescriptors(classDescriptors);

        //now we serialize the XML and class list to bytecode, to remove the need to re-parse the XML on JVM startup
        recorder.registerNonDefaultConstructor(ParsedPersistenceXmlDescriptor.class.getDeclaredConstructor(URL.class),
                (i) -> Collections.singletonList(i.getPersistenceUnitRootUrl()));
        return new BeanContainerListenerBuildItem(template.initMetadata(descriptors, scanner));
    }

    @BuildStep
    public HibernateEnhancersRegisteredBuildItem enhancerDomainObjects(JpaEntitiesBuildItems domainObjects,
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
            List<PersistenceUnitDescriptorBuildItem> descriptors) throws Exception {

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
    public void startPersistenceUnits(HibernateOrmTemplate template, BeanContainerBuildItem beanContainer) throws Exception {
        template.startAllPersistenceUnits(beanContainer.getValue());
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
            ArchiveRootBuildItem root,
            Optional<DataSourceDriverBuildItem> driverBuildItem,
            ApplicationArchivesBuildItem applicationArchivesBuildItem) {
        if (descriptors.isEmpty()) {
            //we have no persistence.xml so we will create a default one
            Optional<String> dialect = hibernate.dialect;
            if (!dialect.isPresent()) {
                dialect = guessDialect(driverBuildItem.map(DataSourceDriverBuildItem::getDriver));
            }
            dialect.ifPresent(s -> {
                // we found one
                ParsedPersistenceXmlDescriptor desc = new ParsedPersistenceXmlDescriptor(null); //todo URL
                desc.setName("default");
                desc.setTransactionType(PersistenceUnitTransactionType.JTA);
                desc.getProperties().setProperty(AvailableSettings.DIALECT, s);
                hibernate.schemaGeneration.ifPresent(
                        p -> desc.getProperties().setProperty(AvailableSettings.HBM2DDL_DATABASE_ACTION, p));
                if (hibernate.showSql) {
                    desc.getProperties().setProperty(AvailableSettings.SHOW_SQL, "true");
                    desc.getProperties().setProperty(AvailableSettings.FORMAT_SQL, "true");
                }
                if (hibernate.generateStatistics) {
                    desc.getProperties().setProperty(AvailableSettings.GENERATE_STATISTICS, "true");
                }

                // sql-load-script-source
                // explicit file or default one
                String file = hibernate.sqlLoadScriptSource.orElse("import.sql"); //default Hibernate ORM file imported

                Optional<Path> loadScriptPath = Optional
                        .ofNullable(applicationArchivesBuildItem.getRootArchive().getChildPath(file));
                // enlist resource if present
                loadScriptPath
                        .filter(path -> !Files.isDirectory(path))
                        .ifPresent(path -> {
                            String resourceAsString = root.getPath().relativize(loadScriptPath.get()).toString();
                            resourceProducer.produce(new SubstrateResourceBuildItem(resourceAsString));
                            hotDeploymentProducer.produce(new HotDeploymentConfigFileBuildItem(resourceAsString));
                            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE, file);
                        });

                //raise exception if explicit file is not present (i.e. not the default)
                hibernate.sqlLoadScriptSource
                        .filter(o -> !loadScriptPath.filter(path -> !Files.isDirectory(path)).isPresent())
                        .ifPresent(
                                c -> {
                                    throw new ConfigurationError(
                                            "Unable to find file referenced in 'shamrock.hibernate.sql-load-script-source="
                                                    + c + "'. Remove property or add file to your path.");
                                });

                String prefix = "shamrock.hibernate.cache.";
                for (String propName : ConfigProvider.getConfig().getPropertyNames()) {
                    if (propName.startsWith(prefix)) {
                        String value = ShamrockConfig.getString(propName, null, false);
                        String hibernateKey = propName.replace("shamrock.", "");
                        desc.getProperties().setProperty(hibernateKey, value);
                    }
                }

                descriptors.add(desc);
            });
        } else {
            if (hibernate.isAnyPropertySet()) {
                throw new ConfigurationError(
                        "Hibernate ORM configuration present in persistence.xml and Shamrock config file at the same time\n"
                                + "If you use persistence.xml remove all shamrock.hibernate.* properties from the Shamrock config file.");
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
                        + "'. Add an explicit 'shamrock.hibernate.dialect' property."
                : "Hibernate extension cannot guess the dialect as no JDBC driver is specified by 'shamrock.datasource.driver'";
        throw new ConfigurationError(error);
    }

    private void enhanceEntities(final JpaEntitiesBuildItems domainObjects,
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
