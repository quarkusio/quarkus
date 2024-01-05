package io.quarkus.it.opentelemetry.vertx.exporter;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterConfig.Protocol.GRPC;
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

import io.opentelemetry.proto.collector.logs.v1.ExportLogsServiceRequest;
import io.opentelemetry.proto.collector.metrics.v1.ExportMetricsServiceRequest;
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

    private static final String COLLECTOR_IMAGE = "ghcr.io/open-telemetry/opentelemetry-java/" +
            "otel-collector@sha256:0d928e02b0ef5abbba775da205eb102f58b29aa75ea623465ec42445dfc5c443";
    private static final Integer COLLECTOR_OTLP_GRPC_PORT = 4317;
    private static final Integer COLLECTOR_OTLP_HTTP_PORT = 4318;
    private static final Integer COLLECTOR_OTLP_GRPC_MTLS_PORT = 5317;
    private static final Integer COLLECTOR_OTLP_HTTP_MTLS_PORT = 5318;
    private static final Integer COLLECTOR_HEALTH_CHECK_PORT = 13133;
    private static final ServiceName TRACE_SERVICE_NAME = ServiceName
            .create("opentelemetry.proto.collector.trace.v1.TraceService");
    private static final ServiceName METRIC_SERVICE_NAME = ServiceName
            .create("opentelemetry.proto.collector.metrics.v1.MetricsService");
    private static final ServiceName LOG_SERVICE_NAME = ServiceName
            .create("opentelemetry.proto.collector.logs.v1.LogsService");
    private static final String EXPORT_METHOD_NAME = "Export";

    private SelfSignedCertificate serverTls;
    private SelfSignedCertificate clientTlS;

    private String protocol = GRPC;
    private boolean enableTLS = false;
    private boolean preventTrustCert = false;
    private boolean enableCompression = false;
    private String tlsRegistryName = null;
    private Vertx vertx;

    private HttpServer server;

    private GenericContainer<?> collector;
    private Traces collectedTraces;
    private Metrics collectedMetrics;
    private Logs collectedLogs;

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

        if (initArgs.containsKey("tlsRegistryName")) {
            tlsRegistryName = initArgs.get("tlsRegistryName");
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
        result.put("quarkus.otel.exporter.otlp.metrics.protocol", protocol);
        result.put("quarkus.otel.exporter.otlp.logs.protocol", protocol);

        boolean isGrpc = GRPC.equals(protocol);
        int secureEndpointPort = isGrpc ? COLLECTOR_OTLP_GRPC_MTLS_PORT : COLLECTOR_OTLP_HTTP_MTLS_PORT;
        int inSecureEndpointPort = isGrpc ? COLLECTOR_OTLP_GRPC_PORT : COLLECTOR_OTLP_HTTP_PORT;

        if (enableTLS) {
            result.put("quarkus.otel.exporter.otlp.traces.endpoint",
                    "https://" + collector.getHost() + ":" + collector.getMappedPort(secureEndpointPort));
            result.put("quarkus.otel.exporter.otlp.metrics.endpoint",
                    "https://" + collector.getHost() + ":" + collector.getMappedPort(secureEndpointPort));
            result.put("quarkus.otel.exporter.otlp.logs.endpoint",
                    "https://" + collector.getHost() + ":" + collector.getMappedPort(secureEndpointPort));
            if (tlsRegistryName != null) {
                result.put("quarkus.otel.exporter.otlp.traces.tls-configuration-name", tlsRegistryName);
                result.put("quarkus.otel.exporter.otlp.metrics.tls-configuration-name", tlsRegistryName);
                result.put("quarkus.otel.exporter.otlp.logs.tls-configuration-name", tlsRegistryName);
                if (!preventTrustCert) {
                    result.put(String.format("quarkus.tls.%s.trust-store.pem.certs", tlsRegistryName),
                            serverTls.certificatePath());
                }
                result.put(String.format("quarkus.tls.%s.key-store.pem.0.cert", tlsRegistryName), clientTlS.certificatePath());
                result.put(String.format("quarkus.tls.%s.key-store.pem.0.key", tlsRegistryName), clientTlS.privateKeyPath());
            } else {
                if (!preventTrustCert) {
                    result.put("quarkus.otel.exporter.otlp.traces.trust-cert.certs", serverTls.certificatePath());
                    result.put("quarkus.otel.exporter.otlp.metrics.trust-cert.certs", serverTls.certificatePath());
                    result.put("quarkus.otel.exporter.otlp.logs.trust-cert.certs", serverTls.certificatePath());
                }
                result.put("quarkus.otel.exporter.otlp.traces.key-cert.certs", clientTlS.certificatePath());
                result.put("quarkus.otel.exporter.otlp.traces.key-cert.keys", clientTlS.privateKeyPath());
                result.put("quarkus.otel.exporter.otlp.metrics.key-cert.certs", clientTlS.certificatePath());
                result.put("quarkus.otel.exporter.otlp.metrics.key-cert.keys", clientTlS.privateKeyPath());
                result.put("quarkus.otel.exporter.otlp.logs.key-cert.certs", clientTlS.certificatePath());
                result.put("quarkus.otel.exporter.otlp.logs.key-cert.keys", clientTlS.privateKeyPath());
            }
        } else {
            result.put("quarkus.otel.exporter.otlp.traces.endpoint",
                    "http://" + collector.getHost() + ":" + collector.getMappedPort(inSecureEndpointPort));
            result.put("quarkus.otel.exporter.otlp.metrics.endpoint",
                    "http://" + collector.getHost() + ":" + collector.getMappedPort(inSecureEndpointPort));
            result.put("quarkus.otel.exporter.otlp.logs.endpoint",
                    "http://" + collector.getHost() + ":" + collector.getMappedPort(inSecureEndpointPort));
        }

        if (enableCompression) {
            result.put("quarkus.otel.exporter.otlp.traces.compression", "gzip");
            result.put("quarkus.otel.exporter.otlp.metrics.compression", "gzip");
            result.put("quarkus.otel.exporter.otlp.logs.compression", "gzip");
        }

        return result;
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(collectedTraces, f -> f.getType().equals(Traces.class));
        testInjector.injectIntoFields(collectedMetrics, f -> f.getType().equals(Metrics.class));
        testInjector.injectIntoFields(collectedLogs, f -> f.getType().equals(Logs.class));
    }

    private void setupVertxGrpcServer() {
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1).setEventLoopPoolSize(1));
        GrpcServer grpcServer = GrpcServer.server(vertx);
        collectedTraces = new Traces();
        collectedMetrics = new Metrics();
        collectedLogs = new Logs();
        grpcServer.callHandler(request -> {

            if (request.serviceName().equals(TRACE_SERVICE_NAME) && request.methodName().equals(EXPORT_METHOD_NAME)) {

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
            } else if (request.serviceName().equals(METRIC_SERVICE_NAME) && request.methodName().equals(EXPORT_METHOD_NAME)) {

                request.handler(message -> {
                    try {
                        collectedMetrics.getMetricRequests().add(ExportMetricsServiceRequest.parseFrom(message.getBytes()));
                        request.response().end(Buffer.buffer(ExportMetricsServiceRequest.getDefaultInstance().toByteArray()));
                    } catch (InvalidProtocolBufferException e) {
                        request.response()
                                .status(GrpcStatus.INVALID_ARGUMENT)
                                .end();
                    }
                });
            } else if (request.serviceName().equals(LOG_SERVICE_NAME) && request.methodName().equals(EXPORT_METHOD_NAME)) {

                request.handler(message -> {
                    try {
                        collectedLogs.getLogsRequests().add(ExportLogsServiceRequest.parseFrom(message.getBytes()));
                        request.response().end(Buffer.buffer(ExportLogsServiceRequest.getDefaultInstance().toByteArray()));
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
