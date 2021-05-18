package io.quarkus.vertx.http;

import java.net.URL;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.awaitility.Awaitility;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
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
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.predicate.ResponsePredicate;

public class NonApplicationEscapeTest {
    private static final String APP_PROPS = "" +
            "quarkus.http.root-path=/api\n" +
            "quarkus.http.non-application-root-path=${quarkus.http.root-path}\n";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
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
                                .route("/non-app-absolute")
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

    @TestHTTPResource("/")
    URL uri;

    @Inject
    Vertx vertx;

    public static class MyHandler implements Handler<RoutingContext> {
        @Override
        public void handle(RoutingContext routingContext) {
            routingContext.response()
                    .setStatusCode(200)
                    .end(routingContext.request().query() != null
                            ? routingContext.request().path() + "?" + routingContext.request().query()
                            : routingContext.request().path());
        }
    }

    @Test
    public void testNonApplicationEndpointEscaped() {
        AtomicReference<String> result = new AtomicReference<>();

        WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/non-app-absolute")
                .expect(ResponsePredicate.SC_OK)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        result.set(response.bodyAsString());
                    } else {
                        result.set(ar.cause().getMessage());
                    }
                });

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> result.get() != null);

        Assertions.assertEquals("/non-app-absolute", result.get());
    }

    @Test
    public void testNonApplicationEndpointWithQueryEscaped() {
        AtomicReference<String> result = new AtomicReference<>();

        WebClient.create(vertx)
                .get(uri.getPort(), uri.getHost(), "/non-app-absolute?query=true")
                .expect(ResponsePredicate.SC_OK)
                .send(ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<Buffer> response = ar.result();
                        result.set(response.bodyAsString());
                    } else {
                        result.set(ar.cause().getMessage());
                    }
                });

        Awaitility.await().atMost(Duration.ofMinutes(2)).until(() -> result.get() != null);

        Assertions.assertEquals("/non-app-absolute?query=true", result.get());
    }

    @Singleton
    static class MyObserver {

        void test(@Observes String event) {
            //Do Nothing
        }

    }
}
