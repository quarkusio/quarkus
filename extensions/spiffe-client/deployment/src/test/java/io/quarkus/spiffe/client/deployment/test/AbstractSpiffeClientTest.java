package io.quarkus.spiffe.client.deployment.test;

import static io.quarkus.spiffe.client.deployment.SpiffeDevServicesProcessor.*;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.mutiny.core.Vertx.newInstance;
import static java.time.Instant.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;
import java.net.URI;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.spiffe.client.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.SpiffeClient;
import io.quarkus.spiffe.client.SpiffeConnectionException;
import io.quarkus.spiffe.client.WorkloadCertificateDocument;
import io.quarkus.spiffe.client.WorkloadJsonWebToken;
import io.quarkus.spiffe.client.deployment.SpiffeDevServicesProcessor.ServerMode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.http.HttpClient;

abstract class AbstractSpiffeClientTest {

    private static final String RESOURCE_SERVER_AUD = TEST_TRUST_DOMAIN + "/resource-server";
    private static final String API_GATEWAY_AUD = TEST_TRUST_DOMAIN + "/api-gateway";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private static HttpClient devServerHttpClient;

    @Inject
    Vertx vertx;

    @Inject
    SpiffeClient spiffeClient;

    @ConfigProperty(name = BASE_URL_CONFIG_KEY)
    String devServerBaseUrl;

    @AfterEach
    void resetServerMode() {
        setServerMode(ServerMode.HEALTHY);
    }

    @AfterAll
    static void closeDevServerHttpClient() {
        if (devServerHttpClient != null) {
            devServerHttpClient.close().await().atMost(TIMEOUT);
            devServerHttpClient = null;
        }
    }

    // also verifies the security header is added by our client as our DEV server rejects other requests
    @Test
    void getWorkloadJsonWebTokenSingleAudience() {
        WorkloadJsonWebToken svid = fetchSvid(RESOURCE_SERVER_AUD);

        assertThat(svid.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
        assertThat(svid.audience()).contains(RESOURCE_SERVER_AUD);
    }

    @Test
    void getWorkloadJsonWebTokenMultipleAudiences() {
        WorkloadJsonWebToken svid = spiffeClient.getWorkloadJsonWebToken(Set.of(RESOURCE_SERVER_AUD, API_GATEWAY_AUD))
                .await().atMost(TIMEOUT);

        assertThat(svid.audience()).containsExactlyInAnyOrder(RESOURCE_SERVER_AUD, API_GATEWAY_AUD);
    }

    @Test
    void getWorkloadJsonWebTokenTokenIsValidJws() {
        WorkloadJsonWebToken svid = fetchSvid(RESOURCE_SERVER_AUD);

        assertThat(svid.token()).isNotBlank();
        assertThat(svid.token().split("\\.")).hasSize(3);
    }

    @Test
    void getWorkloadJsonWebTokenExpiryParsed() {
        WorkloadJsonWebToken svid = fetchSvid(RESOURCE_SERVER_AUD);

        assertThat(svid.expiry()).isNotNull();
        assertThat(svid.expiry()).isAfter(now());
    }

    @Test
    void getWorkloadJsonWebTokenPermissionDenied() {
        setServerMode(ServerMode.PERMISSION_DENIED);
        assertThatThrownBy(() -> fetchSvid(RESOURCE_SERVER_AUD))
                .hasCauseInstanceOf(SpiffeAuthorizationException.class);
    }

    @Test
    void getWorkloadJsonWebTokenUnavailable() {
        setServerMode(ServerMode.UNAVAILABLE);
        assertThatThrownBy(() -> fetchSvid(RESOURCE_SERVER_AUD))
                .hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    @Test
    void getWorkloadJsonWebTokenRecoveryAfterGrpcDown() {
        WorkloadJsonWebToken svid = fetchSvid(RESOURCE_SERVER_AUD);
        assertThat(svid.subject()).isEqualTo(DEFAULT_SPIFFE_ID);

        setServerMode(ServerMode.GRPC_DOWN);
        assertThatThrownBy(() -> fetchSvid(RESOURCE_SERVER_AUD))
                .hasCauseInstanceOf(SpiffeConnectionException.class);

        setServerMode(ServerMode.HEALTHY);
        svid = fetchSvid(RESOURCE_SERVER_AUD);
        assertThat(svid.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    @Test
    void spiffeIdFromProto() {
        assertThat(fetchSvid(RESOURCE_SERVER_AUD).subject()).startsWith("spiffe://");
    }

    @Test
    void jwtPayloadClaimsMatchApiFields() {
        WorkloadJsonWebToken svid = fetchSvid(RESOURCE_SERVER_AUD);

        String[] parts = svid.token().split("\\.");
        JsonObject payload = new JsonObject(new String(Base64.getUrlDecoder().decode(parts[1])));
        assertThat(payload.getString("sub")).isEqualTo(svid.subject());
        assertThat(payload.getJsonArray("aud")).contains(RESOURCE_SERVER_AUD);
        assertThat(payload.getLong("exp")).isEqualTo(svid.expiry().getEpochSecond());
    }

    @Test
    void jwtSignatureVerifiableWithBundleKey() throws Exception {
        WorkloadJsonWebToken svid = fetchSvid(RESOURCE_SERVER_AUD);
        JsonObject bundle = new JsonObject(devServerRequest(GET, BUNDLE_PATH, null));
        JsonObject jwk = bundle.getJsonArray("keys").getJsonObject(0);

        byte[] x = Base64.getUrlDecoder().decode(jwk.getString("x"));
        byte[] y = Base64.getUrlDecoder().decode(jwk.getString("y"));
        ECPoint point = new ECPoint(new BigInteger(1, x), new BigInteger(1, y));
        AlgorithmParameters params = AlgorithmParameters.getInstance("EC");
        params.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec ecSpec = params.getParameterSpec(ECParameterSpec.class);
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey publicKey = kf.generatePublic(new ECPublicKeySpec(point, ecSpec));

        String[] parts = svid.token().split("\\.");
        byte[] signingInput = (parts[0] + "." + parts[1]).getBytes();
        byte[] signature = jwsToDer(Base64.getUrlDecoder().decode(parts[2]));

        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initVerify(publicKey);
        sig.update(signingInput);
        assertThat(sig.verify(signature)).isTrue();
    }

    @Test
    void bundleEndpointServesJwks() {
        String body = devServerRequest(GET, BUNDLE_PATH, null);

        JsonObject bundle = new JsonObject(body);
        assertThat(bundle.getInteger("spiffe_sequence")).isEqualTo(1);
        assertThat(bundle.getInteger("spiffe_refresh_hint")).isEqualTo(300);

        JsonArray keys = bundle.getJsonArray("keys");
        assertThat(keys).hasSize(1);

        JsonObject key = keys.getJsonObject(0);
        assertThat(key.getString("kty")).isEqualTo("EC");
        assertThat(key.getString("crv")).isEqualTo("P-256");
        assertThat(key.getString("use")).isEqualTo("jwt-svid");
        assertThat(key.getString("kid")).isNotBlank();
        assertThat(key.getString("x")).isNotBlank();
        assertThat(key.getString("y")).isNotBlank();
    }

    @Test
    void noArgFetchFailsWithoutConfiguredAudiences() {
        assertThatThrownBy(() -> spiffeClient.getWorkloadJsonWebToken().await().atMost(TIMEOUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quarkus.spiffe-client.audiences");
    }

    @Test
    void getWorkloadCertificateSubject() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
        assertThat(cert.keyMaterial().certificateChainPem()).isNotEmpty();
        assertThat(cert.keyMaterial().certificateChainPem().size()).isEqualTo(cert.keyMaterial().certificateChain().size());
        for (int i = 0; i < cert.keyMaterial().certificateChain().size(); i++) {
            assertThat(cert.keyMaterial().certificateChain().get(i).getEncoded())
                    .isEqualTo(parsePemDer(cert.keyMaterial().certificateChainPem().get(i), "CERTIFICATE"));
        }
    }

    @Test
    void getWorkloadCertificateLeafHasSpiffeIdInSanUri() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().certificateChain()).isNotEmpty();
        var leaf = cert.keyMaterial().certificateChain().get(0);
        var sans = leaf.getSubjectAlternativeNames();
        assertThat(sans).isNotNull();
        // URI SAN type = 6
        String spiffeUri = sans.stream()
                .filter(san -> (int) san.get(0) == 6)
                .map(san -> san.get(1).toString())
                .findFirst()
                .orElse(null);
        assertThat(spiffeUri).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    @Test
    void getWorkloadCertificatePrivateKeyMatchesCert() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        verifyKeyPairMatches(cert);
        assertThat(cert.keyMaterial().privateKeyPem()).startsWith("-----BEGIN PRIVATE KEY-----");
        assertThat(cert.keyMaterial().privateKey().getEncoded())
                .isEqualTo(parsePemDer(cert.keyMaterial().privateKeyPem(), "PRIVATE KEY"));
    }

    @Test
    void getWorkloadCertificateTrustBundleValidatesChain() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.trustMaterial().trustBundle()).isNotEmpty();
        validateCertificateChain(cert.keyMaterial().certificateChain(), cert.trustMaterial().trustBundle());
        assertThat(cert.trustMaterial().trustBundlePem()).isNotEmpty();
        assertThat(cert.trustMaterial().trustBundlePem().size()).isEqualTo(cert.trustMaterial().trustBundle().size());
        for (int i = 0; i < cert.trustMaterial().trustBundle().size(); i++) {
            assertThat(cert.trustMaterial().trustBundle().get(i).getEncoded())
                    .isEqualTo(parsePemDer(cert.trustMaterial().trustBundlePem().get(i), "CERTIFICATE"));
        }
    }

    @Test
    void getWorkloadCertificatePermissionDenied() {
        setServerMode(ServerMode.PERMISSION_DENIED);
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeAuthorizationException.class);
    }

    @Test
    void getWorkloadCertificateServerUnavailable() {
        setServerMode(ServerMode.UNAVAILABLE);
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    @Test
    void getWorkloadCertificateRecoveryAfterGrpcDown() {
        WorkloadCertificateDocument cert = fetchCert();
        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);

        setServerMode(ServerMode.GRPC_DOWN);
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);

        setServerMode(ServerMode.HEALTHY);
        cert = fetchCert();
        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    // proto spiffe_id and leaf cert URI SAN must agree on the identity
    @Test
    void getWorkloadCertificateProtoSpiffeIdMatchesSanUri() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        var uriSans = extractUriSans(cert);
        assertThat(uriSans).hasSize(1);
        assertThat(cert.subject()).isEqualTo(uriSans.get(0));
    }

    @Test
    void getWorkloadCertificateLeafIsCurrentlyValid() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        cert.keyMaterial().certificateChain().get(0).checkValidity();
    }

    @Test
    void getWorkloadCertificateTrustBundleValidatesChainDepth2() throws Exception {
        setServerMode(ServerMode.HEALTHY_X509_CHAIN_DEPTH_2);
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().certificateChain()).hasSize(2);
        validateCertificateChain(cert.keyMaterial().certificateChain(), cert.trustMaterial().trustBundle());
    }

    @Test
    void getWorkloadCertificateTrustBundleValidatesChainDepth3() throws Exception {
        setServerMode(ServerMode.HEALTHY_X509_CHAIN_DEPTH_3);
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().certificateChain()).hasSize(3);
        validateCertificateChain(cert.keyMaterial().certificateChain(), cert.trustMaterial().trustBundle());
    }

    @Test
    void getWorkloadCertificateEcP384() throws Exception {
        setServerMode(ServerMode.HEALTHY_X509_EC_P384);
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().privateKey().getAlgorithm()).isEqualTo("EC");
        verifyKeyPairMatches(cert);
    }

    @Test
    void getWorkloadCertificateRsa2048() throws Exception {
        setServerMode(ServerMode.HEALTHY_X509_RSA_2048);
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().privateKey().getAlgorithm()).isEqualTo("RSA");
        verifyKeyPairMatches(cert);
    }

    // X.509-SVID 4.1: leaf SVID must have cA=false
    @Test
    void getWorkloadCertificateLeafIsNotCa() {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().certificateChain().get(0).getBasicConstraints()).isEqualTo(-1);
    }

    // X.509-SVID 4.3: leaf SVID must set digitalSignature
    @Test
    void getWorkloadCertificateLeafHasDigitalSignatureKeyUsage() {
        WorkloadCertificateDocument cert = fetchCert();

        boolean[] keyUsage = cert.keyMaterial().certificateChain().get(0).getKeyUsage();
        assertThat(keyUsage).isNotNull();
        assertThat(keyUsage[0]).isTrue();
    }

    // X.509-SVID 4.3: leaf SVID must not have keyCertSign — a leaf that can sign certs could forge identities
    @Test
    void getWorkloadCertificateLeafDoesNotHaveKeyCertSign() {
        WorkloadCertificateDocument cert = fetchCert();

        boolean[] keyUsage = cert.keyMaterial().certificateChain().get(0).getKeyUsage();
        assertThat(keyUsage).isNotNull();
        assertThat(keyUsage[5]).isFalse();
    }

    // X.509-SVID 2: leaf must have exactly one URI SAN containing the SPIFFE ID
    @Test
    void getWorkloadCertificateLeafHasExactlyOneUriSan() throws Exception {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(extractUriSans(cert)).hasSize(1);
    }

    @Test
    void getWorkloadCertificateVertxKeyCertOptions() {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.keyMaterial().asVertxKeyCertOptions()).isNotNull();
    }

    @Test
    void getWorkloadCertificateVertxTrustOptions() {
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.trustMaterial().asVertxTrustOptions()).isNotNull();
    }

    // TODO: add SpiffeX509TrustManager rejection tests after dev service cert generator is replaced
    // - rejects leaf with cA=true
    // - rejects leaf with keyCertSign
    // - rejects leaf with multiple URI SANs
    // - rejects leaf with non-spiffe URI SAN
    // - rejects leaf with root-path SPIFFE ID
    // - rejects signing cert with path-component SPIFFE ID

    @Test
    void getWorkloadCertificateReturnsDefaultIdentity() {
        setServerMode(ServerMode.HEALTHY_X509_MULTI_SVID);
        WorkloadCertificateDocument cert = fetchCert();

        assertThat(cert.subject()).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    @Test
    void getWorkloadCertificateEmptySvids() {
        setServerMode(ServerMode.HEALTHY_X509_EMPTY_SVIDS);
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    @Test
    void getWorkloadCertificateCorruptedCert() {
        setServerMode(ServerMode.HEALTHY_X509_CORRUPTED_CERT);
        assertThatThrownBy(this::fetchCert).hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    private static void validateCertificateChain(List<X509Certificate> chain, List<X509Certificate> trustBundle)
            throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        CertPath path = cf.generateCertPath(chain);
        Set<TrustAnchor> anchors = trustBundle.stream()
                .map(ca -> new TrustAnchor(ca, null))
                .collect(Collectors.toSet());
        PKIXParameters params = new PKIXParameters(anchors);
        params.setRevocationEnabled(false);
        CertPathValidator.getInstance("PKIX").validate(path, params);
    }

    private static List<String> extractUriSans(WorkloadCertificateDocument cert) throws Exception {
        var sans = cert.keyMaterial().certificateChain().get(0).getSubjectAlternativeNames();
        if (sans == null) {
            return List.of();
        }
        return sans.stream()
                .filter(san -> (int) san.get(0) == 6)
                .map(san -> san.get(1).toString())
                .toList();
    }

    private static void verifyKeyPairMatches(WorkloadCertificateDocument cert) throws Exception {
        byte[] data = "spiffe-key-pair-test".getBytes();
        var leaf = cert.keyMaterial().certificateChain().get(0);
        Signature sig = Signature.getInstance(leaf.getSigAlgName());
        sig.initSign(cert.keyMaterial().privateKey());
        sig.update(data);
        byte[] signed = sig.sign();
        sig.initVerify(leaf.getPublicKey());
        sig.update(data);
        assertThat(sig.verify(signed)).isTrue();
    }

    private static byte[] parsePemDer(String pem, String type) {
        String base64 = pem
                .replace("-----BEGIN " + type + "-----", "")
                .replace("-----END " + type + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(base64);
    }

    private WorkloadCertificateDocument fetchCert() {
        return spiffeClient.getWorkloadCertificate().await().atMost(TIMEOUT);
    }

    private WorkloadJsonWebToken fetchSvid(String audience) {
        return spiffeClient.getWorkloadJsonWebToken(audience).await().atMost(TIMEOUT);
    }

    private void setServerMode(ServerMode mode) {
        devServerRequest(POST, ADMIN_API_MODE_PATH, Buffer.buffer(mode.name()));
    }

    // Inverse of JwtSvidSigner#derToJws in SpiffeDevServiceProcessor; per RFC 7518 section 3.4
    private static byte[] jwsToDer(byte[] jws) {
        int half = jws.length / 2;
        byte[] r = trimLeadingZeros(jws, 0, half);
        byte[] s = trimLeadingZeros(jws, half, half);
        int rLen = r.length + ((r[0] & 0x80) != 0 ? 1 : 0);
        int sLen = s.length + ((s[0] & 0x80) != 0 ? 1 : 0);
        byte[] der = new byte[6 + rLen + sLen];
        der[0] = 0x30;
        der[1] = (byte) (4 + rLen + sLen);
        der[2] = 0x02;
        der[3] = (byte) rLen;
        int offset = 4;
        if (rLen > r.length) {
            der[offset++] = 0;
        }
        System.arraycopy(r, 0, der, offset, r.length);
        offset += r.length;
        der[offset++] = 0x02;
        der[offset++] = (byte) sLen;
        if (sLen > s.length) {
            der[offset++] = 0;
        }
        System.arraycopy(s, 0, der, offset, s.length);
        return der;
    }

    private static byte[] trimLeadingZeros(byte[] data, int off, int len) {
        int start = off;
        while (start < off + len - 1 && data[start] == 0) {
            start++;
        }
        byte[] result = new byte[off + len - start];
        System.arraycopy(data, start, result, 0, result.length);
        return result;
    }

    private String devServerRequest(HttpMethod method, String path, Buffer body) {
        if (devServerHttpClient == null) {
            devServerHttpClient = newInstance(vertx).createHttpClient();
        }
        URI uri = URI.create(devServerBaseUrl);
        return devServerHttpClient.request(method, uri.getPort(), uri.getHost(), path)
                .flatMap(req -> body != null ? req.send(body) : req.send())
                .flatMap(resp -> {
                    assertThat(resp.statusCode()).isEqualTo(200);
                    return resp.body();
                })
                .await().atMost(TIMEOUT)
                .toString();
    }
}
