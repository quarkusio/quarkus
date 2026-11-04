package io.quarkus.it.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.RestAssured;

@QuarkusIntegrationTest
public class NettyDirectBufferIT extends NettyDirectBufferTest {
    @Test
    void linkerCleanerIsSelected() {
        String result = RestAssured.get("/netty-direct-buffer/cleaner").then().statusCode(200).extract().asString();
        assertThat(result).isEqualTo("io.netty.util.internal.CleanerJava24Linker$CleanableDirectBufferImpl");
    }
}
