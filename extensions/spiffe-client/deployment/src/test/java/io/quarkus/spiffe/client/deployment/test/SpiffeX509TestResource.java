package io.quarkus.spiffe.client.deployment.test;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

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
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

public final class SpiffeX509TestResource implements QuarkusTestResourceLifecycleManager {

    static final String TEST_TRUST_DOMAIN = "spiffe://test.quarkus.io";
    static final String DEFAULT_SPIFFE_ID = TEST_TRUST_DOMAIN + "/test-workload";
    static final String SECONDARY_SPIFFE_ID = TEST_TRUST_DOMAIN + "/secondary-workload";
    static final String MODE_PATH = "/api/mode";

    private static final ServiceName SERVICE_NAME = ServiceName.create("SpiffeWorkloadAPI");
    private static final String FETCH_X509_SVID_METHOD = "FetchX509SVID";
    private static final String SECURITY_HEADER = "workload.spiffe.io";

    private volatile X509CertMaterial certMaterial;
    private volatile Vertx vertx;
    private volatile HttpServer grpcServer;
    private volatile HttpServer httpServer;
    private volatile SocketAddress grpcAddress;
    private volatile X509Mode mode = X509Mode.HEALTHY;

    private enum X509Mode {
        HEALTHY,
        EC_P384,
        RSA_2048,
        CHAIN_DEPTH_2,
        MULTI_SVID,
        EMPTY_SVIDS,
        CORRUPTED_CERT,
        PERMISSION_DENIED,
        UNAVAILABLE,
        GRPC_DOWN
    }

    private record CertAuthority(X509Certificate certificate, PrivateKey privateKey) {
    }

    private record WorkloadSvid(String spiffeId, List<X509Certificate> chain, PrivateKey privateKey,
            List<X509Certificate> trustBundle) {
    }

    @Override
    public Map<String, String> start() {
        certMaterial = new X509CertMaterial();
        vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1).setEventLoopPoolSize(1));

        grpcAddress = SocketAddress.inetSocketAddress(0, "127.0.0.1");
        startGrpcServer();
        grpcAddress = SocketAddress.inetSocketAddress(grpcServer.actualPort(), "127.0.0.1");
        if (grpcAddress.port() <= 0) {
            throw new IllegalStateException("Expected to start gRPC server on a random port, but got " + grpcAddress.port());
        }

        httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));
        try {
            httpServer.requestHandler(this::handleHttpRequest)
                    .listen(SocketAddress.inetSocketAddress(0, "127.0.0.1"))
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start X509 test HTTP server", e);
        }

        if (httpServer.actualPort() <= 0) {
            throw new IllegalStateException(
                    "Expected to start HTTP server on a random port, but got " + httpServer.actualPort());
        }

        return Map.of(
                "x509.quarkus.spiffe-client.endpoint-socket", "tcp://127.0.0.1:" + grpcServer.actualPort(),
                "spiffe.x509.test.admin-url", "http://127.0.0.1:" + httpServer.actualPort());
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
        if (httpServer != null) {
            try {
                httpServer.close().await(10, TimeUnit.SECONDS);
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

    private void startGrpcServer() {
        grpcServer = vertx.createHttpServer(new HttpServerOptions());
        try {
            grpcServer.requestHandler(createGrpcServer())
                    .listen(grpcAddress)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .get(20, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to start X509 test gRPC server", e);
        }
    }

    private GrpcServer createGrpcServer() {
        GrpcServer server = GrpcServer.server(vertx);
        server.callHandler(request -> {
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
            if (mode == X509Mode.UNAVAILABLE) {
                request.response()
                        .status(GrpcStatus.UNAVAILABLE)
                        .statusMessage("agent initializing")
                        .end();
                return;
            }
            if (mode == X509Mode.PERMISSION_DENIED) {
                request.response()
                        .status(GrpcStatus.PERMISSION_DENIED)
                        .statusMessage("no identity issued")
                        .end();
                return;
            }
            if (FETCH_X509_SVID_METHOD.equals(request.methodName())) {
                request.handler(message -> handleFetchX509Svid(request, message));
            } else {
                request.response().status(GrpcStatus.UNIMPLEMENTED).end();
            }
        });
        return server;
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

        X509SVIDResponse.Builder responseBuilder = X509SVIDResponse.newBuilder();

        try {
            switch (mode) {
                case EC_P384:
                    responseBuilder.addSvids(buildProtoSvid(certMaterial.p384Svid()));
                    break;
                case RSA_2048:
                    responseBuilder.addSvids(buildProtoSvid(certMaterial.rsaSvid()));
                    break;
                case CHAIN_DEPTH_2:
                    responseBuilder.addSvids(buildProtoSvid(certMaterial.chainDepth2Svid()));
                    break;
                case MULTI_SVID:
                    responseBuilder.addSvids(buildProtoSvid(certMaterial.defaultSvid()));
                    responseBuilder.addSvids(buildProtoSvid(certMaterial.secondarySvid()));
                    break;
                case EMPTY_SVIDS:
                    break;
                case CORRUPTED_CERT:
                    responseBuilder.addSvids(X509SVID.newBuilder()
                            .setSpiffeId(DEFAULT_SPIFFE_ID)
                            .setX509Svid(ByteString.copyFrom(new byte[] { 0x00, 0x01, 0x02 }))
                            .setX509SvidKey(ByteString.copyFrom(new byte[] { 0x00 }))
                            .setBundle(ByteString.copyFrom(new byte[] { 0x00 }))
                            .build());
                    break;
                default:
                    responseBuilder.addSvids(buildProtoSvid(certMaterial.defaultSvid()));
                    break;
            }
        } catch (Exception e) {
            request.response()
                    .status(GrpcStatus.INTERNAL)
                    .statusMessage("failed to generate certificate material: " + e.getMessage())
                    .end();
            return;
        }

        var grpcResponse = request.response();
        grpcResponse.write(Buffer.buffer(responseBuilder.build().toByteArray()));
        vertx.setTimer(2000, id -> {
            if (!grpcResponse.isCancelled()) {
                grpcResponse.end();
            }
        });
    }

    private static X509SVID buildProtoSvid(WorkloadSvid svid) throws CertificateEncodingException {
        return X509SVID.newBuilder()
                .setSpiffeId(svid.spiffeId())
                .setX509Svid(ByteString.copyFrom(concatDer(svid.chain())))
                .setX509SvidKey(ByteString.copyFrom(svid.privateKey().getEncoded()))
                .setBundle(ByteString.copyFrom(concatDer(svid.trustBundle())))
                .build();
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

    private void handleHttpRequest(HttpServerRequest request) {
        if (MODE_PATH.equals(request.path())) {
            handleModeRequest(request);
        } else {
            request.response().setStatusCode(404).end();
        }
    }

    private void handleModeRequest(HttpServerRequest request) {
        if (GET.equals(request.method())) {
            request.response()
                    .putHeader("content-type", "text/plain")
                    .end(mode.name());
        } else if (POST.equals(request.method())) {
            request.body().onSuccess(body -> {
                X509Mode newMode;
                try {
                    newMode = X509Mode.valueOf(body.toString().trim());
                } catch (IllegalArgumentException e) {
                    request.response().setStatusCode(400).end("Unknown mode: " + body);
                    return;
                }
                X509Mode oldMode = mode;
                if (oldMode != X509Mode.GRPC_DOWN && newMode == X509Mode.GRPC_DOWN) {
                    grpcServer.close().onComplete(ar -> {
                        if (ar.failed()) {
                            request.response().setStatusCode(500)
                                    .end("Failed to close gRPC server: " + ar.cause().getMessage());
                            return;
                        }
                        mode = newMode;
                        request.response().setStatusCode(200).end(mode.name());
                    });
                } else if (oldMode == X509Mode.GRPC_DOWN && newMode != X509Mode.GRPC_DOWN) {
                    grpcServer = vertx.createHttpServer(new HttpServerOptions());
                    grpcServer.requestHandler(createGrpcServer())
                            .listen(grpcAddress)
                            .onComplete(ar -> {
                                if (ar.failed()) {
                                    request.response().setStatusCode(500)
                                            .end("Failed to restart gRPC server: " + ar.cause().getMessage());
                                    return;
                                }
                                mode = newMode;
                                request.response().setStatusCode(200).end(mode.name());
                            });
                } else {
                    mode = newMode;
                    request.response().setStatusCode(200).end(mode.name());
                }
            });
        } else {
            request.response().setStatusCode(405).end();
        }
    }

    // --- Certificate generation using SmallRye Certificate Generator ---

    static final class X509CertMaterial {

        private final Path tempDir;
        private final CertAuthority defaultCa;
        private final CertAuthority p384Ca;
        private final CertAuthority rsaCa;
        private final WorkloadSvid chainDepth2Svid;

        X509CertMaterial() {
            try {
                tempDir = Files.createTempDirectory("spiffe-x509-test");
                defaultCa = generateCaFromChain("ec-p256", KeyAlgorithm.EC_P256);
                p384Ca = generateCaFromChain("ec-p384", KeyAlgorithm.EC_P384);
                rsaCa = generateCaFromChain("rsa-2048", KeyAlgorithm.RSA_2048);
                chainDepth2Svid = loadChainDepth2Svid();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate certificate material", e);
            }
        }

        WorkloadSvid defaultSvid() {
            return createLeafSvid(DEFAULT_SPIFFE_ID, defaultCa, KeyAlgorithm.EC_P256);
        }

        WorkloadSvid p384Svid() {
            return createLeafSvid(DEFAULT_SPIFFE_ID, p384Ca, KeyAlgorithm.EC_P384);
        }

        WorkloadSvid rsaSvid() {
            return createLeafSvid(DEFAULT_SPIFFE_ID, rsaCa, KeyAlgorithm.RSA_2048);
        }

        WorkloadSvid chainDepth2Svid() {
            return chainDepth2Svid;
        }

        WorkloadSvid secondarySvid() {
            return createLeafSvid(SECONDARY_SPIFFE_ID, defaultCa, KeyAlgorithm.EC_P256);
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

        private CertAuthority generateCaFromChain(String dirName, KeyAlgorithm keyAlgorithm) throws Exception {
            File chainDir = tempDir.resolve(dirName).toFile();
            Files.createDirectories(chainDir.toPath());
            new CertificateChainGenerator(chainDir)
                    .withCN("spiffe-dev-ca")
                    .withKeyAlgorithm(keyAlgorithm)
                    .withCaSAN(List.of("URI:" + TEST_TRUST_DOMAIN))
                    .withSAN(List.of("URI:" + DEFAULT_SPIFFE_ID))
                    .generate();
            X509Certificate rootCert = loadCertificate(new File(chainDir, "root.crt"));
            PrivateKey rootKey = loadPrivateKey(new File(chainDir, "root.key"), keyAlgorithm);
            return new CertAuthority(rootCert, rootKey);
        }

        private WorkloadSvid loadChainDepth2Svid() throws Exception {
            File chainDir = tempDir.resolve("ec-p256").toFile();
            List<X509Certificate> chain = loadCertificates(new File(chainDir, "spiffe-dev-ca.crt"));
            PrivateKey leafKey = loadPrivateKey(new File(chainDir, "spiffe-dev-ca.key"), KeyAlgorithm.EC_P256);
            X509Certificate rootCert = loadCertificate(new File(chainDir, "root.crt"));
            return new WorkloadSvid(DEFAULT_SPIFFE_ID, chain, leafKey, List.of(rootCert));
        }

        private static WorkloadSvid createLeafSvid(String spiffeId, CertAuthority ca, KeyAlgorithm keyAlgorithm) {
            try {
                var leafKeyPair = keyAlgorithm.generateKeyPair();
                X509Certificate leafCert = CertificateUtils.generateSignedCertificate(
                        leafKeyPair, "SPIFFE Workload",
                        List.of("URI:" + spiffeId),
                        Duration.ofDays(1),
                        new CertificateRequest.Issuer(ca.certificate(), ca.privateKey()),
                        keyAlgorithm);
                return new WorkloadSvid(spiffeId, List.of(leafCert), leafKeyPair.getPrivate(),
                        List.of(ca.certificate()));
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate leaf SVID", e);
            }
        }

        private static X509Certificate loadCertificate(File file) throws Exception {
            try (var in = new FileInputStream(file)) {
                return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(in);
            }
        }

        private static List<X509Certificate> loadCertificates(File file) throws Exception {
            try (var in = new FileInputStream(file)) {
                return CertificateFactory.getInstance("X.509").generateCertificates(in).stream()
                        .map(X509Certificate.class::cast)
                        .toList();
            }
        }

        private static PrivateKey loadPrivateKey(File file, KeyAlgorithm keyAlgorithm) throws Exception {
            String pem = Files.readString(file.toPath());
            String base64 = pem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            String jcaAlgorithm = keyAlgorithm == KeyAlgorithm.RSA_2048 ? "RSA" : "EC";
            return KeyFactory.getInstance(jcaAlgorithm).generatePrivate(new PKCS8EncodedKeySpec(der));
        }
    }
}
