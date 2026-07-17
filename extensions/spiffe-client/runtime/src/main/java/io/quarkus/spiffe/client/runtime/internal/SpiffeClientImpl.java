package io.quarkus.spiffe.client.runtime.internal;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.annotation.PreDestroy;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.spiffe.client.KeyMaterial;
import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.TrustMaterial;
import io.quarkus.spiffe.client.WorkloadCertificateDocument;
import io.quarkus.spiffe.client.WorkloadJsonWebToken;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVID;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.JWTSVIDResponse;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVID;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVIDRequest;
import io.quarkus.spiffe.client.runtime.internal.proto.X509SVIDResponse;
import io.smallrye.common.os.OS;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.SocketAddress;
import io.vertx.core.net.TrustOptions;
import io.vertx.core.tracing.TracingPolicy;
import io.vertx.grpc.client.GrpcClient;
import io.vertx.grpc.common.GrpcStatus;
import io.vertx.grpc.common.ServiceName;

final class SpiffeClientImpl implements SpiffeClient {

    private static final Base64.Encoder PEM_ENCODER = Base64.getMimeEncoder(64, new byte[] { '\n' });
    private static final ServiceName SERVICE_NAME = ServiceName.create("SpiffeWorkloadAPI");
    private static final String FETCH_JWT_SVID_METHOD = "FetchJWTSVID";
    private static final String FETCH_X509_SVID_METHOD = "FetchX509SVID";
    private static final String SECURITY_HEADER_KEY = "workload.spiffe.io";
    private static final String SECURITY_HEADER_VALUE = "true";
    private static final long REQUEST_TIMEOUT_MS = 10_000;

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
    public Uni<WorkloadCertificateDocument> getWorkloadCertificate() {
        Buffer payload = Buffer.buffer(X509SVIDRequest.newBuilder().build().toByteArray());

        return Uni.createFrom().emitter(emitter -> client.request(server)
                .timeout(REQUEST_TIMEOUT_MS, MILLISECONDS)
                .onFailure(t -> emitter.fail(new SpiffeConnectionException("Failed to connect to SPIRE agent", t)))
                .onSuccess(grpcRequest -> {
                    grpcRequest.serviceName(SERVICE_NAME);
                    grpcRequest.methodName(FETCH_X509_SVID_METHOD);
                    grpcRequest.headers().set(SECURITY_HEADER_KEY, SECURITY_HEADER_VALUE);

                    grpcRequest.send(payload)
                            .onFailure(t -> emitter.fail(
                                    new SpiffeConnectionException("Failed to send request to SPIRE agent", t)))
                            .onSuccess(grpcResponse -> grpcResponse
                                    .exceptionHandler(t -> emitter.fail(
                                            new SpiffeConnectionException("SPIRE agent connection error", t)))
                                    .errorHandler(error -> emitter
                                            .fail(mapGrpcError(error.status, grpcResponse.statusMessage())))
                                    .handler(message -> {
                                        try {
                                            X509SVIDResponse response = X509SVIDResponse.parseFrom(message.getBytes());
                                            emitter.complete(toWorkloadCertificate(response));
                                        } catch (SpiffeConnectionException e) {
                                            emitter.fail(e);
                                        } catch (Exception e) {
                                            emitter.fail(new SpiffeConnectionException(
                                                    "Failed to parse X.509-SVID response from SPIRE agent", e));
                                        } finally {
                                            grpcRequest.cancel();
                                        }
                                    })
                                    .endHandler(v -> {
                                        GrpcStatus status = grpcResponse.status();
                                        if (status != null && status != GrpcStatus.OK) {
                                            emitter.fail(mapGrpcError(status, grpcResponse.statusMessage()));
                                        } else {
                                            // this should be NO-OP if the message arrived
                                            emitter.fail(new SpiffeConnectionException(
                                                    "X.509-SVID stream ended without sending any message"));
                                        }
                                    }));
                }));
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
                    grpcRequest.methodName(FETCH_JWT_SVID_METHOD);
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

    private static WorkloadJsonWebToken toWorkloadJsonWebToken(JWTSVID svid,
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
        SpiffeValidator.validateSpiffeId(sub);
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

    private static WorkloadCertificateDocument toWorkloadCertificate(X509SVIDResponse response)
            throws SpiffeConnectionException {
        List<X509SVID> svids = response.getSvidsList();
        if (svids.isEmpty()) {
            throw new SpiffeConnectionException("X.509-SVID response from SPIRE agent contains no SVIDs");
        }
        X509SVID svid = svids.get(0);

        String protoSpiffeId = svid.getSpiffeId();
        SpiffeValidator.validateSpiffeId(protoSpiffeId);
        if (svid.getX509Svid().isEmpty()) {
            throw new SpiffeConnectionException("X.509-SVID response from SPIRE agent has empty certificate chain");
        }
        if (svid.getX509SvidKey().isEmpty()) {
            throw new SpiffeConnectionException("X.509-SVID response from SPIRE agent has empty private key");
        }
        if (svid.getBundle().isEmpty()) {
            throw new SpiffeConnectionException("X.509-SVID response from SPIRE agent has empty trust bundle");
        }

        List<X509Certificate> certChain = parseCertificates(svid.getX509Svid().toByteArray(), "certificate chain");
        if (certChain.isEmpty()) {
            throw new SpiffeConnectionException("X.509-SVID certificate chain is empty");
        }

        X509Certificate leaf = certChain.get(0);
        String sanSpiffeId = SpiffeValidator.validateLeaf(leaf);
        if (!protoSpiffeId.equals(sanSpiffeId)) {
            throw new SpiffeConnectionException(
                    "X.509-SVID proto SPIFFE ID does not match the leaf certificate URI SAN; proto: "
                            + protoSpiffeId + ", SAN: " + sanSpiffeId);
        }
        for (int i = 1; i < certChain.size(); i++) {
            SpiffeValidator.validateIntermediate(certChain.get(i));
        }

        String keyAlgorithm = leaf.getPublicKey().getAlgorithm();
        PrivateKey privateKey;
        try {
            privateKey = KeyFactory.getInstance(keyAlgorithm)
                    .generatePrivate(new PKCS8EncodedKeySpec(svid.getX509SvidKey().toByteArray()));
        } catch (Exception e) {
            throw new SpiffeConnectionException("X.509-SVID response from SPIRE agent contains an invalid private key", e);
        }

        List<X509Certificate> trustBundle = parseCertificates(svid.getBundle().toByteArray(), "trust bundle");

        var keyMaterial = new KeyMaterialImpl(unmodifiableList(certChain), privateKey);
        var trustMaterial = new TrustMaterialImpl(unmodifiableList(trustBundle));
        return new WorkloadCertificateDocumentImpl(protoSpiffeId, keyMaterial, trustMaterial);
    }

    private static List<X509Certificate> parseCertificates(byte[] derBytes, String description)
            throws SpiffeConnectionException {
        if (derBytes.length == 0) {
            throw new SpiffeConnectionException("X.509-SVID response contains empty " + description);
        }
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Collection<?> certs = cf.generateCertificates(new ByteArrayInputStream(derBytes));
            List<X509Certificate> result = new ArrayList<>(certs.size());
            for (var cert : certs) {
                if (cert instanceof X509Certificate x509) {
                    result.add(x509);
                } else {
                    throw new SpiffeConnectionException(
                            "X.509-SVID response from SPIRE agent contains a non-X.509 certificate in "
                                    + description + ": " + cert.getClass().getName());
                }
            }
            return result;
        } catch (Exception e) {
            throw new SpiffeConnectionException(
                    "X.509-SVID response from SPIRE agent contains an invalid " + description, e);
        }
    }

    private static List<String> certsToPem(List<X509Certificate> certs) {
        try {
            List<String> result = new ArrayList<>(certs.size());
            for (X509Certificate cert : certs) {
                result.add(toPem("CERTIFICATE", cert.getEncoded()));
            }
            return unmodifiableList(result);
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Failed to encode certificate to PEM", e);
        }
    }

    private static String toPem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
                + PEM_ENCODER.encodeToString(der)
                + "\n-----END " + type + "-----\n";
    }

    private record WorkloadCertificateDocumentImpl(String subject, KeyMaterialImpl keyMaterial,
            TrustMaterialImpl trustMaterial) implements WorkloadCertificateDocument {
    }

    private record KeyMaterialImpl(List<X509Certificate> certificateChain,
            PrivateKey privateKey) implements KeyMaterial {

        @Override
        public List<String> certificateChainPem() {
            return certsToPem(certificateChain);
        }

        @Override
        public String privateKeyPem() {
            return toPem("PRIVATE KEY", privateKey.getEncoded());
        }

        @Override
        public KeyCertOptions asVertxKeyCertOptions() {
            var options = new PemKeyCertOptions();
            for (String pem : certificateChainPem()) {
                options.addCertValue(Buffer.buffer(pem));
            }
            options.addKeyValue(Buffer.buffer(privateKeyPem()));
            return options;
        }
    }

    private record TrustMaterialImpl(List<X509Certificate> trustBundle) implements TrustMaterial {

        @Override
        public List<String> trustBundlePem() {
            return certsToPem(trustBundle);
        }

        @Override
        public TrustOptions asVertxTrustOptions() {
            var pemTrust = new PemTrustOptions();
            for (String pem : trustBundlePem()) {
                pemTrust.addCertValue(Buffer.buffer(pem));
            }
            return new SpiffeTrustOptions(pemTrust);
        }
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
            return SocketAddress.domainSocketAddress(uri.getPath());
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
}
