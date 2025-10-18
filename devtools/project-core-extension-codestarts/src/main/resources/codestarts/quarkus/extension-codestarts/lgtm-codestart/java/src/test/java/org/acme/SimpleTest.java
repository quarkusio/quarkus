package org.acme;

import java.io.UncheckedIOException;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class SimpleTest {
    protected final Logger log = Logger.getLogger(getClass());

    @Inject
    SimpleService service;

    @Test
    public void testPoke() {
        log.info("Testing poke ...");
        String result = service.poke(100);
        log.info("Result: " + result);
    }
}
