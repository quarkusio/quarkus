package io.quarkus.observability.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import io.restassured.RestAssured;

@DisabledOnOs(OS.WINDOWS)
public class SharedTracingTest {

    @Test
    public void testTracing() throws Exception {
        String response = RestAssured.get("/api/poke?f=100").body().asString();
    }

}
