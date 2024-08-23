package io.quarkus.deployment.builditem;

import io.quarkus.builder.item.MultiBuildItem;

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
        this.name = name;
        this.data = data;
        this.excludeFromDevCL = false;
    }

    public GeneratedResourceBuildItem(String name, byte[] data, boolean excludeFromDevCL) {
        this.name = name;
        this.data = data;
        this.excludeFromDevCL = excludeFromDevCL;
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
