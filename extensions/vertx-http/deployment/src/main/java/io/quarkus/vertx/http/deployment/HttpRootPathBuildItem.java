package io.quarkus.vertx.http.deployment;

import java.net.URI;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.deployment.util.UriNormalizationUtil;

public final class HttpRootPathBuildItem extends SimpleBuildItem {

    /**
     * Normalized from quarkus.http.root-path.
     * This path will always end in a slash
     */
    private final URI rootPath;

    public HttpRootPathBuildItem(String rootPath) {
        this.rootPath = UriNormalizationUtil.toURI(rootPath, true);
    }

    /**
     * Return normalized Http root path configured from {@literal quarkus.http.root-path}.
     * This path will always end in a slash.
     * <p>
     * Use {@link #resolvePath(String)} if you need to construct a Uri from the Http root path.
     *
     * @return Normalized Http root path ending with a slash
     * @see #resolvePath(String)
     */
    public String getRootPath() {
        return rootPath.getPath();
    }

    /**
     * Adjusts the path in relation to `quarkus.http.root-path`.
     * Any leading slash will be removed to resolve relatively.
     *
     * @deprecated Use {@code resolvePath} instead. Do not use this method. Will be removed in Quarkus 2.0
     */
    public String adjustPath(String path) {
        return resolvePath(path.startsWith("/") ? path.substring(1) : path);
    }

    /**
     * Resolve path into an absolute path.
     * If path is relative, it will be resolved against `quarkus.http.root-path`.
     * An absolute path will be normalized and returned.
     * <p>
     * Given {@literal quarkus.http.root-path=/}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * Given {@literal quarkus.http.root-path=/app}
     * <ul>
     * <li>{@code resolvePath("foo")} will return {@literal /app/foo}</li>
     * <li>{@code resolvePath("/foo")} will return {@literal /foo}</li>
     * </ul>
     * <p>
     * The returned path will not end with a slash.
     *
     * @param path Path to be resolved to an absolute path.
     * @return An absolute path not ending with a slash
     * @see UriNormalizationUtil#normalizeWithBase(URI, String, boolean)
     */
    public String resolvePath(String path) {
        return UriNormalizationUtil.normalizeWithBase(rootPath, path, false).getPath();
    }
}
