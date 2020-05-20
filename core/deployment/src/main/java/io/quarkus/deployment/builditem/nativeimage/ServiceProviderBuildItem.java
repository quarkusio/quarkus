package io.quarkus.deployment.builditem.nativeimage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        return new ServiceProviderBuildItem(serviceInterfaceClassName, new ArrayList<>(classNames));
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
