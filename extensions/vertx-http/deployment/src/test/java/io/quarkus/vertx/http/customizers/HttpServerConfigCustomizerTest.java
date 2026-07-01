package io.quarkus.vertx.http.customizers;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.vertx.http.HttpServerConfigCustomizer;
import io.restassured.RestAssured;
import io.vertx.core.http.HttpServerConfig;
import io.vertx.core.net.ServerSSLOptions;
import io.vertx.mutiny.ext.web.Router;

public class HttpServerConfigCustomizerTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
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
    public static class MyCustomizer implements HttpServerConfigCustomizer {

        AtomicInteger count = new AtomicInteger();

        @Override
        public void customizeHttpServer(HttpServerConfig config) {
            count.incrementAndGet();
            config.setPort(9998);
        }

        @Override
        public void customizeHttpsServer(HttpServerConfig config, ServerSSLOptions sslOptions) {
            count.incrementAndGet();
        }

        @Override
        public void customizeDomainSocketServer(HttpServerConfig config) {
            count.incrementAndGet();
        }

        public int count() {
            return count.get();
        }
    }
}
