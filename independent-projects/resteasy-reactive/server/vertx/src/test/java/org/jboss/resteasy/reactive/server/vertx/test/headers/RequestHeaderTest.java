package org.jboss.resteasy.reactive.server.vertx.test.headers;

import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.util.Locale;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;

import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.restassured.RestAssured;

public class RequestHeaderTest {

    private static final String BASE_PATH = "/test";

    @RegisterExtension
    static ResteasyReactiveUnitTest TEST = new ResteasyReactiveUnitTest()
            .addScanCustomizer(scanStep -> scanStep.setSingleDefaultProduces(true))
            .withApplicationRoot((jar) -> jar.addClasses(TestResource.class));

    @Test
    public void testISO2Language() {
        String expected = "en";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testISO3Language() {
        String expected = "tlh";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testScriptSubtag() {
        String expected = "zh-Hans";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testRegionSubtag() {
        String expected = "en-GB";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testRegionSubtag2() {
        String expected = "es-005";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testRegionSubtag3() {
        String expected = "zh-Hant-HK";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testVariantSubtag() {
        String expected = "sl-nedis";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testVariantSubtag2() {
        String expected = "sl-IT-nedis";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testExtensionSubtag() {
        String expected = "de-DE-u-co-phonebk";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Test
    public void testPrivateUseSubtag() {
        String expected = "en-US-x-twain";

        RestAssured
                .given()
                .header(HttpHeaders.ACCEPT_LANGUAGE, expected)
                .get(BASE_PATH)
                .then()
                .statusCode(200)
                .assertThat().body(equalToIgnoringCase(expected));
    }

    @Path(BASE_PATH)
    public static class TestResource {

        @Context
        HttpHeaders headers;

        @GET
        public String echo() {
            final Locale locale = headers.getAcceptableLanguages().isEmpty() ? Locale.ENGLISH
                    : headers.getAcceptableLanguages().get(0);

            return locale.toLanguageTag();
        }

    }
}
