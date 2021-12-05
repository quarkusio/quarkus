package io.quarkus.hibernate.orm.panache.deployment;

import static io.quarkus.panache.common.deployment.PanacheConstants.META_INF_PANACHE_ARCHIVE_MARKER;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Id;

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
import io.quarkus.deployment.builditem.AdditionalApplicationArchiveMarkerBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
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
    AdditionalApplicationArchiveMarkerBuildItem marker() {
        return new AdditionalApplicationArchiveMarkerBuildItem(META_INF_PANACHE_ARCHIVE_MARKER);
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
    @Record(ExecutionTime.STATIC_INIT)
    @Consume(HibernateEnhancersRegisteredBuildItem.class)
    void build(
            PanacheHibernateOrmRecorder recorder,
            CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheEntityClassBuildItem> entityClasses,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

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

        recordPanacheEntityPersistenceUnits(recorder, jpaModelPersistenceUnitMapping, panacheEntities);
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

    void recordPanacheEntityPersistenceUnits(PanacheHibernateOrmRecorder recorder,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping,
            Set<String> panacheEntityClasses) {
        Map<String, String> panacheEntityToPersistenceUnit = new HashMap<>();
        if (jpaModelPersistenceUnitMapping.isPresent()) {
            Map<String, Set<String>> collectedEntityToPersistenceUnits = jpaModelPersistenceUnitMapping.get()
                    .getEntityToPersistenceUnits();

            Map<String, Set<String>> violatingPanacheEntities = new TreeMap<>();

            for (Map.Entry<String, Set<String>> entry : collectedEntityToPersistenceUnits.entrySet()) {
                String entityName = entry.getKey();
                Set<String> selectedPersistenceUnits = entry.getValue();
                boolean isPanacheEntity = panacheEntityClasses.contains(entityName);

                if (!isPanacheEntity) {
                    continue;
                }

                if (selectedPersistenceUnits.size() == 1) {
                    panacheEntityToPersistenceUnit.put(entityName, selectedPersistenceUnits.iterator().next());
                } else {
                    violatingPanacheEntities.put(entityName, selectedPersistenceUnits);
                }
            }

            if (violatingPanacheEntities.size() > 0) {
                StringBuilder message = new StringBuilder(
                        "Panache entities do not support being attached to several persistence units:\n");
                for (Entry<String, Set<String>> violatingEntityEntry : violatingPanacheEntities
                        .entrySet()) {
                    message.append("\t- ").append(violatingEntityEntry.getKey()).append(" is attached to: ")
                            .append(String.join(",", violatingEntityEntry.getValue()));
                    throw new IllegalStateException(message.toString());
                }
            }
        }

        recorder.setEntityToPersistenceUnit(panacheEntityToPersistenceUnit);
    }
}
