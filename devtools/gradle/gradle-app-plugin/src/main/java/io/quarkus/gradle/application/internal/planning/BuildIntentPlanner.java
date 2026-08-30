package io.quarkus.gradle.application.internal.planning;

import java.util.LinkedHashMap;
import java.util.Map;

import io.quarkus.gradle.application.model.QuarkusApplicationBuildDescriptor;
import io.quarkus.gradle.application.model.QuarkusApplicationBuildType;
import io.quarkus.gradle.application.model.QuarkusApplicationImageDescriptor;

public final class BuildIntentPlanner {

    public BuildIntent packageIntent(QuarkusApplicationBuildDescriptor descriptor,
            Map<String, String> commonProperties, Map<String, String> nativeArguments) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.putAll(commonProperties);
        properties.putAll(nativeArguments);
        if (descriptor.type().isNativeOutput()) {
            properties.put("quarkus.native.enabled", "true");
        }
        if (descriptor.type() == QuarkusApplicationBuildType.NATIVE_SOURCES) {
            properties.put("quarkus.native.sources-only", "true");
        }
        return new BuildIntent(properties);
    }

    public BuildIntent imageBuildIntent(QuarkusApplicationImageDescriptor image) {
        return imageIntent(image, false);
    }

    public BuildIntent imagePushIntent(QuarkusApplicationImageDescriptor image) {
        return imageIntent(image, true);
    }

    private static BuildIntent imageIntent(QuarkusApplicationImageDescriptor image, boolean push) {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("quarkus.container-image.build", "true");
        image.optionalBuilder()
                .ifPresent(builder -> properties.put("quarkus.container-image.builder", builder.quarkusBuilderName()));
        if (push) {
            properties.put("quarkus.container-image.push", "true");
        }
        return new BuildIntent(properties);
    }
}
