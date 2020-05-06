package io.quarkus.grpc.runtime;

import static io.quarkus.grpc.runtime.GrpcSslUtils.applySslOptions;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.enterprise.inject.Instance;

import org.jboss.logging.Logger;

import grpc.health.v1.HealthOuterClass;
import io.grpc.BindableService;
import io.grpc.ServerServiceDefinition;
import io.quarkus.arc.Arc;
import io.quarkus.grpc.runtime.config.GrpcServerConfiguration;
import io.quarkus.grpc.runtime.health.GrpcHealthStorage;
import io.quarkus.grpc.runtime.reflection.ReflectionService;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ProfileManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.grpc.VertxServer;
import io.vertx.grpc.VertxServerBuilder;

@Recorder
public class GrpcServerRecorder {
    private static final Logger LOGGER = Logger.getLogger(GrpcServerRecorder.class.getName());

    public void initializeGrpcServer(RuntimeValue<Vertx> vertxSupplier,
            GrpcServerConfiguration configuration,
            ShutdownContext shutdown) {
        GrpcContainer grpcContainer = Arc.container().instance(GrpcContainer.class).get();
        if (grpcContainer == null) {
            throw new IllegalStateException("Grpc not initialized, GrpcContainer not found");
        }
        Vertx vertx = vertxSupplier.getValue();
        if (hasNoServices(grpcContainer.getServices())) {
            throw new IllegalStateException(
                    "Unable to find beans exposing the `BindableService` interface - not starting the gRPC server");
        }

        // TODO Support scalability model (using a verticle and instance number)

        synchronized (GrpcServerHolder.class) {
            if (GrpcServerHolder.server == null) {
                VertxServerBuilder builder = VertxServerBuilder
                        .forAddress(vertx, configuration.host, configuration.port);

                builder.useSsl(options -> {
                    try {
                        applySslOptions(configuration, options);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });

                configuration.maxInboundMessageSize.ifPresent(builder::maxInboundMessageSize);
                configuration.handshakeTimeout.ifPresent(d -> builder.handshakeTimeout(d.toMillis(), TimeUnit.MILLISECONDS));

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

                if (grpcContainer.getServices().isUnsatisfied()) {
                    LOGGER.warn("Unable to find bean exposing the `BindableService` interface - not starting the gRPC server");
                    return;
                }

                boolean reflectionServiceEnabled = configuration.enableReflectionService
                        || ProfileManager.getLaunchMode() == LaunchMode.DEVELOPMENT;
                List<ServerServiceDefinition> definitions = gatherServices(grpcContainer.getServices());
                definitions.forEach(builder::addService);

                if (reflectionServiceEnabled) {
                    LOGGER.info("Registering gRPC reflection service");
                    builder.addService(new ReflectionService(definitions));
                }

                grpcContainer.getSortedInterceptors().forEach(builder::intercept);

                LOGGER.infof("Starting GRPC Server on %s:%d  [SSL enabled: %s]...",
                        configuration.host, configuration.port, !configuration.plainText);

                CountDownLatch started = new CountDownLatch(1);
                VertxServer server = builder.build();
                GrpcServerHolder.server = server.start(new Handler<AsyncResult<Void>>() { // NOSONAR
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        onServerStarted(ar, configuration, !configuration.plainText, grpcContainer, started);
                    }
                });

                try {
                    started.await(1, TimeUnit.MINUTES);
                    shutdown.addLastShutdownTask(new Runnable() { // NOSONAR
                        @Override
                        public void run() {
                            onShutdown();
                        }
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Unable to start the gRPC server, still not listening after 1 minute");
                }
            }
        }
    }

    private void onShutdown() {
        if (GrpcServerHolder.server != null) {
            LOGGER.info("Stopping gRPC server");
            GrpcServerHolder.server.shutdown(new Handler<AsyncResult<Void>>() { // NOSONAR
                @Override
                public void handle(AsyncResult<Void> ar) {
                    if (ar.failed()) {
                        LOGGER.errorf(ar.cause(), "Unable to stop the gRPC server gracefully");
                    }
                }
            });

            try {
                GrpcServerHolder.server.awaitTermination(10, TimeUnit.SECONDS);
                LOGGER.debug("gRPC Server stopped");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Unable to stop the gRPC server gracefully");
            }

            GrpcServerHolder.server = null; // NOSONAR - this assignment is required for the hot reload.
        }
    }

    private void onServerStarted(AsyncResult<Void> ar, GrpcServerConfiguration configuration,
            boolean usePlainText, GrpcContainer grpcContainer, CountDownLatch started) {
        try {
            if (ar.succeeded()) {
                LOGGER.infof("gRPC Server started on %s:%d [SSL enabled: %s]",
                        configuration.host, configuration.port, !usePlainText);
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
            } else {
                LOGGER.errorf(ar.cause(), "Unable to start gRPC server on %s:%d", configuration.host,
                        configuration.port);
            }
        } finally {
            started.countDown();
        }

    }

    private static boolean hasNoServices(Instance<BindableService> services) {
        return services.isUnsatisfied()
                || services.stream().count() == 1
                        && services.get().bindService().getServiceDescriptor().getName().equals("grpc.health.v1.Health");
    }

    private static List<ServerServiceDefinition> gatherServices(Instance<BindableService> services) {
        List<ServerServiceDefinition> definitions = new ArrayList<>();

        services.forEach(bindable -> {
            ServerServiceDefinition definition = bindable.bindService();
            LOGGER.infof("Registered GRPC service '%s'", definition.getServiceDescriptor().getName());
            definitions.add(definition);
        });
        return definitions;
    }
}
