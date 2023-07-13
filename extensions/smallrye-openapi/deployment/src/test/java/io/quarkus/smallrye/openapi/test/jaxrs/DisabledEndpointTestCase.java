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
                            EnabledEndpoint.class)
                    .add(new StringAsset("quarkus.http.root-path=/root\n"), "application.properties"));

    @EndpointDisabled(name = "xxx", disableIfMissing = true, stringValue = "xxx")
    @Path("/disabled")
    public static class DisabledEndpoint {

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

    @EndpointDisabled(name = "xxx", disableIfMissing = false, stringValue = "xxx")
    @Path("/enabled")
    public static class EnabledEndpoint {

        @Path("/hello3")
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
                .body("paths.\"/root/disabled/hello\".get", nullValue())
                .body("paths.\"/root/disabled/hello2/{param1}\".get", nullValue())
                .body("paths.\"/root/enabled/hello3\".get", notNullValue())
                .body("paths.\"/root/enabled/hello4/{param1}\".get", notNullValue())
                .body("paths.\"/root/enabled/hello5\".post", notNullValue())
                .body("paths.\"/root/enabled/hello5\".put", nullValue());
    }

}
