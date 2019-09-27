package io.quarkus.mongodb.panache.deployment;

import java.util.HashSet;
import java.util.Set;

import org.bson.types.ObjectId;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.mongodb.panache.PanacheMongoEntity;
import io.quarkus.mongodb.panache.PanacheMongoEntityBase;
import io.quarkus.mongodb.panache.PanacheMongoRepository;
import io.quarkus.mongodb.panache.PanacheMongoRepositoryBase;
import io.quarkus.mongodb.panache.jackson.ObjectMapperProducer;
import io.quarkus.mongodb.panache.jsonb.PanacheMongoJsonbContextResolver;
import io.quarkus.panache.common.deployment.PanacheFieldAccessEnhancer;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;

public class PanacheResourceProcessor {
    static final DotName DOTNAME_PANACHE_REPOSITORY_BASE = DotName.createSimple(PanacheMongoRepositoryBase.class.getName());
    private static final DotName DOTNAME_PANACHE_REPOSITORY = DotName.createSimple(PanacheMongoRepository.class.getName());
    static final DotName DOTNAME_PANACHE_ENTITY_BASE = DotName.createSimple(PanacheMongoEntityBase.class.getName());
    private static final DotName DOTNAME_PANACHE_ENTITY = DotName.createSimple(PanacheMongoEntity.class.getName());

    private static final DotName DOTNAME_OBJECT_ID = DotName.createSimple(ObjectId.class.getName());

    @BuildStep(providesCapabilities = "io.quarkus.mongodb.panache")
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.MONGODB_PANACHE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerJacksonSerDer(Capabilities capabilities) {
        if (capabilities.isCapabilityPresent("io.quarkus.resteasy.jackson")) {
            return AdditionalBeanBuildItem.unremovableOf(ObjectMapperProducer.class);
        }
        return null;
    }

    @BuildStep
    ResteasyJaxrsProviderBuildItem registerJsonbSerDer(Capabilities capabilities) {
        if (capabilities.isCapabilityPresent("io.quarkus.resteasy.jsonb")) {
            return new ResteasyJaxrsProviderBuildItem(PanacheMongoJsonbContextResolver.class.getName());
        }
        return null;
    }

    @BuildStep
    ReflectiveHierarchyBuildItem registerForReflection(CombinedIndexBuildItem index) {
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        IndexingUtil.indexClass(ObjectId.class.getName(), indexer, index.getIndex(), additionalIndex,
                PanacheResourceProcessor.class.getClassLoader());
        CompositeIndex compositeIndex = CompositeIndex.create(index.getIndex(), indexer.complete());
        Type type = Type.create(DOTNAME_OBJECT_ID, Type.Kind.CLASS);
        return new ReflectiveHierarchyBuildItem(type, compositeIndex);
    }

    @BuildStep
    void build(CombinedIndexBuildItem index,
            ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers) throws Exception {

        PanacheMongoRepositoryEnhancer daoEnhancer = new PanacheMongoRepositoryEnhancer(index.getIndex());
        Set<String> daoClasses = new HashSet<>();
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY_BASE)) {
            // Skip PanacheRepository
            if (classInfo.name().equals(DOTNAME_PANACHE_REPOSITORY))
                continue;
            daoClasses.add(classInfo.name().toString());
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(DOTNAME_PANACHE_REPOSITORY)) {
            daoClasses.add(classInfo.name().toString());
        }
        for (String daoClass : daoClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(daoClass, daoEnhancer));
        }

        PanacheMongoEntityEnhancer modelEnhancer = new PanacheMongoEntityEnhancer(index.getIndex());
        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheMongoEntity if we ask for subtypes of PanacheMongoEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownSubclasses(DOTNAME_PANACHE_ENTITY_BASE)) {
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
        }

        if (!modelEnhancer.entities.isEmpty()) {
            PanacheFieldAccessEnhancer panacheFieldAccessEnhancer = new PanacheFieldAccessEnhancer(
                    modelEnhancer.getModelInfo());
            for (ClassInfo classInfo : applicationIndex.getIndex().getKnownClasses()) {
                String className = classInfo.name().toString();
                if (!modelClasses.contains(className)) {
                    transformers.produce(new BytecodeTransformerBuildItem(className, panacheFieldAccessEnhancer));
                }
            }
        }
    }
}
