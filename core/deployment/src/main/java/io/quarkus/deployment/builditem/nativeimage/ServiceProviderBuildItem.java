package io.quarkus.deployment.builditem.nativeimage;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a Service Provider registration.
 * When processed, it embeds the service interface descriptor (META-INF/services/...) and allow reflection (instantiation only)
 * on a set of provider
 * classes.
 */
public final class ServiceProviderBuildItem extends MultiBuildItem {

    public static final String SPI_ROOT = "META-INF/services/";
    private final String serviceInterface;
    private final List<String> providers;

    /**
     * Registers the specified service interface descriptor to be embedded and allow reflection (instantiation only)
     * of the specified provider classes. Note that the service interface descriptor file has to exist and match the
     * list of specified provider class names.
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @param providerClassNames the list of provider class names that must already be mentioned in the file
     */
    public ServiceProviderBuildItem(String serviceInterfaceClassName, String... providerClassNames) {
        this(serviceInterfaceClassName, Arrays.asList(providerClassNames));
    }

    /**
     * Registers the specified service interface descriptor to be embedded and allow reflection (instantiation only)
     * of the specified provider classes. Note that the service interface descriptor file has to exist and match the
     * list of specified provider class names.
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @param providers the list of provider class names that must already be mentioned in the file
     */
    public ServiceProviderBuildItem(String serviceInterfaceClassName, List<String> providers) {
        this.serviceInterface = Objects.requireNonNull(serviceInterfaceClassName, "The service interface must not be `null`");
        this.providers = providers;

        // Validation
        if (serviceInterface.length() == 0) {
            throw new IllegalArgumentException("The serviceDescriptorFile interface cannot be blank");
        }

        providers.forEach(s -> {
            if (s == null || s.length() == 0) {
                throw new IllegalArgumentException("The provider class name cannot be null or blank");
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
