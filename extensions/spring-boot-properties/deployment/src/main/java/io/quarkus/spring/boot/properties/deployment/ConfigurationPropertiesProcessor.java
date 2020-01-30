package io.quarkus.spring.boot.properties.deployment;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.springframework.boot.context.properties.ConfigurationProperties;

import io.quarkus.arc.config.ConfigProperties;
import io.quarkus.arc.deployment.configproperties.ConfigPropertiesMetadataBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class ConfigurationPropertiesProcessor {

    private static final DotName CONFIGURATION_PROPERTIES_ANNOTATION = DotName
            .createSimple(ConfigurationProperties.class.getName());

    @BuildStep
    public FeatureBuildItem registerFeature() {
        return new FeatureBuildItem(FeatureBuildItem.SPRING_BOOT_PROPERTIES);
    }

    @BuildStep
    public void produceConfigPropertiesMetadata(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ConfigPropertiesMetadataBuildItem> configPropertiesMetadataProducer) {
        combinedIndex.getIndex()
                .getAnnotations(CONFIGURATION_PROPERTIES_ANNOTATION)
                .stream()
                .map(annotation -> createConfigPropertiesMetadata(annotation, combinedIndex.getIndex()))
                .forEach(configPropertiesMetadataProducer::produce);
    }

    private ConfigPropertiesMetadataBuildItem createConfigPropertiesMetadata(AnnotationInstance annotation, IndexView index) {
        switch (annotation.target().kind()) {
            case CLASS:
                return createConfigPropertiesMetadataFromClass(annotation);
            case METHOD:
                return createConfigPropertiesMetadataFromMethod(annotation, index);
            default:
                throw new IllegalArgumentException("Unsupported annotation target kind " + annotation.target().kind().name());
        }
    }

    private ConfigPropertiesMetadataBuildItem createConfigPropertiesMetadataFromClass(AnnotationInstance annotation) {
        return new ConfigPropertiesMetadataBuildItem(annotation.target().asClass(), getPrefix(annotation),
                ConfigProperties.NamingStrategy.VERBATIM);
    }

    private ConfigPropertiesMetadataBuildItem createConfigPropertiesMetadataFromMethod(AnnotationInstance annotation,
            IndexView index) {
        return new ConfigPropertiesMetadataBuildItem(index.getClassByName(annotation.target().asMethod().returnType().name()),
                getPrefix(annotation), ConfigProperties.NamingStrategy.VERBATIM);
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
