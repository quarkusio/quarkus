package io.quarkus.deployment.util;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Common URI path resolution
 */
public class UriNormalizationUtil {
    private UriNormalizationUtil() {
    }

    /**
     * Create a URI path from a string. The specified path can not contain
     * relative {@literal ..} segments or {@literal %} characters.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code toUri("/", true)} will return a URI with path {@literal /}</li>
     * <li>{@code toUri("/", false)} will return a URI with an empty path {@literal /}</li>
     * <li>{@code toUri("./", true)} will return a URI with path {@literal /}</li>
     * <li>{@code toUri("./", false)} will return a URI with an empty path {@literal /}</li>
     * <li>{@code toUri("foo/", true)} will return a URI with path {@literal foo/}</li>
     * <li>{@code toUri("foo/", false)} will return a URI with an empty path {@literal foo}</li>
     * </ul>
     *
     *
     * @param path String to convert into a URI
     * @param trailingSlash true if resulting URI must end with a '/'
     * @throws IllegalArgumentException if the path contains invalid characters or path segments.
     */
    public static URI toURI(String path, boolean trailingSlash) {
        try {
            // replace inbound // with /
            path = path.replaceAll("//", "/");
            // remove trailing slash if result shouldn't have one
            if (!trailingSlash && path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            if (path.contains("..") || path.contains("%")) {
                throw new IllegalArgumentException("Specified path can not contain '..' or '%'. Path was " + path);
            }
            URI uri = new URI(path).normalize();
            if (uri.getPath().equals("")) {
                return trailingSlash ? new URI("/") : new URI("");
            } else if (trailingSlash && !path.endsWith("/")) {
                uri = new URI(uri.getPath() + "/");
            }
            return uri;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Specified path is an invalid URI. Path was " + path, e);
        }
    }

    /**
     * Resolve a string path against a URI base. The specified path can not contain
     * relative {@literal ..} segments or {@literal %} characters.
     *
     * Relative paths will be resolved against the specified base URI.
     * Absolute paths will be normalized and returned.
     * <p>
     * Examples:
     * <ul>
     * <li>{@code normalizeWithBase(new URI("/"), "example", true)}
     * will return a URI with path {@literal /example/}</li>
     * <li>{@code normalizeWithBase(new URI("/"), "example", false)}
     * will return a URI with an empty path {@literal /example}</li>
     * <li>{@code normalizeWithBase(new URI("/"), "/example", true)}
     * will return a URI with path {@literal /example/}</li>
     * <li>{@code normalizeWithBase(new URI("/"), "/example", false)}
     * will return a URI with an empty {@literal /example</li>
     *
     * <li>{@code normalizeWithBase(new URI("/prefix/"), "example", true)}
     * will return a URI with path {@literal /prefix/example/}</li>
     * <li>{@code normalizeWithBase(new URI("/prefix/"), "example", false)}
     * will return a URI with an empty path {@literal /prefix/example}</li>
     * <li>{@code normalizeWithBase(new URI("/prefix/"), "/example", true)}
     * will return a URI with path {@literal /example/}</li>
     * <li>{@code normalizeWithBase(new URI("/prefix/"), "/example", false)}
     * will return a URI with an empty path {@literal /example}</li>
     *
     * <li>{@code normalizeWithBase(new URI("foo/"), "example", true)}
     * will return a URI with path {@literal foo/example/}</li>
     * <li>{@code normalizeWithBase(new URI("foo/"), "example", false)}
     * will return a URI with an empty path {@literal foo/example}</li>
     * <li>{@code normalizeWithBase(new URI("foo/"), "/example", true)}
     * will return a URI with path {@literal /example/}</li>
     * <li>{@code normalizeWithBase(new URI("foo/"), "/example", false)}
     * will return a URI with an empty path {@literal /example}</li>
     * </ul>
     *
     * @param base URI to resolve relative paths. Use {@link #toURI(String, boolean)} to construct this parameter.
     *
     * @param segment Relative or absolute path
     * @param trailingSlash true if resulting URI must end with a '/'
     * @throws IllegalArgumentException if the path contains invalid characters or path segments.
     */
    public static URI normalizeWithBase(URI base, String segment, boolean trailingSlash) {
        if (segment == null || segment.trim().isEmpty()) {
            if ("/".equals(base.getPath())) {
                return base;
            }
            // otherwise, make sure trailingSlash is honored
            return toURI(base.getPath(), trailingSlash);
        }
        URI segmentUri = toURI(segment, trailingSlash);
        URI resolvedUri = base.resolve(segmentUri);
        return resolvedUri;
    }

    public static String relativize(String rootPath, String leafPath) {
        if (leafPath.startsWith(rootPath)) {
            return leafPath.substring(rootPath.length());
        }

        return null;
    }
}
