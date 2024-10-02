package io.quarkus.vertx.http.runtime.handlers;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.quarkus.vertx.http.runtime.GeneratedStaticResourcesRecorder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.StaticHandler;

/**
 * {@link StaticHandler} implementation to handle static resources using the Classpath.
 * This is meant to be used on {@link io.quarkus.runtime.LaunchMode#DEVELOPMENT} mode.
 */
public class DevStaticHandler implements Handler<RoutingContext> {

    private static final Logger LOG = Logger.getLogger(DevStaticHandler.class);
    private static final int HTTP_STATUS_OK = 200;
    private static final int HTTP_STATUS_NO_CONTENT = 204;
    private static final String ALLOW_HEADER = "Allow";
    private static final String ALLOW_HEADER_VALUE = "HEAD,GET,OPTIONS";
    private final Set<String> generatedClasspathResources;
    private final Map<String, String> generatedFilesResources;
    private final Set<String> compressedMediaTypes;
    private final ClassLoader currentClassLoader;
    private final boolean enableCompression;
    private final String indexPage;
    private final Charset defaultEncoding;

    public DevStaticHandler(Set<String> generatedClasspathResources, Map<String, String> generatedFilesResources,
            DevClasspathStaticHandlerOptions options) {
        this.generatedClasspathResources = generatedClasspathResources;
        this.generatedFilesResources = generatedFilesResources;
        this.compressedMediaTypes = options.getCompressMediaTypes();
        this.currentClassLoader = Thread.currentThread().getContextClassLoader();
        this.enableCompression = options.isEnableCompression();
        this.indexPage = options.getIndexPage();
        this.defaultEncoding = options.getDefaultEncoding();
    }

    @Override
    public void handle(RoutingContext context) {

        String resolvedPath = resolvePath(context);
        String path = resolvedPath.endsWith("/") ? resolvedPath.concat(this.indexPage) : resolvedPath;

        if (LOG.isDebugEnabled()) {
            LOG.debugf("Handling request for path '%s'", path);
        }

        boolean containsGeneratedResource = this.generatedClasspathResources.contains(path)
                || this.generatedFilesResources.containsKey(path);

        if (!containsGeneratedResource) {
            beforeNextHandler(this.currentClassLoader, context);
            return;
        }

        if (context.request().method().equals(HttpMethod.OPTIONS)) {
            context.response().putHeader(ALLOW_HEADER, ALLOW_HEADER_VALUE).setStatusCode(HTTP_STATUS_NO_CONTENT)
                    .send();
            return;
        }

        compressIfNeeded(context, path);

        if (generatedFilesResources.containsKey(path)) {
            context.vertx().fileSystem().readFile(generatedFilesResources.get(path), r -> {
                if (r.succeeded()) {
                    handleAsyncResultSucceeded(context, r.result(), path);
                } else {
                    context.fail(r.cause());
                }
            });
            return;
        }

        context.vertx().executeBlocking(future -> {
            try {
                byte[] content = getClasspathResourceContent(path);
                future.complete(content);
            } catch (Exception e) {
                future.fail(e);
            }
        }, asyncResult -> {
            if (asyncResult.succeeded()) {
                byte[] result = (byte[]) asyncResult.result();
                handleAsyncResultSucceeded(context, result == null ? null : Buffer.buffer(result), path);
            } else {
                context.fail(asyncResult.cause());
            }
        });
    }

    private void handleAsyncResultSucceeded(RoutingContext context, Buffer result, String path) {

        if (result == null) {
            LOG.warnf("The '%s' file does not contain any content. Proceeding to the next handler if it exists", path);
            beforeNextHandler(this.currentClassLoader, context);
            return;
        }
        String contentType = MimeMapping.getMimeTypeForFilename(path);
        if (contentType != null) {
            if (contentType.startsWith("text")) {
                context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType + ";charset=" + defaultEncoding);
            } else {
                context.response().putHeader(HttpHeaders.CONTENT_TYPE, contentType);
            }
        }
        if (context.request().method().equals(HttpMethod.HEAD)) {
            handleHeadMethod(context, result);
        } else {
            context.response().send(result);
        }
    }

    private void handleHeadMethod(RoutingContext context, Buffer content) {
        context.response().putHeader(HttpHeaders.CONTENT_LENGTH, Long.toString(content.length()));
        context.response().setStatusCode(HTTP_STATUS_OK).end();
    }

    private byte[] getClasspathResourceContent(String name) {
        String resourceName = GeneratedStaticResourcesRecorder.META_INF_RESOURCES + name;
        URL resource = getClassLoader().getResource(resourceName);
        if (resource == null) {
            LOG.warnf("The resource '%s' does not exist on classpath", resourceName);
            return null;
        }
        try {
            try (InputStream inputStream = resource.openStream()) {
                return inputStream.readAllBytes();
            }
        } catch (IOException e) {
            LOG.error("Error while reading file from Classpath for path " + resourceName, e);
            return null;
        }
    }

    private static String resolvePath(RoutingContext ctx) {
        return (ctx.mountPoint() == null) ? ctx.normalizedPath()
                : ctx.normalizedPath().substring(
                        // let's be extra careful here in case Vert.x normalizes the mount points at
                        // some point
                        ctx.mountPoint().endsWith("/") ? ctx.mountPoint().length() - 1 : ctx.mountPoint().length());
    }

    private static void beforeNextHandler(ClassLoader cl, RoutingContext ctx) {
        // make sure we don't lose the correct TCCL to Vert.x...
        Thread.currentThread().setContextClassLoader(cl);
        ctx.next();
    }

    private void compressIfNeeded(RoutingContext ctx, String path) {
        if (enableCompression && isCompressed(path)) {
            // VertxHttpRecorder is adding "Content-Encoding: identity" to all requests if compression is enabled.
            // Handlers can remove the "Content-Encoding: identity" header to enable compression.
            ctx.response().headers().remove(HttpHeaders.CONTENT_ENCODING);
        }
    }

    private boolean isCompressed(String path) {
        if (this.compressedMediaTypes.isEmpty()) {
            return false;
        }
        final String resourcePath = path.endsWith("/") ? path + this.indexPage : path;
        String contentType = MimeMapping.getMimeTypeForFilename(resourcePath);
        return contentType != null && this.compressedMediaTypes.contains(contentType);
    }

    private ClassLoader getClassLoader() {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = getClass().getClassLoader();
        }
        if (cl == null) {
            cl = Object.class.getClassLoader();
        }
        return cl;
    }

}
