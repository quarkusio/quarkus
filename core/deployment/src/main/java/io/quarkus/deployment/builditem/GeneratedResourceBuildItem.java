package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Representing a resource file generated during the build
 */
public final class GeneratedResourceBuildItem extends MultiBuildItem {
    final String name;
    final byte[] data;

    public GeneratedResourceBuildItem(String name, byte[] data) {
        this(name, data, false);
    }

    private GeneratedResourceBuildItem(String name, byte[] data,
            boolean allowMetaInfServices) {
        if (name.startsWith("META-INF/services/") && !allowMetaInfServices) {
            throw new IllegalArgumentException(
                    "Use GeneratedServiceProviderBuildItem to register service providers instead of GeneratedResourceBuildItem, or use GeneratedResourceBuildItem.allowingMetaInfServices(...) if your "
                            + name + " resource is not a service provider");
        }
        this.name = name;
        this.data = data;
    }

    /**
     * Use only for {@code META-INF/services/} resources with a non-standard format incompatible with
     * {@link GeneratedServiceProviderBuildItem}. Prefer {@link GeneratedServiceProviderBuildItem} for standard
     * Java {@link java.util.ServiceLoader} registrations.
     */
    public static GeneratedResourceBuildItem allowingMetaInfServices(String name, byte[] data) {
        return new GeneratedResourceBuildItem(name, data, true);
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }
}
