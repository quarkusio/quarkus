package io.quarkus.deployment.builditem.nativeimage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.util.ServiceUtil;

/**
 * Represents a Service Provider registration.
 * When processed, it embeds the service interface descriptor (META-INF/services/...) in the native image
 * and registers the classes returned by {@link #providers()} for reflection (instantiation only).
 */
public final class ServiceProviderBuildItem extends MultiBuildItem {

    public static final String SPI_ROOT = "META-INF/services/";
    private final String serviceInterface;
    private final List<String> providers;

    /**
     * Creates and returns a {@link ServiceProviderBuildItem} for the {@code serviceInterfaceClassName} by including
     * all the providers that are listed in the service interface descriptor file.
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @param serviceInterfaceDescriptorFile the path to the service interface descriptor file
     * @return
     * @throws IOException
     */
    public static ServiceProviderBuildItem allProviders(final String serviceInterfaceClassName,
            final Path serviceInterfaceDescriptorFile)
            throws IOException {
        if (serviceInterfaceClassName == null || serviceInterfaceClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("service interface name cannot be null or blank");
        }
        if (serviceInterfaceDescriptorFile == null) {
            throw new IllegalArgumentException("service interface descriptor file path cannot be null");
        }
        final Set<String> classNames = new LinkedHashSet<>();
        final List<String> lines = Files.readAllLines(serviceInterfaceDescriptorFile, StandardCharsets.UTF_8);
        // parse each line and add each listed provider
        for (String line : lines) {
            final int commentIndex = line.indexOf('#');
            if (commentIndex >= 0) {
                // strip off anything after the # (including the #)
                line = line.substring(0, commentIndex);
            }
            line = line.trim();
            if (line.length() != 0) {
                classNames.add(line);
            }
        }
        return new ServiceProviderBuildItem(serviceInterfaceClassName, List.copyOf(classNames), false);
    }

    /**
     * Creates and returns a new {@link ServiceProviderBuildItem} for the given {@code serviceInterfaceClassName} by
     * including all the providers that are listed in service interface descriptor files
     * {@code "META-INF/services/" + serviceInterfaceClassName} findable in the Context Class Loader of the current
     * thread.
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @return a new {@link ServiceProviderBuildItem}
     * @throws RuntimeException wrapping any {@link IOException}s thrown when accessing class path resources
     */
    public static ServiceProviderBuildItem allProvidersFromClassPath(final String serviceInterfaceClassName) {
        if (serviceInterfaceClassName == null || serviceInterfaceClassName.trim().isEmpty()) {
            throw new IllegalArgumentException("service interface name cannot be null or blank");
        }
        final String resourcePath = SPI_ROOT + serviceInterfaceClassName;
        try {
            Set<String> implementations = ServiceUtil.classNamesNamedIn(
                    Thread.currentThread().getContextClassLoader(),
                    resourcePath);
            return new ServiceProviderBuildItem(
                    serviceInterfaceClassName,
                    List.copyOf(implementations),
                    false);
        } catch (IOException e) {
            throw new RuntimeException("Could not read class path resources having path '" + resourcePath + "'", e);
        }
    }

    /**
     * Registers the specified service interface descriptor to be embedded and allow reflection (instantiation only)
     * of the specified provider classes. Note that the service interface descriptor file has to exist and match the
     * list of specified provider class names.
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @param providerClassNames the list of provider class names that must already be mentioned in the file
     */
    public ServiceProviderBuildItem(String serviceInterfaceClassName, String... providerClassNames) {
        this(serviceInterfaceClassName, List.of(providerClassNames), false);
    }

    /**
     * Registers the specified service interface descriptor to be embedded and allow reflection (instantiation only)
     * of the specified provider classes. Note that the service interface descriptor file has to exist and match the
     * list of specified provider class names.
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @param providers a collection of provider class names that must already be mentioned in the file
     */
    public ServiceProviderBuildItem(String serviceInterfaceClassName, Collection<String> providers) {
        this(serviceInterfaceClassName, List.copyOf(providers), false);
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
        this(serviceInterfaceClassName, List.copyOf(providers), false);
    }

    /**
     * An internal overload that must be called with an immutable {@link List} of {@code providers}
     *
     * @param serviceInterfaceClassName the interface whose service interface descriptor file we want to embed
     * @param providers the list of provider class names that must already be mentioned in the file
     * @param marker just a way to differentiate this constructor from {@link #ServiceProviderBuildItem(String, List)};
     *        the value is ignored
     */
    private ServiceProviderBuildItem(String serviceInterfaceClassName, List<String> providers, boolean marker) {
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

    /**
     * @return an immutable {@link List} of provider class names
     */
    public List<String> providers() {
        return providers;
    }

    /**
     * @return the resource path for the service descriptor file
     */
    public String serviceDescriptorFile() {
        return SPI_ROOT + serviceInterface;
    }
}
