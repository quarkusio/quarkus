package io.quarkus.deployment.builditem.substrate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Represents a Service Provider registration.
 * When processed, it embeds the service interface descriptor (META-INF/services/...) and allow reflection (instantiation only)
 * on a set of provider
 * classes.
 * 
 * @deprecated Use {@link io.quarkus.deployment.builditem.nativeimage.ServiceProviderBuildItem ServiceProviderBuildItem}
 *             instead.
 */
@Deprecated
public final class ServiceProviderBuildItem extends MultiBuildItem {

    @Deprecated
    public static final String SPI_ROOT = "META-INF/services/";
    private final String serviceInterface;
    private final List<String> providers;

    public ServiceProviderBuildItem(String serviceInterfaceClassName, String... providerClassNames) {
        this(serviceInterfaceClassName, Arrays.asList(providerClassNames));
    }

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

    public String getServiceInterface() {
        return serviceInterface;
    }

    public List<String> providers() {
        return providers;
    }

    public String serviceDescriptorFile() {
        return SPI_ROOT + serviceInterface;
    }
}
