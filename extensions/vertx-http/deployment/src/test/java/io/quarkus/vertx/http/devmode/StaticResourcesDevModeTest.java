package io.quarkus.vertx.http.devmode;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.vertx.http.AbstractStaticResourcesTest;
import io.restassured.RestAssured;

public class StaticResourcesDevModeTest extends AbstractStaticResourcesTest {

    @RegisterExtension
    final static QuarkusDevModeTest test = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .add(new StringAsset("quarkus.http.enable-compression=true\n"),
                            "application.properties")
                    .addAsResource("static-file.html", "META-INF/resources/dir/file.txt")
                    .addAsResource("static-file.html", "META-INF/resources/l'équipe.pdf")
                    .addAsResource("static-file.html", "META-INF/resources/static file.txt")
                    .addAsResource("static-file.html", "META-INF/resources/static-file.html")
                    .addAsResource("static-file.html", "META-INF/resources/.hidden-file.html")
                    .addAsResource("static-file.html", "META-INF/resources/index.html")
                    .addAsResource("static-file.html", "META-INF/resources/image.svg"));

    @Test
    void shouldChangeContentOnModification() {
        RestAssured.when().get("/static-file.html")
                .then()
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
        RestAssured.when().get("/")
                .then()
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
        test.modifyResourceFile("META-INF/resources/static-file.html", s -> s.replace("webpage", "Andy"));
        RestAssured.when().get("/static-file.html")
                .then()
                .body(Matchers.containsString("This is the title of the Andy!"))
                .statusCode(200);
        RestAssured.when().get("/")
                .then()
                .body(Matchers.containsString("This is the title of the webpage!"))
                .statusCode(200);
    }
}
