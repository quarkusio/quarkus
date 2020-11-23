package io.quarkus.grpc.runtime;

import static io.quarkus.grpc.runtime.GrpcSslUtils.applySslOptions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import grpc.health.v1.HealthOuterClass;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.devmode.GrpcHotReplacementInterceptor;
import io.quarkus.grpc.runtime.devmode.GrpcServerReloader;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.reflection.ReflectionService;
import io.quarkus.grpc.runtime.supports.BlockingServerInterceptor;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

@Recorder
public class GrpcServerRecorder {
    private static final Logger LOGGER = Logger.getLogger(GrpcServerRecorder.class.getName());

    private static final AtomicInteger grpcVerticleCount = new AtomicInteger(0);
    private Map<String, List<String>> blockingMethodsPerService = Collections.emptyMap();

    public void initializeGrpcServer(RuntimeValue<Vertx> vertxSupplier,
            GrpcConfiguration cfg,
            ShutdownContext shutdown,
            Map<String, List<String>> blockingMethodsPerServiceImplementationClass) {
        GrpcContainer grpcContainer = Arc.container().instance(GrpcContainer.class).get();
        if (grpcContainer == null) {
            throw new IllegalStateException("gRPC not initialized, GrpcContainer not found");
        }
        Vertx vertx = vertxSupplier.getValue();
        if (hasNoServices(grpcContainer.getServices())) {
            throw new IllegalStateException(
                    "Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
        }

        this.blockingMethodsPerService = blockingMethodsPerServiceImplementationClass;

        GrpcServerConfiguration configuration = cfg.server;
        final boolean devMode = ProfileManager.getLaunchMode() == LaunchMode.DEVELOPMENT;

        if (devMode) {
            // start single server, not in a verticle, regardless of the configuration.instances
            // for reason unknown to me, verticles occasionally get undeployed on dev mode reload
            if (GrpcServerReloader.getServer() == null) {
                devModeStart(grpcContainer, vertx, configuration, shutdown);
            } else {
                devModeReload(grpcContainer);
            }
        } else {
            prodStart(grpcContainer, vertx, configuration);
        }
    }

    private void prodStart(GrpcContainer grpcContainer, Vertx vertx, GrpcServerConfiguration configuration) {
        CompletableFuture<Void> startResult = new CompletableFuture<>();

        vertx.deployVerticle(
                new Supplier<Verticle>() {
                    @Override
                    public Verticle get() {
                        return new GrpcServerVerticle(configuration, grpcContainer);
                    }
                },
                new DeploymentOptions().setInstances(configuration.instances),
                new Handler<AsyncResult<String>>() {
                    @Override
                    public void handle(AsyncResult<String> result) {
                        if (result.failed()) {
                            startResult.completeExceptionally(result.cause());
                        } else {
                            GrpcServerRecorder.this.postStartup(grpcContainer, configuration);

                            startResult.complete(null);
                        }
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

    private void postStartup(GrpcContainer grpcContainer, GrpcServerConfiguration configuration) {
        grpcContainer.getHealthStorage().stream().forEach(new Consumer<GrpcHealthStorage>() { //NOSONAR
            @Override
            public void accept(GrpcHealthStorage storage) {
                storage.setStatus(GrpcHealthStorage.DEFAULT_SERVICE_NAME,
                        HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING);
                grpcContainer.getServices().forEach(
                        new Consumer<BindableService>() { // NOSONAR
                            @Override
                            public void accept(BindableService service) {
                                ServerServiceDefinition definition = service.bindService();
                                storage.setStatus(definition.getServiceDescriptor().getName(),
                                        HealthOuterClass.HealthCheckResponse.ServingStatus.SERVING);
                            }
                        });
            }
        });
        LOGGER.infof("gRPC Server started on %s:%d [SSL enabled: %s]",
                configuration.host, configuration.port, !configuration.plainText);
    }

    private void devModeStart(GrpcContainer grpcContainer, Vertx vertx, GrpcServerConfiguration configuration,
            ShutdownContext shutdown) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        VertxServer vertxServer = buildServer(vertx, configuration, grpcContainer, true)
                .start(new Handler<AsyncResult<Void>>() { // NOSONAR
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        if (ar.failed()) {
                            LOGGER.error("Unable to start the gRPC server", ar.cause());
                            future.completeExceptionally(ar.cause());
                        } else {
                            postStartup(grpcContainer, configuration);
                            future.complete(true);
                            grpcVerticleCount.incrementAndGet();
                        }
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
        shutdown.addShutdownTask(
                new Runnable() { // NOSONAR
                    @Override
                    public void run() {
                        GrpcServerReloader.reset();
                    }
                });
    }

    private void applyTransportSecurityConfig(GrpcServerConfiguration configuration, VertxServerBuilder builder) {
        if (configuration.transportSecurity != null) {
            File cert = configuration.transportSecurity.certificate
                    .map(new Function<String, File>() { // NOSONAR
                        @Override
                        public File apply(String pathname) {
                            return new File(pathname);
                        }
                    })
                    .orElse(null);
            File key = configuration.transportSecurity.key
                    .map(new Function<String, File>() { // NOSONAR
                        @Override
                        public File apply(String pathname) {
                            return new File(pathname);
                        }
                    })
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
        return definitions;
    }

    private static class GrpcServiceDefinition {
        public final BindableService service;
        public final ServerServiceDefinition definition;

        GrpcServiceDefinition(BindableService service, ServerServiceDefinition definition) {
            this.service = service;
            this.definition = definition;
        }

        public String getImplementationClassName() {
            return service.getClass().getName();
        }
    }

    private static void devModeReload(GrpcContainer grpcContainer) {
        List<GrpcServiceDefinition> svc = collectServiceDefinitions(grpcContainer.getServices());

        List<ServerServiceDefinition> definitions = new ArrayList<>();
        Map<String, ServerMethodDefinition<?, ?>> methods = new HashMap<>();
        for (GrpcServiceDefinition service : svc) {
            for (ServerMethodDefinition<?, ?> method : service.definition.getMethods()) {
                methods.put(method.getMethodDescriptor().getFullMethodName(), method);
            }
            definitions.add(service.definition);
        }

        ServerServiceDefinition reflectionService = new ReflectionService(definitions).bindService();

        for (ServerMethodDefinition<?, ?> method : reflectionService.getMethods()) {
            methods.put(method.getMethodDescriptor().getFullMethodName(), method);
        }

        GrpcServerReloader.reinitialize(definitions, methods, grpcContainer.getSortedInterceptors());
    }

    public static int getVerticleCount() {
        return grpcVerticleCount.get();
    }

    private VertxServer buildServer(Vertx vertx, GrpcServerConfiguration configuration,
            GrpcContainer grpcContainer, boolean devMode) {
        VertxServerBuilder builder = VertxServerBuilder
                .forAddress(vertx, configuration.host, configuration.port);

        AtomicBoolean usePlainText = new AtomicBoolean();
        builder.useSsl(new Handler<HttpServerOptions>() { // NOSONAR
            @Override
            public void handle(HttpServerOptions options) {
                try {
                    usePlainText.set(applySslOptions(configuration, options));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        });

        if (configuration.maxInboundMessageSize.isPresent()) {
            builder.maxInboundMessageSize(configuration.maxInboundMessageSize.getAsInt());
        }

        if (configuration.maxInboundMetadataSize.isPresent()) {
            builder.maxInboundMetadataSize(configuration.maxInboundMetadataSize.getAsInt());
        }

        Optional<Duration> handshakeTimeout = configuration.handshakeTimeout;
        if (handshakeTimeout.isPresent()) {
            builder.handshakeTimeout(handshakeTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
        }

        applyTransportSecurityConfig(configuration, builder);

        boolean reflectionServiceEnabled = configuration.enableReflectionService
                || ProfileManager.getLaunchMode() == LaunchMode.DEVELOPMENT;
        List<GrpcServiceDefinition> toBeRegistered = collectServiceDefinitions(grpcContainer.getServices());
        List<ServerServiceDefinition> definitions = new ArrayList<>();
        for (GrpcServiceDefinition service : toBeRegistered) {
            // We only register the blocking interceptor if needed by at least one method of the service.
            if (blockingMethodsPerService.isEmpty()) {
                // Fast track - no usage of @Blocking
                builder.addService(service.definition);
            } else {
                List<String> list = blockingMethodsPerService.get(service.getImplementationClassName());
                if (list == null) {
                    // The service does not contain any methods annotated with @Blocking - no need for the itcp
                    builder.addService(service.definition);
                } else {
                    builder.addService(
                            ServerInterceptors.intercept(service.definition, new BlockingServerInterceptor(vertx, list)));
                }
            }
            LOGGER.debugf("Registered gRPC service '%s'", service.definition.getServiceDescriptor().getName());
            definitions.add(service.definition);
        }

        if (reflectionServiceEnabled) {
            LOGGER.info("Registering gRPC reflection service");
            builder.addService(new ReflectionService(definitions));
        }

        for (ServerInterceptor serverInterceptor : grpcContainer.getSortedInterceptors()) {
            builder.intercept(serverInterceptor);
        }

        if (devMode) {
            builder.commandDecorator(new Consumer<Runnable>() {
                @Override
                public void accept(Runnable command) {
                    vertx.executeBlocking(new Handler<Promise<Boolean>>() {
                        @Override
                        public void handle(Promise<Boolean> event) {
                            event.complete(GrpcHotReplacementInterceptor.fire());
                        }
                    },
                            false,
                            new Handler<AsyncResult<Boolean>>() {
                                @Override
                                public void handle(AsyncResult<Boolean> result) {
                                    command.run();
                                }
                            });
                }
            });
        }

        LOGGER.debugf("Starting gRPC Server on %s:%d  [SSL enabled: %s]...",
                configuration.host, configuration.port, !usePlainText.get());

        return builder.build();
    }

    private class GrpcServerVerticle extends AbstractVerticle {
        private final GrpcServerConfiguration configuration;
        private final GrpcContainer grpcContainer;

        private VertxServer grpcServer;

        GrpcServerVerticle(GrpcServerConfiguration configuration, GrpcContainer grpcContainer) {
            this.configuration = configuration;
            this.grpcContainer = grpcContainer;
        }

        @Override
        public void start(Promise<Void> startPromise) {
            if (grpcContainer.getServices().isUnsatisfied()) {
                LOGGER.warn(
                        "Unable to find bean exposing the `BindableService` interface - not starting the gRPC server");
                return;
            }
            grpcServer = buildServer(getVertx(), configuration, grpcContainer, false)
                    .start(new Handler<AsyncResult<Void>>() { // NOSONAR
                        @Override
                        public void handle(AsyncResult<Void> ar) {
                            if (ar.failed()) {
                                LOGGER.error("Unable to start the gRPC server", ar.cause());
                                startPromise.fail(ar.cause());
                            } else {
                                startPromise.complete();
                                grpcVerticleCount.incrementAndGet();
                            }
                        }
                    });
        }

        @Override
        public void stop(Promise<Void> stopPromise) {
            grpcServer.shutdown(new Handler<AsyncResult<Void>>() { // NOSONAR
                @Override
                public void handle(AsyncResult<Void> ar) {
                    if (ar.failed()) {
                        LOGGER.errorf(ar.cause(), "Unable to stop the gRPC server gracefully");
                    } else {
                        LOGGER.debug("gRPC Server stopped");
                        stopPromise.complete();
                        grpcVerticleCount.decrementAndGet();
                    }
                }
            });
        }
    }
}
