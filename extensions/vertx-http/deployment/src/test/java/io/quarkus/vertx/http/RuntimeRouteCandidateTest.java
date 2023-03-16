package io.quarkus.vertx.http;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.restassured.RestAssured;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class RuntimeRouteCandidateTest {

    private static final String APP_PROPS = "" +
            "quarkus.http.root-path=/api\n" +
            "route[1]=/build-time-route";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"))
            .addBuildChainCustomizer(buildCustomizer())
            .overrideRuntimeConfigKey("route[1]", "/runtime-route");

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(RouteBuildItem.builder()
                                .route(new PathSupplier())
                                .handler(new MyHandler())
                                .build());
                    }
                }).produces(RouteBuildItem.class)
                        .consumes(NonApplicationRootPathBuildItem.class)
                        .build();
            }
        };
    }

    public static class MyHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext routingContext) {
            routingContext.response()
                    .setStatusCode(200)
                    .end(routingContext.request().path());
        }
    }

    @Test
    public void testRouteCreatedFromRuntimeProperty() {
        RestAssured.given().get("/runtime-route").then().statusCode(200).body(Matchers.equalTo("/api/runtime-route"));
        RestAssured.given().get("/build-time-route").then().statusCode(404);
    }

    public static class PathSupplier implements Supplier<String> {

        @Override
        public String get() {
            return ConfigProvider.getConfig().getConfigValue("route[1]").getRawValue();
        }
    }

}
