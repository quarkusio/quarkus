package io.quarkus.resteasy.reactive.qute.deployment;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.qute.i18n.LocaleAware;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * A message bundle injected with {@link LocaleAware} is resolved using
 * the locale negotiated from the {@code Accept-Language} header of the current
 * HTTP request.
 */
public class LocaleAwareTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root
                    .addClasses(GreetingMessages.class, CzechGreetingMessages.class, GreetingResource.class));

    @Test
    public void testDefaultLocaleWhenNoHeader() {
        when()
                .get("/greeting")
                .then()
                .statusCode(200)
                .body(is("Hello!"));
    }

    @Test
    public void testExactLocaleMatch() {
        given()
                .header("Accept-Language", "cs")
                .when()
                .get("/greeting").then()
                .statusCode(200)
                .body(is("Ahoj!"));
    }

    @Test
    public void testLanguageMatchAndQValueOrdering() {
        // cs-CZ has no exact bundle - falls back to the language-only "cs" match; also
        // the highest q-value wins
        given()
                .header("Accept-Language", "cs-CZ;q=0.9,en;q=0.5")
                .when()
                .get("/greeting")
                .then()
                .statusCode(200)
                .body(is("Ahoj!"));
    }

    @Test
    public void testFallbackToDefaultForUnknownLocale() {
        given()
                .header("Accept-Language", "fr")
                .when()
                .get("/greeting")
                .then()
                .statusCode(200)
                .body(is("Hello!"));
    }

    @Test
    public void testMessageWithParameter() {
        given()
                .header("Accept-Language", "cs")
                .when()
                .get("/greeting/Honza")
                .then()
                .statusCode(200)
                .body(is("Ahoj Honza!"));
        when()
                .get("/greeting/Honza")
                .then()
                .statusCode(200)
                .body(is("Hello Honza!"));
    }

}
