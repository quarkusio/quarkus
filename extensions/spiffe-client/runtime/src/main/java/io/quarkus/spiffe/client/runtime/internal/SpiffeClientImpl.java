package io.quarkus.spiffe.client.runtime.internal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.annotation.PreDestroy;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.api.JwtSvid;
import io.quarkus.spiffe.client.api.JwtSvidRequest;
import io.quarkus.spiffe.client.api.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.api.SpiffeClient;
import io.quarkus.spiffe.client.api.SpiffeConnectionException;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDResponse;
import io.smallrye.common.os.OS;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;

final class SpiffeClientImpl implements SpiffeClient {

    private static final ServiceName SERVICE_NAME = ServiceName.create("SpiffeWorkloadAPI");
    private static final String METHOD_NAME = "FetchJWTSVID";
    private static final String SECURITY_HEADER_KEY = "workload.spiffe.io";
    private static final String SECURITY_HEADER_VALUE = "true";
    private static final long REQUEST_TIMEOUT_MS = 10_000;

    private final GrpcClient client;
    private final SocketAddress server;
    private final JwtSvidRequest defaultRequest;

    SpiffeClientImpl(SpiffeClientConfig config, Vertx vertx) {
        URI endpointSocket = config.endpointSocket().orElseThrow(() -> new ConfigurationException(
                "The 'quarkus.spiffe-client.endpoint-socket' configuration property is not set"
                        + " and the 'SPIFFE_ENDPOINT_SOCKET' environment variable is not defined"));
        this.server = toSocketAddress(endpointSocket);
        this.client = GrpcClient.client(vertx, new HttpClientOptions()
                .setHttp2ClearTextUpgrade(false)
                .setReadIdleTimeout(30)
                .setConnectTimeout(5000)
                .setTracingPolicy(TracingPolicy.IGNORE));
        this.defaultRequest = config.audiences()
                .filter(Predicate.not(List::isEmpty))
                .map(a -> JwtSvidRequest.builder().audiences(a).build())
                .orElse(null);
    }

    @Override
    public Uni<JwtSvid> fetchJwtSvid() {
        if (defaultRequest == null) {
            throw new IllegalStateException(
                    "No default audiences configured via 'quarkus.spiffe-client.audiences'; "
                            + "either configure default audiences or use fetchJwtSvid(JwtSvidRequest) with explicit audiences");
        }
        return fetchJwtSvid(defaultRequest);
    }

    @Override
    public Uni<JwtSvid> fetchJwtSvid(JwtSvidRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("JWT-SVID request must not be null");
        }
        if (request.audiences().isEmpty()) {
            if (defaultRequest != null) {
                return fetchJwtSvids(defaultRequest.audiences(), request.spiffeId()).toUni();
            } else {
                throw new IllegalArgumentException(
                        "No audience specified in the request and no default audiences configured via "
                                + "'quarkus.spiffe-client.audiences'; at least one audience is required to prevent "
                                + "identity tokens from being sent to unintended recipients");
            }
        }
        return fetchJwtSvids(request.audiences(), request.spiffeId()).toUni();
    }

    private Multi<JwtSvid> fetchJwtSvids(List<String> audiences, String spiffeId) {
        JWTSVIDRequest.Builder proto = JWTSVIDRequest.newBuilder();
        proto.addAllAudience(audiences);
        if (spiffeId != null) {
            proto.setSpiffeId(spiffeId);
        }
        Buffer payload = Buffer.buffer(proto.build().toByteArray());

        return Multi.createFrom().emitter(emitter -> client.request(server)
                .timeout(REQUEST_TIMEOUT_MS, MILLISECONDS)
                .onFailure(t -> emitter.fail(new SpiffeConnectionException("Failed to connect to SPIRE agent", t)))
                .onSuccess(grpcRequest -> {
                    grpcRequest.serviceName(SERVICE_NAME);
                    grpcRequest.methodName(METHOD_NAME);
                    grpcRequest.headers().set(SECURITY_HEADER_KEY, SECURITY_HEADER_VALUE);

                    grpcRequest.send(payload)
                            .onFailure(t -> emitter.fail(
                                    new SpiffeConnectionException("Failed to send request to SPIRE agent", t)))
                            .onSuccess(grpcResponse -> {
                                Buffer body = Buffer.buffer();
                                grpcResponse
                                        .exceptionHandler(t -> emitter.fail(
                                                new SpiffeConnectionException("SPIRE agent connection error", t)))
                                        .errorHandler(error -> emitter
                                                .fail(mapGrpcError(error.status, grpcResponse.statusMessage())))
                                        .handler(body::appendBuffer)
                                        .endHandler(v -> {
                                            GrpcStatus status = grpcResponse.status();
                                            if (status != null && status != GrpcStatus.OK) {
                                                emitter.fail(mapGrpcError(status, grpcResponse.statusMessage()));
                                                return;
                                            }
                                            try {
                                                JWTSVIDResponse response = JWTSVIDResponse.parseFrom(body.getBytes());
                                                for (var svid : response.getSvidsList()) {
                                                    emitter.emit(toJwtSvid(svid));
                                                }
                                                emitter.complete();
                                            } catch (SpiffeConnectionException e) {
                                                emitter.fail(e);
                                            } catch (Exception e) {
                                                emitter.fail(new SpiffeConnectionException(
                                                        "Failed to parse response from SPIRE agent", e));
                                            }
                                        });
                            });
                }));
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private static JwtSvid toJwtSvid(io.quarkus.spiffe.client.runtime.internal.proto.JWTSVID svid)
            throws SpiffeConnectionException {
        String token = svid.getSvid();
        if (token.isBlank()) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent has no token");
        }
        String spiffeId = svid.getSpiffeId();
        if (spiffeId.isBlank()) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent has no SPIFFE ID");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is not a valid JWS Compact Serialization");
        }
        JsonObject payload = new JsonObject(new String(Base64.getUrlDecoder().decode(parts[1])));

        Set<String> audience = new HashSet<>();
        JsonArray aud = payload.getJsonArray("aud");
        if (aud != null) {
            for (int i = 0; i < aud.size(); i++) {
                audience.add(aud.getString(i));
            }
        }
        if (audience.isEmpty()) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is missing the required 'aud' claim");
        }

        Long exp = payload.getLong("exp");
        if (exp == null) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is missing the required 'exp' claim");
        }
        Instant expiry = Instant.ofEpochSecond(exp);
        if (expiry.isBefore(Instant.now())) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is already expired");
        }

        record JwtSvidImpl(String token, String spiffeId, Set<String> audience,
                Instant expiry) implements JwtSvid {
        }
        return new JwtSvidImpl(token, spiffeId, Set.copyOf(audience), expiry);
    }

    private static Exception mapGrpcError(GrpcStatus status, String message) {
        String detail = message != null ? status.name() + ": " + message : status.name();
        if (status == GrpcStatus.PERMISSION_DENIED) {
            return new SpiffeAuthorizationException(detail);
        }
        return new SpiffeConnectionException(detail);
    }

    private static SocketAddress toSocketAddress(URI uri) {
        if ("unix".equals(uri.getScheme())) {
            if (OS.WINDOWS.isCurrent()) {
                throw new ConfigurationException(
                        "The SPIFFE client extension does not support unix scheme on Windows, use tcp:// instead.");
            }
            return new DomainSocketAddressWithAuthority(SocketAddress.domainSocketAddress(uri.getPath()));
        }
        return SocketAddress.inetSocketAddress(uri.getPort(), uri.getHost());
    }

    /**
     * Works around <a href="https://github.com/eclipse-vertx/vert.x/issues/6220">Vert.x #6220 issue</a>.
     * Setting invalid host is enough to avoid validation failure while still using the domain socket.
     */
    private record DomainSocketAddressWithAuthority(SocketAddress delegate) implements SocketAddress {

        @Override
        public String host() {
            // https://www.rfc-editor.org/rfc/rfc2606.html#section-2 says '.invalid' is reserved for invalid domain names
            return "uds.invalid";
        }

        @Override
        public int port() {
            return 0;
        }

        @Override
        public String path() {
            return delegate.path();
        }

        @Override
        public String hostName() {
            return delegate.hostName();
        }

        @Override
        public String hostAddress() {
            return delegate.hostAddress();
        }

        @Override
        public boolean isInetSocket() {
            return delegate.isInetSocket();
        }

        @Override
        public boolean isDomainSocket() {
            return delegate.isDomainSocket();
        }

        @Override
        public String toString() {
            return delegate.toString();
        }
    }
}
