package io.quarkus.resteasy.reactive.server.servlet.test;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.resteasy.reactive.server.test.simple.SimpleQuarkusRestTestCase;

public class ServletSimpleRestTestCase extends SimpleQuarkusRestTestCase {

    @Test
    @Override
    @Disabled("servlet always execute on blocking thread pool")
    public void testCompletableFutureBlocking() {

    }
}
