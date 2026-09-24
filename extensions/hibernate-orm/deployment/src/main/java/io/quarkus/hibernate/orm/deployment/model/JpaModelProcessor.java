package io.quarkus.hibernate.orm.deployment.model;

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
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.logging.Logger;

import io.quarkus.arc.deployment.ValidationPhaseBuildItem.ValidationErrorBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.index.LazyIndexer;
import io.quarkus.hibernate.orm.deployment.ClassNames;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfig;
import io.quarkus.hibernate.orm.deployment.HibernateOrmConfigPersistenceUnit;
import io.quarkus.hibernate.orm.deployment.HibernateOrmEnabled;
import io.quarkus.hibernate.orm.deployment.IgnorableNonIndexedClasses;
import io.quarkus.hibernate.orm.deployment.JpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelPerPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaPersistenceUnitModel;
import io.quarkus.hibernate.orm.deployment.PersistenceUnitDescriptorBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalPersistenceUnitBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.PersistenceUnitDefinedBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.QuarkusDataModelBuildItem;
import io.quarkus.hibernate.orm.runtime.HibernateOrmRuntimeConfig;
import io.quarkus.hibernate.orm.runtime.PersistenceUnitUtil;
import io.quarkus.panache.hibernate.common.deployment.HibernateModelClassCandidatesForFieldAccessBuildItem;
import io.quarkus.runtime.configuration.ConfigurationException;

@BuildSteps(onlyIf = HibernateOrmEnabled.class)
final class JpaModelProcessor {

    public static final String HIBERNATE_ORM_CONFIG_PREFIX = "quarkus.hibernate-orm.";

    private static final Logger LOG = Logger.getLogger(JpaModelProcessor.class);

    // --- Indexing setup ---

    @BuildStep
    AdditionalIndexedClassesBuildItem addPersistenceUnitAnnotationToIndex() {
        return new AdditionalIndexedClassesBuildItem(ClassNames.QUARKUS_PERSISTENCE_UNIT.toString());
    }

    @BuildStep
    void includeArchivesHostingEntityPackagesInIndex(HibernateOrmConfig hibernateOrmConfig,
            BuildProducer<io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem> additionalApplicationArchiveMarkers) {
        for (HibernateOrmConfigPersistenceUnit persistenceUnit : hibernateOrmConfig.persistenceUnits()
                .values()) {
            if (persistenceUnit.packages().isPresent()) {
                for (String pakkage : persistenceUnit.packages().get()) {
                    additionalApplicationArchiveMarkers
                            .produce(new io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem(
                                    pakkage.replace('.', '/')));
                }
            }
        }
    }

    // --- JPA model contributions ---

    @BuildStep
    public void contributeAdditionalPersistenceUnitModelClasses(
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits,
            BuildProducer<AdditionalJpaModelBuildItem> additionalJpaModel) {
        // Synthesize the JPA model build items for the managed classes of SPI-contributed persistence units,
        // so that those classes are indexed, bytecode-enhanced, registered for reflection and assigned to their
        // persistence unit without the contributing extension having to produce a separate AdditionalJpaModelBuildItem.
        for (AdditionalPersistenceUnitBuildItem additionalPersistenceUnit : additionalPersistenceUnits) {
            Set<String> persistenceUnits = Set.of(additionalPersistenceUnit.getPersistenceUnitName());
            for (String className : additionalPersistenceUnit.getManagedClassNames()) {
                additionalJpaModel.produce(new AdditionalJpaModelBuildItem(className, persistenceUnits));
            }
        }
    }

    @BuildStep
    public void contributeAdditionalPersistenceUnitsToJpaModel(
            BuildProducer<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits) {
        for (AdditionalPersistenceUnitBuildItem additionalPersistenceUnit : additionalPersistenceUnits) {
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    additionalPersistenceUnit.getPersistenceUnitName(), null,
                    additionalPersistenceUnit.getManagedClassNames(),
                    additionalPersistenceUnit.getMappingFileNames()));
        }
    }

    @BuildStep
    public void contributeQuarkusConfigToJpaModel(
            BuildProducer<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions,
            HibernateOrmConfig hibernateOrmConfig,
            List<io.quarkus.hibernate.orm.deployment.PersistenceXmlDescriptorBuildItem> persistenceXmlDescriptors) {
        // TODO move this validation to a dedicated method, preferably very early in the build?
        //   See also a conceptually similar check in buildBlockingPersistenceUnitFromPersistenceXml
        if (!persistenceXmlDescriptors.isEmpty()) {
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
        // When no build-time config property is set for the default PU,
        // it won't appear in persistenceUnits().keySet().
        // We still need to contribute it if META-INF/orm.xml exists,
        // since that file is picked up by default.
        if (!hibernateOrmConfig.persistenceUnits()
                .containsKey(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME)
                && Thread.currentThread().getContextClassLoader().getResource("META-INF/orm.xml") != null) {
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME, null, Collections.emptySet(),
                    Collections.emptySet()));
        }
        for (Entry<String, HibernateOrmConfigPersistenceUnit> entry : hibernateOrmConfig.persistenceUnits()
                .entrySet()) {
            String name = entry.getKey();
            HibernateOrmConfigPersistenceUnit config = entry.getValue();
            jpaModelPuContributions.produce(new JpaModelPersistenceUnitContributionBuildItem(
                    name, null, Collections.emptySet(),
                    config.mappingFiles().orElse(Collections.emptySet())));
        }
    }

    // --- Model building ---

    @BuildStep
    @SuppressWarnings("deprecation")
    public JpaModelIndexBuildItem jpaEntitiesIndexer(
            CombinedIndexBuildItem index,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem> deprecatedAdditionalJpaModelBuildItems) {
        // build a composite index with additional jpa model classes
        LazyIndexer indexer = new LazyIndexer(JpaModelProcessor.class.getClassLoader(), index.getIndex());
        for (AdditionalJpaModelBuildItem jpaModel : additionalJpaModelBuildItems) {
            indexer.add(jpaModel.getClassName());
        }
        for (io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem jpaModel : deprecatedAdditionalJpaModelBuildItems) {
            indexer.add(jpaModel.getClassName());
        }
        LazyIndexer.Result result = indexer.complete();
        CompositeIndex compositeIndex = CompositeIndex.create(index.getComputingIndex(), result.index());
        return new JpaModelIndexBuildItem(compositeIndex);
    }

    @SuppressWarnings("deprecation")
    @BuildStep
    public void defineJpaModel(
            JpaModelIndexBuildItem indexBuildItem,
            BuildProducer<JpaModelBuildItem> domainObjectsProducer,
            List<io.quarkus.hibernate.orm.deployment.spi.IgnorableNonIndexedClasses> ignorableNonIndexedClassesBuildItems,
            List<IgnorableNonIndexedClasses> deprecatedIgnorableNonIndexedClassesBuildItems,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<HotDeploymentWatchedFileBuildItem> hotDeploymentWatchedFiles,
            List<JpaModelPersistenceUnitContributionBuildItem> jpaModelPuContributions) throws BuildException {

        Set<String> ignorableNonIndexedClasses = Collections.emptySet();
        if (!ignorableNonIndexedClassesBuildItems.isEmpty() || !deprecatedIgnorableNonIndexedClassesBuildItems.isEmpty()) {
            ignorableNonIndexedClasses = new HashSet<>();
            for (io.quarkus.hibernate.orm.deployment.spi.IgnorableNonIndexedClasses buildItem : ignorableNonIndexedClassesBuildItems) {
                ignorableNonIndexedClasses.addAll(buildItem.getClasses());
            }
            for (IgnorableNonIndexedClasses buildItem : deprecatedIgnorableNonIndexedClassesBuildItems) {
                ignorableNonIndexedClasses.addAll(buildItem.getClasses());
            }
        }

        JpaJandexScavenger scavenger = new JpaJandexScavenger(reflectiveClass, hotDeploymentWatchedFiles,
                jpaModelPuContributions, indexBuildItem.getIndex(), ignorableNonIndexedClasses);
        final JpaModelBuildItem domainObjects = scavenger.discoverModelAndRegisterForReflection();
        domainObjectsProducer.produce(domainObjects);
    }

    // --- Per-PU model assignment ---

    @BuildStep
    public JpaModelPerPersistenceUnitBuildItem buildJpaModelPerPersistenceUnit(HibernateOrmConfig hibernateOrmConfig,
            List<AdditionalJpaModelBuildItem> additionalJpaModelBuildItems,
            List<QuarkusDataModelBuildItem> quarkusDataModelBuildItems,
            JpaModelBuildItem jpaModel,
            CombinedIndexBuildItem indexBuildItem) {
        IndexView index = indexBuildItem.getIndex();
        Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit = new HashMap<>();

        boolean hasPackagesInQuarkusConfig = hasPackagesInQuarkusConfig(hibernateOrmConfig);
        Collection<AnnotationInstance> packageLevelPersistenceUnitAnnotations = getPackageLevelPersistenceUnitAnnotations(
                index);

        Map<String, Set<String>> packageRules = new HashMap<>();

        Set<String> persistenceUnitsConfiguredThroughQuarkusConfiguration = new LinkedHashSet<>();
        Set<String> persistenceUnitsConfiguredThroughPackageLevelAnnotations = new LinkedHashSet<>();

        if (hasPackagesInQuarkusConfig) {
            // Config based packages have priorities over annotations.
            // As long as there is one defined, annotations are ignored.
            if (!packageLevelPersistenceUnitAnnotations.isEmpty()) {
                // TODO shouldn't this be an error?
                LOG.warn(
                        "Mixing Quarkus configuration and @PersistenceUnit annotations to define the persistence units is not supported. Ignoring the annotations.");
            }

            for (Entry<String, HibernateOrmConfigPersistenceUnit> candidatePersistenceUnitEntry : hibernateOrmConfig
                    .persistenceUnits()
                    .entrySet()) {
                String candidatePersistenceUnitName = candidatePersistenceUnitEntry.getKey();
                Set<String> candidatePersistenceUnitPackages = candidatePersistenceUnitEntry.getValue().packages()
                        // Missing values are a problem, but will be reported elsewhere:
                        // we can't do it here since we don't even know
                        // the full set of PUs (some might not even have an entry in hibernateOrmConfig)
                        .orElse(Collections.emptySet());
                if (!candidatePersistenceUnitPackages.isEmpty()) {
                    persistenceUnitsConfiguredThroughQuarkusConfiguration.add(candidatePersistenceUnitName);
                }
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
                    persistenceUnitsConfiguredThroughPackageLevelAnnotations.add(persistenceUnitName);
                }
            }
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
                        model.allModelClassNames().add(modelClassName);

                        // also add the hierarchy to the persistence unit
                        // we would need to add all the underlying model to it but adding the hierarchy
                        // is necessary for Panache as we need to add PanacheEntity to the PU
                        model.allModelClassNames().addAll(relatedModelClassNames);
                    }
                }
            }
        }

        Set<String> assignedEntityClassNames = new HashSet<>();
        Set<String> assignedModelClassAndPackageNames = new HashSet<>();
        for (AdditionalJpaModelBuildItem additionalJpaModel : additionalJpaModelBuildItems) {
            var className = additionalJpaModel.getClassName();
            var persistenceUnits = additionalJpaModel.getPersistenceUnits();
            if (persistenceUnits == null) {
                // Legacy behavior -- remove when the deprecated one-argument constructor of AdditionalJpaModelBuildItem gets removed.
                continue;
            }
            boolean isEntity = jpaModel.getEntityClassNames().contains(className);
            // Even if persistenceUnits is empty, the class is still assigned (to nothing)
            if (isEntity) {
                assignedEntityClassNames.add(className);
            }
            assignedModelClassAndPackageNames.add(className);
            for (String persistenceUnitName : persistenceUnits) {
                var model = modelPerPersistenceUnit.computeIfAbsent(persistenceUnitName,
                        ignored -> new JpaPersistenceUnitModel());

                if (isEntity) {
                    model.entityClassNames().add(className);
                }
                model.allModelClassNames().add(className);
            }
        }

        if (!modelClassesWithPersistenceUnitAnnotations.isEmpty()) {
            throw new IllegalStateException(String.format(Locale.ROOT,
                    "@PersistenceUnit annotations are not supported at the class level on model classes:\n\t- %s\nUse the `.packages` configuration property or package-level annotations instead.",
                    String.join("\n\t- ", modelClassesWithPersistenceUnitAnnotations)));
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
                model.modelPackageNames().add(modelPackageName);
            }
        }

        // Copy xmlMappings, so that consumers of JpaModelPerPersistenceUnitBuildItem
        // don't need to consume JpaModelBuildItem
        for (var entry : jpaModel.getXmlMappingsByPU().entrySet()) {
            var model = modelPerPersistenceUnit.computeIfAbsent(entry.getKey(),
                    ignored -> new JpaPersistenceUnitModel());
            model.xmlMappings().addAll(entry.getValue());
        }

        for (QuarkusDataModelBuildItem quarkusDataModel : quarkusDataModelBuildItems) {
            var className = quarkusDataModel.getClassName();
            Set<String> persistenceUnits = findEnclosingEntityPersistenceUnits(
                    quarkusDataModel.getEnclosingEntityClassName(), modelPerPersistenceUnit);
            if (persistenceUnits.isEmpty()) {
                persistenceUnits = Set.of(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME);
            }
            assignedModelClassAndPackageNames.add(className);
            for (String persistenceUnitName : persistenceUnits) {
                modelPerPersistenceUnit.computeIfAbsent(persistenceUnitName,
                        ignored -> new JpaPersistenceUnitModel())
                        .allModelClassNames().add(className);
            }
        }

        assignedEntityClassNames.addAll(modelPerPersistenceUnit.values().stream()
                .map(JpaPersistenceUnitModel::entityClassNames).flatMap(Set::stream).toList());
        assignedModelClassAndPackageNames.addAll(modelPerPersistenceUnit.values().stream()
                .map(JpaPersistenceUnitModel::allModelClassNames).flatMap(Set::stream).toList());
        assignedModelClassAndPackageNames.addAll(modelPerPersistenceUnit.values().stream()
                .map(JpaPersistenceUnitModel::modelPackageNames).flatMap(Set::stream).toList());
        Set<String> unaffectedEntityClassNames = jpaModel.getEntityClassNames().stream()
                .filter(c -> !assignedEntityClassNames.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> unaffectedModelClassNames = jpaModel.getAllModelClassNames().stream()
                .filter(c -> !assignedModelClassAndPackageNames.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> unaffectedModelPackageNames = jpaModel.getAllModelPackageNames().stream()
                .filter(c -> !assignedModelClassAndPackageNames.contains(c))
                .collect(Collectors.toCollection(TreeSet::new));
        Set<String> unaffectedModelClassAndPackageNames = new TreeSet<>();
        unaffectedModelClassAndPackageNames.addAll(unaffectedModelClassNames);
        unaffectedModelClassAndPackageNames.addAll(unaffectedModelPackageNames);
        if (!unaffectedEntityClassNames.isEmpty() || !unaffectedModelClassAndPackageNames.isEmpty()) {
            if (!hasPackagesInQuarkusConfig && packageLevelPersistenceUnitAnnotations.isEmpty()) {
                // No .packages configuration and no package-level persistence unit annotations:
                // all unaffected entities will be associated with the default one.
                var model = modelPerPersistenceUnit.computeIfAbsent(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME,
                        ignored -> new JpaPersistenceUnitModel());
                model.entityClassNames().addAll(unaffectedEntityClassNames);
                model.allModelClassNames().addAll(unaffectedModelClassNames);
                model.modelPackageNames().addAll(unaffectedModelPackageNames);
            } else {
                // unaffectedEntityClassNames would necessarily be in unaffectedModelClassAndPackageNames
                LOG.warnf("Could not find a suitable persistence unit for model classes/packages:\n\t- %s",
                        String.join("\n\t- ", unaffectedModelClassAndPackageNames));
            }
        }

        return new JpaModelPerPersistenceUnitBuildItem(modelPerPersistenceUnit,
                persistenceUnitsConfiguredThroughQuarkusConfiguration,
                persistenceUnitsConfiguredThroughPackageLevelAnnotations);
    }

    @SuppressWarnings("deprecation")
    @BuildStep
    public void build(
            BuildProducer<io.quarkus.hibernate.orm.deployment.spi.JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            BuildProducer<JpaModelPersistenceUnitMappingBuildItem> deprecatedJpaModelPersistenceUnitMapping,
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

        jpaModelPersistenceUnitMapping.produce(
                new io.quarkus.hibernate.orm.deployment.spi.JpaModelPersistenceUnitMappingBuildItem(
                        entityPersistenceUnitMapping, incomplete));
        deprecatedJpaModelPersistenceUnitMapping
                .produce(new JpaModelPersistenceUnitMappingBuildItem(entityPersistenceUnitMapping, incomplete));
    }

    @BuildStep
    public void validateJpaModelPerPersistenceUnit(JpaModelPerPersistenceUnitBuildItem jpaModelPerPersistenceUnit,
            List<PersistenceUnitDefinedBuildItem> definedPersistenceUnits,
            List<AdditionalPersistenceUnitBuildItem> additionalPersistenceUnits,
            BuildProducer<ValidationErrorBuildItem> validationErrors) {
        // Exclude SPI-contributed PUs: they manage their own entity assignment
        Set<String> spiPuNames = additionalPersistenceUnits.stream()
                .map(AdditionalPersistenceUnitBuildItem::getPersistenceUnitName)
                .collect(Collectors.toSet());
        Set<String> puNames = definedPersistenceUnits.stream()
                .map(PersistenceUnitDefinedBuildItem::getPersistenceUnitName)
                .filter(name -> !spiPuNames.contains(name))
                .collect(Collectors.toSet());
        boolean hasNamedPUs = puNames.size() > 1
                || (puNames.size() == 1 && !puNames.contains(PersistenceUnitUtil.DEFAULT_PERSISTENCE_UNIT_NAME));
        if (!hasNamedPUs) {
            return;
        }

        // Check entity -> persistence unit assignment is done consistently

        var pusWithPackageQuarkusConfig = jpaModelPerPersistenceUnit.getPersistenceUnitsConfiguredThroughQuarkusConfiguration();
        var pusWithPackageLevelAnnotations = jpaModelPerPersistenceUnit
                .getPersistenceUnitsConfiguredThroughPackageLevelAnnotations();

        Set<String> pusWithoutPackageQuarkusConfig = new HashSet<>(puNames);
        pusWithoutPackageQuarkusConfig.removeAll(pusWithPackageQuarkusConfig);
        Set<String> missingPackagePropertyKeys = pusWithoutPackageQuarkusConfig.stream()
                .map(name -> HibernateOrmRuntimeConfig.puPropertyKey(name, "packages"))
                .collect(Collectors.toSet());
        // TODO should we enforce this for package-level annotations? Traditionally we have not.
        if (!pusWithPackageQuarkusConfig.isEmpty()) {
            if (!pusWithoutPackageQuarkusConfig.isEmpty()) {
                validationErrors.produce(new ValidationErrorBuildItem(
                        new ConfigurationException(
                                String.format(Locale.ROOT, "Packages must be configured for persistence units %s.",
                                        missingPackagePropertyKeys),
                                missingPackagePropertyKeys)));
            }
        }
        if (pusWithPackageLevelAnnotations.isEmpty() && pusWithPackageQuarkusConfig.isEmpty()) {
            validationErrors.produce(new ValidationErrorBuildItem(new ConfigurationException(
                    """
                            Named persistence units are defined but the entities are not mapped to them. \
                            You should either use the .packages Quarkus configuration property or package-level @PersistenceUnit annotations.\
                            Refer to https://quarkus.io/guides/hibernate-orm#multiple-persistence-units for guidance.
                            """,
                    missingPackagePropertyKeys)));
        }
    }

    // --- Reflection / post-processing ---

    @BuildStep
    public HibernateModelClassCandidatesForFieldAccessBuildItem candidatesForFieldAccess(JpaModelBuildItem jpaModel) {
        // Ask Panache to replace direct access to public fields with calls to accessors for all model classes.
        return new HibernateModelClassCandidatesForFieldAccessBuildItem(jpaModel.getManagedClassNames());
    }

    @BuildStep
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

    // --- Private helpers ---

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

    private static Set<String> findEnclosingEntityPersistenceUnits(String enclosingClassName,
            Map<String, JpaPersistenceUnitModel> modelPerPersistenceUnit) {
        Set<String> result = new HashSet<>();
        for (var entry : modelPerPersistenceUnit.entrySet()) {
            if (entry.getValue().entityClassNames().contains(enclosingClassName)) {
                result.add(entry.getKey());
            }
        }
        return result;
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
}
