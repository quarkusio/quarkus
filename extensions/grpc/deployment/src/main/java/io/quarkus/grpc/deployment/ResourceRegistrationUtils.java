package io.quarkus.grpc.deployment;

import java.util.regex.Pattern;

import org.eclipse.microprofile.config.Config;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

class ResourceRegistrationUtils {

    static void registerResourcesForProperties(Config config,
            BuildProducer<NativeImageResourceBuildItem> resourceBuildItem,
            Pattern... patterns) {
        for (String propertyName : config.getPropertyNames()) {
            for (Pattern pattern : patterns) {
                if (pattern.matcher(propertyName).matches()) {
                    String maybeResource = config.getValue(propertyName, String.class);
                    registerResourceForProperty(resourceBuildItem, maybeResource);
                }
            }
        }
    }

    static void registerResourceForProperty(BuildProducer<NativeImageResourceBuildItem> resourceBuildItem,
            String maybeResource) {
        if (isOnClassPath(maybeResource)) {
            resourceBuildItem.produce(new NativeImageResourceBuildItem(maybeResource));
        }
    }

    private static boolean isOnClassPath(String path) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path) != null;
    }

    private ResourceRegistrationUtils() {
    }
}
