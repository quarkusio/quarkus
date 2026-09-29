package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.LogRecord;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.options.HttpServerOptionsUtils;
import io.restassured.RestAssured;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ProxyProtocolUnusedListenerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(RemoteAddressRoute.class))
            .overrideConfigKey("quarkus.http.proxy.use-proxy-protocol", "true")
            .overrideConfigKey("quarkus.http.proxy.proxy-protocol-listeners", "https")
            .overrideConfigKey("quarkus.management.enabled", "true")
            .overrideConfigKey("quarkus.management.proxy.use-proxy-protocol", "true")
            .overrideConfigKey("quarkus.management.proxy.proxy-protocol-listeners", "https")
            .addBuildChainCustomizer(buildCustomizer())
            .setLogRecordPredicate(r -> r.getLoggerName().equals(HttpServerOptionsUtils.class.getName()))
            .assertLogRecords(ProxyProtocolUnusedListenerTest::assertWarnings);

    @TestHTTPResource(value = "/management-remote", management = true)
    URL managementUrl;

    @Test
    public void plainHttpIsServedWithoutProxyHeader() {
        RestAssured.get("/remote").then().statusCode(200).body(Matchers.is("127.0.0.1"));
    }

    @Test
    public void managementIsServedWithoutProxyHeader() {
        RestAssured.get(managementUrl).then().statusCode(200).body(Matchers.is("127.0.0.1"));
    }

    private static void assertWarnings(List<LogRecord> records) {
        assertThat(records).extracting(r -> String.format(r.getMessage(), r.getParameters()))
                .anySatisfy(m -> assertThat(m).startsWith("'quarkus.http.proxy.use-proxy-protocol' is enabled")
                        .contains("[HTTPS]", "[HTTP]"))
                .anySatisfy(m -> assertThat(m).startsWith("'quarkus.management.proxy.use-proxy-protocol' is enabled")
                        .contains("[HTTPS]", "[HTTP]"));
    }

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return builder -> builder.addBuildStep(new BuildStep() {
            @Override
            public void execute(BuildContext context) {
                NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                context.produce(buildItem.routeBuilder()
                        .management()
                        .route("management-remote")
                        .handler(new RemoteAddressHandler())
                        .build());
            }
        }).produces(RouteBuildItem.class)
                .consumes(NonApplicationRootPathBuildItem.class)
                .build();
    }

    public static class RemoteAddressHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext rc) {
            rc.response().end(rc.request().remoteAddress().host());
        }
    }

    @ApplicationScoped
    public static class RemoteAddressRoute {

        void register(@Observes Router router) {
            router.get("/remote").handler(new RemoteAddressHandler());
        }
    }
}
