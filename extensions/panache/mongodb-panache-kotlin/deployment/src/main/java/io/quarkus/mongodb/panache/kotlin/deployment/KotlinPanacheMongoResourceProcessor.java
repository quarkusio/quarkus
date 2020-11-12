package io.quarkus.mongodb.panache.kotlin.deployment;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.jandex.ClassInfo;

import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationIndexBuildItem;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CapabilityBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.mongodb.panache.deployment.BasePanacheMongoResourceProcessor;
import io.quarkus.mongodb.panache.deployment.PropertyMappingClassBuildStep;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizerBuildItem;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;
import io.quarkus.panache.common.deployment.TypeBundle;

public class KotlinPanacheMongoResourceProcessor extends BasePanacheMongoResourceProcessor {
    public static final KotlinImperativeTypeBundle IMPERATIVE_TYPE_BUNDLE = new KotlinImperativeTypeBundle();
    public static final KotlinReactiveTypeBundle REACTIVE_TYPE_BUNDLE = new KotlinReactiveTypeBundle();

    @BuildStep
    public void buildImperativeCompanions(CombinedIndexBuildItem index, ApplicationIndexBuildItem applicationIndex,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        processCompanions(index, transformers, reflectiveClass, propertyMappingClass,
                createCompanionEnhancer(index, methodCustomizers),
                getImperativeTypeBundle());
    }

    @BuildStep
    public void buildReactiveCompanions(CombinedIndexBuildItem index,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            List<PanacheMethodCustomizerBuildItem> methodCustomizersBuildItems) {

        List<PanacheMethodCustomizer> methodCustomizers = methodCustomizersBuildItems.stream()
                .map(bi -> bi.getMethodCustomizer()).collect(Collectors.toList());

        processCompanions(index, transformers, reflectiveClass, propertyMappingClass,
                createReactiveCompanionEnhancer(index, methodCustomizers),
                getReactiveTypeBundle());
    }

    protected KotlinImperativeTypeBundle getImperativeTypeBundle() {
        return IMPERATIVE_TYPE_BUNDLE;
    }

    protected KotlinReactiveTypeBundle getReactiveTypeBundle() {
        return REACTIVE_TYPE_BUNDLE;
    }

    @BuildStep
    protected CapabilityBuildItem capability() {
        return new CapabilityBuildItem(Capability.MONGODB_PANACHE_KOTLIN);
    }

    @BuildStep
    protected FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.MONGODB_PANACHE_KOTLIN);
    }

    @Override
    public PanacheEntityEnhancer<?> createEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new KotlinPanacheMongoEntityEnhancer(index.getIndex(), methodCustomizers, getImperativeTypeBundle());
    }

    private PanacheMongoCompanionEnhancer createCompanionEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new PanacheMongoCompanionEnhancer(index.getIndex(), methodCustomizers, getImperativeTypeBundle());
    }

    private PanacheMongoCompanionEnhancer createReactiveCompanionEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new PanacheMongoCompanionEnhancer(index.getIndex(), methodCustomizers, getReactiveTypeBundle());
    }

    @Override
    public PanacheEntityEnhancer<?> createReactiveEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new KotlinPanacheMongoEntityEnhancer(index.getIndex(), methodCustomizers, getImperativeTypeBundle());
    }

    @Override
    public PanacheRepositoryEnhancer createRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new KotlinPanacheMongoRepositoryEnhancer(index.getIndex(), getImperativeTypeBundle());
    }

    @Override
    public PanacheRepositoryEnhancer createReactiveRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new KotlinPanacheMongoRepositoryEnhancer(index.getIndex(), getReactiveTypeBundle());
    }

    private void processCompanions(CombinedIndexBuildItem index,
            BuildProducer<BytecodeTransformerBuildItem> transformers,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<PropertyMappingClassBuildStep> propertyMappingClass,
            PanacheEntityEnhancer<?> entityEnhancer, TypeBundle typeBundle) {

        Set<String> modelClasses = new HashSet<>();
        // Note that we do this in two passes because for some reason Jandex does not give us subtypes
        // of PanacheMongoEntity if we ask for subtypes of PanacheMongoEntityBase
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(typeBundle.entityCompanionBase().dotName())) {
            if (classInfo.name().equals(typeBundle.entityCompanion().dotName())) {
                continue;
            }
            if (modelClasses.add(classInfo.name().toString()))
                entityEnhancer.collectFields(classInfo);
        }
        for (ClassInfo classInfo : index.getIndex().getAllKnownImplementors(typeBundle.entityCompanion().dotName())) {
            if (modelClasses.add(classInfo.name().toString()))
                entityEnhancer.collectFields(classInfo);
        }

        // iterate over all the entity classes
        for (String modelClass : modelClasses) {
            transformers.produce(new BytecodeTransformerBuildItem(modelClass, entityEnhancer));

            //register for reflection entity classes
            reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, modelClass));

            // Register for building the property mapping cache
            propertyMappingClass.produce(new PropertyMappingClassBuildStep(modelClass));
        }
    }
}
