package io.quarkus.grpc.runtime;

import static io.quarkus.grpc.runtime.GrpcSslUtils.applySslOptions;
import static io.quarkus.grpc.runtime.GrpcTestPortUtils.testPort;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.BindException;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jakarta.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import grpc.health.v1.HealthOuterClass;
import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.grpc.netty.NettyServerBuilder;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.arc.Subclass;
import io.quarkus.grpc.auth.GrpcSecurityInterceptor;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerNettyConfig;
import io.quarkus.grpc.runtime.devmode.DevModeInterceptor;
import io.quarkus.grpc.runtime.devmode.GrpcHotReplacementInterceptor;
import io.quarkus.grpc.runtime.devmode.GrpcServerReloader;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.reflection.ReflectionServiceV1;
import io.quarkus.grpc.runtime.reflection.ReflectionServiceV1alpha;
import io.quarkus.grpc.runtime.supports.CompressionInterceptor;
import io.quarkus.grpc.runtime.supports.blocking.BlockingServerInterceptor;
import io.quarkus.grpc.spi.GrpcBuilderProvider;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.QuarkusBindException;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.PortSystemProperties;
import io.quarkus.virtual.threads.VirtualThreadsRecorder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServiceBridge;

@Recorder
public class GrpcServerRecorder {
    private static final Logger LOGGER = Logger.getLogger(GrpcServerRecorder.class.getName());

    private static final AtomicInteger grpcVerticleCount = new AtomicInteger(0);
    private static volatile DevModeWrapper devModeWrapper;
    private static volatile List<GrpcServiceDefinition> services = Collections.emptyList();

    private static final Pattern GRPC_CONTENT_TYPE = Pattern.compile("^application/grpc.*");

    public static List<GrpcServiceDefinition> getServices() {
        return services;
    }

    public void initializeGrpcServer(RuntimeValue<Vertx> vertxSupplier,
            RuntimeValue<Router> routerSupplier,
            GrpcConfiguration cfg,
            ShutdownContext shutdown,
            Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            LaunchMode launchMode, boolean securityPresent) {
        GrpcContainer grpcContainer = Arc.container().instance(GrpcContainer.class).get();
        if (grpcContainer == null) {
            throw new IllegalStateException("gRPC not initialized, GrpcContainer not found");
        }
        if (hasNoServices(grpcContainer.getServices()) && LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            LOGGER.error("Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
            return; // OK?
        }

        Vertx vertx = vertxSupplier.getValue();
        GrpcServerConfiguration configuration = cfg.server;
        GrpcBuilderProvider<?> provider = GrpcBuilderProvider.findServerBuilderProvider(configuration);

        if (configuration.useSeparateServer) {
            if (provider == null) {
                LOGGER.warn(
                        "Using legacy gRPC support, with separate new HTTP server instance. " +
                                "Switch to single HTTP server instance usage with quarkus.grpc.server.use-separate-server=false property");
            }

            if (launchMode == LaunchMode.DEVELOPMENT) {
                // start single server, not in a verticle, regardless of the configuration.instances
                // for reason unknown to me, verticles occasionally get undeployed on dev mode reload
                if (GrpcServerReloader.getServer() != null || (provider != null && provider.serverAlreadyExists())) {
                    devModeReload(grpcContainer, vertx, configuration, provider, blockingMethodsPerService,
                            virtualMethodsPerService, shutdown);
                } else {
                    devModeStart(grpcContainer, vertx, configuration, provider, blockingMethodsPerService,
                            virtualMethodsPerService, shutdown,
                            launchMode);
                }
            } else {
                prodStart(grpcContainer, vertx, configuration, provider, blockingMethodsPerService, virtualMethodsPerService,
                        launchMode);
            }
        } else {
            buildGrpcServer(vertx, configuration, routerSupplier, shutdown, blockingMethodsPerService, virtualMethodsPerService,
                    grpcContainer, launchMode, securityPresent);
        }
    }

    // TODO -- handle XDS
    private void buildGrpcServer(Vertx vertx, GrpcServerConfiguration configuration, RuntimeValue<Router> routerSupplier,
            ShutdownContext shutdown, Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            GrpcContainer grpcContainer, LaunchMode launchMode, boolean securityPresent) {

        GrpcServer server = GrpcServer.server(vertx);
        List<ServerInterceptor> globalInterceptors = grpcContainer.getSortedGlobalInterceptors();

        if (launchMode == LaunchMode.DEVELOPMENT) {
            // add as last, so they will run first
            globalInterceptors.add(new DevModeInterceptor(Thread.currentThread().getContextClassLoader()));
            globalInterceptors.add(new GrpcHotReplacementInterceptor());
        }

        List<GrpcServiceDefinition> toBeRegistered = collectServiceDefinitions(grpcContainer.getServices());
        List<ServerServiceDefinition> definitions = new ArrayList<>();

        CompressionInterceptor compressionInterceptor = prepareCompressionInterceptor(configuration);

        for (GrpcServiceDefinition service : toBeRegistered) {
            ServerServiceDefinition defWithInterceptors = serviceWithInterceptors(
                    vertx, grpcContainer, blockingMethodsPerService, virtualMethodsPerService, compressionInterceptor, service,
                    launchMode == LaunchMode.DEVELOPMENT);
            LOGGER.debugf("Registered gRPC service '%s'", service.definition.getServiceDescriptor().getName());
            ServerServiceDefinition serviceDefinition = ServerInterceptors.intercept(defWithInterceptors, globalInterceptors);
            GrpcServiceBridge bridge = GrpcServiceBridge.bridge(serviceDefinition);
            bridge.bind(server);
            definitions.add(service.definition);
        }

        boolean reflectionServiceEnabled = configuration.enableReflectionService || launchMode == LaunchMode.DEVELOPMENT;

        if (reflectionServiceEnabled) {
            LOGGER.info("Registering gRPC reflection service");
            ReflectionServiceV1 reflectionServiceV1 = new ReflectionServiceV1(definitions);
            ReflectionServiceV1alpha reflectionServiceV1alpha = new ReflectionServiceV1alpha(definitions);
            ServerServiceDefinition serviceDefinition = ServerInterceptors.intercept(reflectionServiceV1, globalInterceptors);
            GrpcServiceBridge bridge = GrpcServiceBridge.bridge(serviceDefinition);
            bridge.bind(server);
            ServerServiceDefinition serviceDefinitionAlpha = ServerInterceptors.intercept(reflectionServiceV1alpha,
                    globalInterceptors);
            GrpcServiceBridge bridgeAlpha = GrpcServiceBridge.bridge(serviceDefinitionAlpha);
            bridgeAlpha.bind(server);
        }

        initHealthStorage();

        LOGGER.info("Starting new Vert.x gRPC server ...");
        Route route = routerSupplier.getValue().route().handler(ctx -> {
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
                                server.handle(ctx.request());
                            }
                        });
                        return;
                    }
                }
                server.handle(ctx.request());
            }
        });
        shutdown.addShutdownTask(route::remove); // remove this route at shutdown, this should reset it

        initHealthStorage();
    }

    // TODO -- handle Avro, plain text ... when supported / needed
    private static boolean isGrpc(RoutingContext rc) {
        String header = rc.request().getHeader("content-type");
        return header != null && GRPC_CONTENT_TYPE.matcher(header.toLowerCase(Locale.ROOT)).matches();
    }

    private void prodStart(GrpcContainer grpcContainer, Vertx vertx, GrpcServerConfiguration configuration,
            GrpcBuilderProvider<?> provider, Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            LaunchMode launchMode) {
        CompletableFuture<Void> startResult = new CompletableFuture<>();

        vertx.deployVerticle(
                () -> new GrpcServerVerticle(configuration, grpcContainer, provider, launchMode, blockingMethodsPerService,
                        virtualMethodsPerService),
                new DeploymentOptions().setInstances(configuration.instances),
                result -> {
                    if (result.failed()) {
                        startResult.completeExceptionally(result.cause());
                    } else {
                        GrpcServerRecorder.this.postStartup(configuration, provider, launchMode == LaunchMode.TEST);

                        startResult.complete(null);
                    }
                });

        try {
            startResult.get(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Unable to start the gRPC server, waiting for server start interrupted");
        } catch (TimeoutException e) {
            LOGGER.error("Unable to start the gRPC server, still not listening after 1 minute");
        } catch (ExecutionException e) {
            LOGGER.error("Unable to start the gRPC server", e.getCause());
        }
    }

    private void postStartup(GrpcServerConfiguration configuration, GrpcBuilderProvider<?> provider, boolean test) {
        initHealthStorage();
        int port = test ? testPort(configuration) : configuration.port;
        String msg = "Started ";
        if (provider != null)
            msg += provider.serverInfo(configuration.host, port, configuration);
        else
            msg += String.format("gRPC server on %s:%d [%s]",
                    configuration.host, port, "TLS enabled: " + !configuration.plainText);
        LOGGER.info(msg);
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

    private void devModeStart(GrpcContainer grpcContainer, Vertx vertx, GrpcServerConfiguration configuration,
            GrpcBuilderProvider<?> provider, Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            ShutdownContext shutdown,
            LaunchMode launchMode) {

        Map.Entry<Integer, Server> portToServer = buildServer(vertx, configuration, provider,
                blockingMethodsPerService, virtualMethodsPerService, grpcContainer, launchMode);

        Server server = portToServer.getValue();
        if (provider == null) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();

            devModeWrapper = new DevModeWrapper(Thread.currentThread().getContextClassLoader());

            VertxServer vertxServer = (VertxServer) server;
            vertxServer.start(ar -> {
                if (ar.failed()) {
                    Throwable effectiveCause = getEffectiveThrowable(ar, portToServer);
                    if (effectiveCause instanceof QuarkusBindException) {
                        LOGGER.error("Unable to start the gRPC server");
                    } else {
                        LOGGER.error("Unable to start the gRPC server", effectiveCause);
                    }
                    future.completeExceptionally(effectiveCause);
                } else {
                    postStartup(configuration, provider, false);
                    future.complete(true);
                    grpcVerticleCount.incrementAndGet();
                }
            });

            try {
                future.get(1, TimeUnit.MINUTES);
            } catch (TimeoutException e) {
                LOGGER.error("Failed to start grpc server in time", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("grpc server start failed", e);
            } catch (InterruptedException e) {
                LOGGER.warn("Waiting for grpc server start interrupted", e);
                Thread.currentThread().interrupt();
            }

            GrpcServerReloader.init(vertxServer);
            shutdown.addShutdownTask(GrpcServerReloader::reset);
        } else {
            try {
                provider.startServer(server);
            } catch (Exception e) {
                LOGGER.error("Unable to start the gRPC server", e);
                throw new IllegalStateException(e);
            }
            postStartup(configuration, provider, false);
            provider.postStartup(server, shutdown);
        }
    }

    private void applyNettySettings(GrpcServerConfiguration configuration, VertxServerBuilder builder) {
        if (configuration.netty != null) {
            GrpcServerNettyConfig config = configuration.netty;
            NettyServerBuilder nettyServerBuilder = builder.nettyBuilder();

            config.keepAliveTime.ifPresent(
                    duration -> nettyServerBuilder.keepAliveTime(duration.toNanos(), TimeUnit.NANOSECONDS));

            config.permitKeepAliveTime.ifPresent(
                    duration -> nettyServerBuilder.permitKeepAliveTime(duration.toNanos(), TimeUnit.NANOSECONDS));
            config.permitKeepAliveWithoutCalls.ifPresent(nettyServerBuilder::permitKeepAliveWithoutCalls);
        }
    }

    @SuppressWarnings("rawtypes")
    private void applyTransportSecurityConfig(GrpcServerConfiguration configuration, ServerBuilder builder) {
        if (configuration.transportSecurity != null) {
            File cert = configuration.transportSecurity.certificate
                    .map(File::new)
                    .orElse(null);
            File key = configuration.transportSecurity.key
                    .map(File::new)
                    .orElse(null);
            if (cert != null || key != null) {
                builder.useTransportSecurity(cert, key);
            }
        }
    }

    private static boolean hasNoServices(Instance<BindableService> services) {
        return services.isUnsatisfied()
                || services.stream().count() == 1
                        && services.get().bindService().getServiceDescriptor().getName().equals("grpc.health.v1.Health");
    }

    private static List<GrpcServiceDefinition> collectServiceDefinitions(Instance<BindableService> services) {
        List<GrpcServiceDefinition> definitions = new ArrayList<>();
        for (BindableService service : services) {
            ServerServiceDefinition definition = service.bindService();
            definitions.add(new GrpcServiceDefinition(service, definition));
        }

        // Set the last service definitions in use, referenced in the Dev UI
        GrpcServerRecorder.services = definitions;

        return definitions;
    }

    private Throwable getEffectiveThrowable(AsyncResult<Void> ar, Map.Entry<Integer, Server> portToServer) {
        Throwable effectiveCause = ar.cause();
        while (effectiveCause.getCause() != null) {
            effectiveCause = effectiveCause.getCause();
        }
        if (effectiveCause instanceof BindException) {
            effectiveCause = new QuarkusBindException(portToServer.getKey());
        }
        return effectiveCause;
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

    private void devModeReload(GrpcContainer grpcContainer, Vertx vertx, GrpcServerConfiguration configuration,
            GrpcBuilderProvider<?> provider, Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService, ShutdownContext shutdown) {
        List<GrpcServiceDefinition> services = collectServiceDefinitions(grpcContainer.getServices());

        List<ServerServiceDefinition> definitions = new ArrayList<>();
        Map<String, ServerMethodDefinition<?, ?>> methods = new HashMap<>();
        for (GrpcServiceDefinition service : services) {
            definitions.add(service.definition);
        }

        List<ServerServiceDefinition> servicesWithInterceptors = new ArrayList<>();
        CompressionInterceptor compressionInterceptor = prepareCompressionInterceptor(configuration);
        for (GrpcServiceDefinition service : services) {
            servicesWithInterceptors.add(
                    serviceWithInterceptors(vertx, grpcContainer, blockingMethodsPerService, virtualMethodsPerService,
                            compressionInterceptor, service, true));
        }

        // add after actual services, so we don't inspect them for interceptors, etc
        servicesWithInterceptors.add(new ReflectionServiceV1(definitions).bindService());
        servicesWithInterceptors.add(new ReflectionServiceV1alpha(definitions).bindService());

        for (ServerServiceDefinition serviceWithInterceptors : servicesWithInterceptors) {
            for (ServerMethodDefinition<?, ?> method : serviceWithInterceptors.getMethods()) {
                methods.put(method.getMethodDescriptor().getFullMethodName(), method);
            }
        }

        initHealthStorage();

        List<ServerInterceptor> globalInterceptors = grpcContainer.getSortedGlobalInterceptors();

        if (provider != null) {
            globalInterceptors.add(new DevModeInterceptor(Thread.currentThread().getContextClassLoader()));
            globalInterceptors.add(new GrpcHotReplacementInterceptor());
            provider.devModeReload(servicesWithInterceptors, methods, globalInterceptors, shutdown);
        } else {
            devModeWrapper = new DevModeWrapper(Thread.currentThread().getContextClassLoader());
            GrpcServerReloader.reinitialize(servicesWithInterceptors, methods, globalInterceptors);
            shutdown.addShutdownTask(GrpcServerReloader::reset);
        }
    }

    public static int getVerticleCount() {
        return grpcVerticleCount.get();
    }

    public RuntimeValue<ServerInterceptorStorage> initServerInterceptorStorage(
            Map<String, Set<Class<?>>> perServiceInterceptors,
            Set<Class<?>> globalInterceptors) {
        return new RuntimeValue<>(new ServerInterceptorStorage(perServiceInterceptors, globalInterceptors));
    }

    @SuppressWarnings("rawtypes")
    private Map.Entry<Integer, Server> buildServer(Vertx vertx, GrpcServerConfiguration configuration,
            GrpcBuilderProvider provider, Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            GrpcContainer grpcContainer, LaunchMode launchMode) {

        int port = launchMode == LaunchMode.TEST ? configuration.testPort : configuration.port;

        AtomicBoolean usePlainText = new AtomicBoolean();

        ServerBuilder builder;
        if (provider != null) {
            builder = provider.createServerBuilder(vertx, configuration, launchMode);
        } else {
            VertxServerBuilder vsBuilder = VertxServerBuilder.forAddress(vertx, configuration.host, port);
            // add Vert.x specific stuff here
            vsBuilder.useSsl(options -> {
                try {
                    usePlainText.set(applySslOptions(configuration, options));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            applyNettySettings(configuration, vsBuilder);
            if (launchMode == LaunchMode.DEVELOPMENT) {
                vsBuilder.commandDecorator(command -> vertx.executeBlocking(
                        GrpcHotReplacementInterceptor::fire,
                        false)
                        .onComplete(result -> devModeWrapper.run(command)));
            }
            builder = vsBuilder;
        }

        if (configuration.maxInboundMessageSize.isPresent()) {
            builder.maxInboundMessageSize(configuration.maxInboundMessageSize.getAsInt());
        }

        if (configuration.maxInboundMetadataSize.isPresent()) {
            builder.maxInboundMetadataSize(configuration.maxInboundMetadataSize.getAsInt());
        }

        Optional<Duration> handshakeTimeout = configuration.handshakeTimeout;
        handshakeTimeout.ifPresent(duration -> builder.handshakeTimeout(duration.toMillis(), TimeUnit.MILLISECONDS));

        applyTransportSecurityConfig(configuration, builder);

        boolean reflectionServiceEnabled = configuration.enableReflectionService || launchMode == LaunchMode.DEVELOPMENT;
        List<GrpcServiceDefinition> toBeRegistered = collectServiceDefinitions(grpcContainer.getServices());
        List<ServerServiceDefinition> definitions = new ArrayList<>();

        CompressionInterceptor compressionInterceptor = prepareCompressionInterceptor(configuration);

        for (GrpcServiceDefinition service : toBeRegistered) {
            builder.addService(
                    serviceWithInterceptors(vertx, grpcContainer, blockingMethodsPerService,
                            virtualMethodsPerService,
                            compressionInterceptor, service, launchMode == LaunchMode.DEVELOPMENT));
            LOGGER.debugf("Registered gRPC service '%s'", service.definition.getServiceDescriptor().getName());
            definitions.add(service.definition);
        }

        if (reflectionServiceEnabled) {
            LOGGER.info("Registering gRPC reflection service");
            builder.addService(new ReflectionServiceV1(definitions));
            builder.addService(new ReflectionServiceV1alpha(definitions));
        }

        for (ServerInterceptor serverInterceptor : grpcContainer.getSortedGlobalInterceptors()) {
            builder.intercept(serverInterceptor);
        }

        String msg = "Starting ";
        if (provider != null)
            msg += provider.serverInfo(configuration.host, port, configuration);
        else
            msg += String.format("gRPC server on %s:%d [TLS enabled: %s]", configuration.host, port, !usePlainText.get());
        LOGGER.debug(msg);

        return new AbstractMap.SimpleEntry<>(port, builder.build());
    }

    /**
     * Compression interceptor if needed, null otherwise
     *
     * @param configuration gRPC server configuration
     * @return interceptor or null
     */
    private CompressionInterceptor prepareCompressionInterceptor(GrpcServerConfiguration configuration) {
        CompressionInterceptor compressionInterceptor = null;
        if (configuration.compression.isPresent()) {
            compressionInterceptor = new CompressionInterceptor(configuration.compression.get());
        }
        return compressionInterceptor;
    }

    private ServerServiceDefinition serviceWithInterceptors(Vertx vertx, GrpcContainer grpcContainer,
            Map<String, List<String>> blockingMethodsPerService,
            Map<String, List<String>> virtualMethodsPerService,
            CompressionInterceptor compressionInterceptor,
            GrpcServiceDefinition service, boolean devMode) {
        List<ServerInterceptor> interceptors = new ArrayList<>();
        if (compressionInterceptor != null) {
            interceptors.add(compressionInterceptor);
        }

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
        return ServerInterceptors.intercept(service.definition, interceptors);
    }

    private class GrpcServerVerticle extends AbstractVerticle {
        private final GrpcServerConfiguration configuration;
        private final GrpcContainer grpcContainer;
        private final GrpcBuilderProvider provider;
        private final LaunchMode launchMode;
        private final Map<String, List<String>> blockingMethodsPerService;
        private final Map<String, List<String>> virtualMethodsPerService;
        private volatile PortSystemProperties portSystemProperties;

        private Server grpcServer;

        GrpcServerVerticle(GrpcServerConfiguration configuration, GrpcContainer grpcContainer,
                GrpcBuilderProvider provider, LaunchMode launchMode,
                Map<String, List<String>> blockingMethodsPerService,
                Map<String, List<String>> virtualMethodsPerService) {
            this.configuration = configuration;
            this.grpcContainer = grpcContainer;
            this.provider = provider;
            this.launchMode = launchMode;
            this.blockingMethodsPerService = blockingMethodsPerService;
            this.virtualMethodsPerService = virtualMethodsPerService;
        }

        @Override
        public void start(Promise<Void> startPromise) {
            if (grpcContainer.getServices().isUnsatisfied()) {
                LOGGER.warn(
                        "Unable to find bean exposing the `BindableService` interface - not starting the gRPC server");
                return;
            }
            Map.Entry<Integer, Server> portToServer = buildServer(getVertx(), configuration, provider,
                    blockingMethodsPerService, virtualMethodsPerService, grpcContainer, launchMode);

            grpcServer = portToServer.getValue();
            if (grpcServer instanceof VertxServer) {
                VertxServer server = (VertxServer) grpcServer;
                server.start(ar -> {
                    if (ar.failed()) {
                        Throwable effectiveCause = getEffectiveThrowable(ar, portToServer);
                        if (effectiveCause instanceof QuarkusBindException) {
                            LOGGER.error("Unable to start the gRPC server");
                        } else {
                            LOGGER.error("Unable to start the gRPC server", effectiveCause);
                        }
                        startPromise.fail(effectiveCause);
                    } else {
                        try {
                            int actualPort = grpcServer.getPort();
                            if (actualPort != portToServer.getKey()) {
                                portSystemProperties = new PortSystemProperties();
                                portSystemProperties.set("grpc.server", actualPort, launchMode);
                            }
                        } catch (Exception e) {
                            // Ignore, port reused.
                        }
                        startPromise.complete();
                        grpcVerticleCount.incrementAndGet();
                    }
                });
            } else {
                // XDS server blocks on initialStartFuture
                vertx.executeBlocking(() -> {
                    try {
                        grpcServer.start();
                        int actualPort = grpcServer.getPort();
                        if (actualPort != portToServer.getKey()) {
                            portSystemProperties = new PortSystemProperties();
                            portSystemProperties.set("grpc.server", actualPort, launchMode);
                        }
                        startPromise.complete();
                    } catch (Exception e) {
                        LOGGER.error("Unable to start gRPC server", e);
                        startPromise.fail(e);
                    }
                    return null;
                });
            }
        }

        @Override
        public void stop(Promise<Void> stopPromise) {
            if (grpcServer instanceof VertxServer) {
                VertxServer server = (VertxServer) grpcServer;
                server.shutdown(ar -> {
                    if (ar.failed()) {
                        Throwable cause = ar.cause();
                        LOGGER.errorf(cause, "Unable to stop the gRPC server gracefully");
                        stopPromise.fail(cause);
                    } else {
                        LOGGER.debug("gRPC Server stopped");
                        stopPromise.complete();
                        grpcVerticleCount.decrementAndGet();
                    }
                    if (portSystemProperties != null) {
                        portSystemProperties.restore();
                    }
                });
            } else {
                try {
                    grpcServer.shutdownNow().awaitTermination(10, TimeUnit.SECONDS);
                    stopPromise.complete();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    stopPromise.fail(e);
                    throw new IllegalStateException(e);
                } catch (Exception e) {
                    LOGGER.errorf(e, "Unable to stop the gRPC server gracefully");
                    stopPromise.fail(e);
                } finally {
                    if (portSystemProperties != null) {
                        portSystemProperties.restore();
                    }
                }
            }
        }
    }

    private static class DevModeWrapper {
        private final ClassLoader classLoader;

        public DevModeWrapper(ClassLoader contextClassLoader) {
            classLoader = contextClassLoader;
        }

        public void run(Runnable command) {
            ClassLoader originalTccl = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
            try {
                command.run();
            } finally {
                Thread.currentThread().setContextClassLoader(originalTccl);
            }
        }
    }

}
