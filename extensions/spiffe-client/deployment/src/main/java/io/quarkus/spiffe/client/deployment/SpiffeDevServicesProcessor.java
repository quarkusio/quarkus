package io.quarkus.spiffe.client.deployment;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;

import io.quarkus.deployment.IsDevServicesSupportedByLaunchMode;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.Startable;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.deployment.SpiffeClientBuildTimeConfig.DevServices.Transport;
import io.quarkus.spiffe.client.deployment.SpiffeClientProcessor.SpiffeClientEnabled;
import io.quarkus.spiffe.client.deployment.SpiffeDevServicesCertificateGenerator.CertAuthority;
import io.quarkus.spiffe.client.deployment.SpiffeDevServicesCertificateGenerator.WorkloadSvid;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVID;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDResponse;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVID;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVIDResponse;
import io.smallrye.common.os.OS;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;
import io.vertx.grpc.server.GrpcServer;
import io.vertx.grpc.server.GrpcServerRequest;

@BuildSteps(onlyIf = { SpiffeClientEnabled.class, SpiffeDevServicesProcessor.SpiffeDevServicesEnabled.class })
public final class SpiffeDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(SpiffeDevServicesProcessor.class);

    public static final String TEST_TRUST_DOMAIN = "spiffe://test.quarkus.io";
    public static final String DEFAULT_SPIFFE_ID = TEST_TRUST_DOMAIN + "/test-workload";
    public static final String SECONDARY_SPIFFE_ID = TEST_TRUST_DOMAIN + "/secondary-workload";
    public static final String BUNDLE_PATH = "/bundle";
    public static final String ADMIN_API_MODE_PATH = "/api/admin/mode";
    public static final String ENDPOINT_SOCKET_CONFIG_KEY = "quarkus.spiffe-client.endpoint-socket";
    // avoid the Quarkus prefix in order to prevent warnings when the application starts, this is internal property
    public static final String BASE_URL_CONFIG_KEY = "dev-svc.quarkus.spiffe-client.devservices.base-url";

    @BuildStep(onlyIf = IsDevServicesSupportedByLaunchMode.class)
    void startDevService(SpiffeClientBuildTimeConfig config,
            BuildProducer<DevServicesResultBuildItem> devServicesResultProducer,
            LaunchModeBuildItem launchModeBuildItem) {
        if (ConfigProvider.getConfig().getOptionalValue(ENDPOINT_SOCKET_CONFIG_KEY, String.class).isPresent()) {
            LOG.debug("SPIFFE endpoint socket already configured, skipping SPIFFE Dev Services");
            return;
        }
        if (launchModeBuildItem.getLaunchMode().isRemoteDev()) {
            LOG.warn("SPIFFE Dev Services is not supported in the remote development mode");
            return;
        }
        Transport transport = config.devservices().transport().orElseGet(() -> {
            if (OS.WINDOWS.isCurrent()) {
                return Transport.TCP;
            }
            return Transport.UNIX;
        });
        if (transport == Transport.UNIX && OS.WINDOWS.isCurrent()) {
            throw new ConfigurationException(
                    "The SPIFFE client extension does not support unix transport on Windows, use tcp instead.");
        }
        devServicesResultProducer.produce(
                DevServicesResultBuildItem.<SpiffeWorkloadApiDevServer> owned()
                        .feature(SpiffeClientProcessor.FEATURE)
                        .serviceConfig(transport)
                        .startable(() -> new SpiffeWorkloadApiDevServer(transport))
                        .configProvider(Map.of(
                                ENDPOINT_SOCKET_CONFIG_KEY, Startable::getConnectionInfo,
                                BASE_URL_CONFIG_KEY, SpiffeWorkloadApiDevServer::baseUrl))
                        .postStartHook(server -> {
                            if (!server.errorMessages.isEmpty()) {
                                for (String errorMessage : server.errorMessages) {
                                    LOG.error(errorMessage);
                                }
                            } else if (server.endpointSocket != null) {
                                LOG.infof("SPIFFE Workload API server started on %s", server.endpointSocket);
                            }
                        })
                        .build());
    }

    public enum ServerMode {
        HEALTHY,
        HEALTHY_X509_EC_P384,
        HEALTHY_X509_RSA_2048,
        HEALTHY_X509_CHAIN_DEPTH_2,
        HEALTHY_X509_CHAIN_DEPTH_3,
        HEALTHY_X509_MULTI_SVID,
        HEALTHY_X509_EMPTY_SVIDS,
        HEALTHY_X509_CORRUPTED_CERT,
        PERMISSION_DENIED,
        UNAVAILABLE,
        GRPC_DOWN
    }

    private static final class SpiffeWorkloadApiDevServer implements Startable {

        private static final ServiceName SERVICE_NAME = ServiceName.create("SpiffeWorkloadAPI");
        private static final String FETCH_JWT_SVID_METHOD = "FetchJWTSVID";
        private static final String FETCH_X509_SVID_METHOD = "FetchX509SVID";
        private static final String SECURITY_HEADER = "workload.spiffe.io";
        private static final long DEFAULT_TTL_SECONDS = 300;
        private static final String UNIX = "unix://";

        private final Transport transport;
        private final Set<String> errorMessages;
        private volatile JwtSvidSigner signer;
        private volatile X509CertMaterial x509Material;
        private volatile Vertx vertx;
        private volatile HttpServer grpcServer;
        private volatile HttpServer httpServer;
        private volatile SocketAddress grpcAddress;
        private volatile String endpointSocket;
        private volatile ServerMode mode = ServerMode.HEALTHY;

        private SpiffeWorkloadApiDevServer(Transport transport) {
            this.transport = transport;
            this.errorMessages = new HashSet<>();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + httpServer.actualPort();
        }

        private void handleHttpRequest(HttpServerRequest request) {
            if (BUNDLE_PATH.equals(request.path()) && GET.equals(request.method())) {
                serveBundleEndpoint(request);
            } else if (ADMIN_API_MODE_PATH.equals(request.path())) {
                handleControlMode(request);
            } else {
                request.response().setStatusCode(404).end();
            }
        }

        private void serveBundleEndpoint(HttpServerRequest request) {
            try {
                JsonObject bundle = new JsonObject()
                        .put("spiffe_sequence", 1)
                        .put("spiffe_refresh_hint", 300)
                        .put("keys", new JsonArray().add(signer.publicKeyJwk()));
                request.response()
                        .putHeader("content-type", "application/json")
                        .end(bundle.encode());
            } catch (Exception e) {
                LOG.error("Failed to serve SPIFFE bundle endpoint", e);
                request.response().setStatusCode(500).end(e.getMessage());
            }
        }

        private void handleControlMode(HttpServerRequest request) {
            if (GET.equals(request.method())) {
                request.response()
                        .putHeader("content-type", "text/plain")
                        .end(mode.name());
            } else if (POST.equals(request.method())) {
                request.body().onSuccess(body -> {
                    ServerMode newMode;
                    try {
                        newMode = ServerMode.valueOf(body.toString().trim());
                    } catch (IllegalArgumentException e) {
                        request.response().setStatusCode(400).end("Unknown mode: " + body);
                        return;
                    }
                    ServerMode oldMode = mode;
                    if (oldMode != ServerMode.GRPC_DOWN && newMode == ServerMode.GRPC_DOWN) {
                        grpcServer.close().onComplete(ar -> {
                            if (ar.failed()) {
                                request.response().setStatusCode(500)
                                        .end("Failed to close gRPC server: " + ar.cause().getMessage());
                                return;
                            }
                            mode = newMode;
                            LOG.debugf("Changed SPIFFE server mode to %s", mode);
                            request.response().setStatusCode(200).end(mode.name());
                        });
                    } else if (oldMode == ServerMode.GRPC_DOWN && newMode != ServerMode.GRPC_DOWN) {
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
                                    LOG.debugf("Changed SPIFFE server mode to %s", mode);
                                    request.response().setStatusCode(200).end(mode.name());
                                });
                    } else {
                        mode = newMode;
                        LOG.debugf("Changed SPIFFE server mode to %s", mode);
                        request.response().setStatusCode(200).end(mode.name());
                    }
                });
            } else {
                request.response().setStatusCode(405).end();
            }
        }

        private GrpcServer createGrpcServer() {
            GrpcServer grpcServer = GrpcServer.server(vertx);
            grpcServer.callHandler(request -> {
                if (!SERVICE_NAME.equals(request.serviceName())) {
                    LOG.warnf("Received a call to unimplemented service %s", request.serviceName());
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
                if (mode == ServerMode.UNAVAILABLE) {
                    request.response()
                            .status(GrpcStatus.UNAVAILABLE)
                            .statusMessage("agent initializing")
                            .end();
                    return;
                }
                if (mode == ServerMode.PERMISSION_DENIED) {
                    request.response()
                            .status(GrpcStatus.PERMISSION_DENIED)
                            .statusMessage("no identity issued")
                            .end();
                    return;
                }
                if (FETCH_JWT_SVID_METHOD.equals(request.methodName())) {
                    request.handler(message -> handleFetchJwtSvid(request, message));
                } else if (FETCH_X509_SVID_METHOD.equals(request.methodName())) {
                    request.handler(message -> handleFetchX509Svid(request, message));
                } else {
                    LOG.warnf("Received a call to unimplemented method %s/%s", request.serviceName(), request.methodName());
                    request.response().status(GrpcStatus.UNIMPLEMENTED).end();
                }
            });
            return grpcServer;
        }

        private void handleFetchJwtSvid(GrpcServerRequest<Buffer, Buffer> request, Buffer message) {
            JWTSVIDRequest jwtRequest;
            try {
                jwtRequest = JWTSVIDRequest.parseFrom(message.getBytes());
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Failed to parse FetchJWTSVID request", e);
                request.response()
                        .status(GrpcStatus.INVALID_ARGUMENT)
                        .statusMessage("invalid protobuf request: " + e.getMessage())
                        .end();
                return;
            }

            Set<String> audiences = Set.copyOf(jwtRequest.getAudienceList());
            if (audiences.isEmpty()) {
                request.response()
                        .status(GrpcStatus.INVALID_ARGUMENT)
                        .statusMessage("audience is mandatory")
                        .end();
                return;
            }

            try {
                Instant expiry = Instant.now().plusSeconds(DEFAULT_TTL_SECONDS);
                String token = signer.sign(DEFAULT_SPIFFE_ID, audiences, expiry);
                JWTSVID svid = JWTSVID.newBuilder()
                        .setSpiffeId(DEFAULT_SPIFFE_ID)
                        .setSvid(token)
                        .build();
                JWTSVIDResponse response = JWTSVIDResponse.newBuilder()
                        .addSvids(svid)
                        .build();
                request.response().end(Buffer.buffer(response.toByteArray()));
            } catch (Exception e) {
                LOG.error("Failed to sign JWT-SVID in SPIFFE dev service", e);
                request.response()
                        .status(GrpcStatus.INTERNAL)
                        .statusMessage("failed to sign JWT-SVID: " + e.getMessage())
                        .end();
            }
        }

        private void handleFetchX509Svid(GrpcServerRequest<Buffer, Buffer> request, Buffer message) {
            try {
                X509SVIDRequest.parseFrom(message.getBytes());
            } catch (InvalidProtocolBufferException e) {
                LOG.error("Failed to parse FetchX509SVID request", e);
                request.response()
                        .status(GrpcStatus.INVALID_ARGUMENT)
                        .statusMessage("invalid protobuf request: " + e.getMessage())
                        .end();
                return;
            }

            X509CertMaterial material = x509Material;
            X509SVIDResponse.Builder responseBuilder = X509SVIDResponse.newBuilder();

            try {
                switch (mode) {
                    case HEALTHY_X509_EC_P384:
                        responseBuilder.addSvids(buildX509Svid(material.p384Svid()));
                        break;
                    case HEALTHY_X509_RSA_2048:
                        responseBuilder.addSvids(buildX509Svid(material.rsaSvid()));
                        break;
                    case HEALTHY_X509_CHAIN_DEPTH_2:
                        responseBuilder.addSvids(buildX509Svid(material.chainDepth2Svid()));
                        break;
                    case HEALTHY_X509_CHAIN_DEPTH_3:
                        responseBuilder.addSvids(buildX509Svid(material.chainDepth3Svid()));
                        break;
                    case HEALTHY_X509_MULTI_SVID:
                        responseBuilder.addSvids(buildX509Svid(material.defaultSvid()));
                        responseBuilder.addSvids(buildX509Svid(material.secondarySvid()));
                        break;
                    case HEALTHY_X509_EMPTY_SVIDS:
                        break;
                    case HEALTHY_X509_CORRUPTED_CERT:
                        responseBuilder.addSvids(X509SVID.newBuilder()
                                .setSpiffeId(DEFAULT_SPIFFE_ID)
                                .setX509Svid(ByteString.copyFrom(new byte[] { 0x00, 0x01, 0x02 }))
                                .setX509SvidKey(ByteString.copyFrom(new byte[] { 0x00 }))
                                .setBundle(ByteString.copyFrom(new byte[] { 0x00 }))
                                .build());
                        break;
                    default:
                        responseBuilder.addSvids(buildX509Svid(material.defaultSvid()));
                        break;
                }
            } catch (Exception e) {
                LOG.error("Failed to generate certificate material in SPIFFE dev service", e);
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

        private static X509SVID buildX509Svid(WorkloadSvid svid) throws CertificateEncodingException {
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

        @Override
        public void close() {
            if (grpcServer != null) {
                try {
                    grpcServer.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.debug("Failed to close SPIFFE gRPC server", e);
                }
            }
            if (httpServer != null) {
                try {
                    httpServer.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.debug("Failed to close SPIFFE HTTP server", e);
                }
            }
            if (vertx != null) {
                try {
                    vertx.close().toCompletionStage().toCompletableFuture().get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    LOG.debug("Failed to close Vertx instance", e);
                }
            }
            if (endpointSocket != null && endpointSocket.startsWith(UNIX)) {
                String path = endpointSocket.substring(UNIX.length());
                try {
                    Files.deleteIfExists(Path.of(path));
                } catch (IOException e) {
                    LOG.debug("Failed to clean up socket file", e);
                }
            }
        }

        @Override
        public void start() {
            signer = new JwtSvidSigner();
            x509Material = new X509CertMaterial();
            // trying to keep resources minimal:
            vertx = Vertx.vertx(new VertxOptions().setWorkerPoolSize(1).setEventLoopPoolSize(1));

            startGrpcServer();
            startHttpServer();
        }

        private void startGrpcServer() {
            if (transport == Transport.UNIX) {
                Path socketPath;
                try {
                    socketPath = Files.createTempFile(Path.of("/tmp"), "spiffe-", ".sock");
                    Files.delete(socketPath);
                } catch (IOException e) {
                    errorMessages.add("Failed to create temp socket path: " + e.getMessage());
                    throw new RuntimeException("Failed to create temp socket path", e);
                }
                grpcAddress = SocketAddress.domainSocketAddress(socketPath.toAbsolutePath().toString());
            } else {
                grpcAddress = SocketAddress.inetSocketAddress(0, "127.0.0.1");
            }
            listenGrpcServer();
            if (transport != Transport.UNIX) {
                grpcAddress = SocketAddress.inetSocketAddress(grpcServer.actualPort(), "127.0.0.1");
            }
            endpointSocket = transport == Transport.UNIX
                    ? UNIX + grpcAddress.path()
                    : "tcp://127.0.0.1:" + grpcServer.actualPort();
        }

        private void listenGrpcServer() {
            grpcServer = vertx.createHttpServer(new HttpServerOptions());
            try {
                grpcServer.requestHandler(createGrpcServer())
                        .listen(grpcAddress)
                        .toCompletionStage()
                        .toCompletableFuture()
                        .get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Failed to start SPIFFE gRPC server on " + transport, e);
            }
        }

        private void startHttpServer() {
            httpServer = vertx.createHttpServer(new HttpServerOptions().setPort(0));
            try {
                httpServer.requestHandler(this::handleHttpRequest)
                        .listen(SocketAddress.inetSocketAddress(0, "127.0.0.1"))
                        .toCompletionStage()
                        .toCompletableFuture()
                        .get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                errorMessages.add("Failed to start SPIFFE HTTP server: " + e.getMessage());
                throw new RuntimeException("Failed to start SPIFFE HTTP server", e);
            }
        }

        @Override
        public String getConnectionInfo() {
            return endpointSocket;
        }

        @Override
        public String getContainerId() {
            return null;
        }
    }

    private static final class X509CertMaterial {

        private final SpiffeDevServicesCertificateGenerator generator = new SpiffeDevServicesCertificateGenerator();
        private volatile CertAuthority defaultCa;
        private volatile CertAuthority p384Ca;
        private volatile CertAuthority rsaCa;

        private CertAuthority defaultCa() {
            CertAuthority ca = defaultCa;
            if (ca == null) {
                ca = generator.createCertAuthority(TEST_TRUST_DOMAIN, "ec-p256");
                defaultCa = ca;
            }
            return ca;
        }

        private CertAuthority p384Ca() {
            CertAuthority ca = p384Ca;
            if (ca == null) {
                ca = generator.createCertAuthority(TEST_TRUST_DOMAIN, "ec-p384");
                p384Ca = ca;
            }
            return ca;
        }

        private CertAuthority rsaCa() {
            CertAuthority ca = rsaCa;
            if (ca == null) {
                ca = generator.createCertAuthority(TEST_TRUST_DOMAIN, "rsa-2048");
                rsaCa = ca;
            }
            return ca;
        }

        WorkloadSvid defaultSvid() {
            return generator.createWorkloadSvid(DEFAULT_SPIFFE_ID, defaultCa(), "ec-p256");
        }

        WorkloadSvid p384Svid() {
            return generator.createWorkloadSvid(DEFAULT_SPIFFE_ID, p384Ca(), "ec-p384");
        }

        WorkloadSvid rsaSvid() {
            return generator.createWorkloadSvid(DEFAULT_SPIFFE_ID, rsaCa(), "rsa-2048");
        }

        WorkloadSvid chainDepth2Svid() {
            return generator.createWorkloadSvidWithChain(DEFAULT_SPIFFE_ID, defaultCa(), "ec-p256", 1);
        }

        WorkloadSvid chainDepth3Svid() {
            return generator.createWorkloadSvidWithChain(DEFAULT_SPIFFE_ID, defaultCa(), "ec-p256", 2);
        }

        WorkloadSvid secondarySvid() {
            return generator.createWorkloadSvid(SECONDARY_SPIFFE_ID, defaultCa(), "ec-p256");
        }
    }

    private static final class JwtSvidSigner {

        private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
        private static final String HEADER = URL_ENCODER.encodeToString("{\"alg\":\"ES256\",\"typ\":\"JWT\"}".getBytes());
        private static final int P256_COORDINATE_LENGTH = 32;

        private volatile KeyPair keyPair;

        private KeyPair keyPair() {
            KeyPair kp = keyPair;
            if (kp == null) {
                try {
                    KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                    generator.initialize(new ECGenParameterSpec("secp256r1"));
                    kp = generator.generateKeyPair();
                    keyPair = kp;
                } catch (Exception e) {
                    throw new RuntimeException("Failed to generate EC P-256 key pair", e);
                }
            }
            return kp;
        }

        JsonObject publicKeyJwk() {
            ECPublicKey pub = (ECPublicKey) keyPair().getPublic();
            byte[] x = toFixedLength(pub.getW().getAffineX().toByteArray());
            byte[] y = toFixedLength(pub.getW().getAffineY().toByteArray());
            return new JsonObject()
                    .put("kty", "EC")
                    .put("kid", "dev-authority")
                    .put("use", "jwt-svid")
                    .put("crv", "P-256")
                    .put("x", URL_ENCODER.encodeToString(x))
                    .put("y", URL_ENCODER.encodeToString(y));
        }

        private static byte[] toFixedLength(byte[] coordinate) {
            if (coordinate.length == P256_COORDINATE_LENGTH) {
                return coordinate;
            }
            byte[] result = new byte[P256_COORDINATE_LENGTH];
            if (coordinate.length > P256_COORDINATE_LENGTH) {
                System.arraycopy(coordinate, coordinate.length - P256_COORDINATE_LENGTH, result, 0, P256_COORDINATE_LENGTH);
            } else {
                System.arraycopy(coordinate, 0, result, P256_COORDINATE_LENGTH - coordinate.length, coordinate.length);
            }
            return result;
        }

        String sign(String spiffeId, Set<String> audiences, Instant expiry) {
            long now = Instant.now().getEpochSecond();
            long exp = expiry.getEpochSecond();

            String payload = URL_ENCODER.encodeToString(
                    new JsonObject()
                            .put("sub", spiffeId)
                            .put("aud", new JsonArray(List.copyOf(audiences)))
                            .put("exp", exp)
                            .put("iat", now)
                            .encode()
                            .getBytes());

            String signingInput = HEADER + "." + payload;

            try {
                Signature sig = Signature.getInstance("SHA256withECDSA");
                sig.initSign(keyPair().getPrivate());
                sig.update(signingInput.getBytes());
                byte[] derSignature = sig.sign();
                byte[] jwsSignature = derToJws(derSignature);
                return signingInput + "." + URL_ENCODER.encodeToString(jwsSignature);
            } catch (Exception e) {
                throw new RuntimeException("Failed to sign JWT", e);
            }
        }

        private static byte[] derToJws(byte[] der) {
            int offset = 2;
            int rLength = der[offset + 1] & 0xFF;
            offset += 2;
            byte[] r = extractCoordinate(der, offset, rLength);
            offset += rLength;
            int sLength = der[offset + 1] & 0xFF;
            offset += 2;
            byte[] s = extractCoordinate(der, offset, sLength);

            byte[] result = new byte[P256_COORDINATE_LENGTH * 2];
            System.arraycopy(r, 0, result, 0, P256_COORDINATE_LENGTH);
            System.arraycopy(s, 0, result, P256_COORDINATE_LENGTH, P256_COORDINATE_LENGTH);
            return result;
        }

        private static byte[] extractCoordinate(byte[] der, int offset, int length) {
            byte[] coord = new byte[P256_COORDINATE_LENGTH];
            if (length > P256_COORDINATE_LENGTH) {
                System.arraycopy(der, offset + (length - P256_COORDINATE_LENGTH), coord, 0, P256_COORDINATE_LENGTH);
            } else {
                System.arraycopy(der, offset, coord, P256_COORDINATE_LENGTH - length, length);
            }
            return coord;
        }
    }

    static final class SpiffeDevServicesEnabled implements BooleanSupplier {

        private final boolean enabled;

        SpiffeDevServicesEnabled(SpiffeClientBuildTimeConfig config) {
            this.enabled = config.devservices().enabled();
        }

        @Override
        public boolean getAsBoolean() {
            return enabled;
        }
    }
}
