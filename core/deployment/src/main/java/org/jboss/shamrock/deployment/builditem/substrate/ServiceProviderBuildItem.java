package org.jboss.shamrock.deployment.builditem.substrate;

import org.jboss.builder.item.MultiBuildItem;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Represents a Service Provider registration.
 * When processed, it embeds the service interface descriptor (META-INF/services/...) and allow reflection (instantiation only) on a set of provider
 * classes.
 */
public final class ServiceProviderBuildItem extends MultiBuildItem {

    public static final String SPI_ROOT = "META-INF/services/";
    private final String serviceInterface;
    private final List<String> providers;

    public ServiceProviderBuildItem(String serviceInterfaceClassName, String... providerClassNames) {
        serviceInterface = Objects.requireNonNull(serviceInterfaceClassName, "The service interface must not be `null`");
        providers = Arrays.asList(Objects.requireNonNull(providerClassNames));

        // Validation
        if (serviceInterface.length() == 0) {
            throw new IllegalArgumentException("The serviceDescriptorFile interface cannot be blank");
        }

        providers.forEach(s -> {
            if (s == null || s.length() == 0) {
                throw new IllegalArgumentException("The provider class name cannot be blank");
            }
        });
    }

    public List<String> providers() {
        return providers;
    }

    public String serviceDescriptorFile() {
        return SPI_ROOT + serviceInterface;
    }
}
