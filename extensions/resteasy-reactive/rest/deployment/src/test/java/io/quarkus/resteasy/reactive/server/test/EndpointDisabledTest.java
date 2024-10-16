package io.quarkus.resteasy.reactive.server.test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.CoreMatchers.equalTo;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;
import io.quarkus.resteasy.reactive.server.test.multipart.InvalidEncodingTest;
import io.quarkus.test.QuarkusUnitTest;

public class EndpointDisabledTest {

    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(InvalidEncodingTest.FeedbackBody.class, InvalidEncodingTest.FeedbackResource.class)
                    .addAsResource(new StringAsset("dummy.disabled=true"),
                            "application.properties"));

    @Test
    public void endpointWithNoAnnotation() {
        get("/no-annotation")
                .then()
                .statusCode(200)
                .body(equalTo("no"));
    }

    @Test
    public void shouldBeDisabledBecauseOfMatchingProperty() {
        get("/dummy-disabled-true")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldBeEnabledBecauseOfNonMatchingProperty() {
        get("/dummy-disabled-false")
                .then()
                .statusCode(200)
                .body(equalTo("dummy.disabled=false"));
    }

    @Test
    public void shouldBeDisabledBecauseOfNonExistingProperty() {
        get("/other-dummy-disabled-missing-true")
                .then()
                .statusCode(404);
    }

    @Test
    public void shouldBeEnabledBecauseOfNonExistingProperty() {
        get("/other-dummy-disabled-missing-false")
                .then()
                .statusCode(200)
                .body(equalTo("missing=false"));
    }

    @Path("no-annotation")
    public static class NoAnnotation {

        @GET
        public String get() {
            return "no";
        }

    }

    @Path("dummy-disabled-true")
    @EndpointDisabled(name = "dummy.disabled", stringValue = "true", disableIfMissing = false)
    public static class DummyDisabledTrue {

        @GET
        public String get() {
            return "dummy.disabled=true";
        }
    }

    @Path("dummy-disabled-false")
    @EndpointDisabled(name = "dummy.disabled", stringValue = "false", disableIfMissing = false)
    public static class DummyDisabledFalse {

        @GET
        public String get() {
            return "dummy.disabled=false";
        }
    }

    @Path("other-dummy-disabled-missing-true")
    @EndpointDisabled(name = "other.dummy.disabled", stringValue = "true", disableIfMissing = true)
    public static class OtherDummyDisabledMissingTrue {

        @GET
        public String get() {
            return "missing=true";
        }
    }

    @Path("other-dummy-disabled-missing-false")
    @EndpointDisabled(name = "other.dummy.disabled", stringValue = "true", disableIfMissing = false)
    public static class OtherDummyDisabledMissingFalse {

        @GET
        public String get() {
            return "missing=false";
        }
    }
}
