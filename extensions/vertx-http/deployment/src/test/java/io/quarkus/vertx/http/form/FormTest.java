package io.quarkus.vertx.http.form;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class FormTest {

    private static final String APP_PROPS = """
            quarkus.http.limits.max-form-fields=10
            quarkus.http.limits.max-form-buffered-bytes=100
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource(new StringAsset(APP_PROPS), "application.properties")
                    .addClasses(BeanRegisteringRouteUsingObserves.class));

    @Inject
    Vertx vertx;

    @Test
    public void testTooManyFields() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpClientResponse> reference = new AtomicReference<>();
        HttpClient client = vertx.createHttpClient();
        client.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI("http://localhost:8081/form"))
                .onComplete(ar -> {
                    var req = ar.result();
                    req.setChunked(true);
                    req.putHeader("content-type", "application/x-www-form-urlencoded");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 20; i++) {
                        if (i > 0) {
                            sb.append('&');
                        }
                        sb.append("a").append(i).append("=").append("b");
                    }
                    req.write(sb.toString());
                    vertx.setTimer(10, id -> {
                        req.end();
                    });

                    req.response().onComplete(rc -> {
                        reference.set(rc.result());
                        latch.countDown();
                    });
                });
        latch.await(10, TimeUnit.SECONDS);
        Assertions.assertEquals(400, reference.get().statusCode());
    }

    @Test
    public void testOkForm() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpClientResponse> reference = new AtomicReference<>();
        HttpClient client = vertx.createHttpClient();
        client.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI("http://localhost:8081/form"))
                .onComplete(ar -> {
                    var req = ar.result();
                    req.setChunked(true);
                    req.putHeader("content-type", "application/x-www-form-urlencoded");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 10; i++) {
                        if (i > 0) {
                            sb.append('&');
                        }
                        sb.append("a").append(i).append("=").append("b");
                    }
                    req.write(sb.toString());
                    vertx.setTimer(10, id -> {
                        req.end();
                    });

                    req.response().onComplete(rc -> {
                        reference.set(rc.result());
                        latch.countDown();
                    });
                });
        latch.await(10, TimeUnit.SECONDS);
        Assertions.assertEquals(200, reference.get().statusCode());
    }

    @Test
    public void testTooManyBytes() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpClientResponse> reference = new AtomicReference<>();
        HttpClient client = vertx.createHttpClient();
        client.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI("http://localhost:8081/form"))
                .onComplete(ar -> {
                    var req = ar.result();
                    req.setChunked(true);
                    req.putHeader("content-type", "application/x-www-form-urlencoded");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 200; i++) {
                        sb.append("a");
                    }
                    req.write(sb.toString());
                    vertx.setTimer(10, id -> {
                        req.end("=b");
                    });

                    req.response().onComplete(rc -> {
                        reference.set(rc.result());
                        latch.countDown();
                    });
                });
        latch.await(10, TimeUnit.SECONDS);
        Assertions.assertEquals(400, reference.get().statusCode());
    }

    @Test
    public void testBytesOk() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<HttpClientResponse> reference = new AtomicReference<>();
        HttpClient client = vertx.createHttpClient();
        client.request(new RequestOptions().setMethod(HttpMethod.POST).setAbsoluteURI("http://localhost:8081/form"))
                .onComplete(ar -> {
                    var req = ar.result();
                    req.setChunked(true);
                    req.putHeader("content-type", "application/x-www-form-urlencoded");
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < 100; i++) {
                        sb.append("a");
                    }
                    req.write(sb.toString());
                    vertx.setTimer(10, id -> {
                        req.end("=b");
                    });

                    req.response().onComplete(rc -> {
                        reference.set(rc.result());
                        latch.countDown();
                    });
                });
        latch.await(10, TimeUnit.SECONDS);
        Assertions.assertEquals(200, reference.get().statusCode());
    }

    @ApplicationScoped
    static class BeanRegisteringRouteUsingObserves {

        public void register(@Observes Router router) {

            router
                    .post().order(Integer.MIN_VALUE).handler(rc -> {
                        rc.request().setExpectMultipart(true);
                        rc.next();
                    });
            router.post().handler(BodyHandler.create());
            router.post("/form")
                    .handler(rc -> {
                        rc.response().end("OK");
                    });
        }

    }
}
