package io.quarkus.grpc.runtime;

import static io.quarkus.grpc.runtime.GrpcSslUtils.applySslOptions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import grpc.health.v1.HealthOuterClass;
import io.grpc.BindableService;
import io.grpc.ServerInterceptor;
import io.grpc.ServerServiceDefinition;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.runtime.config.GrpcConfiguration;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.reflection.ReflectionService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

@Recorder
public class GrpcServerRecorder {
    private static final Logger LOGGER = Logger.getLogger(GrpcServerRecorder.class.getName());

    private static final AtomicInteger grpcVerticleCount = new AtomicInteger(0);

    public void initializeGrpcServer(RuntimeValue<Vertx> vertxSupplier, GrpcConfiguration cfg) {
        GrpcContainer grpcContainer = Arc.container().instance(GrpcContainer.class).get();
        if (grpcContainer == null) {
            throw new IllegalStateException("gRPC not initialized, GrpcContainer not found");
        }
        Vertx vertx = vertxSupplier.getValue();
        if (hasNoServices(grpcContainer.getServices())) {
            throw new IllegalStateException(
                    "Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
        }

        CompletableFuture<Void> startResult = new CompletableFuture<>();

        GrpcServerConfiguration configuration = cfg.server;
        vertx.deployVerticle(() -> new GrpcServerVerticle(configuration, grpcContainer),
                new DeploymentOptions().setInstances(configuration.instances),
                result -> {
                    if (result.failed()) {
                        startResult.completeExceptionally(result.cause());
                    } else {
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

    private static List<ServerServiceDefinition> gatherServices(Instance<BindableService> services) {
        List<ServerServiceDefinition> definitions = new ArrayList<>();

        services.forEach(new Consumer<BindableService>() { // NOSONAR
            @Override
            public void accept(BindableService bindable) {
                ServerServiceDefinition definition = bindable.bindService();
                LOGGER.debugf("Registered gRPC service '%s'", definition.getServiceDescriptor().getName());
                definitions.add(definition);
            }
        });
        return definitions;
    }

    public static int getVerticleCount() {
        return grpcVerticleCount.get();
    }

    private class GrpcServerVerticle extends AbstractVerticle {
        private final GrpcServerConfiguration configuration;
        private final GrpcContainer grpcContainer;

        private VertxServer grpcServer;

        public GrpcServerVerticle(GrpcServerConfiguration configuration, GrpcContainer grpcContainer) {
            this.configuration = configuration;
            this.grpcContainer = grpcContainer;
        }

        @Override
        public void start(Promise<Void> startPromise) throws Exception {
            VertxServerBuilder builder = VertxServerBuilder
                    .forAddress(getVertx(), configuration.host, configuration.port);

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
            Optional<Duration> handshakeTimeout = configuration.handshakeTimeout;
            if (handshakeTimeout.isPresent()) {
                builder.handshakeTimeout(handshakeTimeout.get().toMillis(), TimeUnit.MILLISECONDS);
            }

            applyTransportSecurityConfig(configuration, builder);

            if (grpcContainer.getServices().isUnsatisfied()) {
                LOGGER.warn(
                        "Unable to find bean exposing the `BindableService` interface - not starting the gRPC server");
                return;
            }

            boolean reflectionServiceEnabled = configuration.enableReflectionService
                    || ProfileManager.getLaunchMode() == LaunchMode.DEVELOPMENT;
            List<ServerServiceDefinition> definitions = gatherServices(grpcContainer.getServices());
            for (ServerServiceDefinition definition : definitions) {
                builder.addService(definition);
            }

            if (reflectionServiceEnabled) {
                LOGGER.info("Registering gRPC reflection service");
                builder.addService(new ReflectionService(definitions));
            }

            for (ServerInterceptor serverInterceptor : grpcContainer.getSortedInterceptors()) {
                builder.intercept(serverInterceptor);
            }

            LOGGER.debugf("Starting gRPC Server on %s:%d  [SSL enabled: %s]...",
                    configuration.host, configuration.port, !usePlainText.get());

            VertxServer server = builder.build();
            grpcServer = server.start(new Handler<AsyncResult<Void>>() { // NOSONAR
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
        public void stop(Promise<Void> stopPromise) throws Exception {
            LOGGER.debug("Stopping gRPC server");
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
