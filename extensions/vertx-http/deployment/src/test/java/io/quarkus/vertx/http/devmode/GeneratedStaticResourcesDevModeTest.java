package io.quarkus.vertx.http.devmode;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class GeneratedStaticResourcesDevModeTest {

    @RegisterExtension
    final static QuarkusDevModeTest devMode = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset("quarkus.http.enable-compression=true\n"),
                            "application.properties")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/bytes/static-file.html")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/bytes/.hidden-file.html")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/bytes/index.html")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/bytes/image.svg")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/static-file.html")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/.hidden-file.html")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/index.html")
                    .addAsResource("static-file.html", "META-INF/generated-resources-test/image.svg"));

    @Test
    void shouldUpdateResourceIndexHtmlOnUserChange() {
        RestAssured.given()
                .get("/bytes/")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the webpage!"));

        devMode.modifyResourceFile("META-INF/generated-resources-test/bytes/index.html", s -> s.replace("webpage", "Matheus"));

        RestAssured.given()
                .get("/bytes/")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the Matheus!"));
    }

    @Test
    void shouldUpdateHiddenResourceOnUserChange() {
        RestAssured.given()
                .get("/bytes/.hidden-file.html")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the webpage!"));
        devMode.modifyResourceFile("META-INF/generated-resources-test/bytes/.hidden-file.html",
                s -> s.replace("webpage", "Matheus"));
        RestAssured.given()
                .get("/bytes/.hidden-file.html")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the Matheus!"));
    }

    @Test
    void shouldUpdateFileResourceIndexHtmlOnUserChange() {
        RestAssured.given()
                .get("/")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the webpage!"));

        devMode.modifyResourceFile("META-INF/generated-resources-test/index.html", s -> s.replace("webpage", "Matheus"));

        RestAssured.given()
                .get("/")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the Matheus!"));
    }

    @Test
    void shouldUpdateHiddenFileResourceOnUserChange() {
        RestAssured.given()
                .get("/.hidden-file.html")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the webpage!"));
        devMode.modifyResourceFile("META-INF/generated-resources-test/.hidden-file.html",
                s -> s.replace("webpage", "Matheus"));
        RestAssured.given()
                .get("/.hidden-file.html")
                .then()
                .statusCode(200)
                .body(Matchers.containsString("This is the title of the Matheus!"));
    }
}
