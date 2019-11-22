package io.quarkus.config.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.config.ConfigViewerServlet;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.undertow.deployment.ServletBuildItem;

/** Provides a servlet, which lists all configured properties. */
class ConfigViewerProcessor {

    private static final String FEATURE = "config";

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
            BuildProducer<ServletBuildItem> servlets) {

        if (launchMode.getLaunchMode() == LaunchMode.DEVELOPMENT) {
            feature.produce(new FeatureBuildItem(FEATURE));
            beans.produce(new AdditionalBeanBuildItem(ConfigViewerServlet.class));
            servlets.produce(ServletBuildItem.builder("config", ConfigViewerServlet.class.getName())
                    .addMapping(config.path)
                    .build());
        }
    }
}
