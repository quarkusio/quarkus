package io.quarkus.runtime.configuration;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RandomTestPortGeneratorTestCase {

    @Test
    public void testRandomPort() {
        final int randomPort = RandomTestPortGenerator.generate();
        assertTrue(randomPort >= RandomTestPortGenerator.FIRST_PRIVATE_PORT);
        assertTrue(randomPort <= RandomTestPortGenerator.LAST_PRIVATE_PORT);
    }

}
