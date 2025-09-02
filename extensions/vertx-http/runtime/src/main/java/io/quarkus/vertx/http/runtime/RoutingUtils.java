package io.quarkus.vertx.http.runtime;

import java.util.Set;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

public final class RoutingUtils {

    private RoutingUtils() throws IllegalAccessException {
        throw new IllegalAccessException("Avoid direct instantiation");
    }

    /**
     * Get the normalized and decoded path:
     * - normalize based on RFC3986
     * - convert % encoded characters to their non encoded form (using {@link URIDecoder#decodeURIComponent})
     * - invalid if the path contains '?' (query section of the path)
     *
     * @param ctx the RoutingContext
     * @return the normalized and decoded path or null if not valid
     */
    public static String getNormalizedAndDecodedPath(RoutingContext ctx) {
        String normalizedPath = ctx.normalizedPath();
        if (normalizedPath.indexOf('?') != -1) {
            return null;
        }
        if (normalizedPath.indexOf('%') == -1) {
            return normalizedPath;
        }
        return URIDecoder.decodeURIComponent(normalizedPath);
    }

    /**
     * Normalize and decode the path then strip the mount point from it
     *
     * @param ctx the RoutingContext
     * @return the normalized and decoded path without the mount point or null if not valid
     */
    public static String resolvePath(RoutingContext ctx) {
        String path = getNormalizedAndDecodedPath(ctx);
        if (path == null) {
            return null;
        }
        return (ctx.mountPoint() == null) ? path
                : path.substring(
                        // let's be extra careful here in case Vert.x normalizes the mount points at
                        // some point
                        ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
    }

    /**
     * Enabled compression by removing CONTENT_ENCODING header as specified in Vert.x when the media-type should be compressed
     * and config enable compression.
     *
     * @param config
     * @param compressMediaTypes
     * @param ctx
     * @param path
     */
    public static void compressIfNeeded(VertxHttpBuildTimeConfig config, Set<String> compressMediaTypes, RoutingContext ctx,
            String path) {
        if (config.enableCompression() && isCompressed(compressMediaTypes, path)) {
            // VertxHttpRecorder is adding "Content-Encoding: identity" to all requests if compression is enabled.
            // Handlers can remove the "Content-Encoding: identity" header to enable compression.
            ctx.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
        }
    }

    private static boolean isCompressed(Set<String> compressMediaTypes, String path) {
        if (compressMediaTypes.isEmpty()) {
            return false;
        }
        final String resourcePath = path.endsWith("/") ? path + StaticHandler.DEFAULT_INDEX_PAGE : path;
        final String contentType = MimeMapping.getMimeTypeForFilename(resourcePath);
        return contentType != null && compressMediaTypes.contains(contentType);
    }
}
