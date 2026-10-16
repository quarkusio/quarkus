package io.quarkus.narayana.lra.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.smallrye.jwt.build.Jwt;

/**
 * Smoke tests for the Narayana LRA 2.x JWT bearer-token feature.
 * <p>
 * The feature's {@code @PropagateToken}/{@code BearerTokenResolver} machinery is
 * a Narayana extension, <em>not</em> part of the MicroProfile LRA specification
 * (MP-LRA does not mandate bearer-token propagation). What these tests validate
 * against a specification is <strong>MicroProfile JWT (MP-JWT 2.1)</strong>: a
 * valid MP-JWT is produced/verified by quarkus-smallrye-jwt and round-tripped
 * through Narayana's CDI resolver ({@link io.narayana.lra.BearerTokenResolver}).
 * <p>
 * The RSA key pair is generated in-memory at startup; no key material is ever
 * written to disk or committed.
 */
public class LRAJwtPropagationSmokeTest {

    private static final String ISSUER = "https://lra-jwt-smoke-test";

    private static final KeyPair KEY_PAIR = generateKeyPair();

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(LRAJwtResource.class))
            // Do not try to start a coordinator container for these hermetic tests.
            .overrideConfigKey("quarkus.lra.devservices.enabled", "false")
            // Authenticate every request so a valid bearer token populates the
            // CDI JsonWebToken even on endpoints without a security constraint.
            .overrideConfigKey("quarkus.http.auth.proactive", "true")
            .overrideConfigKey("mp.jwt.verify.issuer", ISSUER)
            .overrideConfigKey("mp.jwt.verify.publickey",
                    Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded()))
            .overrideConfigKey("smallrye.jwt.sign.key",
                    Base64.getEncoder().encodeToString(KEY_PAIR.getPrivate().getEncoded()));

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to generate RSA key pair for the LRA JWT smoke test", e);
        }
    }

    @Test
    public void bootsWithJwtFeatureEnabled() {
        RestAssured.when().get("/lra-jwt/ping")
                .then().statusCode(200)
                .body(org.hamcrest.CoreMatchers.is("pong"));
    }

    @Test
    public void resolvesBearerTokenFromCdi() {
        String token = Jwt.issuer(ISSUER).upn("alice").groups("User").sign();

        String resolved = RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .when().get("/lra-jwt/resolved")
                .then().statusCode(200)
                .extract().body().asString();

        assertThat(resolved).isEqualTo(token);
    }

    @Test
    public void resolvesToNoTokenWhenUnauthenticated() {
        String resolved = RestAssured.when().get("/lra-jwt/resolved")
                .then().statusCode(200)
                .extract().body().asString();

        assertThat(resolved).isEmpty();
    }
}
