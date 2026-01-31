package io.quarkus.swaggerui.deployment;

import static org.hamcrest.Matchers.containsString;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

public class CustomUrlFromBuildItemTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.swagger-ui.urls.\"user-custom-1\"", "/user/custom/1")
            .overrideConfigKey("quarkus.swagger-ui.urls.\"user-custom-2\"", "/user/custom/2")
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder
                            .addBuildStep(new BuildStep() {
                                @Override
                                public void execute(BuildContext context) {
                                    context.produce(new SwaggerUiUrlBuildItem("extension-custom-1", "/extension/custom/1"));
                                    context.produce(new SwaggerUiUrlBuildItem("extension-custom-2", "/extension/custom/2"));
                                    context.produce(new SwaggerUiUrlBuildItem("user-custom-1", "/extension/custom/3"));
                                }
                            })
                            .produces(SwaggerUiUrlBuildItem.class).build();
                }
            });

    @Test
    void extensionProvidedSwaggerUrls() {
        RestAssured.when().get("/q/swagger-ui").then().log().all().and().statusCode(200)
                .body(
                        containsString("extension-custom-1"),
                        containsString("extension/custom/1"),
                        containsString("extension-custom-2"),
                        containsString("extension/custom/2"),
                        // The extension provided URL for user-custom-1 should not override the one specified in the app config
                        containsString("user-custom-1"),
                        containsString("/user/custom/1"),
                        containsString("user-custom-2"),
                        containsString("/user/custom/2"));
    }
}
