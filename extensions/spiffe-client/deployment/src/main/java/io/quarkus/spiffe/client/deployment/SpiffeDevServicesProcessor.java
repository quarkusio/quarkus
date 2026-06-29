package io.quarkus.spiffe.client.deployment;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
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
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVID;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDResponse;
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

@BuildSteps(onlyIf = { SpiffeClientEnabled.class, SpiffeDevServicesProcessor.SpiffeDevServicesEnabled.class })
public final class SpiffeDevServicesProcessor {

    private static final Logger LOG = Logger.getLogger(SpiffeDevServicesProcessor.class);

    public static final String TEST_TRUST_DOMAIN = "spiffe://test.quarkus.io";
    public static final String DEFAULT_SPIFFE_ID = TEST_TRUST_DOMAIN + "/test-workload";
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
        PERMISSION_DENIED,
        UNAVAILABLE,
        GRPC_DOWN
    }

    private static final class SpiffeWorkloadApiDevServer implements Startable {

        private static final ServiceName SERVICE_NAME = ServiceName.create("SpiffeWorkloadAPI");
        private static final String METHOD_NAME = "FetchJWTSVID";
        private static final String SECURITY_HEADER = "workload.spiffe.io";
        private static final long DEFAULT_TTL_SECONDS = 300;
        private static final String UNIX = "unix://";

        private final Transport transport;
        private final Set<String> errorMessages;
        private volatile JwtSvidSigner signer;
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
            JsonObject bundle = new JsonObject()
                    .put("spiffe_sequence", 1)
                    .put("spiffe_refresh_hint", 300)
                    .put("keys", new JsonArray().add(signer.publicKeyJwk()));
            request.response()
                    .putHeader("content-type", "application/json")
                    .end(bundle.encode());
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
                if (SERVICE_NAME.equals(request.serviceName()) && METHOD_NAME.equals(request.methodName())) {
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
                    request.handler(message -> handleFetchJwtSvid(request, message));
                } else {
                    LOG.warnf("Received a call to unimplemented method %s/%s", request.serviceName(), request.methodName());
                    request.response().status(GrpcStatus.UNIMPLEMENTED).end();
                }
            });
            return grpcServer;
        }

        private void handleFetchJwtSvid(io.vertx.grpc.server.GrpcServerRequest<Buffer, Buffer> request, Buffer message) {
            JWTSVIDRequest jwtRequest;
            try {
                jwtRequest = JWTSVIDRequest.parseFrom(message.getBytes());
            } catch (InvalidProtocolBufferException e) {
                request.response()
                        .status(GrpcStatus.INVALID_ARGUMENT)
                        .statusMessage("invalid protobuf request")
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

    private static final class JwtSvidSigner {

        private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
        private static final String HEADER = URL_ENCODER.encodeToString("{\"alg\":\"ES256\",\"typ\":\"JWT\"}".getBytes());
        private static final int P256_COORDINATE_LENGTH = 32;

        private final KeyPair keyPair;

        private JwtSvidSigner() {
            try {
                KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
                generator.initialize(new ECGenParameterSpec("secp256r1"));
                this.keyPair = generator.generateKeyPair();
            } catch (Exception e) {
                throw new RuntimeException("Failed to generate EC P-256 key pair", e);
            }
        }

        JsonObject publicKeyJwk() {
            ECPublicKey pub = (ECPublicKey) keyPair.getPublic();
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
                sig.initSign(keyPair.getPrivate());
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
