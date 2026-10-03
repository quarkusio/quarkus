package io.quarkus.resteasy.reactive.server.test.staticresource;

import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.LOGGER;
import static io.quarkus.resteasy.reactive.server.test.staticresource.ShadowedEndpointsWarning.assertShadowedEndpoints;
import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;

import java.nio.charset.StandardCharsets;

import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.resteasy.reactive.server.EndpointDisabled;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;

class ShadowedEndpointsWarningTest {

    @RegisterExtension
    static QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SecuredResource.class, HelloResource.class, FilesResource.class, HeadOnlyResource.class,
                            IndexResource.class, LocatorResource.class, SubResource.class, ThingResource.class,
                            OtherThingResource.class, MultiResource.class, GeneratedResource.class,
                            DisabledResource.class, PostOnlyResource.class, FallbackResource.class, RootResource.class,
                            DocsResource.class)
                    .addAsResource(new StringAsset("static secure"), "META-INF/resources/api/secure")
                    .addAsResource(new StringAsset("root index"), "META-INF/resources/index.html")
                    .addAsResource(new StringAsset("some file"), "META-INF/resources/api/docs/some file.txt")
                    .addAsResource(new StringAsset("braces"), "META-INF/resources/api/docs/{id}.txt")
                    .addAsResource(new StringAsset("braces without endpoint"), "META-INF/resources/api/{id}.txt")
                    .addAsResource(new StringAsset("static hello"), "META-INF/resources/api/hello")
                    .addAsResource(new StringAsset("readme"), "META-INF/resources/api/files/readme.txt")
                    .addAsResource(new StringAsset("head only"), "META-INF/resources/api/head-only")
                    .addAsResource(new StringAsset("index"), "META-INF/resources/api/idx/index.html")
                    .addAsResource(new StringAsset("thing"), "META-INF/resources/api/loc/thing")
                    .addAsResource(new StringAsset("summary"), "META-INF/resources/api/things/1/summary")
                    .addAsResource(new StringAsset("multi"), "META-INF/resources/api/multi")
                    .addAsResource(new StringAsset("disabled"), "META-INF/resources/api/disabled")
                    .addAsResource(new StringAsset("post only"), "META-INF/resources/api/post-only")
                    .addAsResource(new StringAsset("no endpoint"), "META-INF/resources/api/no-endpoint.txt")
                    .addAsResource(new StringAsset("app"), "META-INF/resources/app/index.html")
                    .addAsResource(new StringAsset("app css"), "META-INF/resources/app/css/app.css")
                    .addAsResource(new StringAsset("favicon"), "META-INF/resources/app/favicon.ico")
                    .addAsResource(new StringAsset("app js"), "META-INF/resources/app/js/app.js")
                    .addAsResource(new StringAsset("vendor js"), "META-INF/resources/app/js/vendor.js")
                    .addAsResource(new StringAsset("manifest"), "META-INF/resources/app/manifest.json"))
            .addBuildChainCustomizer((BuildChainBuilder builder) -> builder.addBuildStep(new BuildStep() {
                @Override
                public void execute(BuildContext context) {
                    context.produce(new GeneratedStaticResourceBuildItem("/api/generated",
                            "generated".getBytes(StandardCharsets.UTF_8)));
                    context.produce(new GeneratedStaticResourceBuildItem("/api/gen/index.html",
                            "generated index".getBytes(StandardCharsets.UTF_8)));
                }
            }).produces(GeneratedStaticResourceBuildItem.class).produces(GeneratedResourceBuildItem.class).build())
            .overrideRuntimeConfigKey("disable.endpoint", "true")
            .setLogRecordPredicate(record -> LOGGER.equals(record.getLoggerName()))
            .assertLogRecords(records -> assertShadowedEndpoints(records,
                    "GET, HEAD / -> " + RootResource.class.getName() + "#get",
                    "GET, HEAD /api/docs/some file.txt, /api/docs/{id}.txt -> " + DocsResource.class.getName() + "#doc",
                    "GET, HEAD /api/secure -> " + SecuredResource.class.getName() + "#get",
                    "GET, HEAD /api/hello -> " + HelloResource.class.getName() + "#hello",
                    "GET, HEAD /api/files/readme.txt -> " + FilesResource.class.getName() + "#file",
                    "HEAD /api/head-only -> " + HeadOnlyResource.class.getName() + "#head",
                    "GET, HEAD /api/idx/ -> " + IndexResource.class.getName() + "#get",
                    "GET, HEAD /api/loc/thing -> " + LocatorResource.class.getName()
                            + "#locate (sub-resource locator)",
                    "GET, HEAD /api/things/1/summary -> " + OtherThingResource.class.getName() + "#summary",
                    "GET, HEAD /api/multi -> resource methods with the path template /api/multi",
                    "GET, HEAD /api/generated -> " + GeneratedResource.class.getName() + "#generated",
                    "GET, HEAD /api/gen/ -> " + GeneratedResource.class.getName() + "#index",
                    // the fallback is meant for the paths without a static resource, but is shadowed by all of them
                    "GET, HEAD /app/, /app/css/app.css, /app/favicon.ico, /app/index.html, /app/js/app.js and 2 more -> "
                            + FallbackResource.class.getName() + "#fallback"));

    @Test
    void staticResourcesAreServedInsteadOfTheShadowedEndpoints() {
        when().get("/api/secure").then().statusCode(200).body(is("static secure"));
        when().get("/").then().statusCode(200).body(is("root index"));
        when().get("/api/docs/some file.txt").then().statusCode(200).body(is("some file"));
        when().get("/api/docs/other.txt").then().statusCode(200).body(is("doc endpoint other.txt"));
        given().urlEncodingEnabled(false).get("/api/docs/%7Bid%7D.txt").then().statusCode(200).body(is("braces"));
        given().urlEncodingEnabled(false).get("/api/%7Bid%7D.txt").then().statusCode(200)
                .body(is("braces without endpoint"));
        when().get("/api/idx/").then().statusCode(200).body(is("index"));
        when().get("/api/things/1/summary").then().statusCode(200).body(is("summary"));
        when().get("/api/things/1/detail").then().statusCode(200).body(is("detail of 1"));
        when().get("/api/generated").then().statusCode(200).body(is("generated"));
        when().get("/api/gen/").then().statusCode(200).body(is("generated index"));
        when().get("/api/disabled").then().statusCode(200).body(is("disabled"));
        when().get("/app/js/app.js").then().statusCode(200).body(is("app js"));
        when().get("/app/settings").then().statusCode(200).body(is("fallback endpoint"));
    }

    @Path("/api/secure")
    public static class SecuredResource {

        @GET
        @RolesAllowed("admin")
        public String get() {
            return "secured endpoint";
        }
    }

    @Path("/api/hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello endpoint";
        }
    }

    @Path("/api/files")
    public static class FilesResource {

        @GET
        @Path("{name: .+\\.txt}")
        public String file(@PathParam("name") String name) {
            return "file endpoint " + name;
        }
    }

    @Path("/api/head-only")
    public static class HeadOnlyResource {

        @HEAD
        public void head() {
        }
    }

    @Path("/api/idx")
    public static class IndexResource {

        @GET
        public String get() {
            return "index endpoint";
        }
    }

    @Path("/api/loc")
    public static class LocatorResource {

        @Path("{name}")
        public SubResource locate() {
            return new SubResource();
        }
    }

    public static class SubResource {

        @GET
        public String get() {
            return "sub-resource endpoint";
        }
    }

    @Path("/api/things/{id}")
    public static class ThingResource {

        @GET
        @Path("detail")
        public String detail(@PathParam("id") String id) {
            return "detail of " + id;
        }
    }

    @Path("/api/things/{name}")
    public static class OtherThingResource {

        @GET
        @Path("summary")
        public String summary(@PathParam("name") String name) {
            return "summary of " + name;
        }
    }

    @Path("/api/multi")
    public static class MultiResource {

        @GET
        @Produces(MediaType.TEXT_PLAIN)
        public String text() {
            return "text";
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public String json() {
            return "{}";
        }
    }

    @Path("/api")
    public static class GeneratedResource {

        @GET
        @Path("generated")
        public String generated() {
            return "generated endpoint";
        }

        @GET
        @Path("gen")
        public String index() {
            return "index endpoint";
        }
    }

    @Path("/api/disabled")
    @EndpointDisabled(name = "disable.endpoint", stringValue = "true")
    public static class DisabledResource {

        @GET
        public String get() {
            return "disabled endpoint";
        }
    }

    @Path("/api/post-only")
    public static class PostOnlyResource {

        @POST
        public String post() {
            return "post endpoint";
        }
    }

    @Path("/app")
    public static class FallbackResource {

        @GET
        @Path("{path: .*}")
        public String fallback() {
            return "fallback endpoint";
        }
    }

    @Path("/")
    public static class RootResource {

        @GET
        public String get() {
            return "root endpoint";
        }
    }

    @Path("/api/docs")
    public static class DocsResource {

        @GET
        @Path("{name: [^/\\s]+}")
        public String doc(@PathParam("name") String name) {
            return "doc endpoint " + name;
        }
    }
}
