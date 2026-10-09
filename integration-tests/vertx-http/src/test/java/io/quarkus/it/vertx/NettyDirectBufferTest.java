package io.quarkus.it.vertx;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
public class NettyDirectBufferTest {
    @Test
    void emptyBuffer() {
        String result = RestAssured.get("/netty-direct-buffer/empty").then().statusCode(200).extract().asString();
        assertThat(result).isEqualTo("true:0");
    }

    @ParameterizedTest
    @ValueSource(ints = { 16, 128 })
    void resizePreservesContents(int capacity) {
        byte[] result = RestAssured.get("/netty-direct-buffer/resize/" + capacity).then().statusCode(200).extract()
                .asByteArray();
        byte[] expected = new byte[Math.min(capacity, 64)];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) i;
        }
        assertThat(result).containsExactly(expected);
    }

    @Test
    void releaseOnAnotherThread() {
        String result = RestAssured.get("/netty-direct-buffer/cross-thread").then().statusCode(200).extract().asString();
        assertThat(result).isEqualTo("42");
    }
}
