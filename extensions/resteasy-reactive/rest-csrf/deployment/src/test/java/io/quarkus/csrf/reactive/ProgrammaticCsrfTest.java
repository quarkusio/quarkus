package io.quarkus.csrf.reactive;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.csrf.reactive.runtime.RestCsrfConfig;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.security.CSRF;
import io.quarkus.vertx.http.security.HttpSecurity;
import io.restassured.RestAssured;
import io.restassured.config.EncoderConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.http.ContentType;
import io.smallrye.mutiny.Uni;

public class ProgrammaticCsrfTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestResource.class, ProgrammaticCsrfConfig.class)
                    .addAsResource("templates/csrfToken.html"))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-security", Version.getVersion())));

    private final static String COOKIE_NAME = "custom-csrf-token";
    private final static String HEADER_NAME = "CUSTOM-X-CSRF-TOKEN";

    @Test
    public void testCsrfBuilder() {
        var fullConfig = (RestCsrfConfig) CSRF.builder()
                .tokenHeaderName("tokenHeader1")
                .formFieldName("formField1")
                .cookieName("cookie1")
                .cookieForceSecure()
                .cookieHttpOnly(false)
                .cookiePath("path1")
                .requireFormUrlEncoded(false)
                .tokenSize(1234)
                .tokenSignatureKey("12345".repeat(10))
                .cookieMaxAge(Duration.ofHours(5))
                .cookieDomain("Domain1")
                .createTokenPath("tokenPath1")
                .build();
        Assertions.assertEquals("tokenHeader1", fullConfig.tokenHeaderName());
        Assertions.assertEquals("formField1", fullConfig.formFieldName());
        Assertions.assertEquals("cookie1", fullConfig.cookieName());
        Assertions.assertTrue(fullConfig.cookieForceSecure());
        Assertions.assertFalse(fullConfig.cookieHttpOnly());
        Assertions.assertEquals("path1", fullConfig.cookiePath());
        Assertions.assertFalse(fullConfig.requireFormUrlEncoded());
        Assertions.assertEquals(1234, fullConfig.tokenSize());
        Assertions.assertEquals("12345".repeat(10), fullConfig.tokenSignatureKey().get());
        Assertions.assertEquals(Duration.ofHours(5), fullConfig.cookieMaxAge());
        Assertions.assertEquals("Domain1", fullConfig.cookieDomain().get());
        Assertions.assertEquals(Set.of("tokenPath1"), fullConfig.createTokenPath().get());
        var defaultConfig = (RestCsrfConfig) CSRF.builder().build();
        Assertions.assertEquals("X-CSRF-TOKEN", defaultConfig.tokenHeaderName());
        Assertions.assertEquals("csrf-token", defaultConfig.formFieldName());
        Assertions.assertEquals("csrf-token", defaultConfig.cookieName());
        Assertions.assertFalse(defaultConfig.cookieForceSecure());
        Assertions.assertTrue(defaultConfig.cookieHttpOnly());
        Assertions.assertEquals("/", defaultConfig.cookiePath());
        Assertions.assertTrue(defaultConfig.requireFormUrlEncoded());
        Assertions.assertEquals(16, defaultConfig.tokenSize());
        Assertions.assertTrue(defaultConfig.verifyToken());
        Assertions.assertTrue(defaultConfig.tokenSignatureKey().isEmpty());
        Assertions.assertEquals(Duration.ofHours(2), defaultConfig.cookieMaxAge());
        Assertions.assertTrue(defaultConfig.cookieDomain().isEmpty());
        Assertions.assertTrue(defaultConfig.createTokenPath().isEmpty());
    }

    @Test
    public void testForm() {
        String token = when()
                .get("/csrfTokenForm")
                .then()
                .statusCode(200)
                .cookie(COOKIE_NAME)
                .extract()
                .cookie(COOKIE_NAME);
        EncoderConfig encoderConfig = EncoderConfig.encoderConfig().encodeContentTypeAs("multipart/form-data",
                ContentType.TEXT);
        RestAssuredConfig restAssuredConfig = RestAssured.config().encoderConfig(encoderConfig);

        //no token
        given()
                .cookie(COOKIE_NAME, token)
                .config(restAssuredConfig)
                .formParam("name", "testName")
                .contentType(ContentType.URLENC)
                .when()
                .post("csrfTokenForm")
                .then()
                .statusCode(400);

        //wrong token
        given()
                .cookie(COOKIE_NAME, token)
                .config(restAssuredConfig)
                .formParam(COOKIE_NAME, "WRONG")
                .formParam("name", "testName")
                .contentType(ContentType.URLENC)
                .when()
                .post("csrfTokenForm")
                .then()
                .statusCode(400);

        //valid token
        given()
                .cookie(COOKIE_NAME, token)
                .config(restAssuredConfig)
                .formParam(COOKIE_NAME, token)
                .formParam("name", "testName")
                .contentType(ContentType.URLENC)
                .when()
                .post("csrfTokenForm")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("testName"));
    }

    @Test
    public void testNoBody() {
        String token = when().get("/csrfTokenForm")
                .then().statusCode(200).cookie(COOKIE_NAME)
                .extract().cookie(COOKIE_NAME);

        // no token
        given()
                .cookie(COOKIE_NAME, token)
                .when()
                .post("csrfTokenPost")
                .then()
                .statusCode(400);

        //wrong token
        given()
                .cookie(COOKIE_NAME, token)
                .header(HEADER_NAME, "WRONG")
                .when()
                .post("csrfTokenPost")
                .then()
                .statusCode(400);

        //valid token
        given()
                .cookie(COOKIE_NAME, token)
                .header(HEADER_NAME, token)
                .when()
                .post("csrfTokenPost")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("no user"));
    }

    @Test
    public void testWithBody() {
        String token = when()
                .get("/csrfTokenForm")
                .then()
                .statusCode(200)
                .cookie(COOKIE_NAME)
                .extract()
                .cookie(COOKIE_NAME);

        // no token
        given()
                .cookie(COOKIE_NAME, token)
                .body("testName")
                .contentType(ContentType.TEXT)
                .when()
                .post("csrfTokenPostBody")
                .then()
                .statusCode(400);

        //wrong token
        given()
                .cookie(COOKIE_NAME, token)
                .header(HEADER_NAME, "WRONG")
                .body("testName")
                .contentType(ContentType.TEXT)
                .when()
                .post("csrfTokenPostBody")
                .then()
                .statusCode(400);

        //valid token => This test fails but should work
        given()
                .cookie(COOKIE_NAME, token)
                .header(HEADER_NAME, token)
                .body("testName")
                .contentType(ContentType.TEXT)
                .when()
                .post("csrfTokenPostBody")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("testName"));
    }

    @Path("")
    public static class TestResource {

        @Inject
        Template csrfToken;

        @GET
        @Path("/csrfTokenForm")
        @Produces(MediaType.TEXT_HTML)
        public TemplateInstance getCsrfTokenForm() {
            return csrfToken.instance();
        }

        @POST
        @Path("/csrfTokenForm")
        @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> postCsrfTokenForm(@FormParam("name") String userName) {
            return Uni.createFrom().item(userName);
        }

        @POST
        @Path("/csrfTokenPost")
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> postJson() {
            return Uni.createFrom().item("no user");
        }

        @POST
        @Path("/csrfTokenPostBody")
        @Consumes(MediaType.TEXT_PLAIN)
        @Produces(MediaType.TEXT_PLAIN)
        public Uni<String> postJson(String body) {
            return Uni.createFrom().item(body);
        }
    }

    public static class ProgrammaticCsrfConfig {
        void configure(@Observes HttpSecurity httpSecurity) {
            httpSecurity.csrf(CSRF.builder()
                    .cookieName(COOKIE_NAME)
                    .formFieldName(COOKIE_NAME)
                    .tokenHeaderName(HEADER_NAME)
                    .build());
        }
    }
}
