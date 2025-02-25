package io.quarkus.vertx.http.management;

import java.io.File;
import java.util.function.Consumer;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.assertj.core.api.Assertions;
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
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-management-interface-alias-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12,
        Format.PEM }, aliases = @Alias(name = "alias", password = "alias-password", subjectAlternativeNames = "DNS:localhost")))
public class ManagementWithJksWithAliasAndTlsRegistryTest {
    private static final String configuration = """
            quarkus.management.enabled=true
            quarkus.management.root-path=/management
            quarkus.management.tls-configuration-name=mgt
            quarkus.tls.mgt.key-store.jks.path=server-keystore.jks
            quarkus.tls.mgt.key-store.jks.password=secret
            quarkus.tls.mgt.key-store.jks.alias=alias
            quarkus.tls.mgt.key-store.jks.alias-password=alias-password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File("target/certs/ssl-management-interface-alias-test-keystore.jks"),
                            "server-keystore.jks")
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
                                .route("my-route")
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
        public void handle(RoutingContext rc) {
            Assertions.assertThat(rc.request().connection().isSsl()).isTrue();
            Assertions.assertThat(rc.request().isSSL()).isTrue();
            Assertions.assertThat(rc.request().connection().sslSession()).isNotNull();
            rc.response().end("ssl");
        }
    }

    @Test
    public void testTLSWithJks() {
        RestAssured.given()
                .trustStore(new File("target/certs/ssl-management-interface-alias-test-truststore.jks"), "secret")
                .get("https://localhost:9001/management/my-route")
                .then().statusCode(200).body(Matchers.equalTo("ssl"));
    }

    @Singleton
    static class MyObserver {

        void test(@Observes String event) {
            //Do Nothing
        }

    }
}
