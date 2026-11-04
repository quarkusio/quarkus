package io.quarkus.resteasy.test.files;

import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.assertNoShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

class NoShadowedEndpointsWarningTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HelloResource.class, UsersResource.class, PostOnlyResource.class)
                    .addAsResource(new StringAsset("index"), "META-INF/resources/index.html")
                    .addAsResource(new StringAsset("css"), "META-INF/resources/css/site.css")
                    .addAsResource(new StringAsset("readme"), "META-INF/resources/api/readme.txt")
                    .addAsResource(new StringAsset("post only"), "META-INF/resources/api/post-only"))
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertNoShadowedEndpoints(records));

    @Test
    void staticResourcesAndEndpointsAreServed() {
        when().get("/").then().statusCode(200).body(is("index"));
        when().get("/api/hello").then().statusCode(200).body(is("hello endpoint"));
        when().get("/api/users/1").then().statusCode(200).body(is("user 1"));
        when().post("/api/post-only").then().statusCode(200).body(is("post endpoint"));
    }

    @Path("/api/hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello endpoint";
        }
    }

    @Path("/api/users")
    public static class UsersResource {

        @GET
        @Path("{id}")
        public String user(@PathParam("id") String id) {
            return "user " + id;
        }
    }

    @Path("/api/post-only")
    public static class PostOnlyResource {

        @POST
        public String post() {
            return "post endpoint";
        }
    }
}
