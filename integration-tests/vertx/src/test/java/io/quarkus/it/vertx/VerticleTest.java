package io.quarkus.it.vertx;

import static io.restassured.RestAssured.get;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class VerticleTest {

    @Test
    public void testBareVerticle() {
        String s = get("/vertx-test/verticles/bare").asString();
        assertThat(s).isEqualTo("OK-bare");
    }

    @Test
    public void testBareWithClassNameVerticle() {
        String s = get("/vertx-test/verticles/bare-classname").asString();
        assertThat(s).isEqualTo("OK-bare-classname");
    }
}
