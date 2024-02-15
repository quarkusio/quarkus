package io.quarkus.hibernate.orm.panache.deployment;

import static io.quarkus.hibernate.orm.panache.deployment.EntityToPersistenceUnitUtil.determineEntityPersistenceUnits;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Id;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.deployment.ValidationPhaseBuildItem;
import io.quarkus.arc.deployment.staticmethods.InterceptedStaticMethodsTransformersRegisteredBuildItem;
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
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.deployment.spi.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.runtime.PanacheHibernateOrmRecorder;
import io.quarkus.panache.common.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.panache.common.deployment.PanacheJpaEntityOperationsEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;

public final class PanacheHibernateResourceProcessor {

    static final DotName DOTNAME_PANACHE_REPOSITORY_BASE = DotName.createSimple(PanacheRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_REPOSITORY = DotName.createSimple(PanacheRepository.class.getName());

    static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheEntity.class.getName());
    static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheEntityBase.class.getName());

    private static final DotName DOTNAME_ENTITY_MANAGER = DotName.createSimple(EntityManager.class.getName());

    private static final DotName DOTNAME_ID = DotName.createSimple(Id.class.getName());

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_ORM_PANACHE);
    }

    @BuildStep
    AdditionalJpaModelBuildItem produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return new AdditionalJpaModelBuildItem("io.quarkus.hibernate.orm.panache.PanacheEntity");
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypeExclusion(DOTNAME_ENTITY_MANAGER));
    }

    @BuildStep
    void collectEntityClasses(CombinedIndexBuildItem index, BuildProducer<PanacheEntityClassBuildItem> entityClasses) {
        // NOTE: we don't skip abstract/generic entities because they still need accessors
        for (ClassInfo panacheEntityBaseSubclass : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
            // FIXME: should we really skip PanacheEntity or all MappedSuperClass?
            if (!panacheEntityBaseSubclass.name().equals(DOTNAME_PANACHE_ENTITY)) {
                entityClasses.produce(new PanacheEntityClassBuildItem(panacheEntityBaseSubclass));
            }
        }
    }

    @BuildStep
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    @Consume(InterceptedStaticMethodsTransformersRegisteredBuildItem.class)
    void build(
            CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheEntityClassBuildItem> entityClasses,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems,
            BuildProducer<EntityToPersistenceUnitBuildItem> entityToPersistenceUnit) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(PanacheMethodCustomizerBuildItem::getMethodCustomizer).collect(Collectors.toList());

        PanacheJpaRepositoryEnhancer daoEnhancer = new PanacheJpaRepositoryEnhancer(index.getIndex(),
                JavaJpaTypeBundle.BUNDLE);
        Set<String> panacheEntities = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(DOTNAME_PANACHE_REPOSITORY))
                continue;
            if (daoEnhancer.skipRepository(classInfo))
                continue;
            List<org.jboss.jandex.Type> typeParameters = JandexUtil
                    .resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_BASE, index.getIndex());
            panacheEntities.add(typeParameters.get(0).name().toString());
            transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), daoEnhancer));
        }

        PanacheJpaEntityOperationsEnhancer entityOperationsEnhancer = new PanacheJpaEntityOperationsEnhancer(index.getIndex(),
                methodCustomizers, JavaJpaTypeBundle.BUNDLE);
        Set<String> modelClasses = new HashSet<>();
        for (PanacheEntityClassBuildItem entityClass : entityClasses) {
            String entityClassName = entityClass.get().name().toString();
            modelClasses.add(entityClassName);
            transformers.produce(new BytecodeTransformerBuildItem(true, entityClassName, entityOperationsEnhancer));
        }

        panacheEntities.addAll(modelClasses);

        determineEntityPersistenceUnits(jpaModelPersistenceUnitMapping, panacheEntities, "Panache")
                .forEach((e, pu) -> entityToPersistenceUnit.produce(new EntityToPersistenceUnitBuildItem(e, pu)));
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void recordEntityToPersistenceUnit(List<EntityToPersistenceUnitBuildItem> items, PanacheHibernateOrmRecorder recorder) {
        Map<String, String> map = new HashMap<>();
        for (EntityToPersistenceUnitBuildItem item : items) {
            map.put(item.getEntityClass(), item.getPersistenceUnitName());
        }
        recorder.setEntityToPersistenceUnit(map);
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
