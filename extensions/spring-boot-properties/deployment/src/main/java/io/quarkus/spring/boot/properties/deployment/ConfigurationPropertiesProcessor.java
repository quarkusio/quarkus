package io.quarkus.spring.boot.properties.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.ArcConfig;
import io.quarkus.arc.deployment.configproperties.ConfigPropertiesMetadataBuildItem;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ConfigurationPropertiesProcessor {

    private static final DotName CONFIGURATION_PROPERTIES = DotName.createSimple(ConfigurationProperties.class.getName());

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(Feature.SPRING_BOOT_PROPERTIES);
    }

    @BuildStep
    public void produceConfigPropertiesMetadata(CombinedIndexBuildItem combinedIndex, ArcConfig arcConfig,
            BuildProducer<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataProducer) {
        ConfigProperties.NamingStrategy namingStrategy = arcConfig.configPropertiesDefaultNamingStrategy;
        for (AnnotationInstance annotation : combinedIndex.getIndex().getAnnotations(CONFIGURATION_PROPERTIES)) {
            configPropertiesMetadataProducer.produce(
                    createConfigPropertiesMetadata(annotation, combinedIndex.getIndex(), namingStrategy));
        }
    }

    private ConfigPropertiesMetadataBuildItem createConfigPropertiesMetadata(AnnotationInstance annotation,
            IndexView index, ConfigProperties.NamingStrategy namingStrategy) {
        switch (annotation.target().kind()) {
            case CLASS:
                return createConfigPropertiesMetadataFromClass(annotation, namingStrategy);
            case METHOD:
                return createConfigPropertiesMetadataFromMethod(annotation, index, namingStrategy);
            default:
                throw new IllegalArgumentException(
                        "Unsupported annotation target kind " + annotation.target().kind().name());
        }
    }

    private ConfigPropertiesMetadataBuildItem createConfigPropertiesMetadataFromClass(AnnotationInstance annotation,
            ConfigProperties.NamingStrategy namingStrategy) {
        return new ConfigPropertiesMetadataBuildItem(annotation.target().asClass(), getPrefix(annotation),
                namingStrategy, true, false);
    }

    private ConfigPropertiesMetadataBuildItem createConfigPropertiesMetadataFromMethod(AnnotationInstance annotation,
            IndexView index, ConfigProperties.NamingStrategy namingStrategy) {
        return new ConfigPropertiesMetadataBuildItem(
                index.getClassByName(annotation.target().asMethod().returnType().name()), getPrefix(annotation),
                namingStrategy, true, false);
    }

    private String getPrefix(AnnotationInstance annotation) {
        if (annotation.value() != null) {
            return annotation.value().asString();
        } else if (annotation.value("prefix") != null) {
            return annotation.value("prefix").asString();
        }

        return null;
    }
}
