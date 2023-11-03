package io.quarkus.smallrye.openapi.test.jaxrs;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.EndpointDisabled;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Verify that REST endpoints that are disabled via the {@link EndpointDisabled} annotation are not included in the OpenAPI
 * document.
 */
public class DisabledEndpointTestCase {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DisabledEndpoint.class,
                            DisabledOtherEndpoint.class,
                            DisabledRootEndpoint.class,
                            EnabledEndpoint.class)
                    .add(new StringAsset(""), "application.properties"));

    @EndpointDisabled(name = "xxx", disableIfMissing = true, stringValue = "xxx")
    @Path("/disabled")
    public static class DisabledEndpoint {

        @Path("/")
        @GET
        public String helloRoot() {
            return null;
        }

        @Path("/hello")
        @GET
        public String hello() {
            return null;
        }

        @Path("/hello2/{param1}")
        @GET
        public String hello2(@QueryParam("param1") String param1) {
            return null;
        }

        @Path("/hello5")
        @PUT
        public String hello5() {
            return null;
        }

    }

    @EndpointDisabled(name = "xxx", disableIfMissing = true, stringValue = "xxx")
    @Path("/enabled")
    public static class DisabledOtherEndpoint {

        @Path("/hello5")
        @GET
        public String hello5() {
            return null;
        }

    }

    @EndpointDisabled(name = "xxx", disableIfMissing = true, stringValue = "xxx")
    @Path("/")
    public static class DisabledRootEndpoint {

        @GET
        public String helloRoot() {
            return null;
        }

    }

    @EndpointDisabled(name = "xxx", disableIfMissing = false, stringValue = "xxx")
    @Path("/enabled")
    public static class EnabledEndpoint {

        @Path("/")
        @GET
        public String helloRoot() {
            return null;
        }

        @Path("/hello3/")
        @GET
        public String hello() {
            return null;
        }

        @Path("/hello4/{param1}")
        @GET
        public String hello4(@QueryParam("param1") String param1) {
            return null;
        }

        @Path("/hello5")
        @POST
        public String hello5() {
            return null;
        }

    }

    @Test
    public void testDisabledEndpoint() {
        RestAssured.given().header("Accept", "application/json")
                .when().get("/q/openapi")
                .prettyPeek().then()
                // All paths from DisabledEndpoint missing
                .body("paths.\"/disabled\"", nullValue())
                // Paths from DisabledOtherEndpoint
                .body("paths.\"/enabled/hello5\".get", nullValue())
                // Paths from DisabledRootEndpoint
                .body("paths.\"/\".get", nullValue())
                // Paths from EnabledEndpoint
                .body("paths.\"/enabled\".get", notNullValue())
                .body("paths.\"/enabled/hello3\".get", notNullValue())
                .body("paths.\"/enabled/hello4/{param1}\".get", notNullValue())
                .body("paths.\"/enabled/hello5\".post", notNullValue());
    }

}
