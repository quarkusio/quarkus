package io.quarkus.jwt.test.dev;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.jwt.Claims;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.jwt.test.GreetingResource;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.smallrye.jwt.build.Jwt;
import io.smallrye.jwt.util.KeyUtils;

public class SmallryeJwtProcessorDevModeTest {

    @RegisterExtension
    static QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar.addClasses(
                    GreetingResource.class, TokenResource.class).addAsResource(
                            new StringAsset(""),
                            "application.properties"));

    @Test
    public void shouldNotBeNecessaryToAddSignKeysOnApplicationProperties() {
        String token = RestAssured.given()
                .header(new Header("Accept", "text/plain"))
                .get("/token")
                .andReturn()
                .body()
                .asString();

        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200);
    }

    @Test
    public void shouldUseTheSameTokenEvenWhenTheUserChangesTheConfiguration() {
        String token = RestAssured.given()
                .header(new Header("Accept", "text/plain"))
                .get("/token")
                .andReturn()
                .body()
                .asString();

        // there is no need to get another token
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200)
                .body(Matchers.containsString("Hello from Quarkus REST"));

        devMode.modifyResourceFile("application.properties", s -> """
                smallrye.jwt.sign.key.location=invalidLocation.pem
                mp.jwt.verify.publickey.location=invalidLocation.pem
                """);

        // should throw error because the private/public are invalid
        String newToken = RestAssured.given()
                .header(new Header("Accept", "text/plain"))
                .get("/token")
                .andReturn()
                .body()
                .asString();

        // should return 500 because the location is invalid
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + newToken))
                .get("/only-user")
                .then().assertThat().statusCode(500);

        devMode.modifyResourceFile("application.properties", s -> "");

        // there is no need to get another token
        // should work with old token
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200)
                .body(Matchers.containsString("Hello from Quarkus REST"));
    }

    @Test
    public void shouldUseTheSameKeyPairOnLiveReload() {
        String token = RestAssured.given()
                .header(new Header("Accept", "text/plain"))
                .get("/token")
                .andReturn()
                .body()
                .asString();

        devMode.modifySourceFile("GreetingResource.java", s -> s.replace("Hello from Quarkus", "Hello from JWT"));

        // there is no need to get another token
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200)
                .body(Matchers.containsString("Hello from JWT"));
    }

    @Test
    public void shouldUseTheSameTokenEvenWhenTheUserChangesTheConfigWithKeyProps() {

        devMode.modifyResourceFile("application.properties", s -> "");

        String token = RestAssured.given()
                .header(new Header("Accept", "text/plain"))
                .get("/token")
                .andReturn()
                .body()
                .asString();

        // there is no need to get another token
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200)
                .body(Matchers.containsString("Hello from Quarkus REST"));

        try {
            String privateKey = KeyUtils.readKeyContent("/privateKey.pem");
            String publicKey = KeyUtils.readKeyContent("/publicKey.pem");
            devMode.modifyResourceFile("application.properties", s -> """
                    smallrye.jwt.sign.key=%s
                    mp.jwt.verify.publickey=%s
                    """.formatted(privateKey, publicKey));
        } catch (IOException e) {
            fail("Was not possible for reading keys from resource");
        }

        // should throw error because the private/public are invalid
        String newToken = RestAssured.given()
                .header(new Header("Accept", "text/plain"))
                .get("/token")
                .andReturn()
                .body()
                .asString();

        // should return 200 because the keys are valid
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + newToken))
                .get("/only-user")
                .then().assertThat().statusCode(200);

        devMode.modifyResourceFile("application.properties", s -> "");

        // there is no need to get another token
        // should work with old token
        RestAssured.given()
                .header(new Header("Authorization", "Bearer " + token))
                .get("/only-user")
                .then().assertThat().statusCode(200)
                .body(Matchers.containsString("Hello from Quarkus REST"));
    }

    @Path("/token")
    static class TokenResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        @PermitAll
        public String hello() {
            return Jwt.upn("jdoe@quarkus.io")
                    .groups("User")
                    .claim(Claims.birthdate.name(), "2001-07-13")
                    .sign();
        }
    }
}
