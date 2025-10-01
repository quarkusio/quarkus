package io.quarkus.it.rest.client.http2;

import static io.restassured.RestAssured.get;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusProdModeTest;

/**
 * This test does not really belong here, but the only way to create a test for
 * <a href="https://github.com/quarkusio/quarkus/issues/50314">this issue</a>
 * is to build a similar REST application in prod mode.
 */
public class RestMethodInAbstractPMT {

    @RegisterExtension
    static QuarkusProdModeTest runner = new QuarkusProdModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(BaseResource.class, Resource1.class, Resource2.class, Client.class))
            .setRun(true);

    @Test
    public void test() {
        get("/res1")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("foo"));
    }

    public static abstract class BaseResource {

        @GET
        @Path("/base")
        @Produces(MediaType.TEXT_PLAIN)
        public String base() {
            return "base";
        }
    }

    @Path("res1")
    public static class Resource1 extends BaseResource {

        @GET
        public String foo() {
            return "foo";
        }
    }

    @Path("res2")
    public static class Resource2 extends BaseResource {

        @RestClient
        Client client;

        @GET
        public String foo() {
            return "foo";
        }

        @GET
        @Path("bar")
        public String bar() {
            return "bar";
        }
    }

}
