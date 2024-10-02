package io.quarkus.vertx.http;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.restassured.RestAssured;

public class GeneratedStaticClasspathResourcesTest {

    @RegisterExtension
    final static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar.add(new StringAsset("quarkus.http.enable-compression=true\n" +
                    "quarkus.http.static-resources.index-page=default.html"), "application.properties"))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {

                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new GeneratedStaticResourceBuildItem(
                                    "/default.html", "Hello from Quarkus".getBytes(StandardCharsets.UTF_8)));
                            context.produce(new GeneratedStaticResourceBuildItem("/hello-from-generated-static-resource.html",
                                    "GeneratedStaticResourceBuildItem says: Hello from me!".getBytes(StandardCharsets.UTF_8)));
                            context.produce(new GeneratedStaticResourceBuildItem("/static-file.html",
                                    "I am from static-html.html".getBytes(StandardCharsets.UTF_8)));
                            context.produce(new GeneratedStaticResourceBuildItem(
                                    "/.nojekyll", "{empty}".getBytes(StandardCharsets.UTF_8)));
                            context.produce(new GeneratedStaticResourceBuildItem("/quarkus-openapi-generator/default.html",
                                    "An extension to read OpenAPI specifications...".getBytes(StandardCharsets.UTF_8)));
                        }
                    }).produces(GeneratedStaticResourceBuildItem.class).produces(GeneratedResourceBuildItem.class).build();
                }
            });

    @Test
    public void shouldGetStaticFileHtmlPageWhenThereIsAGeneratedStaticResource() {
        RestAssured.get("/static-file.html").then()
                .body(Matchers.containsString("I am from static-html.html"))
                .statusCode(Matchers.is(200));

        RestAssured.get("hello-from-generated-static-resource.html").then()
                .body(Matchers.containsString("GeneratedStaticResourceBuildItem says"))
                .statusCode(Matchers.is(200));

        RestAssured.get("/").then()
                .body(Matchers.containsString("Hello from Quarkus"))
                .statusCode(200);
    }

    @Test
    public void shouldCompress() {
        RestAssured.get("/static-file.html").then()
                .header("Content-Encoding", "gzip")
                .statusCode(Matchers.is(200));
    }

    @Test
    public void shouldGetHiddenFiles() {
        RestAssured.get("/.nojekyll")
                .then()
                .body(Matchers.containsString("{empty}"))
                .statusCode(200);
    }

    @Test
    public void shouldGetTheIndexPageCorrectly() {
        RestAssured.get("/quarkus-openapi-generator/")
                .then()
                .body(Matchers.containsString("An extension to read OpenAPI specifications..."))
                .statusCode(200);
    }

    @Test
    public void shouldNotGetWhenPathEndsWithoutSlash() {
        RestAssured.get("/quarkus-openapi-generator")
                .then()
                .statusCode(404); // We are using next()
    }

    @Test
    public void shouldGetAllowHeaderWhenUsingOptions() {
        RestAssured.options("/quarkus-openapi-generator/")
                .then()
                .header("Allow", Matchers.is("HEAD,GET,OPTIONS"))
                .statusCode(204);
    }

    @Test
    public void shouldGetHeadersFromHeadRequest() {
        RestAssured.head("/static-file.html")
                .then()
                .header("Content-Length", Integer::parseInt, Matchers.greaterThan(0))
                .header("Content-Type", Matchers.is("text/html;charset=UTF-8"))
                .statusCode(200);
    }
}
