package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static io.quarkus.deployment.util.JandexUtil.resolveTypeParameters;
import static java.util.Collections.singleton;
import static org.jboss.jandex.DotName.createSimple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.processor.BeanInfo;
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
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;
import io.quarkus.hibernate.orm.panache.kotlin.runtime.PanacheKotlinHibernateOrmRecorder;
import io.quarkus.panache.common.deployment.ByteCodeType;
import io.quarkus.panache.common.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;

public final class KotlinPanacheResourceProcessor {
    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheEntity.class.getName());
    private static final Set<DotName> UNREMOVABLE_BEANS = singleton(createSimple(EntityManager.class.getName()));

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    void build(PanacheKotlinHibernateOrmRecorder recorder,
            CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        TypeBundle bundle = KotlinJpaTypeBundle.BUNDLE;

        processRepositories(index, transformers, reflectiveClass, createRepositoryEnhancer(index, methodCustomizers),
                bundle.repositoryBase(), bundle.repository());

        processEntities(index, transformers, reflectiveClass, recorder, jpaModelPersistenceUnitMapping,
                createEntityEnhancer(index, methodCustomizers), bundle.entityBase(), bundle.entity());

        processCompanions(index, transformers, reflectiveClass, createCompanionEnhancer(index, methodCustomizers),
                bundle.entityCompanionBase(), bundle.entityCompanion());
    }

    public PanacheEntityEnhancer createEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new KotlinPanacheEntityEnhancer(index.getIndex(), methodCustomizers);
    }

    private PanacheRepositoryEnhancer createRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new KotlinPanacheRepositoryEnhancer(index.getIndex(), methodCustomizers);
    }

    private KotlinPanacheCompanionEnhancer createCompanionEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> customizers) {
        return new KotlinPanacheCompanionEnhancer(index.getIndex(), customizers);
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return new UnremovableBeanBuildItem(new Predicate<BeanInfo>() {
            @Override
            public boolean test(BeanInfo beanInfo) {
                for (Type t : beanInfo.getTypes()) {
                    if (UNREMOVABLE_BEANS.contains(t.name())) {
                        return true;
                    }
                }

                return false;
            }
        });
    }

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_ORM_PANACHE_KOTLIN);
    }

    private void processEntities(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            PanacheKotlinHibernateOrmRecorder recorder,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            PanacheEntityEnhancer entityEnhancer,
            ByteCodeType baseType,
            ByteCodeType type) {

        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(baseType.dotName())) {
            if (classInfo.name().equals(type.dotName())) {
                continue;
            }
            String name = classInfo.name().toString();
            if (modelClasses.add(name)) {
                transformers.produce(new BytecodeTransformerBuildItem(name, entityEnhancer));
                reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, name));
            }
        }

        Map<String, Set<String>> collectedEntityToPersistenceUnits = new HashMap<>();
        if (jpaModelPersistenceUnitMapping.isPresent()) {
            collectedEntityToPersistenceUnits = jpaModelPersistenceUnitMapping.get().getEntityToPersistenceUnits();
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
        recorder.setEntityToPersistenceUnit(panacheEntityToPersistenceUnit);
    }

    private void processRepositories(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            PanacheRepositoryEnhancer enhancer,
            ByteCodeType baseType,
            ByteCodeType type) {

        Set<Type> typeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(baseType.dotName())) {
            if (classInfo.name().equals(type.dotName()) || enhancer.skipRepository(classInfo)) {
                continue;
            }
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), enhancer));
            typeParameters.addAll(resolveTypeParameters(classInfo.name(), baseType.dotName(), index.getIndex()));
        }
        for (Type parameterType : typeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));
        }
    }

    private void processCompanions(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            KotlinPanacheCompanionEnhancer enhancer,
            ByteCodeType baseType,
            ByteCodeType type) {

        Set<Type> typeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(baseType.dotName())) {
            if (classInfo.name().equals(type.dotName())) {
                continue;
            }
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), enhancer));
            typeParameters.addAll(resolveTypeParameters(classInfo.name(), baseType.dotName(), index.getIndex()));
        }

        for (Type parameterType : typeParameters) {
            // Register for reflection the type parameters of the repository: this should be the entity class and the ID class
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, parameterType.name().toString()));
        }
    }

    @BuildStep
    AdditionalJpaModelBuildItem produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return new AdditionalJpaModelBuildItem("io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity");
    }

    @BuildStep
    ValidationPhaseBuildItem.ValidationErrorBuildItem validate(ValidationPhaseBuildItem validationPhase,
            CombinedIndexBuildItem index) throws BuildException {
        // we verify that no ID fields are defined (via @Id) when extending PanacheEntity
        for (AnnotationInstance annotationInstance : index.getIndex().getAnnotations(DOTNAME_ID)) {
            ClassInfo info = JandexUtil.getEnclosingClass(annotationInstance);
            if (JandexUtil.isSubclassOf(index.getIndex(), info, DOTNAME_PANACHE_ENTITY)) {
                BuildException be = new BuildException("You provide a JPA identifier via @Id inside '" + info.name() +
                        "' but one is already provided by PanacheEntity, " +
                        "your class should extend PanacheEntityBase instead, or use the id provided by PanacheEntity",
                        Collections.emptyList());
                return new ValidationPhaseBuildItem.ValidationErrorBuildItem(be);
            }
        }
        return null;
    }
}
