package io.quarkus.vertx.http.accesslog;

import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.deployment.HttpRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.restassured.RestAssured;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Reproducer for the management-interface variant of
 * https://github.com/quarkusio/quarkus/issues/40420.
 * <p>
 * With the management interface enabled, the non-application (framework) routes move to the
 * management interface, so no framework router exists on the main server. Enabling the access log
 * together with a custom HTTP root path used to crash startup with a {@link NullPointerException}
 * while attaching the access-log handler to the (null) framework router.
 */
public class AccessLogWithManagementInterfaceTestCase {

    private static final String APP_PROPS = """
            quarkus.management.enabled=true
            quarkus.http.root-path=/app
            quarkus.http.non-application-root-path=/q
            quarkus.http.access-log.enabled=true
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(MyObserver.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        // An application route (classified APPLICATION_ROUTE), so no framework router is created.
                        HttpRootPathBuildItem buildItem = context.consume(HttpRootPathBuildItem.class);
                        context.produce(buildItem.routeBuilder()
                                .route("hello")
                                .handler(new MyHandler())
                                .blockingRoute()
                                .build());
                    }
                }).produces(RouteBuildItem.class)
                        .consumes(HttpRootPathBuildItem.class)
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
    public void testApplicationStartsWithManagementInterfaceAndAccessLog() {
        // The application boots (no NPE) and the application route is reachable under the HTTP root path.
        RestAssured.given().get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("/app/hello"));
    }

    @Singleton
    static class MyObserver {
        void test(@Observes String event) {
            // Do nothing
        }
    }
}
