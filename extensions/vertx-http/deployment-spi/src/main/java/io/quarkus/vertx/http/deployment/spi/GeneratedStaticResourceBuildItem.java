package io.quarkus.vertx.http.deployment.spi;

import static java.util.Objects.requireNonNull;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

/**
 * This build item aims to be used by extensions to generate static resources.
 * <br>
 * Those resources will be served on the given {@link GeneratedStaticResourceBuildItem#path}. It is NOT necessary to create the
 * file on disk.
 * <br>
 * Behind the scenes the build step will take care of add those resources to the final build, through
 * {@link AdditionalStaticResourceBuildItem}, {@link NativeImageResourceBuildItem}
 * and {@link io.quarkus.deployment.builditem.GeneratedResourceBuildItem} build items.
 * <br>
 * The value of {@code path} should be prefixed with {@code '/'}.
 */
public final class GeneratedStaticResourceBuildItem extends MultiBuildItem {

    private final String path;
    private final byte[] content;

    public GeneratedStaticResourceBuildItem(final String path, final byte[] content) {
        if (!requireNonNull(path, "path is required").startsWith("/")) {
            throw new IllegalArgumentException("path must start with '/'");
        }
        this.path = path;
        this.content = content;
    }

    public String getPath() {
        return this.path;
    }

    public byte[] getContent() {
        return this.content;
    }

}
