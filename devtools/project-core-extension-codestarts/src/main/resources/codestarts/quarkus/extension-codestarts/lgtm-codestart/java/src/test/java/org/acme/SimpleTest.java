package org.acme;

import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.jboss.logging.Logger;

import io.restassured.RestAssured;
import io.quarkus.test.junit.QuarkusTest;

import org.junit.jupiter.api.Test;

@QuarkusTest
public class SimpleTest {
    protected final Logger log = Logger.getLogger(getClass());

    @Test
    public void testPoke() {
        log.info("Testing poke ...");
        String response = RestAssured.get("/api/poke?f=100").body().asString();
        log.info("Response: " + response);
    }
}
