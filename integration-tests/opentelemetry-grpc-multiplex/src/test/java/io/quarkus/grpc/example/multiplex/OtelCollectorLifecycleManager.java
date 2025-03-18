package io.quarkus.grpc.example.multiplex;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterTracesConfig.Protocol.GRPC;
import static org.testcontainers.Testcontainers.exposeHostPorts;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import com.google.protobuf.InvalidProtocolBufferException;

import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceRequest;
import io.opentelemetry.proto.collector.trace.v1.ExportTraceServiceResponse;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.SelfSignedCertificate;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;

public class OtelCollectorLifecycleManager implements QuarkusTestResourceLifecycleManager {

    private static final String COLLECTOR_IMAGE = "ghcr.io/open-telemetry/opentelemetry-java/otel-collector";
    private static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;
    private static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;
    private static final Integer COLLECTOR_OTLP_GRPC_MTLS_PORT = 5317;
    private static final Integer COLLECTOR_OTLP_HTTP_MTLS_PORT = 5318;
    private static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;
    private static final ServiceName TRACE_SERVICE_NAME = ServiceName
            .create("opentelemetry.proto.collector.trace.v1.TraceService");
    private static final String TRACE_METHOD_NAME = "Export";

    private SelfSignedCertificate serverTls;
    private SelfSignedCertificate clientTlS;

    private String protocol = GRPC;
    private boolean enableTLS = false;
    private boolean preventTrustCert = false;
    private boolean enableCompression = false;
    private Vertx vertx;

    private HttpServer server;

    private GenericContainer<?> collector;
    private Traces collectedTraces;

    @Override
    public void init(Map<String, String> initArgs) {
        var enableTLSStr = initArgs.get("enableTLS");
        if (enableTLSStr != null && !enableTLSStr.isEmpty()) {
            enableTLS = Boolean.parseBoolean(enableTLSStr);

            var preventTrustCertStr = initArgs.get("preventTrustCert");
            if (preventTrustCertStr != null && !preventTrustCertStr.isEmpty()) {
                preventTrustCert = Boolean.parseBoolean(preventTrustCertStr);
            }
        }

        var enableCompressionStr = initArgs.get("enableCompression");
        if (enableCompressionStr != null && !enableCompressionStr.isEmpty()) {
            enableCompression = Boolean.parseBoolean(enableCompressionStr);
        }

        if (initArgs.containsKey("protocol")) {
            protocol = initArgs.get("protocol");
        }
    }

    @Override
    public Map<String, String> start() {
        setupVertxGrpcServer();
        int vertxGrpcServerPort = server.actualPort();

        // Expose the port the in-process OTLP gRPC server will run on before the collector is
        // initialized so the collector can connect to it.
        exposeHostPorts(vertxGrpcServerPort);

        serverTls = SelfSignedCertificate.create();
        clientTlS = SelfSignedCertificate.create();

        collector = new GenericContainer<>(DockerImageName.parse(COLLECTOR_IMAGE))
                .withImagePullPolicy(PullPolicy.alwaysPull())
                .withEnv("LOGGING_EXPORTER_VERBOSITY_LEVEL", "normal")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(serverTls.certificatePath(), 0555),
                        "/server.cert")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(serverTls.privateKeyPath(), 0555), "/server.key")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(clientTlS.certificatePath(), 0555),
                        "/client.cert")
                .withEnv(
                        "OTLP_EXPORTER_ENDPOINT", "host.testcontainers.internal:" + vertxGrpcServerPort)
                .withEnv("MTLS_CLIENT_CERTIFICATE", "/client.cert")
                .withEnv("MTLS_SERVER_CERTIFICATE", "/server.cert")
                .withEnv("MTLS_SERVER_KEY", "/server.key")
                .withClasspathResourceMapping(
                        "otel-config.yaml", "/otel-config.yaml", BindMode.READ_ONLY)
                .withCommand("--config", "/otel-config.yaml")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("otel-collector")))
                .withExposedPorts(
                        COLLECTOR_OTLP_GRPC_PORT,
                        COLLECTOR_OTLP_HTTP_PORT,
                        COLLECTOR_OTLP_GRPC_MTLS_PORT,
                        COLLECTOR_OTLP_HTTP_MTLS_PORT,
                        COLLECTOR_HEALTH_CHECK_PORT)
                .waitingFor(Wait.forHttp("/").forPort(COLLECTOR_HEALTH_CHECK_PORT));
        collector.start();

        Map<String, String> result = new HashMap<>();
        result.put("quarkus.otel.exporter.otlp.traces.protocol", protocol);

        boolean isGrpc = GRPC.equals(protocol);
        int secureEndpointPort = isGrpc ? COLLECTOR_OTLP_GRPC_MTLS_PORT : COLLECTOR_OTLP_HTTP_MTLS_PORT;
        int inSecureEndpointPort = isGrpc ? COLLECTOR_OTLP_GRPC_PORT : COLLECTOR_OTLP_HTTP_PORT;

        if (enableTLS) {
            result.put("quarkus.otel.exporter.otlp.traces.endpoint",
                    "https://" + collector.getHost() + ":" + collector.getMappedPort(secureEndpointPort));
            if (!preventTrustCert) {
                result.put("quarkus.otel.exporter.otlp.traces.trust-cert.certs", serverTls.certificatePath());
            }
            result.put("quarkus.otel.exporter.otlp.traces.key-cert.certs", clientTlS.certificatePath());
            result.put("quarkus.otel.exporter.otlp.traces.key-cert.keys", clientTlS.privateKeyPath());
        } else {
            result.put("quarkus.otel.exporter.otlp.traces.endpoint",
                    "http://" + collector.getHost() + ":" + collector.getMappedPort(inSecureEndpointPort));
        }

        if (enableCompression) {
            result.put("quarkus.otel.exporter.otlp.traces.compression", "gzip");
        }

        return result;
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(collectedTraces, f -> f.getType().equals(Traces.class));
    }

    private void setupVertxGrpcServer() {
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1).setEventLoopPoolSize(1));
        GrpcServer grpcServer = GrpcServer.server(vertx);
        collectedTraces = new Traces();
        grpcServer.callHandler(request -> {

            if (request.serviceName().equals(TRACE_SERVICE_NAME) && request.methodName().equals(TRACE_METHOD_NAME)) {

                request.handler(message -> {
                    try {
                        collectedTraces.getTraceRequests().add(ExportTraceServiceRequest.parseFrom(message.getBytes()));
                        request.response().end(Buffer.buffer(ExportTraceServiceResponse.getDefaultInstance().toByteArray()));
                    } catch (InvalidProtocolBufferException e) {
                        request.response()
                                .status(GrpcStatus.INVALID_ARGUMENT)
                                .end();
                    }
                });
            } else {
                request.response()
                        .status(GrpcStatus.NOT_FOUND)
                        .end();
            }
        });
        server = vertx.createHttpServer(new HttpServerOptions().setPort(0));
        try {
            server.requestHandler(grpcServer).listen().toCompletionStage().toCompletableFuture().get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.close().andThen(ar -> {
                if (vertx != null) {
                    vertx.close();
                }
            });
        }
        if (serverTls != null) {
            serverTls.delete();
        }
        if (clientTlS != null) {
            clientTlS.delete();
        }
        if (collector != null) {
            collector.stop();
        }
    }
}
