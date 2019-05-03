package io.quarkus.runtime.configuration;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RandomTestPortGeneratorTestCase {

    @Test
    public void testRandomPort() {
        final int randomPort1 = RandomTestPortGenerator.generate();
        final int randomPort2 = RandomTestPortGenerator.generate();

        assertTrue(randomPort1 > 1024);
        assertTrue(randomPort2 > 1024);
        assertTrue(randomPort1 != randomPort2);

    }

}
