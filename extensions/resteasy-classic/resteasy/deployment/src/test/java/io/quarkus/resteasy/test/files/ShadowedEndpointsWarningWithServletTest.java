package io.quarkus.resteasy.test.files;

import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.test.files.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.util.List;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.test.QuarkusExtensionTest;

class ShadowedEndpointsWarningWithServletTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class, HelloResource.class, PostOnlyResource.class, RootResource.class)
                    .addAsResource(new StringAsset("static secure"), "META-INF/resources/api/secure")
                    .addAsResource(new StringAsset("root index"), "META-INF/resources/index.html")
                    .addAsResource(new StringAsset("static hello"), "META-INF/resources/api/hello")
                    .addAsResource(new StringAsset("index"), "META-INF/resources/api/idx/index.html")
                    .addAsResource(new StringAsset("no welcome file"), "META-INF/resources/api/dir/readme.txt")
                    .addAsResource(new StringAsset("post only"), "META-INF/resources/api/post-only")
                    .addAsResource(new StringAsset("no endpoint"), "META-INF/resources/api/no-endpoint.txt"))
            .setForcedDependencies(List.of(Dependency.of("io.quarkus", "quarkus-undertow", Version.getVersion())))
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD / -> " + RootResource.class.getName() + "#get",
                    "GET, HEAD /api/secure -> " + SecuredResource.class.getName() + "#get",
                    "GET, HEAD /api/hello -> " + HelloResource.class.getName() + "#hello",
                    "GET, HEAD /api/idx/ -> " + HelloResource.class.getName() + "#index"));

    @Test
    void staticResourcesAreServedInsteadOfTheShadowedEndpoints() {
        when().get("/api/secure").then().statusCode(200).body(is("static secure"));
        when().get("/").then().statusCode(200).body(is("root index"));
        when().get("/api/hello").then().statusCode(200).body(is("static hello"));
        when().get("/api/idx/").then().statusCode(200).body(is("index"));
        when().get("/api/dir/").then().statusCode(200).body(is("directory endpoint"));
        when().post("/api/post-only").then().statusCode(200).body(is("post endpoint"));
    }

    @Path("/api/secure")
    public static class SecuredResource {

        @GET
        @RolesAllowed("admin")
        public String get() {
            return "secured endpoint";
        }
    }

    @Path("/api")
    public static class HelloResource {

        @GET
        @Path("hello")
        public String hello() {
            return "hello endpoint";
        }

        @GET
        @Path("idx")
        public String index() {
            return "index endpoint";
        }

        @GET
        @Path("dir")
        public String directory() {
            return "directory endpoint";
        }
    }

    @Path("/api/post-only")
    public static class PostOnlyResource {

        @POST
        public String post() {
            return "post endpoint";
        }
    }

    @Path("/")
    public static class RootResource {

        @GET
        public String get() {
            return "root endpoint";
        }
    }
}
