package io.quarkus.spiffe.client.deployment.test;

import static io.quarkus.spiffe.client.api.JwtSvidRequest.forAudience;
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
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.time.Duration;
import java.util.Base64;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.spiffe.client.api.JwtSvid;
import io.quarkus.spiffe.client.api.JwtSvidRequest;
import io.quarkus.spiffe.client.api.SpiffeAuthorizationException;
import io.quarkus.spiffe.client.api.SpiffeClient;
import io.quarkus.spiffe.client.api.SpiffeConnectionException;
import io.quarkus.spiffe.client.deployment.SpiffeDevServicesProcessor.ServerMode;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

abstract class AbstractSpiffeClientTest {

    private static final String RESOURCE_SERVER_AUD = TEST_TRUST_DOMAIN + "/resource-server";
    private static final String API_GATEWAY_AUD = TEST_TRUST_DOMAIN + "/api-gateway";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

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

    // also verifies the security header is added by our client as our DEV server rejects other requests
    @Test
    void fetchJwtSvidSingleAudience() {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));

        assertThat(svid.spiffeId()).isEqualTo(DEFAULT_SPIFFE_ID);
        assertThat(svid.audience()).contains(RESOURCE_SERVER_AUD);
    }

    @Test
    void fetchJwtSvidMultipleAudiences() {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD, API_GATEWAY_AUD));

        assertThat(svid.audience()).containsExactlyInAnyOrder(RESOURCE_SERVER_AUD, API_GATEWAY_AUD);
    }

    @Test
    void fetchJwtSvidWithSpiffeIdFilter() {
        setServerMode(ServerMode.MULTI_IDENTITY);

        JwtSvid secondary = fetchSvid(
                JwtSvidRequest.builder().audience(RESOURCE_SERVER_AUD).spiffeId(SECONDARY_SPIFFE_ID).build());
        assertThat(secondary.spiffeId()).isEqualTo(SECONDARY_SPIFFE_ID);

        JwtSvid tertiary = fetchSvid(
                JwtSvidRequest.builder().audience(RESOURCE_SERVER_AUD).spiffeId(TERTIARY_SPIFFE_ID).build());
        assertThat(tertiary.spiffeId()).isEqualTo(TERTIARY_SPIFFE_ID);
    }

    @Test
    void fetchJwtSvidTokenIsValidJws() {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));

        assertThat(svid.token()).isNotBlank();
        assertThat(svid.token().split("\\.")).hasSize(3);
    }

    @Test
    void fetchJwtSvidExpiryParsed() {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));

        assertThat(svid.expiry()).isNotNull();
        assertThat(svid.expiry()).isAfter(now());
    }

    @Test
    void fetchJwtSvidPermissionDenied() {
        setServerMode(ServerMode.PERMISSION_DENIED);
        assertThatThrownBy(() -> fetchSvid(forAudience(RESOURCE_SERVER_AUD)))
                .hasCauseInstanceOf(SpiffeAuthorizationException.class);
    }

    @Test
    void fetchJwtSvidUnavailable() {
        setServerMode(ServerMode.UNAVAILABLE);
        assertThatThrownBy(() -> fetchSvid(forAudience(RESOURCE_SERVER_AUD)))
                .hasCauseInstanceOf(SpiffeConnectionException.class);
    }

    @Test
    void fetchJwtSvidRecoveryAfterGrpcDown() {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));
        assertThat(svid.spiffeId()).isEqualTo(DEFAULT_SPIFFE_ID);

        setServerMode(ServerMode.GRPC_DOWN);
        assertThatThrownBy(() -> fetchSvid(forAudience(RESOURCE_SERVER_AUD)))
                .hasCauseInstanceOf(SpiffeConnectionException.class);

        setServerMode(ServerMode.HEALTHY);
        svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));
        assertThat(svid.spiffeId()).isEqualTo(DEFAULT_SPIFFE_ID);
    }

    @Test
    void spiffeIdFromProto() {
        assertThat(fetchSvid(forAudience(RESOURCE_SERVER_AUD)).spiffeId()).startsWith("spiffe://");
    }

    @Test
    void jwtPayloadClaimsMatchApiFields() {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));

        String[] parts = svid.token().split("\\.");
        JsonObject payload = new JsonObject(new String(Base64.getUrlDecoder().decode(parts[1])));
        assertThat(payload.getString("sub")).isEqualTo(svid.spiffeId());
        assertThat(payload.getJsonArray("aud")).contains(RESOURCE_SERVER_AUD);
        assertThat(payload.getLong("exp")).isEqualTo(svid.expiry().getEpochSecond());
    }

    @Test
    void jwtSignatureVerifiableWithBundleKey() throws Exception {
        JwtSvid svid = fetchSvid(forAudience(RESOURCE_SERVER_AUD));
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
        assertThatThrownBy(() -> spiffeClient.fetchJwtSvid().await().atMost(TIMEOUT))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("quarkus.spiffe-client.audiences");
    }

    @Test
    void emptyRequestAudiencesFailsWithoutConfiguredAudiences() {
        assertThatThrownBy(() -> spiffeClient.fetchJwtSvid(JwtSvidRequest.builder().build())
                .await().atMost(TIMEOUT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("quarkus.spiffe-client.audiences");
    }

    private JwtSvid fetchSvid(JwtSvidRequest request) {
        return spiffeClient.fetchJwtSvid(request).await().atMost(TIMEOUT);
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
        URI uri = URI.create(devServerBaseUrl);
        var client = newInstance(vertx).createHttpClient();
        try {
            return client.request(method, uri.getPort(), uri.getHost(), path)
                    .flatMap(req -> body != null ? req.send(body) : req.send())
                    .flatMap(resp -> {
                        assertThat(resp.statusCode()).isEqualTo(200);
                        return resp.body();
                    })
                    .await().atMost(TIMEOUT)
                    .toString();
        } finally {
            client.close().await().atMost(TIMEOUT);
        }
    }
}
