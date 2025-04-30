package io.quarkus.flyway.deployment;

import org.flywaydb.core.extensibility.Plugin;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeReinitializedClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class FlywayAlwaysEnabledProcessor {

    @BuildStep
    void build(BuildProducer<FeatureBuildItem> featureProducer) {
        featureProducer.produce(new FeatureBuildItem(Feature.FLYWAY));
    }

    /**
     * Reinitialize {@code InsertRowLock} to avoid using a cached seed when invoking {@code getNextRandomString}
     */
    @BuildStep
    public RuntimeReinitializedClassBuildItem reinitInsertRowLock() {
        return new RuntimeReinitializedClassBuildItem(
                "org.flywaydb.core.internal.database.InsertRowLock");
    }

    @BuildStep
    public NativeImageResourceBuildItem resources() {
        return new NativeImageResourceBuildItem("org/flywaydb/database/version.txt");
    }

    @BuildStep
    IndexDependencyBuildItem indexFlyway() {
        // we need to index all Flyway dependencies
        return new IndexDependencyBuildItem("org.flywaydb", null);
    }

    @BuildStep
    public ServiceProviderBuildItem flywayPlugins() {
        return ServiceProviderBuildItem.allProvidersFromClassPath(Plugin.class.getName());
    }
}
