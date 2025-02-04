package io.quarkus.vertx.http.runtime.handlers;

import static io.quarkus.vertx.http.runtime.RoutingUtils.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.quarkus.vertx.http.runtime.GeneratedStaticResourcesRecorder;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
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
    private final Set<String> compressMediaTypes;
    private final ClassLoader currentClassLoader;
    private final String indexPage;
    private final Charset defaultEncoding;
    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;

    public DevStaticHandler(Set<String> generatedClasspathResources, Map<String, String> generatedFilesResources,
            DevClasspathStaticHandlerOptions options) {
        this.generatedClasspathResources = generatedClasspathResources;
        this.generatedFilesResources = generatedFilesResources;
        httpBuildTimeConfig = options.httpBuildTimeConfig();
        if (httpBuildTimeConfig.enableCompression() && httpBuildTimeConfig.compressMediaTypes().isPresent()) {
            this.compressMediaTypes = Set.copyOf(httpBuildTimeConfig.compressMediaTypes().get());
        } else {
            this.compressMediaTypes = Set.of();
        }
        this.currentClassLoader = Thread.currentThread().getContextClassLoader();
        this.indexPage = options.indexPage();
        this.defaultEncoding = options.defaultEncoding();
    }

    @Override
    public void handle(RoutingContext context) {

        String resolvedPath = resolvePath(context);
        if (resolvedPath == null) {
            context.fail(HttpResponseStatus.BAD_REQUEST.code());
            return;
        }
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

        compressIfNeeded(httpBuildTimeConfig, compressMediaTypes, context, path);

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

    private static void beforeNextHandler(ClassLoader cl, RoutingContext ctx) {
        // make sure we don't lose the correct TCCL to Vert.x...
        Thread.currentThread().setContextClassLoader(cl);
        ctx.next();
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
