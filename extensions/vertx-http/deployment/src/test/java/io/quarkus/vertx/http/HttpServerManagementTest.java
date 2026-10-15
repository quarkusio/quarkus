package io.quarkus.vertx.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public class HttpServerManagementTest {
    private static final String APP_PROPS = "quarkus.management.enabled=true\n";

    @RegisterExtension
    static final QuarkusExtensionTest CONFIG = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties"))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                context.produce(buildItem.routeBuilder()
                        .management()
                        .route("test")
                        .handler(new NoOpHandler())
                        .blockingRoute()
                        .build());
            }
        }).produces(RouteBuildItem.class)
                .consumes(NonApplicationRootPathBuildItem.class)
                .build();
    }

    public static class NoOpHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext rc) {
            rc.response().end();
        }
    }

    @Inject
    HttpServer webServer;

    @Test
    void ports() {
        assertTrue(webServer.getPort() > 0);
        assertEquals(-1, webServer.getSecurePort());
        assertTrue(webServer.getManagementPort() > 0);
    }

    @Test
    void uris() {
        assertTrue(webServer.getLocalBaseUri().toString().contains(String.valueOf(webServer.getPort())));
        assertTrue(webServer.getManagementBaseUri().isPresent());
        assertTrue(webServer.getManagementBaseUri().get().toString()
                .contains(String.valueOf(webServer.getManagementPort())));
    }
}
