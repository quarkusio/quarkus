package io.quarkus.tls.runtime.letsencrypt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.Signature;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import io.vertx.core.json.JsonObject;

class JwsTest {

    @Test
    void signWithJwkCarriesTheKeyAndVerifies() throws Exception {
        KeyPair keyPair = Certificates.generateKeyPair();
        Jws jws = new Jws(keyPair);

        JsonObject signed = jws.signWithJwk("https://acme.example/new-account", "nonce-1",
                new JsonObject().put("termsOfServiceAgreed", true));

        JsonObject header = decode(signed.getString("protected"));
        assertThat(header.getString("alg")).isEqualTo("ES256");
        assertThat(header.getString("nonce")).isEqualTo("nonce-1");
        assertThat(header.getString("url")).isEqualTo("https://acme.example/new-account");
        assertThat(header.getJsonObject("jwk")).isEqualTo(jws.jwk());
        assertThat(header.containsKey("kid")).isFalse();
        assertThat(decode(signed.getString("payload")).getBoolean("termsOfServiceAgreed")).isTrue();
        assertThat(verify(keyPair, signed)).isTrue();
    }

    @Test
    void postAsGetHasAnEmptyPayloadAndTheAccountUrl() throws Exception {
        KeyPair keyPair = Certificates.generateKeyPair();
        Jws jws = new Jws(keyPair);

        JsonObject signed = jws.signWithKid("https://acme.example/order/1", "nonce-2",
                "https://acme.example/account/7", null);

        assertThat(signed.getString("payload")).isEmpty();
        JsonObject header = decode(signed.getString("protected"));
        assertThat(header.getString("kid")).isEqualTo("https://acme.example/account/7");
        assertThat(header.containsKey("jwk")).isFalse();
        assertThat(verify(keyPair, signed)).isTrue();
    }

    @Test
    void thumbprintIsTheHashOfTheCanonicalKey() throws Exception {
        Jws jws = new Jws(Certificates.generateKeyPair());
        JsonObject jwk = jws.jwk();

        String canonical = "{\"crv\":\"P-256\",\"kty\":\"EC\",\"x\":\"" + jwk.getString("x") + "\",\"y\":\""
                + jwk.getString("y") + "\"}";
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));

        assertThat(jws.thumbprint()).isEqualTo(expected);
        assertThat(Base64.getUrlDecoder().decode(jwk.getString("x"))).hasSize(32);
        assertThat(Base64.getUrlDecoder().decode(jwk.getString("y"))).hasSize(32);
    }

    @Test
    void rejectsKeysThatAreNotOnP256() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair rsa = generator.generateKeyPair();

        assertThatThrownBy(() -> new Jws(rsa))
                .isInstanceOf(GeneralSecurityException.class)
                .hasMessageContaining("P-256");
    }

    private static JsonObject decode(String base64url) {
        return new JsonObject(new String(Base64.getUrlDecoder().decode(base64url), StandardCharsets.UTF_8));
    }

    private static boolean verify(KeyPair keyPair, JsonObject signed) throws GeneralSecurityException {
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keyPair.getPublic());
        verifier.update((signed.getString("protected") + "." + signed.getString("payload"))
                .getBytes(StandardCharsets.US_ASCII));
        return verifier.verify(Base64.getUrlDecoder().decode(signed.getString("signature")));
    }
}
