package io.quarkus.hibernate.reactive.panache.kotlin.deployment;

import static io.quarkus.deployment.util.JandexUtil.resolveTypeParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Id;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.builder.BuildException;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.reactive.panache.kotlin.runtime.PanacheKotlinReactiveRecorder;
import io.quarkus.panache.common.deployment.KotlinPanacheCompanionEnhancer;
import io.quarkus.panache.common.deployment.KotlinPanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.KotlinPanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;
import io.quarkus.panache.hibernate.common.deployment.HibernateEnhancersRegisteredBuildItem;

public class HibernateReactivePanacheKotlinProcessor {

    private static final DotName DOTNAME_REACTIVE_SESSION = DotName.createSimple(Mutiny.Session.class.getName());
    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());
    private static final TypeBundle TYPE_BUNDLE = ReactiveKotlinJpaTypeBundle.BUNDLE;

    @BuildStep
    public FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_REACTIVE_PANACHE_KOTLIN);
    }

    @BuildStep
    public AdditionalJpaModelBuildItem produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return new AdditionalJpaModelBuildItem("io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity",
                // Only added to persistence units actually using this class, using Jandex-based discovery,
                // so we pass empty sets of PUs.
                // The build items tell the Hibernate extension to process the classes at build time:
                // add to Jandex index, bytecode enhancement, proxy generation, ...
                Set.of());
    }

    @BuildStep
    public UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return UnremovableBeanBuildItem.beanTypes(DOTNAME_REACTIVE_SESSION);
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    public void build(CombinedIndexBuildItem index, BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems,
            PanacheKotlinReactiveRecorder recorder,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        processRepositories(index, transformers, reflectiveClass,
                new KotlinPanacheRepositoryEnhancer(index.getComputingIndex(), methodCustomizers, TYPE_BUNDLE));

        processEntities(index, transformers, reflectiveClass,
                new KotlinPanacheEntityEnhancer(index.getComputingIndex(), methodCustomizers, TYPE_BUNDLE), recorder,
                jpaModelPersistenceUnitMapping);

        processCompanions(index, transformers, reflectiveClass,
                new KotlinPanacheCompanionEnhancer(index.getComputingIndex(), methodCustomizers, TYPE_BUNDLE));
    }

    private void processEntities(CombinedIndexBuildItem index, BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass, PanacheEntityEnhancer entityEnhancer,
            PanacheKotlinReactiveRecorder recorder,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping) {

        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        for (ClassInfo classInfo : index.getComputingIndex().getAllKnownImplementors(TYPE_BUNDLE.entityBase().dotName())) {
            if (classInfo.name().equals(TYPE_BUNDLE.entity().dotName())) {
                continue;
            }
            String name = classInfo.name().toString();
            if (modelClasses.add(name)) {
                transformers.produce(new BytecodeTransformerBuildItem(name, entityEnhancer));
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, name));
            }
        }

        Map<String, Set<String>> collectedEntityToPersistenceUnits;
        boolean incomplete;
        if (jpaModelPersistenceUnitMapping.isPresent()) {
            collectedEntityToPersistenceUnits = jpaModelPersistenceUnitMapping.get().getEntityToPersistenceUnits();
            incomplete = jpaModelPersistenceUnitMapping.get().isIncomplete();
        } else {
            collectedEntityToPersistenceUnits = new HashMap<>();
            // This happens if there is no persistence unit, in which case we definitely know this metadata is complete.
            incomplete = false;
        }

        Map<String, String> panacheEntityToPersistenceUnit = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : collectedEntityToPersistenceUnits.entrySet()) {
            String entityName = entry.getKey();
            List<String> selectedPersistenceUnits = new ArrayList<>(entry.getValue());
            boolean isPanacheEntity = modelClasses.contains(entityName);
            if (selectedPersistenceUnits.size() > 1 && isPanacheEntity) {
                throw new IllegalStateException(String.format(
                        "PanacheEntity '%s' cannot be defined for usage in several persistence units which is not supported. The following persistence units were found: %s.",
                        entityName, String.join(",", selectedPersistenceUnits)));
            }

            panacheEntityToPersistenceUnit.put(entityName, selectedPersistenceUnits.get(0));
        }
        // This is called even if there are no entity types, so that Panache gets properly initialized.
        recorder.addEntityTypesToPersistenceUnit(panacheEntityToPersistenceUnit, incomplete);
    }

    private void processCompanions(CombinedIndexBuildItem index, BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            KotlinPanacheCompanionEnhancer enhancer) {

        Set<org.jboss.jandex.Type> typeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getComputingIndex()
                .getAllKnownImplementors(TYPE_BUNDLE.entityCompanionBase().dotName())) {
            if (classInfo.name().equals(TYPE_BUNDLE.entityCompanion().dotName())) {
                continue;
            }
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), enhancer));
            typeParameters
                    .addAll(resolveTypeParameters(classInfo.name(), TYPE_BUNDLE.entityCompanionBase().dotName(),
                            index.getComputingIndex()));
        }

        for (org.jboss.jandex.Type parameterType : typeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));
        }
    }

    private void processRepositories(CombinedIndexBuildItem index, BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass, PanacheRepositoryEnhancer enhancer) {

        Set<org.jboss.jandex.Type> typeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getComputingIndex().getAllKnownImplementors(TYPE_BUNDLE.repositoryBase().dotName())) {
            if (classInfo.name().equals(TYPE_BUNDLE.repository().dotName()) || enhancer.skipRepository(classInfo)) {
                continue;
            }
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), enhancer));
            typeParameters
                    .addAll(resolveTypeParameters(classInfo.name(), TYPE_BUNDLE.repositoryBase().dotName(),
                            index.getComputingIndex()));
        }
        for (org.jboss.jandex.Type parameterType : typeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));
        }
    }

    @BuildStep
    public ValidationPhaseBuildItem.ValidationErrorBuildItem validate(ValidationPhaseBuildItem validationPhase,
            CombinedIndexBuildItem index) throws BuildException {
        // we verify that no ID fields are defined (via @Id) when extending PanacheEntity
        for (AnnotationInstance annotationInstance : index.getComputingIndex().getAnnotations(DOTNAME_ID)) {
            ClassInfo info = JandexUtil.getEnclosingClass(annotationInstance);
            if (JandexUtil.isSubclassOf(index.getComputingIndex(), info, TYPE_BUNDLE.entity().dotName())) {
                BuildException be = new BuildException(
                        "You provide a JPA identifier via @Id inside '" + info.name()
                                + "' but one is already provided by PanacheEntity, "
                                + "your class should extend PanacheEntityBase instead, or use the id provided by PanacheEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            }
        }
        return null;
    }
}
