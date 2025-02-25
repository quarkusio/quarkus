package io.quarkus.vertx.http;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.runtime.util.ClassPathUtils;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.restassured.RestAssured;

public class GeneratedStaticFileResourcesTest {

    @RegisterExtension
    final static QuarkusUnitTest test = new QuarkusUnitTest().withApplicationRoot(
            (jar) -> jar
                    .addAsResource("static-file.html", "static-file.html")
                    .add(new StringAsset(
                            "quarkus.http.enable-compression=true\nquarkus.http.static-resources.index-page=default.html"),
                            "application.properties"))
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {

                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            final Path file = resolveResource("/static-file.html");
                            context.produce(new GeneratedStaticResourceBuildItem("/static file.txt", file));
                            context.produce(new GeneratedStaticResourceBuildItem("/l'équipe.pdf", file));
                            context.produce(new GeneratedStaticResourceBuildItem(
                                    "/default.html", file));
                            context.produce(new GeneratedStaticResourceBuildItem("/hello-from-generated-static-resource.html",
                                    file));

                            context.produce(new GeneratedStaticResourceBuildItem("/static-file.html",
                                    file));
                            context.produce(new GeneratedStaticResourceBuildItem(
                                    "/.nojekyll", resolveResource("/.nojekyll")));
                            context.produce(new GeneratedStaticResourceBuildItem("/quarkus-openapi-generator/default.html",
                                    file));
                        }
                    }).produces(GeneratedStaticResourceBuildItem.class).produces(GeneratedResourceBuildItem.class).build();
                }
            });

    private static Path resolveResource(String name) {
        return ClassPathUtils.toLocalPath(GeneratedStaticFileResourcesTest.class.getResource(name));
    }

    @Test
    public void shouldGetStaticFileHtmlPageWhenThereIsAGeneratedStaticResource() throws IOException {
        final String result = Files.readString(resolveResource("/static-file.html"));
        RestAssured.get("/static-file.html").then()
                .body(Matchers.is(result))
                .statusCode(Matchers.is(200));

        RestAssured.get("hello-from-generated-static-resource.html").then()
                .body(Matchers.is(result))
                .statusCode(Matchers.is(200));

        RestAssured.get("/").then()
                .body(Matchers.is(result))
                .statusCode(200);
    }

    @Test
    public void shouldCompress() throws IOException {
        final String result = Files.readString(resolveResource("/static-file.html"));
        RestAssured.get("/static-file.html").then()
                .header("Content-Encoding", "gzip")
                .body(Matchers.is(result))
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
    public void shouldGetFileWithSpecialCharacters() throws IOException {
        RestAssured.get("/l'équipe.pdf")
                .then()
                .header("Content-Type", Matchers.is("application/pdf"))
                .statusCode(200);
    }

    @Test
    public void shouldGetFileWithSpaces() throws IOException {
        RestAssured.get("/static file.txt")
                .then()
                .header("Content-Type", Matchers.is("text/plain;charset=UTF-8"))
                .statusCode(200);
    }

    @Test
    public void shouldGetFileWithSpacesAndQuery() throws IOException {
        RestAssured.get("/static file.txt?foo=bar")
                .then()
                .header("Content-Type", Matchers.is("text/plain;charset=UTF-8"))
                .statusCode(200);
    }

    @Test
    public void shouldWorkWithEncodedSlash() throws IOException {
        RestAssured.given().urlEncodingEnabled(false).get("/quarkus-openapi-generator%2Fdefault.html")
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldWorkWithDoubleDot() throws IOException {
        RestAssured.given().urlEncodingEnabled(false).get("/hello/../static-file.html")
                .then()
                .statusCode(200);
    }

    @Test
    public void shouldGetTheIndexPageCorrectly() throws IOException {
        final String result = Files.readString(resolveResource("/static-file.html"));
        RestAssured.get("/quarkus-openapi-generator/")
                .then()
                .body(Matchers.is(result))
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
    public void shouldGetHeadersFromHeadRequest() throws IOException {
        final byte[] result = Files.readAllBytes(resolveResource("/static-file.html"));
        RestAssured.head("/static-file.html")
                .then()
                .header("Content-Length", Integer::parseInt, Matchers.is(result.length))
                .header("Content-Type", Matchers.is("text/html;charset=UTF-8"))
                .statusCode(200);
    }
}
