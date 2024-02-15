package io.quarkus.kubernetes.deployment;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.kubernetes.spi.Property;

public class PropertyUtil {

    private static final Set<String> VISITED_EXTENSION_PROPERTIES = new HashSet<>();
    private static final Logger LOG = Logger.getLogger(PropertyUtil.class);

    public static <T> void printMessages(String usage, String platform, Property<T> kubernetesProperty,
            Optional<Property<T>> extensionProperty) {
        extensionProperty.ifPresent(p -> {
            printMessages(usage, platform, kubernetesProperty, p);
        });
    }

    public static <T> void printMessages(String usage, String platform, Property<T> kubernetesProperty,
            Property<T> extensionProperty) {
        if (!VISITED_EXTENSION_PROPERTIES.add(extensionProperty.getName())) {
            return;
        }

        String platformCapitalized = platform.replace("openshift", "OpenShift");
        platformCapitalized = platformCapitalized.substring(0, 1).toUpperCase() + platformCapitalized.substring(1);
        T kubernetesValue = kubernetesProperty.getValue().orElse(null);
        if (kubernetesValue == null) {
            // If no kubernetes property is provided, this will be used instead.
            String defaultOrProvided = extensionProperty.getValue().isPresent() ? "provided" : "default";
            String stringValue = String.valueOf(extensionProperty.getValue().orElse(extensionProperty.getDefaultValue()));
            LOG.infof("%s manifests are generated with '%s' having %s value '%s'. "
                    + "The app and manifests will get out of sync if the property '%s' is changed at runtime.",
                    platformCapitalized, usage, defaultOrProvided, stringValue, extensionProperty.getName());

        } else if (extensionProperty.getValue().filter(v -> !v.equals(kubernetesValue)).isPresent()) {
            // We have conflicting properties that need to be aligned. Maybe warn?
            String runtimeOrBuildTime = extensionProperty.isRuntime() ? "runtime" : "buildtime";
            LOG.debugf(
                    "%s property '%s' has been set with value '%s' while %s property '%s' is set with '%s'. %s will be set using the former.",
                    platformCapitalized, kubernetesProperty.getName(), kubernetesProperty.getValue().get(), runtimeOrBuildTime,
                    extensionProperty.getName(), extensionProperty.getValue().get(), usage);
        } else {
            // Both proeprties are present and aligned.
        }
    }
}
