package io.quarkus.hibernate.orm.panache.deployment;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.util.JandexUtil;
import io.quarkus.hibernate.orm.deployment.AdditionalJpaModelBuildItem;
import io.quarkus.hibernate.orm.deployment.HibernateEnhancersRegisteredBuildItem;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import io.quarkus.hibernate.orm.panache.PanacheHibernateRecorder;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.deployment.EntityField;
import io.quarkus.panache.common.deployment.EntityModel;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityClassesBuildItem;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public final class PanacheResourceProcessor {

    static final DotName DOTNAME_PANACHE_REPOSITORY_BASE = DotName.createSimple(PanacheRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_REPOSITORY = DotName.createSimple(PanacheRepository.class.getName());
    static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheEntity.class.getName());

    private static final DotName DOTNAME_ENTITY_MANAGER = DotName.createSimple(EntityManager.class.getName());

    private static final DotName DOTNAME_NAMED_QUERY = DotName.createSimple(NamedQuery.class.getName());
    private static final DotName DOTNAME_NAMED_QUERIES = DotName.createSimple(NamedQueries.class.getName());
    private static final DotName DOTNAME_OBJECT = DotName.createSimple(Object.class.getName());

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.HIBERNATE_ORM_PANACHE);
    }

    @BuildStep
    List<AdditionalJpaModelBuildItem> produceModel() {
        // only useful for the index resolution: hibernate will register it to be transformed, but BuildMojo
        // only transforms classes from the application jar, so we do our own transforming
        return Collections.singletonList(
                new AdditionalJpaModelBuildItem(PanacheEntity.class));
    }

    @BuildStep
    UnremovableBeanBuildItem ensureBeanLookupAvailable() {
        return new UnremovableBeanBuildItem(new UnremovableBeanBuildItem.BeanTypeExclusion(DOTNAME_ENTITY_MANAGER));
    }

    @BuildStep
    void build(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            HibernateEnhancersRegisteredBuildItem hibernateMarker,
            BuildProducer<PanacheEntityClassesBuildItem> entityClasses,
            BuildProducer<NamedQueryEntityClassBuildStep> namedQueries,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) throws Exception {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        PanacheJpaRepositoryEnhancer daoEnhancer = new PanacheJpaRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        Set<Type> daoTypeParameters = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(DOTNAME_PANACHE_REPOSITORY))
                continue;
            if (PanacheRepositoryEnhancer.skipRepository(classInfo))
                continue;
            daoClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY)) {
            if (PanacheRepositoryEnhancer.skipRepository(classInfo))
                continue;
            daoClasses.add(classInfo.name().toString());
            daoTypeParameters.addAll(
                    JandexUtil.resolveTypeParameters(classInfo.name(), DOTNAME_PANACHE_REPOSITORY_BASE, index.getIndex()));
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        for (Type parameterType : daoTypeParameters) {
            // lookup for `@NamedQuery` on the hierarchy and produce NamedQueryEntityClassBuildStep
            Set<String> typeNamedQueries = new HashSet<>();
            lookupNamedQueries(index, parameterType.name(), typeNamedQueries);
            namedQueries.produce(new NamedQueryEntityClassBuildStep(parameterType.name().toString(), typeNamedQueries));
        }

        PanacheJpaEntityEnhancer modelEnhancer = new PanacheJpaEntityEnhancer(index.getIndex(), methodCustomizers);
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheEntity if we ask for subtypes of PanacheEntityBase
        // NOTE: we don't skip abstract/generic entities because they still need accessors
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
            // FIXME: should we really skip PanacheEntity or all MappedSuperClass?
            if (classInfo.name().equals(DOTNAME_PANACHE_ENTITY))
                continue;
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY)) {
            if (modelClasses.add(classInfo.name().toString()))
                modelEnhancer.collectFields(classInfo);
        }
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, modelEnhancer));

            // lookup for `@NamedQuery` on the hierarchy and produce NamedQueryEntityClassBuildStep
            Set<String> typeNamedQueries = new HashSet<>();
            lookupNamedQueries(index, DotName.createSimple(modelClass), typeNamedQueries);
            namedQueries.produce(new NamedQueryEntityClassBuildStep(modelClass, typeNamedQueries));
        }
        if (!modelClasses.isEmpty()) {
            entityClasses.produce(new PanacheEntityClassesBuildItem(modelClasses));
        }

        MetamodelInfo<EntityModel<EntityField>> modelInfo = modelEnhancer.getModelInfo();
        if (modelInfo.hasEntities()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(modelInfo);
            for (ClassInfo classInfo : index.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
    }

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void buildNamedQueryMap(List<NamedQueryEntityClassBuildStep> namedQueryEntityClasses,
            PanacheHibernateRecorder panacheHibernateRecorder) {
        Map<String, Set<String>> namedQueryMap = new HashMap<>();
        for (NamedQueryEntityClassBuildStep entityNamedQueries : namedQueryEntityClasses) {
            namedQueryMap.put(entityNamedQueries.getClassName(), entityNamedQueries.getNamedQueries());
        }

        panacheHibernateRecorder.setNamedQueryMap(namedQueryMap);
    }

    private void lookupNamedQueries(CombinedIndexBuildItem index, DotName name, Set<String> namedQueries) {
        ClassInfo classInfo = index.getIndex().getClassByName(name);
        if (classInfo == null) {
            return;
        }

        List<AnnotationInstance> namedQueryInstances = classInfo.annotations().get(DOTNAME_NAMED_QUERY);
        if (namedQueryInstances != null) {
            for (AnnotationInstance namedQueryInstance : namedQueryInstances) {
                namedQueries.add(namedQueryInstance.value("name").asString());
            }
        }

        List<AnnotationInstance> namedQueriesInstances = classInfo.annotations().get(DOTNAME_NAMED_QUERIES);
        if (namedQueriesInstances != null) {
            for (AnnotationInstance namedQueriesInstance : namedQueriesInstances) {
                AnnotationValue value = namedQueriesInstance.value();
                AnnotationInstance[] nestedInstances = value.asNestedArray();
                for (AnnotationInstance nested : nestedInstances) {
                    namedQueries.add(nested.value("name").asString());
                }
            }
        }

        // climb up the hierarchy of types
        if (!classInfo.superClassType().name().equals(DOTNAME_OBJECT)) {
            Type superType = classInfo.superClassType();
            ClassInfo superClass = index.getIndex().getClassByName(superType.name());
            lookupNamedQueries(index, superClass.name(), namedQueries);
        }
    }
}
