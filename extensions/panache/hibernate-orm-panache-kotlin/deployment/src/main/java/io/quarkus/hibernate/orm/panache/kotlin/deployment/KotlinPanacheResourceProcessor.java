package io.quarkus.hibernate.orm.panache.kotlin.deployment;

import static org.jboss.jandex.DotName.createSimple;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;
import org.jetbrains.annotations.NotNull;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepository;
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase;
import io.quarkus.hibernate.orm.panache.runtime.JpaOperations;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public final class KotlinPanacheResourceProcessor {

    private static final Set<DotName> UNREMOVABLE_BEANS = Collections.singleton(
            createSimple(EntityManager.class.getName()));
    static final String JPA_OPERATIONS = createSimple(JpaOperations.class.getName())
            .toString().replace(".", "/");

    static final DotName PANACHE_REPOSITORY_BASE_DOTNAME = createSimple(PanacheRepositoryBase.class.getName());
    static final DotName PANACHE_REPOSITORY_DOTNAME = createSimple(PanacheRepository.class.getName());

    static final DotName PANACHE_ENTITY_BASE_DOTNAME = createSimple(PanacheEntityBase.class.getName());
    static final DotName PANACHE_COMPANION_DOTNAME = createSimple(PanacheCompanion.class.getName());
    static final DotName PANACHE_ENTITY_DOTNAME = createSimple(PanacheEntity.class.getName());

    static final String PANACHE_REPOSITORY_BASE_SIGNATURE = toBinarySignature(PanacheRepositoryBase.class);
    static final String PANACHE_REPOSITORY_SIGNATURE = toBinarySignature(PanacheRepository.class);
    static final String PANACHE_COMPANION_SIGNATURE = toBinarySignature(PanacheCompanion.class);
    static final String PANACHE_ENTITY_SIGNATURE = toBinarySignature(PanacheEntity.class);

    static final String ID_TYPE_SIGNATURE = toBinarySignature(Long.class);
    static final String OBJECT_SIGNATURE = toBinarySignature(Object.class);
    static final String CLASS_SIGNATURE = toBinarySignature(Class.class);

    @NotNull
    static String toBinarySignature(Class<?> type) {
        return org.objectweb.asm.Type.getType(type).getDescriptor();
    }

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.HIBERNATE_ORM_PANACHE_KOTLIN);
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
    void build(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            HibernateEnhancersRegisteredBuildItem hibernateMarker,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        KotlinPanacheRepositoryEnhancer daoEnhancer = new KotlinPanacheRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_REPOSITORY_BASE_DOTNAME)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(PANACHE_REPOSITORY_DOTNAME))
                continue;
            if (PanacheRepositoryEnhancer.skipRepository(classInfo))
                continue;
            daoClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_REPOSITORY_DOTNAME)) {
            if (PanacheRepositoryEnhancer.skipRepository(classInfo))
                continue;
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
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(PANACHE_COMPANION_DOTNAME)) {
            if (classInfo.name().equals(PANACHE_ENTITY_DOTNAME))
                continue;
            if (modelClasses.add(classInfo.name().toString())) {
                transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), companionEnhancer));
            }
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(PANACHE_ENTITY_DOTNAME)) {
            if (classInfo.name().equals(PANACHE_ENTITY_DOTNAME))
                continue;
            if (modelClasses.add(classInfo.name().toString())) {
                entityEnhancer.collectFields(classInfo);
                transformers.produce(new BytecodeTransformerBuildItem(classInfo.name().toString(), entityEnhancer));
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
    }
}
