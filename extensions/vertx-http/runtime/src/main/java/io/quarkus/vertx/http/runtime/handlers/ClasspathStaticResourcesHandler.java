package io.quarkus.vertx.http.runtime.handlers;

import static io.quarkus.vertx.http.runtime.RoutingUtils.compressIfNeeded;
import static io.quarkus.vertx.http.runtime.RoutingUtils.resolvePath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Set;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.vertx.http.runtime.StaticResourcesConfig;
import io.quarkus.vertx.http.runtime.StaticResourcesRecorder;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.MimeMapping;
import io.vertx.ext.web.RoutingContext;

public final class ClasspathStaticResourcesHandler implements Handler<RoutingContext> {

    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME.withZone(ZoneOffset.UTC);
    private static final String ALLOW_HEADER_VALUE = "HEAD,GET,OPTIONS";

    private final Set<String> knownPaths;
    private final String indexPage;
    private final StaticResourcesConfig config;
    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;
    private final Set<String> compressMediaTypes;
    private final ClassLoader classLoader;

    public ClasspathStaticResourcesHandler(Set<String> knownPaths, String indexPage, StaticResourcesConfig config,
            VertxHttpBuildTimeConfig httpBuildTimeConfig, Set<String> compressMediaTypes, ClassLoader classLoader) {
        this.knownPaths = Objects.requireNonNull(knownPaths, "knownPaths");
        this.indexPage = Objects.requireNonNull(indexPage, "indexPage");
        this.config = Objects.requireNonNull(config, "config");
        this.httpBuildTimeConfig = Objects.requireNonNull(httpBuildTimeConfig, "httpBuildTimeConfig");
        this.compressMediaTypes = Objects.requireNonNull(compressMediaTypes, "compressMediaTypes");
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    @Override
    public void handle(RoutingContext ctx) {
        HttpMethod method = ctx.request().method();
        if (method != HttpMethod.GET && method != HttpMethod.HEAD && method != HttpMethod.OPTIONS) {
            ctx.next();
            return;
        }

        String resolvedPath = resolvePath(ctx);
        if (resolvedPath == null) {
            ctx.fail(HttpResponseStatus.BAD_REQUEST.code());
            return;
        }

        final String effectivePath = resolvedPath.endsWith("/") ? resolvedPath.concat(indexPage) : resolvedPath;

        if (!isKnownPath(resolvedPath, effectivePath)) {
            ctx.next();
            return;
        }

        if (!config.includeHidden() && containsHiddenSegment(effectivePath)) {
            ctx.response().setStatusCode(HttpResponseStatus.NOT_FOUND.code()).end();
            return;
        }

        if (method == HttpMethod.OPTIONS) {
            ctx.response().setStatusCode(204).putHeader("Allow", ALLOW_HEADER_VALUE).end();
            return;
        }

        URL resourceUrl = classLoader.getResource(toResourceName(effectivePath));
        if (resourceUrl == null) {
            ctx.next();
            return;
        }

        compressIfNeeded(httpBuildTimeConfig, compressMediaTypes, ctx, effectivePath);
        addCommonHeaders(ctx, effectivePath, resourceUrl);

        byte[] bytes;
        try {
            bytes = readAll(resourceUrl);
        } catch (IOException e) {
            ctx.fail(e);
            return;
        }

        if (bytes == null) {
            ctx.next();
            return;
        }

        if (config.enableRangeSupport()) {
            ctx.response().putHeader("Accept-Ranges", "bytes");
        }

        if (method == HttpMethod.HEAD) {
            if (config.enableRangeSupport()) {
                ctx.response().putHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(bytes.length));
            }
            ctx.response().setStatusCode(HttpResponseStatus.OK.code()).end();
            return;
        }

        // Do not set Content-Length on GET; allow compression handler to decide transfer semantics.
        ctx.response().setStatusCode(HttpResponseStatus.OK.code()).send(Buffer.buffer(bytes));
    }

    private boolean isKnownPath(String resolvedPath, String effectivePath) {
        if (knownPaths.contains(effectivePath)) {
            return true;
        }
        if (resolvedPath.endsWith("/") && knownPaths.contains(resolvedPath.concat(indexPage))) {
            return true;
        }
        return false;
    }

    private void addCommonHeaders(RoutingContext ctx, String effectivePath, URL resourceUrl) {
        String contentType = MimeMapping.mimeTypeForFilename(effectivePath);
        if (contentType != null) {
            if (contentType.startsWith("text")) {
                ctx.response().putHeader(HttpHeaders.CONTENT_TYPE,
                        contentType + ";charset=" + config.contentEncoding().name());
            } else {
                ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
        }

        if (config.cachingEnabled()) {
            long maxAgeSeconds = config.maxAge().toSeconds();
            ctx.response().putHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + maxAgeSeconds);
            long lastModified = getLastModified(resourceUrl);
            if (lastModified > 0) {
                ctx.response().putHeader(HttpHeaders.LAST_MODIFIED, RFC_1123.format(Instant.ofEpochMilli(lastModified)));
            }
        }

        if (config.sendVaryHeader()) {
            ctx.response().putHeader(HttpHeaders.VARY, HttpHeaders.ACCEPT_ENCODING);
        }
    }

    private static long getLastModified(URL resourceUrl) {
        try {
            URLConnection connection = resourceUrl.openConnection();
            // Avoid holding open jar connections; we only need metadata.
            connection.setUseCaches(false);
            return connection.getLastModified();
        } catch (IOException e) {
            return 0;
        }
    }

    private static boolean containsHiddenSegment(String effectivePath) {
        int start = 0;
        while (start < effectivePath.length()) {
            int end = effectivePath.indexOf('/', start);
            if (end == -1) {
                end = effectivePath.length();
            }
            if (end > start) {
                if (effectivePath.charAt(start) == '.' && (start == 0 || effectivePath.charAt(start - 1) == '/')) {
                    return true;
                }
            }
            start = end + 1;
        }
        return false;
    }

    private static String toResourceName(String effectivePath) {
        // effectivePath is expected to start with "/"
        return StaticResourcesRecorder.META_INF_RESOURCES + effectivePath;
    }

    private static byte[] readAll(URL resourceUrl) throws IOException {
        try (InputStream in = resourceUrl.openStream()) {
            return in.readAllBytes();
        }
    }
}
