package io.quarkus.vertx.http;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.codec.compression.Brotli;
import io.quarkus.test.QuarkusExtensionTest;
import io.restassured.RestAssured;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class BrotliDecompressionWithoutBrotli4jTest {

    // "Hello" encoded with Brotli
    private static final byte[] BROTLI_PAYLOAD = { 0x0b, 0x02, (byte) 0x80, 'H', 'e', 'l', 'l', 'o', 0x03 };

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar.addClasses(EchoRoute.class))
            .overrideConfigKey("quarkus.http.enable-decompression", "true");

    @Test
    public void bodyIsNotDecoded() {
        assertThat(Brotli.isAvailable()).isFalse();

        byte[] body = RestAssured.given()
                .header("content-encoding", "br")
                .body(BROTLI_PAYLOAD)
                .post("/echo").then().statusCode(200)
                .extract().asByteArray();

        assertThat(body).isEqualTo(BROTLI_PAYLOAD);
    }

    @ApplicationScoped
    static class EchoRoute {

        public void register(@Observes Router router) {
            router.post("/echo").handler(BodyHandler.create());
            router.post("/echo").handler(rc -> rc.response().end(rc.body().buffer()));
        }
    }
}
