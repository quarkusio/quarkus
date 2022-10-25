package io.quarkus.vertx.http.customizers;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.vertx.http.HttpServerOptionsCustomizer;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.mutiny.ext.web.Router;

public class HttpServerOptionsCustomizerTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class, MyCustomizer.class));

    @Inject
    MyCustomizer customizer;

    @Test
    void test() {
        Assertions.assertThat(customizer.count()).isEqualTo(2);
        Assertions.assertThat(RestAssured.get("http://localhost:9998").body().asString()).isEqualTo("hello");
    }

    @ApplicationScoped
    public static class MyBean {

        public void init(@Observes Router router) {
            router.get().handler(rc -> rc.endAndForget("hello"));
        }

    }

    @ApplicationScoped
    public static class MyCustomizer implements HttpServerOptionsCustomizer {

        AtomicInteger count = new AtomicInteger();

        @Override
        public void customizeHttpServer(HttpServerOptions options) {
            count.incrementAndGet();
            options.setPort(9998);
        }

        @Override
        public void customizeHttpsServer(HttpServerOptions options) {
            count.incrementAndGet();
        }

        @Override
        public void customizeDomainSocketServer(HttpServerOptions options) {
            count.incrementAndGet();
        }

        public int count() {
            return count.get();
        }
    }
}
