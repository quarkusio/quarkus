package io.quarkus.vertx.http;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.spi.AdditionalStaticResourceBuildItem;
import io.restassured.RestAssured;

public class AdditionalStaticResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        String path = "/routes.js";

                        context.produce(new GeneratedResourceBuildItem("META-INF/resources" + path,
                                "//hello".getBytes(StandardCharsets.UTF_8)));
                        context.produce(new AdditionalStaticResourceBuildItem(path, false));
                    }
                }).produces(GeneratedResourceBuildItem.class)
                        .produces(AdditionalStaticResourceBuildItem.class)
                        .build();
            }
        };
    }

    @Test
    public void testNonApplicationEndpointDirect() {
        RestAssured.given()
                .when().get("/routes.js")
                .then()
                .statusCode(200)
                .body(Matchers.is("//hello"));
    }
}