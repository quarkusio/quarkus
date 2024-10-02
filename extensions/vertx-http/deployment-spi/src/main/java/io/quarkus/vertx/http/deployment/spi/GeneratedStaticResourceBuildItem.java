package io.quarkus.vertx.http.deployment.spi;

import static java.util.Objects.requireNonNull;

import java.nio.file.Path;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;

/**
 * This build item aims to be used by extensions to generate static resources.
 * <br>
 * Those resources will be served on the given {@link GeneratedStaticResourceBuildItem#endpoint}. It is NOT necessary to create
 * the
 * file on disk.
 * <br>
 * Behind the scenes the build step will take care of add those resources to the final build, through
 * {@link AdditionalStaticResourceBuildItem}, {@link NativeImageResourceBuildItem}
 * and {@link io.quarkus.deployment.builditem.GeneratedResourceBuildItem} build items.
 * <br>
 * The value of {@code endpoint} should be prefixed with {@code '/'}.
 */
public final class GeneratedStaticResourceBuildItem extends MultiBuildItem {

    private final String endpoint;

    private final Path file;
    private final byte[] content;

    private GeneratedStaticResourceBuildItem(final String endpoint, final byte[] content, final Path file) {
        if (!requireNonNull(endpoint, "endpoint is required").startsWith("/")) {
            throw new IllegalArgumentException("endpoint must start with '/'");
        }
        this.endpoint = endpoint;
        this.file = file;
        this.content = content;
    }

    /**
     * The resource will be served at {@code '{quarkus.http.root-path}{endpoint}'}
     *
     * @param endpoint the endpoint from the {@code '{quarkus.http.root-path}'} for this generated static resource. It should be
     *        prefixed with {@code '/'}
     * @param content the content of this generated static resource
     */
    public GeneratedStaticResourceBuildItem(final String endpoint, final byte[] content) {
        this(endpoint, content, null);
    }

    /**
     * The resource will be served at {root-path}{path}
     *
     * @param endpoint the endpoint from the {@code '{quarkus.http.root-path}'} for this generated static resource. It should be
     *        prefixed with {@code '/'}
     * @param file the file Path on the local filesystem
     */
    public GeneratedStaticResourceBuildItem(final String endpoint, final Path file) {
        this(endpoint, null, file);
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    public boolean isFile() {
        return file != null;
    };

    public byte[] getContent() {
        return this.content;
    }

    public Path getFile() {
        return file;
    }

    public String getFileAbsolutePath() {
        return file.toAbsolutePath().toString();
    }
}
