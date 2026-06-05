package io.quarkus.vertx.http.accesslog;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

import org.awaitility.Awaitility;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.bootstrap.util.IoUtils;
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
 * Reproducer for https://github.com/quarkusio/quarkus/issues/40420.
 * <p>
 * When the access log is enabled together with a custom non-application root path that is disjoint
 * from the HTTP root path, and no framework router was ever created (no route is classified as a
 * framework route), startup used to fail with a {@link NullPointerException} while attaching the
 * access-log handler to the (null) framework router.
 * <p>
 * The application must start and the access log of application routes must keep working.
 */
public class AccessLogWithNonApplicationRootPathTestCase {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    try {
                        Path logDirectory = Files.createTempDirectory("quarkus-tests");
                        Properties p = new Properties();
                        p.setProperty("quarkus.http.root-path", "/auth");
                        p.setProperty("quarkus.http.non-application-root-path", "/q");
                        p.setProperty("quarkus.http.access-log.enabled", "true");
                        p.setProperty("quarkus.http.access-log.log-to-file", "true");
                        p.setProperty("quarkus.http.access-log.base-file-name", "server");
                        p.setProperty("quarkus.http.access-log.log-directory", logDirectory.toAbsolutePath().toString());
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        p.store(out, null);
                        return ShrinkWrap.create(JavaArchive.class)
                                .add(new ByteArrayAsset(out.toByteArray()), "application.properties")
                                .addClasses(MyObserver.class);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            })
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

    // Injected at runtime so the value survives the QuarkusUnitTest classloader boundary
    // (a static field set by the archive producer would not be visible to the test method).
    @ConfigProperty(name = "quarkus.http.access-log.log-directory")
    Path logDirectory;

    @BeforeEach
    public void before() throws IOException {
        Files.createDirectories(logDirectory);
    }

    @AfterEach
    public void after() throws IOException {
        IoUtils.recursiveDelete(logDirectory);
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
    public void testAccessLogWithDisjointNonApplicationRootPath() {
        // The application boots (no NPE) and the application route is reachable under the HTTP root path.
        // RestAssured is configured with the configured HTTP root path (/auth) as its base path.
        RestAssured.given().get("/hello")
                .then().statusCode(200).body(Matchers.equalTo("/auth/hello"));

        // The request to the application route is written to the access log.
        Awaitility.given().pollInterval(100, TimeUnit.MILLISECONDS)
                .atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    Path logFile = logDirectory.resolve("server.log");
                    assertThat(Files.exists(logFile)).isTrue();
                    assertThat(Files.readString(logFile)).contains("/auth/hello");
                });
    }

    @Singleton
    static class MyObserver {
        void test(@Observes String event) {
            // Do nothing
        }
    }
}
