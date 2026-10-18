package io.quarkus.deployment.builditem;

import java.util.Map;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A build item that allows extensions to register additional resources that should be available
 * from the ClassLoader at runtime.
 * <p>
 * These resources are typically generated or discovered during the build process and are not
 * located in the standard {@code src/main/resources} directory. Multiple instances of this
 * build item can be produced, and all registered resources will be aggregated.
 * <p>
 * The key of the map represents the resource path (e.g., {@code META-INF/my-config.properties}),
 * and the value is the byte content of the resource.
 */
public final class AdditionalClassLoaderResourcesBuildItem extends MultiBuildItem {

    /**
     * A map where keys are resource paths and values are the corresponding resource content as byte arrays.
     */
    final Map<String, byte[]> resources;

    /**
     * Constructs a new {@link AdditionalClassLoaderResourcesBuildItem}.
     *
     * @param resources A map containing the resource paths and their byte content.
     *        The keys are the paths where the resources should be accessible via the ClassLoader,
     *        and the values are the raw byte data of the resources.
     */
    public AdditionalClassLoaderResourcesBuildItem(Map<String, byte[]> resources) {
        this.resources = resources;
    }

    /**
     * Returns the map of additional resources to be added to the ClassLoader.
     *
     * @return A map where keys are resource paths (String) and values are resource content (byte[]).
     */
    public Map<String, byte[]> getResources() {
        return resources;
    }

}