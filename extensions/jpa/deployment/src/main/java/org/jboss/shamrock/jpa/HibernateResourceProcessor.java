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

package org.jboss.shamrock.jpa;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.boot.archive.scan.spi.ClassDescriptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.dialect.PostgreSQL95Dialect;
import org.hibernate.jpa.boot.internal.ParsedPersistenceXmlDescriptor;
import org.hibernate.jpa.boot.internal.PersistenceXmlParser;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.arc.deployment.ResourceAnnotationBuildItem;
import org.jboss.shamrock.deployment.Capabilities;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.ArchiveRootBuildItem;
import org.jboss.shamrock.deployment.builditem.BytecodeTransformerBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.GeneratedResourceBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.ReflectiveClassBuildItem;
import org.jboss.shamrock.deployment.builditem.substrate.SubstrateResourceBuildItem;
import org.jboss.shamrock.deployment.configuration.ConfigurationError;
import org.jboss.shamrock.deployment.recording.RecorderContext;


import org.jboss.shamrock.jpa.runtime.DefaultEntityManagerFactoryProducer;
import org.jboss.shamrock.jpa.runtime.DefaultEntityManagerProducer;
import org.jboss.shamrock.jpa.runtime.JPAConfig;
import org.jboss.shamrock.jpa.runtime.JPADeploymentTemplate;
import org.jboss.shamrock.jpa.runtime.TransactionEntityManagers;
import org.jboss.shamrock.jpa.runtime.boot.scan.ShamrockScanner;

import javax.enterprise.inject.Produces;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;
import javax.persistence.spi.PersistenceUnitTransactionType;
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

import static org.jboss.shamrock.annotations.ExecutionTime.RUNTIME_INIT;
import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

/**
 * Simulacrum of JPA bootstrap.
 * <p>
 * This does not address the proper integration with Hibernate
 * Rather prepare the path to providing the right metadata
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Sanne Grinovero  <sanne@hibernate.org>
 */
public final class HibernateResourceProcessor {

    private static final DotName PERSISTENCE_CONTEXT = DotName.createSimple(PersistenceContext.class.getName());
    private static final DotName PERSISTENCE_UNIT = DotName.createSimple(PersistenceUnit.class.getName());
    private static final DotName PRODUCES = DotName.createSimple(Produces.class.getName());

    /**
     * TODO why document this, is it exposed
     */
    @ConfigProperty(name = "shamrock.datasource.driver")
    Optional<String> driver;

    /**
     * Hibernate ORM configuration
     */
    @ConfigProperty(name = "shamrock.hibernate")
    Optional<HibernateOrmConfig> hibernateOrmConfig;

    @BuildStep
    HotDeploymentConfigFileBuildItem configFile() {
        return new HotDeploymentConfigFileBuildItem("META-INF/persistence.xml");
    }

    @BuildStep
    void doParseAndRegisterSubstrateResources(BuildProducer<PersistenceUnitDescriptorBuildItem> persistenceProducer,
                                              BuildProducer<SubstrateResourceBuildItem> resourceProducer,
                                              BuildProducer<HotDeploymentConfigFileBuildItem> hotDeploymentProducer,
                                              ArchiveRootBuildItem root,
                                              ApplicationArchivesBuildItem applicationArchivesBuildItem) throws IOException {
        List<ParsedPersistenceXmlDescriptor> descriptors = loadOriginalXMLParsedDescriptors();
        handleHibernateORMWithNoPersistenceXml(descriptors, resourceProducer, hotDeploymentProducer, root, applicationArchivesBuildItem );
        for (ParsedPersistenceXmlDescriptor i : descriptors) {
            persistenceProducer.produce(new PersistenceUnitDescriptorBuildItem(i));
        }
    }

    @BuildStep
    void handleNativeImageImportSql(BuildProducer<SubstrateResourceBuildItem> resources, List<PersistenceUnitDescriptorBuildItem> descriptors) {
        for (PersistenceUnitDescriptorBuildItem i : descriptors) {
            //add resources
            if (i.getDescriptor().getProperties().containsKey("javax.persistence.sql-load-script-source")) {
                resources.produce(new SubstrateResourceBuildItem((String) i.getDescriptor().getProperties().get("javax.persistence.sql-load-script-source")));
            } else {
                resources.produce(new SubstrateResourceBuildItem("import.sql"));
            }
        }
    }

    @BuildStep
    void registerBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans, CombinedIndexBuildItem combinedIndex, List<PersistenceUnitDescriptorBuildItem> descriptors) {
        additionalBeans.produce(new AdditionalBeanBuildItem(JPAConfig.class, TransactionEntityManagers.class));

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
                "org.jboss.shamrock.jpa.runtime.JPAResourceReferenceProvider".getBytes()));
        resourceAnnotations.produce(new ResourceAnnotationBuildItem(PERSISTENCE_CONTEXT));
        resourceAnnotations.produce(new ResourceAnnotationBuildItem(PERSISTENCE_UNIT));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public BeanContainerListenerBuildItem build(RecorderContext recorder, JPADeploymentTemplate template,
                                                List<PersistenceUnitDescriptorBuildItem> descItems, CombinedIndexBuildItem index,
                                                BuildProducer<BytecodeTransformerBuildItem> transformers,
                                                BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
                                                BuildProducer<FeatureBuildItem> feature) throws Exception {

        feature.produce(new FeatureBuildItem(FeatureBuildItem.JPA));

        List<ParsedPersistenceXmlDescriptor> descriptors = descItems.stream().map(PersistenceUnitDescriptorBuildItem::getDescriptor).collect(Collectors.toList());

        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, descriptors, index.getIndex());
        final KnownDomainObjects domainObjects = scavenger.discoverModelAndRegisterForReflection();

        for (String className : domainObjects.getClassNames()) {
            template.addEntity(className);
        }
        template.enlistPersistenceUnit();
        template.callHibernateFeatureInit();

        //Modify the bytecode of all entities to enable lazy-loading, dirty checking, etc..
        enhanceEntities(domainObjects, transformers);

        //set up the scanner, as this scanning has already been done we need to just tell it about the classes we
        //have discovered. This scanner is bytecode serializable and is passed directly into the template
        ShamrockScanner scanner = new ShamrockScanner();
        Set<ClassDescriptor> classDescriptors = new HashSet<>();
        for (String i : domainObjects.getClassNames()) {
            ShamrockScanner.ClassDescriptorImpl desc = new ShamrockScanner.ClassDescriptorImpl(i, ClassDescriptor.Categorization.MODEL);
            classDescriptors.add(desc);
        }
        scanner.setClassDescriptors(classDescriptors);

        //now we serialize the XML and class list to bytecode, to remove the need to re-parse the XML on JVM startup
        recorder.registerNonDefaultConstructor(ParsedPersistenceXmlDescriptor.class.getDeclaredConstructor(URL.class), (i) -> Collections.singletonList(i.getPersistenceUnitRootUrl()));
        return new BeanContainerListenerBuildItem(template.initMetadata(descriptors, scanner));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public void build(JPADeploymentTemplate template,
                      Capabilities capabilities, BuildProducer<BeanContainerListenerBuildItem> buildProducer,
                      List<PersistenceUnitDescriptorBuildItem> descriptors) throws Exception {

        buildProducer.produce(new BeanContainerListenerBuildItem(template.initializeJpa(capabilities.isCapabilityPresent(Capabilities.TRANSACTIONS))));
        // Bootstrap all persistence units
        for (PersistenceUnitDescriptorBuildItem persistenceUnitDescriptor : descriptors) {
            buildProducer.produce(new BeanContainerListenerBuildItem(template.registerPersistenceUnit(persistenceUnitDescriptor.getDescriptor().getName())));
        }
        buildProducer.produce(new BeanContainerListenerBuildItem(template.initDefaultPersistenceUnit()));
    }

    @BuildStep
    @Record(RUNTIME_INIT)
    public void startPersistenceUnits(JPADeploymentTemplate template, BeanContainerBuildItem beanContainer) throws Exception {
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
            ApplicationArchivesBuildItem applicationArchivesBuildItem) {
        if ( descriptors.isEmpty() ) {
            //we have no persistence.xml so we will create a default one
            Optional<String> dialect = hibernateOrmConfig.flatMap(c -> c.dialect);
            if (!dialect.isPresent()) {
                dialect = guessDialect(driver);
            }
            dialect.ifPresent(s -> {
                // we found one
                ParsedPersistenceXmlDescriptor desc = new ParsedPersistenceXmlDescriptor(null); //todo URL
                desc.setName("default");
                desc.setTransactionType(PersistenceUnitTransactionType.JTA);
                desc.getProperties().setProperty(AvailableSettings.DIALECT, s);
                hibernateOrmConfig
                        .flatMap(c -> c.schemaGeneration)
                        .ifPresent( p -> desc.getProperties().setProperty(AvailableSettings.HBM2DDL_DATABASE_ACTION, p) );
                hibernateOrmConfig
                        .flatMap(c -> c.showSql)
                        .ifPresent( sql -> {
                            if (sql.equals(Boolean.TRUE)) {
                                desc.getProperties().setProperty(AvailableSettings.SHOW_SQL, "true");
                                desc.getProperties().setProperty(AvailableSettings.FORMAT_SQL, "true");
                            }
                        });

                // sql-load-script-source
                // explicit file or default one
                String file = hibernateOrmConfig
                        .flatMap(c -> c.sqlLoadScriptSource)
                        .orElse("import.sql"); //default Hibernate ORM file imported

                Optional<Path> loadScriptPath = Optional.ofNullable(applicationArchivesBuildItem.getRootArchive().getChildPath(file));
                // enlist resource if present
                loadScriptPath
                        .filter( path -> !Files.isDirectory(path))
                        .ifPresent( path -> {
                            String resourceAsString = root.getPath().relativize(loadScriptPath.get()).toString();
                            resourceProducer.produce(new SubstrateResourceBuildItem(resourceAsString));
                            hotDeploymentProducer.produce(new HotDeploymentConfigFileBuildItem(resourceAsString));
                            desc.getProperties().setProperty(AvailableSettings.HBM2DDL_LOAD_SCRIPT_SOURCE, file);
                        });

                //raise exception if explicit file is not present (i.e. not the default)
                hibernateOrmConfig.flatMap(c -> c.sqlLoadScriptSource)
                        .filter(o -> !loadScriptPath.filter( path -> !Files.isDirectory(path)).isPresent())
                        .ifPresent(
                            c -> { throw new ConfigurationError(
                                "Unable to find file referenced in 'shamrock.hibernate.sql-load-script-source="
                                + c + "'. Remove property or add file to your path."
                            );
                        });

                descriptors.add(desc);
            });
        }
        else {
            if (hibernateOrmConfig.isPresent() && hibernateOrmConfig.get().isAnyPropertySet()) {
                hibernateOrmConfig.ifPresent(c -> {
                    throw new ConfigurationError("Hibernate ORM configuration present in persistence.xml and Shamrock config file at the same time\n"
                            + "If you use persistence.xml remove all shamrock.hibernate.* properties from the Shamrock config file.");
                });
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
        if ( resolvedDriver.contains("org.mariadb.jdbc.Driver")) {
            return Optional.of(MariaDB103Dialect.class.getName());
        }
        String error = driver.isPresent() ?
                "Hibernate extension could not guess the dialect from the driver '" + resolvedDriver + "'. Add an explicit 'shamrock.hibernate.dialect' property." :
                "Hibernate extension cannot guess the dialect as no JDBC driver is specified by 'shamrock.datasource.driver'";
        throw new ConfigurationError(error);
    }

    private void enhanceEntities(final KnownDomainObjects domainObjects, BuildProducer<BytecodeTransformerBuildItem> transformers) {
        HibernateEntityEnhancer hibernateEntityEnhancer = new HibernateEntityEnhancer();
        for (String i : domainObjects.getClassNames()) {
            transformers.produce(new BytecodeTransformerBuildItem(i, hibernateEntityEnhancer));
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
