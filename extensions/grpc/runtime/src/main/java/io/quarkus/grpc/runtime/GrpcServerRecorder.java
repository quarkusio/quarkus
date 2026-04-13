package io.quarkus.grpc.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.util.TypeLiteral;

import org.jboss.logging.Logger;

import grpc.health.v1.HealthOuterClass;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerServiceDefinition;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Subclass;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.grpc.api.ServerBuilderCustomizer;
import io.quarkus.grpc.auth.GrpcSecurityInterceptor;
import io.quarkus.grpc.reflection.service.ReflectionServiceV1;
import io.quarkus.grpc.reflection.service.ReflectionServiceV1alpha;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.devmode.DevModeInterceptor;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.supports.CompressionInterceptor;
import io.quarkus.grpc.runtime.supports.blocking.BlockingServerInterceptor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.value.registry.ValueRegistry;
import io.quarkus.vertx.http.runtime.QuarkusErrorHandler;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticator;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.grpc.server.GrpcServerOptions;
import io.vertx.grpcio.server.GrpcIoServer;
import io.vertx.grpcio.server.GrpcIoServiceBridge;

@Recorder
public class GrpcServerRecorder {
    private static final Logger LOGGER = Logger.getLogger(GrpcServerRecorder.class.getName());

    private static volatile List<GrpcServiceDefinition> services = Collections.emptyList();

    private static final Pattern GRPC_CONTENT_TYPE = Pattern.compile("^application/grpc.*");

    private final RuntimeValue<GrpcConfiguration> runtimeConfig;
    private final RuntimeValue<ValueRegistry> valueRegistry;

    public GrpcServerRecorder(
            final RuntimeValue<GrpcConfiguration> runtimeConfig,
            final RuntimeValue<ValueRegistry> valueRegistry) {
        this.runtimeConfig = runtimeConfig;
        this.valueRegistry = valueRegistry;
    }

    public static List<GrpcServiceDefinition> getServices() {
        return services;
    }

    public void addMainRouterErrorHandler(RuntimeValue<Router> mainRouter) {
        mainRouter.getValue().route().last().failureHandler(new Handler<>() {

            private final Handler<RoutingContext> errorHandler = new QuarkusErrorHandler(LaunchMode.current().isDevOrTest(),
                    false, Optional.empty());

            @Override
            public void handle(RoutingContext event) {
                if (isGrpc(event)) {
                    // this is for failures before that occurred before gRPC started processing, it could be:
                    // 1. authentication failure
                    // 2. internal error raised during authentication
                    // 3. unrelated failure
                    // if there is an exception on the gRPC route, we should handle it because the most likely cause
                    // of the failure is authentication; as for the '3.', this is better than having unhandled failures
                    errorHandler.handle(event);
                }
            }
        });
    }

    public void initializeGrpcServer(boolean hasNoBindableServiceBeans, BeanContainer beanContainer,
            RuntimeValue<Vertx> vertxSupplier,
            RuntimeValue<Router> routerSupplier,
            ShutdownContext shutdown,
            Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            LaunchMode launchMode, boolean securityPresent, Map<Integer, Handler<RoutingContext>> securityHandlers) {
        if (hasNoBindableServiceBeans && LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            LOGGER.error("Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
            return;
        }

        Vertx vertx = vertxSupplier.getValue();
        GrpcServerConfiguration configuration = runtimeConfig.getValue().server();

        buildGrpcServer(vertx, configuration, routerSupplier, shutdown, blockingMethodsPerService, virtualMethodsPerService,
                beanContainer.beanInstance(GrpcContainer.class), launchMode, securityPresent, securityHandlers);
    }

    // TODO -- handle XDS
    private void buildGrpcServer(Vertx vertx, GrpcServerConfiguration configuration, RuntimeValue<Router> routerSupplier,
            ShutdownContext shutdown, Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            GrpcContainer grpcContainer, LaunchMode launchMode, boolean securityPresent,
            Map<Integer, Handler<RoutingContext>> securityHandlers) {

        GrpcServerOptions options = new GrpcServerOptions();

        List<ServerBuilderCustomizer> serverBuilderCustomizers = Arc.container()
                .select(new TypeLiteral<ServerBuilderCustomizer>() {
                }, Any.Literal.INSTANCE)
                .stream()
                .sorted(Comparator.<ServerBuilderCustomizer, Integer> comparing(ServerBuilderCustomizer::priority))
                .toList();

        serverBuilderCustomizers.forEach(sbc -> sbc.customize(configuration, options));

        if (!configuration.maxInboundMessageSize().isEmpty()) {
            options.setMaxMessageSize(configuration.maxInboundMessageSize().getAsInt());
        }
        var server = GrpcIoServer.server(vertx, options);
        List<ServerInterceptor> globalInterceptors = grpcContainer.getSortedGlobalInterceptors();

        if (launchMode == LaunchMode.DEVELOPMENT) {
            // add as last, so they will run first
            globalInterceptors.add(new DevModeInterceptor(Thread.currentThread().getContextClassLoader()));
            // TODO Do we need hot-reload, or is it handled by the HTTP server
        }

        var toBeRegistered = collectServiceDefinitions(grpcContainer.getServices());

        CompressionInterceptor compressionInterceptor = prepareCompressionInterceptor(configuration);

        for (var service : toBeRegistered) {
            ServerServiceDefinition serviceDefinition = serviceWithInterceptors(
                    vertx, grpcContainer, blockingMethodsPerService, virtualMethodsPerService, compressionInterceptor,
                    globalInterceptors, service.definition,
                    launchMode == LaunchMode.DEVELOPMENT);

            LOGGER.debugf("Registered gRPC service '%s'", service.definition.definition.getServiceDescriptor().getName());
            GrpcIoServiceBridge bridge = GrpcIoServiceBridge.bridge(serviceDefinition);
            bridge.bind(server);
        }

        boolean reflectionServiceEnabled = configuration.enableReflectionService() || launchMode == LaunchMode.DEVELOPMENT;

        if (reflectionServiceEnabled) {
            LOGGER.debug("Registering gRPC reflection service");

            List<ServerServiceDefinition> definitions = toBeRegistered.stream()
                    .map(s -> s.definition)
                    .map(def -> serviceWithInterceptors(
                            vertx, grpcContainer, blockingMethodsPerService, virtualMethodsPerService, compressionInterceptor,
                            globalInterceptors, def,
                            launchMode == LaunchMode.DEVELOPMENT))
                    .toList();

            ReflectionServiceV1 reflectionServiceV1 = new ReflectionServiceV1(definitions);
            ReflectionServiceV1alpha reflectionServiceV1alpha = new ReflectionServiceV1alpha(definitions);
            server.addService(reflectionServiceV1).addService(reflectionServiceV1alpha);
        }

        Router router = routerSupplier.getValue();
        if (securityHandlers != null) {
            for (Map.Entry<Integer, Handler<RoutingContext>> e : securityHandlers.entrySet()) {
                Handler<RoutingContext> handler = e.getValue();
                boolean isAuthenticationHandler = e.getKey() == -200;
                Route route = router.route().order(e.getKey()).handler(new Handler<RoutingContext>() {
                    @Override
                    public void handle(RoutingContext ctx) {
                        if (!isGrpc(ctx)) {
                            ctx.next();
                        } else if (isAuthenticationHandler && ctx.get(HttpAuthenticator.class.getName()) != null) {
                            // this IF branch shouldn't be invoked with current implementation
                            // when gRPC is attached to the main router when the root path is not '/'
                            // because HTTP authenticator and authorizer handlers are not added by default on the main
                            // router; adding it in case someone made changes without consider this use case
                            // so that we prevent repeated authentication
                            ctx.next();
                        } else {
                            if (!Context.isOnEventLoopThread()) {
                                Context capturedVertxContext = Vertx.currentContext();
                                if (capturedVertxContext != null) {
                                    capturedVertxContext.runOnContext(new Handler<Void>() {
                                        @Override
                                        public void handle(Void unused) {
                                            handler.handle(ctx);
                                        }
                                    });
                                    return;
                                }
                            }
                            handler.handle(ctx);
                        }
                    }
                });
                shutdown.addShutdownTask(route::remove); // remove this route at shutdown, this should reset it
            }
        }

        LOGGER.info("Enabling gRPC server");

        Route route = router.route()
                .handler(ctx -> {
                    if (!isGrpc(ctx)) {
                        ctx.next();
                    } else {
                        if (securityPresent) {
                            GrpcSecurityInterceptor.propagateSecurityIdentityWithDuplicatedCtx(ctx);
                        }
                        if (!Context.isOnEventLoopThread()) {
                            Context capturedVertxContext = Vertx.currentContext();
                            if (capturedVertxContext != null) {
                                capturedVertxContext.runOnContext(new Handler<Void>() {
                                    @Override
                                    public void handle(Void unused) {
                                        routingContextAware(server, ctx);
                                    }
                                });
                                return;
                            }
                        }
                        routingContextAware(server, ctx);
                    }
                });
        shutdown.addShutdownTask(route::remove); // remove this route at shutdown, this should reset it

        initHealthStorage();
    }

    private static void routingContextAware(GrpcIoServer server, RoutingContext context) {
        Context currentContext = Vertx.currentContext();
        var local = currentContext.getLocal(VertxContext.DATA_MAP_LOCAL, ConcurrentHashMap::new);
        local.put(RoutingContext.class.getName(), context);
        try {
            server.handle(context.request());
        } finally {
            local.remove(RoutingContext.class.getName());
        }
    }

    // TODO -- handle Avro, plain text ... when supported / needed
    private static boolean isGrpc(RoutingContext rc) {
        HttpServerRequest request = rc.request();
        HttpVersion version = request.version();
        if (HttpVersion.HTTP_1_0.equals(version) || HttpVersion.HTTP_1_1.equals(version)) {
            LOGGER.debugf("Expecting %s, received %s - not a gRPC request", HttpVersion.HTTP_2, version);
            return false;
        }
        String header = request.getHeader("content-type");
        return header != null && GRPC_CONTENT_TYPE.matcher(header.toLowerCase(Locale.ROOT)).matches();
    }

    private void initHealthStorage() {
        InstanceHandle<GrpcHealthStorage> storageHandle = Arc.container().instance(GrpcHealthStorage.class);
        if (storageHandle.isAvailable()) {
            GrpcHealthStorage storage = storageHandle.get();
            storage.setStatus(GrpcHealthStorage.DEFAULT_SERVICE_NAME,
                    HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING);
            for (GrpcServiceDefinition service : services) {
                storage.setStatus(service.definition.getServiceDescriptor().getName(),
                        HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING);
            }
        }
    }

    private static boolean hasNoServices(Instance<BindableService> services) {
        return services.isUnsatisfied()
                || services.stream().count() == 1
                        && services.get().bindService().getServiceDescriptor().getName().equals("grpc.health.v1.Health");
    }

    record GrpcServiceAndDefinition(BindableService bindable, GrpcServiceDefinition definition) {
    }

    private static List<GrpcServiceAndDefinition> collectServiceDefinitions(Instance<BindableService> services) {
        List<GrpcServiceAndDefinition> results = new ArrayList<>();
        List<GrpcServiceDefinition> definitions = new ArrayList<>();
        for (BindableService service : services) {
            // TODO - This may force a query to port before port being assigned
            ServerServiceDefinition definition = service.bindService();
            GrpcServiceDefinition def = new GrpcServiceDefinition(service, definition);
            definitions.add(def);
            results.add(new GrpcServiceAndDefinition(service, def));
        }

        // Set the last service definitions in use, referenced in the Dev UI
        GrpcServerRecorder.services = definitions;

        return results;
    }

    public static final class GrpcServiceDefinition {

        public final BindableService service;
        public final ServerServiceDefinition definition;

        GrpcServiceDefinition(BindableService service, ServerServiceDefinition definition) {
            this.service = service;
            this.definition = definition;
        }

        public String getImplementationClassName() {
            return getImplementationClassName(service);
        }

        public static String getImplementationClassName(BindableService service) {
            if (service instanceof Subclass) {
                // All intercepted services are represented by a generated subclass
                return service.getClass().getSuperclass().getName();
            }
            return service.getClass().getName();
        }
    }

    public RuntimeValue<ServerInterceptorStorage> initServerInterceptorStorage(
            Map<String, Set<Class<?>>> perServiceInterceptors,
            Set<Class<?>> globalInterceptors) {
        return new RuntimeValue<>(new ServerInterceptorStorage(perServiceInterceptors, globalInterceptors));
    }

    /**
     * Compression interceptor if needed, null otherwise
     *
     * @param configuration gRPC server configuration
     * @return interceptor or null
     */
    private CompressionInterceptor prepareCompressionInterceptor(GrpcServerConfiguration configuration) {
        CompressionInterceptor compressionInterceptor = null;
        if (configuration.compression().isPresent()) {
            compressionInterceptor = new CompressionInterceptor(configuration.compression().get());
        }
        return compressionInterceptor;
    }

    private ServerServiceDefinition serviceWithInterceptors(Vertx vertx, GrpcContainer grpcContainer,
            Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            CompressionInterceptor compressionInterceptor,
            List<ServerInterceptor> globalInterceptors,
            GrpcServiceDefinition service, boolean devMode) {
        List<ServerInterceptor> interceptors = new ArrayList<>();
        if (compressionInterceptor != null) {
            interceptors.add(compressionInterceptor);
        }
        interceptors.addAll(globalInterceptors);
        interceptors.addAll(grpcContainer.getSortedPerServiceInterceptors(service.getImplementationClassName()));

        // We only register the blocking interceptor if needed by at least one method of the service (either blocking or runOnVirtualThread)
        if (!blockingMethodsPerService.isEmpty()) {
            List<String> list = blockingMethodsPerService.get(service.getImplementationClassName());
            List<String> virtuals = virtualMethodsPerService.get(service.getImplementationClassName());
            if (list != null || virtuals != null) {
                interceptors
                        .add(new BlockingServerInterceptor(vertx, list, virtuals,
                                VirtualThreadsRecorder.getCurrent(), devMode));
            }
        }
        interceptors.sort(Interceptors.INTERCEPTOR_COMPARATOR);
        return ServerInterceptors.intercept(service.definition, interceptors);
    }

}
