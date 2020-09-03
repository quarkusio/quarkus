package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static java.util.Arrays.asList;
import static org.jboss.jandex.DotName.createSimple;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Transient;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jetbrains.annotations.NotNull;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.hibernate.orm.deployment.JpaModelPersistenceUnitMappingBuildItem;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.kotlin.runtime.JpaOperations;
import io.quarkus.hibernate.orm.panache.kotlin.runtime.PanacheKotlinHibernateOrmRecorder;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;

public final class KotlinPanacheResourceProcessor {

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.singleton(
            createSimple(EntityManager.class.getName()));
    static final String JPA_OPERATIONS = createSimple(JpaOperations.class.getName())
            .toString().replace(".", "/");

    static final DotName PANACHE_REPOSITORY_BASE = createSimple(PanacheRepositoryBase.class.getName());
    static final DotName PANACHE_REPOSITORY = createSimple(PanacheRepository.class.getName());

    static final DotName PANACHE_ENTITY_BASE = createSimple(PanacheEntityBase.class.getName());
    static final DotName PANACHE_ENTITY = createSimple(PanacheEntity.class.getName());
    static final DotName PANACHE_COMPANION = createSimple(PanacheCompanion.class.getName());

    static final String PANACHE_ENTITY_BASE_SIGNATURE = toBinarySignature(PanacheEntityBase.class);
    static final String PANACHE_ENTITY_SIGNATURE = toBinarySignature(PanacheEntity.class);
    static final String PANACHE_COMPANION_SIGNATURE = toBinarySignature(PanacheCompanion.class);

    static final String OBJECT_SIGNATURE = toBinarySignature(Object.class);
    static final String CLASS_SIGNATURE = toBinarySignature(Class.class);
    static final DotName TRANSIENT = DotName.createSimple(Transient.class.getName());

    static final org.objectweb.asm.Type CLASS_TYPE = org.objectweb.asm.Type.getType(Class.class);
    static final org.objectweb.asm.Type OBJECT_TYPE = org.objectweb.asm.Type.getType(Object.class);
    static final List<org.objectweb.asm.Type> REPOSITORY_TYPES = asList(
            org.objectweb.asm.Type.getType(PanacheRepositoryBase.class),
            org.objectweb.asm.Type.getType(PanacheRepository.class));
    static final List<org.objectweb.asm.Type> ENTITY_TYPES = asList(
            org.objectweb.asm.Type.getType(PanacheEntityBase.class),
            org.objectweb.asm.Type.getType(PanacheEntity.class),
            org.objectweb.asm.Type.getType(PanacheCompanion.class));

    static org.objectweb.asm.Type sanitize(org.objectweb.asm.Type[] argumentTypes) {
        org.objectweb.asm.Type primitiveReplaced = null;

        for (int i = 0; i < argumentTypes.length; i++) {
            if (REPOSITORY_TYPES.contains(argumentTypes[i])) {
                argumentTypes[i] = CLASS_TYPE;
            } else if (ENTITY_TYPES.contains(argumentTypes[i])) {
                argumentTypes[i] = OBJECT_TYPE;
            } else if (argumentTypes[i].getInternalName().length() == 1) {
                primitiveReplaced = argumentTypes[i];
                argumentTypes[i] = OBJECT_TYPE;
            }
        }
        return primitiveReplaced;
    }

    @NotNull
    static String toBinarySignature(Class<?> type) {
        return org.objectweb.asm.Type.getType(type).getDescriptor();
    }

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.HIBERNATE_ORM_PANACHE_KOTLIN);
    }

    @BuildStep
    List<AdditionalJpaModelBuildItem> produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return Collections.singletonList(new AdditionalJpaModelBuildItem(PanacheEntity.class));
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
    @Record(ExecutionTime.STATIC_INIT)
    void build(PanacheKotlinHibernateOrmRecorder recorder,
            CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            HibernateEnhancersRegisteredBuildItem hibernateMarker,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems,
            Optional<JpaModelPersistenceUnitMappingBuildItem> jpaModelPersistenceUnitMapping) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        KotlinPanacheRepositoryEnhancer daoEnhancer = new KotlinPanacheRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        Set<String> panacheEntities = new HashSet<>();

        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(PANACHE_REPOSITORY)) {
                continue;
            }
            if (daoEnhancer.skipRepository(classInfo)) {
                continue;
            }
            List<org.jboss.jandex.Type> typeParameters = JandexUtil
                    .resolveTypeParameters(classInfo.name(), PANACHE_REPOSITORY_BASE, index.getIndex());
            panacheEntities.add(typeParameters.get(0).name().toString());
            daoClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_REPOSITORY)) {
            if (daoEnhancer.skipRepository(classInfo)) {
                continue;
            }
            List<org.jboss.jandex.Type> typeParameters = JandexUtil
                    .resolveTypeParameters(classInfo.name(), PANACHE_REPOSITORY, index.getIndex());
            panacheEntities.add(typeParameters.get(0).name().toString());
            daoClasses.add(classInfo.name().toString());
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        KotlinPanacheCompanionEnhancer companionEnhancer = new KotlinPanacheCompanionEnhancer(index.getIndex(),
                methodCustomizers);
        KotlinPanacheEntityEnhancer entityEnhancer = new KotlinPanacheEntityEnhancer(index.getIndex(), methodCustomizers);
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheEntity if we ask for subtypes of PanacheEntityBase
        // NOTE: we don't skip abstract/generic entities because they still need accessors
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_COMPANION)) {
            if (classInfo.name().equals(PANACHE_ENTITY)) {
                continue;
            }
            String className = classInfo.name().toString();
            if (modelClasses.add(className)) {
                transformers.produce(new BytecodeTransformerBuildItem(className, companionEnhancer));
            }
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_ENTITY_BASE)) {
            if (classInfo.name().equals(PANACHE_ENTITY)) {
                continue;
            }
            String className = classInfo.name().toString();
            if (modelClasses.add(className)) {
                entityEnhancer.collectFields(classInfo);
                transformers.produce(new BytecodeTransformerBuildItem(className, entityEnhancer));
            }
        }

        MetamodelInfo<EntityModel<EntityField>> modelInfo = entityEnhancer.getModelInfo();
        if (modelInfo.hasEntities()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(modelInfo);
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }

        Map<String, String> panacheEntityToPersistenceUnit = new HashMap<>();
        panacheEntities.addAll(modelClasses);

        if (jpaModelPersistenceUnitMapping.isPresent()) {
            Map<String, Set<String>> collectedEntityToPersistenceUnits = jpaModelPersistenceUnitMapping.get()
                    .getEntityToPersistenceUnits();
            Map<String, Set<String>> violatingPanacheEntities = new TreeMap<>();

            for (Map.Entry<String, Set<String>> entry : collectedEntityToPersistenceUnits.entrySet()) {
                String entityName = entry.getKey();
                Set<String> selectedPersistenceUnits = entry.getValue();
                boolean isPanacheEntity = panacheEntities.stream().anyMatch(name -> name.equals(entityName));

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
