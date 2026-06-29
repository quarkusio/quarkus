package io.quarkus.spiffe.client.runtime.internal;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.annotation.PreDestroy;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.WorkloadJsonWebToken;
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
    private static final String SPIFFE_URI_PREFIX = "spiffe://";

    private final GrpcClient client;
    private final SocketAddress server;
    private final Set<String> defaultAudiences;

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
        this.defaultAudiences = config.audiences()
                .filter(Predicate.not(Set::isEmpty))
                .map(audiences -> {
                    for (String audience : audiences) {
                        validateAudience(audience);
                    }
                    return audiences;
                })
                .orElse(null);
    }

    @Override
    public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken() {
        if (defaultAudiences == null) {
            throw new IllegalStateException(
                    "No default audiences configured via 'quarkus.spiffe-client.audiences'; "
                            + "either configure default audiences or use getWorkloadJsonWebToken(String) with an explicit audience");
        }
        return fetchWorkloadJsonWebTokens(defaultAudiences).toUni();
    }

    @Override
    public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(String audience) {
        validateAudience(audience);
        return fetchWorkloadJsonWebTokens(Set.of(audience)).toUni();
    }

    @Override
    public Uni<WorkloadJsonWebToken> getWorkloadJsonWebToken(Set<String> audiences) {
        if (audiences == null) {
            throw new IllegalArgumentException("Audiences must not be null");
        }
        if (audiences.isEmpty()) {
            throw new IllegalArgumentException("Audiences must not be empty");
        }
        for (String audience : audiences) {
            validateAudience(audience);
        }
        return fetchWorkloadJsonWebTokens(audiences).toUni();
    }

    @PreDestroy
    void close() {
        client.close();
    }

    private Multi<WorkloadJsonWebToken> fetchWorkloadJsonWebTokens(Set<String> audiences) {
        JWTSVIDRequest.Builder proto = JWTSVIDRequest.newBuilder();
        proto.addAllAudience(audiences);
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
                                                    emitter.emit(toWorkloadJsonWebToken(svid, audiences));
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

    private static WorkloadJsonWebToken toWorkloadJsonWebToken(io.quarkus.spiffe.client.runtime.internal.proto.JWTSVID svid,
            Set<String> requestedAudiences) throws SpiffeConnectionException {
        String token = svid.getSvid();
        if (token.isBlank()) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent has no token");
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is not a valid JWS Compact Serialization");
        }
        JsonObject payload = new JsonObject(new String(Base64.getUrlDecoder().decode(parts[1])));

        String sub = payload.getString("sub");
        if (sub == null) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is missing the required 'sub' claim");
        }
        if (!sub.startsWith(SPIFFE_URI_PREFIX)) {
            throw new SpiffeConnectionException(
                    "JWT-SVID 'sub' claim does not have the required 'spiffe://' prefix: " + sub);
        }
        if (!sub.equals(svid.getSpiffeId())) {
            throw new SpiffeConnectionException(
                    "JWT-SVID proto SPIFFE ID does not match the 'sub' claim; proto: " + svid.getSpiffeId() + ", sub: " + sub);
        }

        JsonArray aud = payload.getJsonArray("aud");
        if (aud == null) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is missing the required 'aud' claim");
        }
        Set<String> audience = new HashSet<>(aud.size());
        for (int i = 0; i < aud.size(); i++) {
            audience.add(aud.getString(i));
        }
        if (!audience.containsAll(requestedAudiences)) {
            throw new SpiffeConnectionException(
                    "JWT-SVID 'aud' claim does not contain the requested audiences; requested: "
                            + requestedAudiences + ", received: " + audience);
        }
        if (audience.size() != requestedAudiences.size()) {
            throw new SpiffeConnectionException(
                    "JWT-SVID 'aud' claim contains unexpected extra audiences; requested: "
                            + requestedAudiences + ", received: " + audience);
        }

        Long exp = payload.getLong("exp");
        if (exp == null) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is missing the required 'exp' claim");
        }
        Instant expiry = Instant.ofEpochSecond(exp);
        if (expiry.isBefore(Instant.now())) {
            throw new SpiffeConnectionException("JWT-SVID from SPIRE agent is already expired");
        }

        record WorkloadJsonWebTokenImpl(String token, String subject, Set<String> audience,
                Instant expiry) implements WorkloadJsonWebToken {
        }
        return new WorkloadJsonWebTokenImpl(token, sub, Set.copyOf(audience), expiry);
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

    private static void validateAudience(String audience) {
        if (audience == null) {
            throw new IllegalArgumentException("Audience must not be null");
        }
        if (audience.isBlank()) {
            throw new IllegalArgumentException("Audience must not be blank");
        }
        if (audience.indexOf(' ') >= 0) {
            throw new IllegalArgumentException("Audience must not contain spaces: '" + audience + "'");
        }
    }

    /**
     * Works around <a href="https://github.com/eclipse-vertx/vert.x/issues/6220">Vert.x #6220 issue</a>.
     * Setting invalid host is enough to avoid validation failure while still using the domain socket.
     * TODO: drop this when Quarkus moves to Vert.x 5.1.4, however we will still need it for Quarkus 3.x
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
