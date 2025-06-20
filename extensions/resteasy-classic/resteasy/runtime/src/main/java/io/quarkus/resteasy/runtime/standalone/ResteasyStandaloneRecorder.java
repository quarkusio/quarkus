package io.quarkus.resteasy.runtime.standalone;

import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.extractRootCause;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.isOtherAuthenticationFailure;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.markIfOtherAuthenticationFailure;
import static io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler.removeMarkAsOtherAuthenticationFailure;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import org.jboss.resteasy.core.ResourceMethodInvoker;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.specimpl.ResteasyUriInfo;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResourceInvoker;
import org.jboss.resteasy.spi.ResteasyConfiguration;
import org.jboss.resteasy.spi.ResteasyDeployment;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.quarkus.resteasy.runtime.NonJaxRsClassMappings;
import io.quarkus.resteasy.runtime.ResteasyVertxConfig;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.security.AuthenticationCompletionException;
import io.quarkus.security.AuthenticationException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.AuthenticationRedirectException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import io.quarkus.vertx.http.runtime.HttpCompressionHandler;
import io.quarkus.vertx.http.runtime.VertxHttpBuildTimeConfig;
import io.quarkus.vertx.http.runtime.VertxHttpConfig;
import io.quarkus.vertx.http.runtime.devmode.ResourceNotFoundData;
import io.quarkus.vertx.http.runtime.devmode.RouteDescription;
import io.quarkus.vertx.http.runtime.devmode.RouteMethodDescription;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.DefaultAuthFailureHandler;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;

/**
 * Provides the runtime methods to bootstrap Resteasy in standalone mode.
 */
@Recorder
public class ResteasyStandaloneRecorder {

    static final String RESTEASY_URI_INFO = ResteasyUriInfo.class.getName();

    private static boolean useDirect = true;

    private static ResteasyDeployment deployment;
    private static String contextPath;

    private final ResteasyVertxConfig runtimeConfig;
    private final VertxHttpBuildTimeConfig httpBuildTimeConfig;
    private final RuntimeValue<VertxHttpConfig> httpRuntimeConfig;

    public ResteasyStandaloneRecorder(
            final ResteasyVertxConfig runtimeConfig,
            final VertxHttpBuildTimeConfig httpBuildTimeConfig,
            final RuntimeValue<VertxHttpConfig> httpRuntimeConfig) {
        this.runtimeConfig = runtimeConfig;
        this.httpBuildTimeConfig = httpBuildTimeConfig;
        this.httpRuntimeConfig = httpRuntimeConfig;
    }

    public void staticInit(ResteasyDeployment dep, String path) {
        if (dep != null) {
            deployment = dep;
            deployment.getDefaultContextObjects().put(ResteasyConfiguration.class, new ResteasyConfigurationMPConfig());
            deployment.start();
        }
        contextPath = path;
    }

    public void start(ShutdownContext shutdown, boolean isVirtual) {

        shutdown.addShutdownTask(new Runnable() {
            @Override
            public void run() {
                if (deployment != null) {
                    deployment.stop();
                }
            }
        });
        useDirect = !isVirtual;
    }

    public Handler<RoutingContext> vertxRequestHandler(Supplier<Vertx> vertx, Executor executor,
            Map<String, NonJaxRsClassMappings> nonJaxRsClassNameToMethodPaths) {
        if (deployment != null) {
            Handler<RoutingContext> handler = new VertxRequestHandler(vertx.get(), deployment, contextPath,
                    new ResteasyVertxAllocator(
                            runtimeConfig.responseBufferSize()),
                    executor,
                    httpRuntimeConfig.getValue().readTimeout().toMillis());

            Set<String> compressMediaTypes = httpBuildTimeConfig.compressMediaTypes().map(Set::copyOf).orElse(Set.of());
            if (httpBuildTimeConfig.enableCompression() && !compressMediaTypes.isEmpty()) {
                // If compression is enabled and the set of compressed media types is not empty then wrap the standalone handler
                handler = new HttpCompressionHandler(handler, compressMediaTypes);
            }

            if (LaunchMode.current() == LaunchMode.DEVELOPMENT) {
                // For Not Found Screen
                Registry registry = deployment.getRegistry();
                ResourceNotFoundData.setRuntimeRoutes(fromBoundResourceInvokers(registry, nonJaxRsClassNameToMethodPaths));
            }

            return handler;
        }
        return null;
    }

    public Handler<RoutingContext> vertxFailureHandler(Supplier<Vertx> vertx, Executor executor,
            boolean noCustomAuthCompletionExMapper, boolean noCustomAuthFailureExMapper, boolean noCustomAuthRedirectExMapper) {
        if (deployment == null) {
            return null;
        } else {
            // allow customization of auth failures with exception mappers; this failure handler is only
            // used when auth failed before RESTEasy Classic began processing the request
            return new VertxRequestHandler(vertx.get(), deployment, contextPath,
                    new ResteasyVertxAllocator(runtimeConfig.responseBufferSize()), executor,
                    httpRuntimeConfig.getValue().readTimeout().toMillis()) {

                @Override
                public void handle(RoutingContext request) {

                    // special handling when proactive auth is enabled as then we know default auth failure handler already run
                    if (httpBuildTimeConfig.auth().proactive()
                            && request.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER) instanceof DefaultAuthFailureHandler) {
                        // we want to prevent repeated handling of exceptions if user don't want to handle exception himself
                        // we do not pass exception to abort handlers if proactive auth is enabled and user did not
                        // provide custom ex. mapper; we replace default auth failure handler as soon as we can, so that
                        // we can handle Quarkus Security Exceptions ourselves
                        if (request.failure() instanceof AuthenticationFailedException) {
                            if (noCustomAuthFailureExMapper) {
                                request.next();
                            } else {
                                // allow response customization
                                super.handle(request);
                            }
                            return;
                        } else if (request.failure() instanceof AuthenticationCompletionException) {
                            if (noCustomAuthCompletionExMapper) {
                                request.next();
                            } else {
                                // allow response customization
                                super.handle(request);
                            }
                            return;
                        } else if (request.failure() instanceof AuthenticationRedirectException) {
                            if (noCustomAuthRedirectExMapper) {
                                request.next();
                            } else {
                                // allow response customization
                                super.handle(request);
                            }
                            return;
                        }
                    }

                    final Throwable failure = request.failure();
                    final boolean isOtherAuthFailure = isOtherAuthenticationFailure(request)
                            && isFailureHandledByExceptionMappers(failure);
                    if (isOtherAuthFailure) {
                        // prevent circular reference for unhandled exceptions
                        // (which is unnecessary if everything here is done right)
                        removeMarkAsOtherAuthenticationFailure(request);
                        super.handle(request);
                    } else if (failure instanceof AuthenticationException || failure instanceof UnauthorizedException
                            || failure instanceof ForbiddenException) {
                        super.handle(request);
                    } else {
                        request.next();
                    }
                }

                @Override
                protected void setCurrentIdentityAssociation(RoutingContext routingContext) {
                    // security identity is not available as authentication failed
                }
            };
        }
    }

    private boolean isFailureHandledByExceptionMappers(Throwable failure) {
        return failure != null && deployment != null
                && deployment.getProviderFactory().getExceptionMapper(failure.getClass()) != null;
    }

    public Handler<RoutingContext> defaultAuthFailureHandler() {
        return new Handler<RoutingContext>() {
            @Override
            public void handle(RoutingContext event) {
                if (deployment != null
                        && event.get(QuarkusHttpUser.AUTH_FAILURE_HANDLER) instanceof DefaultAuthFailureHandler) {

                    // only replace default auth failure handler if we can extract URI info
                    // as org.jboss.resteasy.plugins.server.BaseHttpRequest requires it;
                    // we need to extract URI info here as if auth failure will happen further upstream
                    // we want to return 401 and correct headers rather than 400 (malformed input) and so on
                    try {
                        event.put(RESTEASY_URI_INFO, VertxUtil.extractUriInfo(event.request(), contextPath));
                    } catch (Exception e) {
                        // URI could be malformed or there has been internal error when extracting URI info
                        // keep default behavior (don't fail event, let default auth failure handler to handle this)
                        event.next();
                        return;
                    }

                    // fail event rather than end it, so that exception mappers can customize response
                    event.put(QuarkusHttpUser.AUTH_FAILURE_HANDLER, new BiConsumer<RoutingContext, Throwable>() {

                        @Override
                        public void accept(RoutingContext event, Throwable throwable) {
                            markIfOtherAuthenticationFailure(event, throwable);
                            if (!event.failed()) {
                                event.fail(extractRootCause(throwable));
                            }
                        }
                    });
                }
                event.next();
            }
        };
    }

    private List<RouteDescription> fromBoundResourceInvokers(Registry registry,
            Map<String, NonJaxRsClassMappings> nonJaxRsClassNameToMethodPaths) {
        Set<Map.Entry<String, List<ResourceInvoker>>> bound = getBounded(registry).entrySet();
        Map<String, RouteDescription> descriptionMap = new HashMap<>();

        for (Map.Entry<String, List<ResourceInvoker>> entry : bound) {
            for (ResourceInvoker invoker : entry.getValue()) {
                // skip those for now
                if (!(invoker instanceof ResourceMethodInvoker)) {
                    continue;
                }
                ResourceMethodInvoker method = (ResourceMethodInvoker) invoker;
                Class<?> resourceClass = method.getResourceClass();
                String resourceClassName = resourceClass.getName();
                String basePath = null;
                NonJaxRsClassMappings nonJaxRsClassMappings = null;
                Path path = resourceClass.getAnnotation(Path.class);
                if (path == null) {
                    nonJaxRsClassMappings = nonJaxRsClassNameToMethodPaths.get(resourceClassName);
                    if (nonJaxRsClassMappings != null) {
                        basePath = nonJaxRsClassMappings.getBasePath();
                    }
                } else {
                    basePath = path.value();
                }

                if (basePath == null) {
                    continue;
                }

                RouteDescription description = descriptionMap.get(basePath);
                if (description == null) {
                    description = new RouteDescription(basePath);
                    descriptionMap.put(basePath, description);
                }

                String subPath = "";
                for (Annotation annotation : method.getMethodAnnotations()) {
                    if (annotation.annotationType().equals(Path.class)) {
                        subPath = ((Path) annotation).value();
                        break;
                    }
                }
                // attempt to find a mapping in the non JAX-RS paths
                if (subPath.isEmpty() && (nonJaxRsClassMappings != null)) {
                    String methodName = method.getMethod().getName();
                    String subPathFromMethodName = nonJaxRsClassMappings.getMethodNameToPath().get(methodName);
                    if (subPathFromMethodName != null) {
                        subPath = subPathFromMethodName;
                    }
                }

                String fullPath = basePath;
                if (!subPath.isEmpty()) {
                    if (basePath.endsWith("/")) {
                        fullPath += subPath;
                    } else {
                        fullPath = basePath + (subPath.startsWith("/") ? "" : "/") + subPath;
                    }
                }

                String produces = mostPreferredOrNull(method.getProduces());
                String consumes = mostPreferredOrNull(method.getConsumes());

                for (String verb : method.getHttpMethods()) {
                    description.addCall(new RouteMethodDescription(verb, fullPath, produces, consumes));
                }
            }
        }

        List<RouteDescription> descriptions = new ArrayList<>(descriptionMap.values());
        return descriptions;
    }

    private String mostPreferredOrNull(MediaType[] mediaTypes) {
        if (mediaTypes == null || mediaTypes.length < 1) {
            return null;
        } else {
            return mediaTypes[0].toString();
        }
    }

    private Map<String, List<ResourceInvoker>> getBounded(Registry registry) {
        Map<String, List<ResourceInvoker>> bounded = null;
        if (registry instanceof ResourceMethodRegistry) {
            bounded = ((ResourceMethodRegistry) registry).getBounded();
        } else if (Proxy.isProxyClass(registry.getClass())
                && registry.toString().startsWith(ResourceMethodRegistry.class.getName())) {
            try {
                bounded = (Map<String, List<ResourceInvoker>>) Proxy.getInvocationHandler(registry).invoke(registry,
                        ResourceMethodRegistry.class.getMethod("getBounded"), new Object[0]);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return bounded;
    }

    private static class ResteasyVertxAllocator implements BufferAllocator {

        private final int bufferSize;

        private ResteasyVertxAllocator(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public ByteBuf allocateBuffer() {
            return allocateBuffer(useDirect);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct) {
            return allocateBuffer(direct, bufferSize);
        }

        @Override
        public ByteBuf allocateBuffer(int bufferSize) {
            return allocateBuffer(useDirect, bufferSize);
        }

        @Override
        public ByteBuf allocateBuffer(boolean direct, int bufferSize) {
            if (direct) {
                return PooledByteBufAllocator.DEFAULT.directBuffer(bufferSize);
            } else {
                return PooledByteBufAllocator.DEFAULT.heapBuffer(bufferSize);
            }
        }

        @Override
        public int getBufferSize() {
            return bufferSize;
        }
    }
}
