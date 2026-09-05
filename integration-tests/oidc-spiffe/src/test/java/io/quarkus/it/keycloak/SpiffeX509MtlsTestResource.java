package io.quarkus.it.keycloak;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import io.quarkus.spiffe.client.runtime.internal.proto.X509SVID;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVIDResponse;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.certs.CertificateRequest;
import io.smallrye.certs.CertificateUtils;
import io.smallrye.certs.KeyAlgorithm;
import io.smallrye.certs.chain.CertificateChainGenerator;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

public final class SpiffeX509MtlsTestResource implements QuarkusTestResourceLifecycleManager {

    private static final String TEST_TRUST_DOMAIN = "spiffe://test.quarkus.io";
    private static final String DEFAULT_SPIFFE_ID = TEST_TRUST_DOMAIN + "/test-workload";
    private static final ServiceName SERVICE_NAME = ServiceName.create("SpiffeWorkloadAPI");
    private static final String FETCH_X509_SVID_METHOD = "FetchX509SVID";
    private static final String SECURITY_HEADER = "workload.spiffe.io";

    private volatile Vertx vertx;
    private volatile HttpServer grpcServer;
    private volatile X509CertMaterial certMaterial;

    @Override
    public Map<String, String> start() {
        certMaterial = new X509CertMaterial();
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1).setEventLoopPoolSize(1));

        GrpcServer grpc = GrpcServer.server(vertx);
        grpc.callHandler(this::handleGrpcCall);

        grpcServer = vertx.createHttpServer(new HttpServerOptions());
        try {
            grpcServer.requestHandler(grpc)
                    .listen(0, "127.0.0.1")
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start X509 mTLS test gRPC server", e);
        }

        int port = grpcServer.actualPort();
        if (port <= 0) {
            throw new IllegalStateException("Expected to start gRPC server on a random port, but got " + port);
        }

        return Map.of(
                "quarkus.spiffe-client.endpoint-socket", "tcp://127.0.0.1:" + port,
                "spiffe.mtls.enabled", "true",
                "quarkus.oidc.tenant-enabled", "false");
    }

    @Override
    public void stop() {
        if (grpcServer != null) {
            try {
                grpcServer.close().await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // ignore
            }
        }
        if (vertx != null) {
            try {
                vertx.close().await(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                // ignore
            }
        }
        if (certMaterial != null) {
            certMaterial.cleanup();
        }
    }

    private void handleGrpcCall(GrpcServerRequest<Buffer, Buffer> request) {
        if (!SERVICE_NAME.equals(request.serviceName())) {
            request.response().status(GrpcStatus.UNIMPLEMENTED).end();
            return;
        }
        String headerValue = request.headers().get(SECURITY_HEADER);
        if (!"true".equals(headerValue)) {
            request.response()
                    .status(GrpcStatus.INVALID_ARGUMENT)
                    .statusMessage("security header '" + SECURITY_HEADER + ": true' is missing")
                    .end();
            return;
        }
        if (FETCH_X509_SVID_METHOD.equals(request.methodName())) {
            request.handler(message -> handleFetchX509Svid(request, message));
        } else {
            request.response().status(GrpcStatus.UNIMPLEMENTED).end();
        }
    }

    private void handleFetchX509Svid(GrpcServerRequest<Buffer, Buffer> request, Buffer message) {
        try {
            X509SVIDRequest.parseFrom(message.getBytes());
        } catch (InvalidProtocolBufferException e) {
            request.response()
                    .status(GrpcStatus.INVALID_ARGUMENT)
                    .statusMessage("invalid protobuf request: " + e.getMessage())
                    .end();
            return;
        }

        try {
            var svid = certMaterial.defaultSvid();
            X509SVIDResponse response = X509SVIDResponse.newBuilder()
                    .addSvids(X509SVID.newBuilder()
                            .setSpiffeId(svid.spiffeId())
                            .setX509Svid(ByteString.copyFrom(concatDer(svid.chain())))
                            .setX509SvidKey(ByteString.copyFrom(svid.privateKey().getEncoded()))
                            .setBundle(ByteString.copyFrom(concatDer(svid.trustBundle())))
                            .build())
                    .build();
            var grpcResponse = request.response();
            grpcResponse.write(Buffer.buffer(response.toByteArray()));
            vertx.setTimer(2000, id -> {
                if (!grpcResponse.isCancelled()) {
                    grpcResponse.end();
                }
            });
        } catch (Exception e) {
            request.response()
                    .status(GrpcStatus.INTERNAL)
                    .statusMessage("failed to generate certificate material: " + e.getMessage())
                    .end();
        }
    }

    private static byte[] concatDer(List<X509Certificate> certs) throws CertificateEncodingException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (X509Certificate cert : certs) {
            try {
                out.write(cert.getEncoded());
            } catch (IOException e) {
                throw new CertificateEncodingException("failed to write certificate DER", e);
            }
        }
        return out.toByteArray();
    }

    private record WorkloadSvid(String spiffeId, List<X509Certificate> chain, PrivateKey privateKey,
            List<X509Certificate> trustBundle) {
    }

    private static final class X509CertMaterial {

        private final Path tempDir;
        private final X509Certificate rootCert;
        private final PrivateKey rootKey;

        X509CertMaterial() {
            try {
                tempDir = Files.createTempDirectory("spiffe-x509-mtls-test");
                new CertificateChainGenerator(tempDir.toFile())
                        .withCN("spiffe-dev-ca")
                        .withKeyAlgorithm(KeyAlgorithm.EC_P256)
                        .withCaSAN(List.of("URI:" + TEST_TRUST_DOMAIN))
                        .withSAN(List.of("URI:" + DEFAULT_SPIFFE_ID))
                        .generate();
                rootCert = loadCertificate(new File(tempDir.toFile(), "root.crt"));
                rootKey = loadPrivateKey(new File(tempDir.toFile(), "root.key"));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate certificate material", e);
            }
        }

        WorkloadSvid defaultSvid() {
            try {
                var leafKeyPair = KeyAlgorithm.EC_P256.generateKeyPair();
                X509Certificate leafCert = CertificateUtils.generateSignedCertificate(
                        leafKeyPair, "SPIFFE Workload",
                        List.of("URI:" + DEFAULT_SPIFFE_ID),
                        Duration.ofDays(1),
                        new CertificateRequest.Issuer(rootCert, rootKey),
                        KeyAlgorithm.EC_P256);
                return new WorkloadSvid(DEFAULT_SPIFFE_ID, List.of(leafCert), leafKeyPair.getPrivate(), List.of(rootCert));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate leaf SVID", e);
            }
        }

        void cleanup() {
            try {
                if (Files.exists(tempDir)) {
                    try (var walk = Files.walk(tempDir)) {
                        for (Path path : walk.sorted(Comparator.reverseOrder()).toList()) {
                            Files.deleteIfExists(path);
                        }
                    }
                }
            } catch (IOException e) {
                // best-effort cleanup
            }
        }

        private static X509Certificate loadCertificate(File file) throws Exception {
            try (var in = new FileInputStream(file)) {
                return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            }
        }

        private static PrivateKey loadPrivateKey(File file) throws Exception {
            String pem = Files.readString(file.toPath());
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            return KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(der));
        }
    }
}
