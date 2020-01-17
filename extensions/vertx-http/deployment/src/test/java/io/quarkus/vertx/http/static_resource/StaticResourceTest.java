package io.quarkus.vertx.http.static_resource;

import static org.hamcrest.core.Is.is;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;

public class StaticResourceTest {

    private static final String JSON_CONTENT = "[1,2,3]";
    private static final String DYNAMIC_RESOURCE_CONTENT = "Dynamic resource";
    private static final String INDEX_HTML_CONTENT = "<html><body>hello from static world</body><html>";
    private static final File TEMPORARY_FOLDER = org.assertj.core.util.Files.newTemporaryFolder();

    static {
        try {
            Files.write(TEMPORARY_FOLDER.toPath().resolve("index.html"), INDEX_HTML_CONTENT.getBytes());
            Files.write(TEMPORARY_FOLDER.toPath().resolve("content.json"), JSON_CONTENT.getBytes());
        } catch (IOException e) {
            Assertions.fail("Failed to create static resources");
        }
    }

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DynamicRoutes.class)
                    .addAsResource(new StringAsset("quarkus.http.static.first-resource.endpoint=/static\n" +
                            "quarkus.http.static.first-resource.path=" + TEMPORARY_FOLDER
                                    .getAbsolutePath()
                                    .replace('\\', '/')),
                            "application.properties"));

    @Test
    public void testServingStaticContent() {
        // requesting the root of static resource endpoint should return default static resource
        RestAssured
                .given()
                .get("/static/")
                .then()
                .statusCode(200)
                .body(is(INDEX_HTML_CONTENT));

        RestAssured
                .given()
                .redirects()
                .follow(false)
                .get("/static")
                .then()
                .statusCode(302).header("Location", "/static/");

        // requesting a static resource that exists should return 200 and the content of the file
        RestAssured
                .given()
                .get("/static/index.html")
                .then()
                .statusCode(200).body(is(INDEX_HTML_CONTENT));

        RestAssured
                .given()
                .get("/static/content.json")
                .then()
                .statusCode(200).body(is(JSON_CONTENT));

        // requesting a dynamic resource with base path equal to the value of static resource should
        // return the result of the dynamic resource
        RestAssured
                .given()
                .get("/static/dynamic-route")
                .then()
                .statusCode(200).body(is(DYNAMIC_RESOURCE_CONTENT));

        // requesting a static resource that does not should return 404
        RestAssured
                .given()
                .get("/static/unknown.file")
                .then()
                .statusCode(404);
    }

    @ApplicationScoped
    public static class DynamicRoutes {
        @Inject
        Router router;

        public void register(@Observes StartupEvent ev) {
            router.get("/static/dynamic-route").handler(rc -> rc.response().end(DYNAMIC_RESOURCE_CONTENT));
        }

    }
}
