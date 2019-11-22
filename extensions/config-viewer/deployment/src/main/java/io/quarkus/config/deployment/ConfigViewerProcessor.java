package io.quarkus.config.deployment;

import javax.inject.Singleton;

import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.config.ConfigHolder;
import io.quarkus.config.ConfigViewerHandler;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;

/** Produces a Vert.x route with a handler, which lists all configured properties. */
class ConfigViewerProcessor {

    private static final DotName SINGLETON = DotName.createSimple(Singleton.class.getName());
    private static final String FEATURE = "config-viewer";

    /** The configuration for config extension. */
    ConfigConfig config;

    @ConfigRoot(name = "config")
    static final class ConfigConfig {

        /** The path of the config servlet. */
        @ConfigItem(defaultValue = "/config")
        String path;
    }

    @BuildStep
    public void build(LaunchModeBuildItem launchMode,
            BuildProducer<FeatureBuildItem> feature,
            BuildProducer<AdditionalBeanBuildItem> beans,
            BuildProducer<RouteBuildItem> routes) {

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            feature.produce(new FeatureBuildItem(FEATURE));
            beans.produce(AdditionalBeanBuildItem.builder()
                    .addBeanClass(ConfigHolder.class)
                    .setDefaultScope(SINGLETON)
                    .setUnremovable()
                    .build());
            routes.produce(new RouteBuildItem(config.path, new ConfigViewerHandler(), HandlerType.BLOCKING));
        }
    }
}
