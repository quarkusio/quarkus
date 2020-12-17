package io.quarkus.mongodb.panache.deployment;

import java.util.List;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.panache.common.deployment.MetamodelInfo;
import io.quarkus.panache.common.deployment.PanacheEntityEnhancer;
import io.quarkus.panache.common.deployment.PanacheMethodCustomizer;
import io.quarkus.panache.common.deployment.PanacheRepositoryEnhancer;

public class PanacheMongoResourceProcessor extends BasePanacheMongoResourceProcessor {
    public static final ImperativeTypeBundle IMPERATIVE_TYPE_BUNDLE = new ImperativeTypeBundle();
    public static final ReactiveTypeBundle REACTIVE_TYPE_BUNDLE = new ReactiveTypeBundle();

    protected ReactiveTypeBundle getReactiveTypeBundle() {
        return REACTIVE_TYPE_BUNDLE;
    }

    protected ImperativeTypeBundle getImperativeTypeBundle() {
        return IMPERATIVE_TYPE_BUNDLE;
    }

    @Override
    public PanacheMongoEntityEnhancer createEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers, MetamodelInfo modelInfo) {
        return new PanacheMongoEntityEnhancer(index.getIndex(), methodCustomizers, getImperativeTypeBundle(), modelInfo);
    }

    @Override
    public PanacheEntityEnhancer createReactiveEntityEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers, MetamodelInfo modelInfo) {
        return new PanacheMongoEntityEnhancer(index.getIndex(), methodCustomizers, getReactiveTypeBundle(), modelInfo);
    }

    @Override
    public PanacheMongoRepositoryEnhancer createReactiveRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new PanacheMongoRepositoryEnhancer(index.getIndex(), getReactiveTypeBundle());
    }

    @Override
    public PanacheRepositoryEnhancer createRepositoryEnhancer(CombinedIndexBuildItem index,
            List<PanacheMethodCustomizer> methodCustomizers) {
        return new PanacheMongoRepositoryEnhancer(index.getIndex(), getImperativeTypeBundle());
    }

    @BuildStep
    protected FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.MONGODB_PANACHE);
    }
}
