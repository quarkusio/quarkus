package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * Representing a resource file generated during the build
 */
public final class GeneratedResourceBuildItem extends MultiBuildItem {
    final String name;
    final byte[] data;

    /**
     * This option is only meant to be set by extensions that also generated the resource on the file system
     * and must rely on Quarkus not getting in the way of loading that resource.
     * It is currently used by Kogito to get serving of static resources in Dev Mode by Vert.x
     * <br>
     *
     * @deprecated If you want to serve static resources use
     *             {@link io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem}
     *             instead.
     */
    @Deprecated
    final boolean excludeFromDevCL;

    public GeneratedResourceBuildItem(String name, byte[] data) {
        this(name, data, false);
    }

    public GeneratedResourceBuildItem(String name, byte[] data, boolean excludeFromDevCL) {
        this(name, data, excludeFromDevCL, false);
    }

    private GeneratedResourceBuildItem(String name, byte[] data, boolean excludeFromDevCL,
            boolean allowMetaInfServices) {
        if (name.startsWith("META-INF/services/") && !allowMetaInfServices) {
            throw new IllegalArgumentException(
                    "Use GeneratedServiceProviderBuildItem to register service providers instead of GeneratedResourceBuildItem, or use GeneratedResourceBuildItem.allowingMetaInfServices(...) if your "
                            + name + " resource is not a service provider");
        }
        this.name = name;
        this.data = data;
        this.excludeFromDevCL = excludeFromDevCL;
    }

    /**
     * Use only for {@code META-INF/services/} resources with a non-standard format incompatible with
     * {@link GeneratedServiceProviderBuildItem}. Prefer {@link GeneratedServiceProviderBuildItem} for standard
     * Java {@link java.util.ServiceLoader} registrations.
     */
    public static GeneratedResourceBuildItem allowingMetaInfServices(String name, byte[] data) {
        return new GeneratedResourceBuildItem(name, data, false, true);
    }

    public String getName() {
        return name;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * @deprecated use {@link GeneratedResourceBuildItem#getData} instead
     */
    @Deprecated(forRemoval = true)
    public byte[] getClassData() {
        return getData();
    }

    public boolean isExcludeFromDevCL() {
        return excludeFromDevCL;
    }
}
