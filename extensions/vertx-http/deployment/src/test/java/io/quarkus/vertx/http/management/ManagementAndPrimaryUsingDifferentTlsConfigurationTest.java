package io.quarkus.vertx.http.management;

import java.io.File;
import java.net.URL;
import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.http.deployment.NonApplicationRootPathBuildItem;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.restassured.RestAssured;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Handler;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "ssl-management-interface-test", password = "secret", formats = { Format.JKS, Format.PKCS12,
                Format.PEM }),
        @Certificate(name = "ssl-main-interface-test", password = "secret-main", formats = { Format.JKS, Format.PKCS12,
                Format.PEM }) })
public class ManagementAndPrimaryUsingDifferentTlsConfigurationTest {
    private static final String APP_PROPS = """
            quarkus.management.enabled=true
            quarkus.management.test-port=0
            quarkus.management.tls-configuration-name=management
            quarkus.http.test-ssl-port=0

            quarkus.tls.management.key-store.p12.path=target/certs/ssl-management-interface-test-keystore.p12
            quarkus.tls.management.key-store.p12.password=secret

            quarkus.tls.key-store.p12.path=target/certs/ssl-main-interface-test-keystore.p12
            quarkus.tls.key-store.p12.password=secret-main
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
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
                        NonApplicationRootPathBuildItem buildItem = context.consume(NonApplicationRootPathBuildItem.class);
                        context.produce(buildItem.routeBuilder()
                                .management()
                                .route("management")
                                .handler(new MyHandler())
                                .blockingRoute()
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
                    .end("Hello management");
        }
    }

    @TestHTTPResource(value = "/route", tls = true)
    URL url;

    @TestHTTPResource(value = "/management", management = true, tls = true)
    URL management;

    @ConfigProperty(name = "quarkus.management.test-port")
    int managementPort;

    @ConfigProperty(name = "quarkus.http.test-ssl-port")
    int primaryPort;

    @Test
    public void test() {
        Assertions.assertNotEquals(url.getPort(), management.getPort());
        Assertions.assertEquals(url.getPort(), primaryPort);
        Assertions.assertEquals(management.getPort(), managementPort);

        for (int i = 0; i < 10; i++) {
            RestAssured.given()
                    .trustStore(new File("target/certs/ssl-main-interface-test-truststore.jks"), "secret-main")
                    .get(url.toExternalForm()).then().body(Matchers.is("Hello primary"));
        }

        for (int i = 0; i < 10; i++) {
            RestAssured.given()
                    .trustStore(new File("target/certs/ssl-management-interface-test-truststore.jks"), "secret")
                    .get(management.toExternalForm()).then().body(Matchers.is("Hello management"));
        }

    }

    @Singleton
    static class MyObserver {

        void register(@Observes Router router) {
            router.get("/route").handler(rc -> rc.response().end("Hello primary"));
        }

        void test(@Observes String event) {
            //Do Nothing
        }

    }
}
