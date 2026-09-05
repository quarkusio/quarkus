package io.quarkus.vertx.http;

import static io.restassured.RestAssured.given;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.deployment.spi.GeneratedStaticResourceBuildItem;
import io.restassured.config.RedirectConfig;
import io.restassured.config.RestAssuredConfig;

/**
 * {@code quarkus.http.static-resources.normalize-index-directories} decides what a request for a directory with an
 * index page gets when the path has no trailing slash, both for {@code META-INF/resources} and for generated static
 * resources.
 * See <a href="https://github.com/quarkusio/quarkus/issues/41245">GitHub issue #41245</a>.
 */
public abstract class AbstractStaticResourcesIndexDirectoriesTest {

    static final String CLASSPATH_INDEX = "classpath index";
    static final String GENERATED_INDEX = "generated index";

    static QuarkusExtensionTest test(String mode) {
        return new QuarkusExtensionTest()
                .withApplicationRoot((jar) -> jar
                        .add(new StringAsset(CLASSPATH_INDEX), "META-INF/resources/classpath/index.html")
                        .add(new StringAsset("a file"), "META-INF/resources/classpath/file.txt")
                        .add(new StringAsset("quarkus.http.static-resources.normalize-index-directories=" + mode),
                                "application.properties"))
                .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                    @Override
                    public void accept(BuildChainBuilder buildChainBuilder) {
                        buildChainBuilder.addBuildStep(new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                context.produce(new GeneratedStaticResourceBuildItem("/generated/index.html",
                                        GENERATED_INDEX.getBytes(StandardCharsets.UTF_8)));
                            }
                        }).produces(GeneratedStaticResourceBuildItem.class).produces(GeneratedResourceBuildItem.class)
                                .build();
                    }
                });
    }

    protected static final RestAssuredConfig NO_REDIRECTS = RestAssuredConfig.config()
            .redirect(RedirectConfig.redirectConfig().followRedirects(false));

    @Test
    public void directoryWithTrailingSlashServesTheIndexPage() {
        given().get("/classpath/").then().statusCode(200).body(org.hamcrest.Matchers.equalTo(CLASSPATH_INDEX));
        given().get("/generated/").then().statusCode(200).body(org.hamcrest.Matchers.equalTo(GENERATED_INDEX));
    }

    @Test
    public void fileIsNotAffected() {
        given().config(NO_REDIRECTS).get("/classpath/file.txt").then().statusCode(200);
        given().config(NO_REDIRECTS).get("/classpath/file.txt/").then().statusCode(404);
    }

    @Test
    public void missingDirectoryIsNotFound() {
        given().config(NO_REDIRECTS).get("/missing").then().statusCode(404);
    }
}
