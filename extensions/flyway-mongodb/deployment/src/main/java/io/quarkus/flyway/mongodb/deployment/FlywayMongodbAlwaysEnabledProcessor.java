package io.quarkus.flyway.mongodb.deployment;

import org.flywaydb.core.extensibility.Plugin;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.IndexDependencyBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem;

public class FlywayMongodbAlwaysEnabledProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.FLYWAY_MONGODB);
    }

    @BuildStep
    IndexDependencyBuildItem indexFlywayArtifacts() {
        // we need to index all Flyway dependencies
        return new IndexDependencyBuildItem("org.flywaydb", null);
    }

    @BuildStep
    public ServiceProviderBuildItem flywayPlugins() {
        return ServiceProviderBuildItem.allProvidersFromClassPath(Plugin.class.getName());
    }

}
